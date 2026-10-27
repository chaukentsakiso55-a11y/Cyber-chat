package com.cyberpulse.cyberchat;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.graphics.Color;
import android.webkit.JavascriptInterface;

public class MainActivity extends Activity {
 WebView web;
 ValueCallback<Uri[]> picker;
 static final int PICK=111;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(Color.rgb(3,8,20));
  getWindow().setNavigationBarColor(Color.rgb(3,8,20));
  web=new WebView(this);
  web.setBackgroundColor(Color.rgb(3,8,20));
  web.setOverScrollMode(View.OVER_SCROLL_NEVER);
  WebSettings settings=web.getSettings();
  settings.setJavaScriptEnabled(true);
  settings.setDomStorageEnabled(true);
  settings.setAllowFileAccess(true);
  settings.setAllowContentAccess(true);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri uri=request.getUrl();
    if("file".equals(uri.getScheme()))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}
    return true;
   }
  });
  web.addJavascriptInterface(new SettingsBridge(),"CyberSettings");
  web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(picker!=null)picker.onReceiveValue(null);
    picker=cb;
    try{startActivityForResult(params.createIntent(),PICK);return true;}
    catch(Exception e){picker=null;return false;}
   }
  });
  setContentView(web);
  web.loadUrl("file:///android_asset/index.html");
 }
 public class SettingsBridge {
  @JavascriptInterface public void saveApiKey(String provider,String key,String endpoint){
   if(!provider.matches("[a-z]{2,24}")||key==null||key.isEmpty())return;
   try{
    java.security.KeyStore store=java.security.KeyStore.getInstance("AndroidKeyStore");store.load(null);
    javax.crypto.SecretKey secret;
    if(!store.containsAlias("cyberchat_byok")){
     javax.crypto.KeyGenerator generator=javax.crypto.KeyGenerator.getInstance("AES","AndroidKeyStore");
     generator.init(new android.security.keystore.KeyGenParameterSpec.Builder("cyberchat_byok",android.security.keystore.KeyProperties.PURPOSE_ENCRYPT|android.security.keystore.KeyProperties.PURPOSE_DECRYPT).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build());
     secret=generator.generateKey();
    }else secret=(javax.crypto.SecretKey)store.getKey("cyberchat_byok",null);
    javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,secret);
    byte[] bytes=(key+"\\n"+(endpoint==null?"":endpoint)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    byte[] iv=cipher.getIV(),encrypted=cipher.doFinal(bytes);
    android.content.SharedPreferences.Editor editor=getSharedPreferences("cyberchat_secure_providers",MODE_PRIVATE).edit();
    editor.putString(provider,android.util.Base64.encodeToString(iv,android.util.Base64.NO_WRAP)+":"+android.util.Base64.encodeToString(encrypted,android.util.Base64.NO_WRAP));editor.apply();
   }catch(Exception ignored){}
  }
  @JavascriptInterface public void deleteApiKey(String provider){
   if(provider!=null&&provider.matches("[a-z]{2,24}"))getSharedPreferences("cyberchat_secure_providers",MODE_PRIVATE).edit().remove(provider).apply();
  }

  private String[] readProvider(String provider)throws Exception{
   String value=getSharedPreferences("cyberchat_secure_providers",MODE_PRIVATE).getString(provider,null);
   if(value==null)throw new Exception("No API key saved for "+provider+". Open Settings > AI API Keys.");
   String[] parts=value.split(":",2);
   java.security.KeyStore store=java.security.KeyStore.getInstance("AndroidKeyStore");store.load(null);
   javax.crypto.SecretKey secret=(javax.crypto.SecretKey)store.getKey("cyberchat_byok",null);
   javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(javax.crypto.Cipher.DECRYPT_MODE,secret,new javax.crypto.spec.GCMParameterSpec(128,android.util.Base64.decode(parts[0],android.util.Base64.DEFAULT)));
   String decoded=new String(cipher.doFinal(android.util.Base64.decode(parts[1],android.util.Base64.DEFAULT)),java.nio.charset.StandardCharsets.UTF_8);
   int cut=decoded.indexOf("\\n");
   if(cut<0)cut=decoded.indexOf('\n');
   return cut<0?new String[]{decoded,""}:new String[]{decoded.substring(0,cut),decoded.substring(cut+(decoded.charAt(cut)=='\\'?2:1))};
  }
  @JavascriptInterface public boolean hasApiKey(String provider){
   if(provider==null)return false;
   return getSharedPreferences("cyberchat_secure_providers",MODE_PRIVATE).contains(provider);
  }
  @JavascriptInterface public void askAI(String provider,String prompt,String requestId){
   if(provider==null||!provider.matches("[a-z]{2,24}")||prompt==null||prompt.trim().isEmpty())return;
   new Thread(()->{
    boolean ok=false;String answer="";
    try{
     String[] stored=readProvider(provider);
     String key=stored[0],custom=stored[1];String url,model;org.json.JSONObject payload=new org.json.JSONObject();
     if("gemini".equals(provider)){
      url="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
      org.json.JSONArray parts=new org.json.JSONArray().put(new org.json.JSONObject().put("text",prompt));
      payload.put("contents",new org.json.JSONArray().put(new org.json.JSONObject().put("parts",parts)));
     }else{
      if("openai".equals(provider)){url="https://api.openai.com/v1/chat/completions";model="gpt-4.1-mini";}
      else if("openrouter".equals(provider)){url="https://openrouter.ai/api/v1/chat/completions";model="openai/gpt-4.1-mini";}
      else if("xai".equals(provider)){url="https://api.x.ai/v1/chat/completions";model="grok-3-mini";}
      else if("fireworks".equals(provider)){url="https://api.fireworks.ai/inference/v1/chat/completions";model="accounts/fireworks/models/llama-v3p1-8b-instruct";}
      else if("custom".equals(provider)){url=custom;model="default";}
      else throw new Exception("This provider is not connected yet.");
      if(!url.startsWith("https://"))throw new Exception("Provider endpoint must use HTTPS.");
      payload.put("model",model);
      payload.put("messages",new org.json.JSONArray().put(new org.json.JSONObject().put("role","user").put("content",prompt)));
     }
     java.net.HttpURLConnection connection=(java.net.HttpURLConnection)new java.net.URL(url).openConnection();
     connection.setRequestMethod("POST");connection.setConnectTimeout(15000);connection.setReadTimeout(45000);connection.setDoOutput(true);
     connection.setRequestProperty("Content-Type","application/json");
     if("gemini".equals(provider))connection.setRequestProperty("x-goog-api-key",key);
     else connection.setRequestProperty("Authorization","Bearer "+key);
     try(java.io.OutputStream out=connection.getOutputStream()){out.write(payload.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
     int status=connection.getResponseCode();
     java.io.InputStream stream=status>=200&&status<300?connection.getInputStream():connection.getErrorStream();
     java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] chunk=new byte[4096];int count;
     while((count=stream.read(chunk))!=-1){buffer.write(chunk,0,count);if(buffer.size()>1000000)throw new Exception("AI response exceeded size limit");}
     org.json.JSONObject result=new org.json.JSONObject(buffer.toString("UTF-8"));
     if(status<200||status>=300)throw new Exception(result.optJSONObject("error")!=null?result.getJSONObject("error").optString("message","Provider HTTP "+status):"Provider HTTP "+status);
     if("gemini".equals(provider))answer=result.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");
     else answer=result.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
     ok=true;
    }catch(Exception e){answer=e.getMessage()==null?"AI request failed":e.getMessage();}
    final boolean success=ok;final String response=answer;
    runOnUiThread(()->web.evaluateJavascript("window.cyberAiResult("+org.json.JSONObject.quote(requestId)+","+(success?"true":"false")+","+org.json.JSONObject.quote(response)+")",null));
   }).start();
  }
  @JavascriptInterface public void setIcon(String choice){
   runOnUiThread(()->{
    try{
     PackageManager pm=getPackageManager();
     String pkg=getPackageName();
     String active="dark".equals(choice)?"CyberChatDark":"CyberChatBlue";
     String other="dark".equals(choice)?"CyberChatBlue":"CyberChatDark";
     pm.setComponentEnabledSetting(new ComponentName(pkg,pkg+"."+active),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
     pm.setComponentEnabledSetting(new ComponentName(pkg,pkg+"."+other),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
     web.evaluateJavascript("document.getElementById('notice').textContent='Launcher icon updated';",null);
    }catch(Exception ignored){web.evaluateJavascript("document.getElementById('notice').textContent='Could not change launcher icon';",null);}
   });
  }
 }
 @Override protected void onActivityResult(int r,int c,Intent data){
  super.onActivityResult(r,c,data);
  if(r==PICK&&picker!=null){picker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(c,data));picker=null;}
 }
 @Override public void onBackPressed(){
  web.evaluateJavascript("(function(){var b=document.querySelector('[data-a=close]');var s=document.querySelector('[data-a=sback]');if(s){s.click();return true}if(b&&document.querySelector('.split.open')){b.click();return true}var c=document.querySelector('[data-a=tab][data-v=chats]');if(c){c.click();return true}return false})()",v->{if("false".equals(v))super.onBackPressed();});
 }
}
