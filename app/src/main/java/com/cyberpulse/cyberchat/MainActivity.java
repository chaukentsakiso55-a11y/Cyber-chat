package com.cyberpulse.cyberchat;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.graphics.Color;
public class MainActivity extends Activity {
 WebView web; ValueCallback<Uri[]> picker; static final int PICK=111;
 @Override public void onCreate(Bundle b){super.onCreate(b);
 getWindow().setStatusBarColor(Color.rgb(3,8,20));getWindow().setNavigationBarColor(Color.rgb(3,8,20));
 web=new WebView(this);web.setBackgroundColor(Color.rgb(3,8,20));web.setOverScrollMode(View.OVER_SCROLL_NEVER);
 WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);
 web.setWebViewClient(new WebViewClient());
 web.setWebChromeClient(new WebChromeClient(){
 @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams p){
 if(picker!=null)picker.onReceiveValue(null);picker=cb;
 try{startActivityForResult(p.createIntent(),PICK);return true;}catch(Exception e){picker=null;return false;}
 }
 });setContentView(web);web.loadUrl("file:///android_asset/online.html");
 }
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&picker!=null){picker.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(c,d));picker=null;}}
 @Override public void onBackPressed(){web.evaluateJavascript("(function(){if(document.querySelector('.split.open .back')){document.querySelector('.split.open .back').click();return true;}if(document.querySelector('[data-a=sback]')){document.querySelector('[data-a=sback]').click();return true;}return false})()",v->{if("false".equals(v))super.onBackPressed();});}
}