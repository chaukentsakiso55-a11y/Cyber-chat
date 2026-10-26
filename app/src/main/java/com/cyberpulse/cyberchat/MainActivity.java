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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import android.webkit.JavascriptInterface;
public class MainActivity extends Activity {
 WebView web; ValueCallback<Uri[]> picker; static final int PICK=111;
 @Override public void onCreate(Bundle b){super.onCreate(b);
 getWindow().setStatusBarColor(Color.rgb(3,8,20));getWindow().setNavigationBarColor(Color.rgb(3,8,20));
 web=new WebView(this);web.setBackgroundColor(Color.rgb(3,8,20));web.setOverScrollMode(View.OVER_SCROLL_NEVER);
 WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);
 web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !"file".equals(request.getUrl().getScheme());}});
 web.addJavascriptInterface(new AuthBridge(),"CyberAuth");
 web.setWebChromeClient(new WebChromeClient(){
 @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams p){
 if(picker!=null)picker.onReceiveValue(null);picker=cb;
 try{startActivityForResult(p.createIntent(),PICK);return true;}catch(Exception e){picker=null;return false;}
 }
 });setContentView(web);web.loadUrl("file:///android_asset/online.html");
 }
 private void authEvent(String type,String message){try{JSONObject o=new JSONObject();o.put("type",type);o.put("message",message);FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u!=null){o.put("uid",u.getUid());o.put("email",u.getEmail());o.put("verified",u.isEmailVerified());}String json=o.toString();runOnUiThread(()->web.evaluateJavascript("window.onCyberAuth&&window.onCyberAuth("+json+")",null));}catch(Exception ignored){}}
 public class AuthBridge {
 @JavascriptInterface public void setIcon(String choice){runOnUiThread(()->{try{PackageManager pm=getPackageManager();String pkg=getPackageName();String active="dark".equals(choice)?"CyberChatDark":"CyberChatBlue";String other="dark".equals(choice)?"CyberChatBlue":"CyberChatDark";pm.setComponentEnabledSetting(new ComponentName(pkg,pkg+"."+active),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);pm.setComponentEnabledSetting(new ComponentName(pkg,pkg+"."+other),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);authEvent("icon","Launcher icon updated");}catch(Exception e){authEvent("error","Could not change launcher icon");}});}

 @JavascriptInterface public void login(String email,String password){runOnUiThread(()->FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password).addOnCompleteListener(t->{if(t.isSuccessful())authEvent("login","Signed in with Firebase");else authEvent("error",t.getException()!=null?t.getException().getMessage():"Sign-in failed");}));}
 @JavascriptInterface public void register(String email,String password){runOnUiThread(()->FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password).addOnCompleteListener(t->{if(t.isSuccessful()){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u!=null)u.sendEmailVerification().addOnCompleteListener(v->authEvent("registered",v.isSuccessful()?"Account created. Check your email to verify it.":"Account created, but verification email could not be sent."));}else authEvent("error",t.getException()!=null?t.getException().getMessage():"Registration failed");}));}
 @JavascriptInterface public void resendVerification(){runOnUiThread(()->{FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null){authEvent("error","Sign in first to resend verification.");return;}u.reload().addOnCompleteListener(t->{if(!t.isSuccessful()){authEvent("error",t.getException()!=null?t.getException().getMessage():"Unable to check account");return;}if(u.isEmailVerified()){authEvent("status","Email is already verified");return;}u.sendEmailVerification().addOnCompleteListener(v->authEvent(v.isSuccessful()?"sent":"error",v.isSuccessful()?"Verification email requested. Check inbox and spam.":v.getException()!=null?v.getException().getMessage():"Email could not be sent"));});});}
 @JavascriptInterface public void reset(String email){runOnUiThread(()->FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener(t->authEvent(t.isSuccessful()?"reset":"error",t.isSuccessful()?"Password reset email sent":t.getException()!=null?t.getException().getMessage():"Reset failed")));}
 @JavascriptInterface public void logout(){runOnUiThread(()->{FirebaseAuth.getInstance().signOut();authEvent("logout","Signed out");});}
 @JavascriptInterface public void status(){runOnUiThread(()->{FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null)authEvent("logout","Sign in to CyberChat");else u.reload().addOnCompleteListener(t->authEvent("login",t.isSuccessful()?"Firebase account ready":"Could not refresh account status"));});}
 }
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&picker!=null){picker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(c,d));picker=null;}}
 @Override public void onBackPressed(){web.evaluateJavascript("(function(){if(document.querySelector('.split.open .back')){document.querySelector('.split.open .back').click();return true;}if(document.querySelector('[data-a=sback]')){document.querySelector('[data-a=sback]').click();return true;}return false})()",v->{if("false".equals(v))super.onBackPressed();});}
}