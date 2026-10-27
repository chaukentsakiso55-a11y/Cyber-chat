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
  web.loadUrl("file:///android_asset/online.html");
 }
 public class SettingsBridge {
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
  web.evaluateJavascript("(function(){var c=document.getElementById('chat');if(c&&!c.hidden){c.hidden=true;return true}var a=document.getElementById('app');if(a&&!a.hidden){var t=document.getElementById('tabChats');t.click();return true}return false})()",v->{if("false".equals(v))super.onBackPressed();});
 }
}