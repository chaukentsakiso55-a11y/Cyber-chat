package com.cyberpulse.cyberchat;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
 final int BG=0xff030a18, PANEL=0xff09162c, TILE=0xff0b1d39, BLUE=0xff008cff, CYAN=0xff00caff, TEXT=0xffedf5ff, MUTED=0xff90a7d2;
 LinearLayout root, body, nav; String page="Chats", aiName="CyberAI", profile="DarthWolf", theme="Dark"; int accent=BLUE;
 Map<String,ArrayList<String>> chats=new HashMap<>();
 SharedPreferences prefs;
 int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
 GradientDrawable bg(int color,int stroke,int rad){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(rad));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
 TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER_VERTICAL);t.setPadding(dp(4),dp(4),dp(4),dp(4));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 void add(LinearLayout p,View v){p.addView(v,new LinearLayout.LayoutParams(-1,-2));}
 void gap(LinearLayout p,int n){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,dp(n)));}
 TextView button(String s,Runnable action){TextView t=text(s,14,TEXT,true);t.setPadding(dp(14),dp(14),dp(12),dp(14));t.setBackground(bg(TILE,0xff1a3764,14));t.setOnClickListener(v->action.run());return t;}
 void section(String name){gap(body,14);TextView h=text(name,19,CYAN,true);add(body,h);gap(body,8);}
 void tile(String title,String subtitle,Runnable action){LinearLayout box=column();box.setPadding(dp(13),dp(11),dp(13),dp(11));box.setBackground(bg(PANEL,0xff18375f,13));add(box,text(title,15,TEXT,true));if(subtitle!=null)add(box,text(subtitle,12,MUTED,false));box.setOnClickListener(v->action.run());add(body,box);gap(body,8);}
 void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);prefs=getSharedPreferences("cyberchat",0);aiName=prefs.getString("ai","CyberAI");profile=prefs.getString("profile","DarthWolf");show("Chats");}
 void show(String p){page=p;root=column();root.setBackgroundColor(BG);setContentView(root);
 LinearLayout top=row();top.setPadding(dp(16),dp(14),dp(12),dp(12));TextView logo=text("◈",30,CYAN,true);top.addView(logo);TextView title=text("CyberChat",25,TEXT,true);top.addView(title);TextView sub=text("  CYBERPULSE",10,CYAN,true);top.addView(sub);add(root,top);
 View line=new View(this);line.setBackgroundColor(0xff17467b);root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
 LinearLayout area=row();area.setGravity(Gravity.TOP);root.addView(area,new LinearLayout.LayoutParams(-1,0,1));
 nav=column();nav.setPadding(dp(5),dp(12),dp(5),0);nav.setBackgroundColor(0xff050f22);area.addView(nav,new LinearLayout.LayoutParams(dp(83),-1));
 String[][] links={{"Chats","▤"},{"Groups","♧"},{"Meetings","▣"},{"Files","▱"},{"AI","✦"},{"Settings","⚙"}};
 for(String[] a:links){TextView item=text(a[1]+"\n"+a[0],11,p.equals(a[0])?CYAN:MUTED,p.equals(a[0]));item.setGravity(Gravity.CENTER);item.setPadding(dp(3),dp(15),dp(3),dp(15));if(p.equals(a[0]))item.setBackground(bg(0xff07356d,BLUE,12));item.setOnClickListener(v->show(a[0]));add(nav,item);gap(nav,7);}
 ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);area.addView(scroll,new LinearLayout.LayoutParams(0,-1,1));body=column();body.setPadding(dp(13),dp(13),dp(13),dp(24));scroll.addView(body);
 switch(p){case "Chats":home();break;case "Groups":groups();break;case "Meetings":meetings();break;case "Files":files();break;case "AI":ai();break;case "Settings":settings();break;case "Appearance":appearance();break;case "Personalization":personalization();break;case "AI Settings":aiSettings();break;default:conversation(p);}
 }
 void home(){section("Messages");add(body,text("Private CyberPulse workspace",12,MUTED,false));gap(body,14);
 tile("⌕  Search conversations","No conversations yet",()->toast("No conversations to search"));
 tile("＋  New local chat","Create a conversation on this device",()->edit("Chat name","New chat",name->{if(!name.trim().isEmpty()){chats.put(name,new ArrayList<>());show(name);}}));
 for(String name:chats.keySet())tile("◉  "+name,"Local conversation",()->show(name));
 tile("✦  "+aiName,"Your offline AI assistant",()->show("AI"));
 gap(body,15);add(body,text("No fake messages. Real member messaging will require a backend.",12,MUTED,false));}
 void groups(){section("Groups");tile("＋  Create local group","Plan a team conversation",()->edit("Group name","Project Team",name->{if(!name.trim().isEmpty()){chats.put(name,new ArrayList<>());show(name);}}));add(body,text("CyberPulse member groups will appear here once connected.",13,MUTED,false));}
 void meetings(){section("Calls & Meetings");tile("▣  New meeting","Meeting controls are planned for a later connected build",()->toast("Live meetings are not available in this offline version"));tile("♧  Participants","No active meeting",()->{});}
 void files(){section("Files");tile("▱  Browse device","Open Android file picker",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,9);});add(body,text("Files can be selected locally. Sending files to other members is not available yet.",12,MUTED,false));}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==9&&result==RESULT_OK&&data!=null)toast("Selected: "+data.getData().getLastPathSegment());}
 void conversation(String name){section(name);add(body,text("Local chat · Not sent to other members",12,MUTED,false));gap(body,12);
 for(String m:chats.getOrDefault(name,new ArrayList<>()))tile(m,null,()->{});
 EditText input=new EditText(this);input.setSingleLine(false);input.setHint("Write a message");input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setBackground(bg(TILE,BLUE,12));input.setPadding(dp(12),dp(10),dp(12),dp(10));add(body,input);gap(body,10);add(body,button("Send locally  ➤",()->{String m=input.getText().toString().trim();if(!m.isEmpty()){chats.computeIfAbsent(name,k->new ArrayList<>()).add("You: "+m);show(name);}}));}
 void ai(){section(aiName);add(body,text("Offline assistant · CyberPulse",12,MUTED,false));gap(body,10);
 tile("✦  Hi "+profile,"I'm "+aiName+". Ask me something simple.",()->{});
 ArrayList<String> history=loadHistory();for(String line:history)tile(line,null,()->{});
 EditText input=new EditText(this);input.setHint("Message "+aiName+"...");input.setTextColor(TEXT);input.setHintTextColor(MUTED);input.setPadding(dp(12),dp(12),dp(12),dp(12));input.setBackground(bg(TILE,BLUE,12));add(body,input);gap(body,10);
 add(body,button("Send to "+aiName+"  ➤",()->{String q=input.getText().toString().trim();if(q.isEmpty())return;history.add("You: "+q);history.add(aiName+": "+reply(q));prefs.edit().putString("history",String.join("\n",history)).apply();show("AI");}));
 gap(body,12);add(body,button("Customize assistant",()->show("AI Settings")));}
 ArrayList<String> loadHistory(){String raw=prefs.getString("history","");return new ArrayList<>(raw.isEmpty()?Collections.emptyList():Arrays.asList(raw.split("\n")));}
 String reply(String q){String s=q.toLowerCase(Locale.ROOT).trim();if(s.matches(".*\\b(hi|hello|hey|yo)\\b.*"))return "Hello "+profile+"! How can I help?";if(s.contains("who created")||s.contains("your creator")||s.contains("who made"))return "I was created by DarthWolf for CyberPulse.";if(s.contains("who are you")||s.contains("your name"))return "I'm "+aiName+", the CyberChat assistant built for CyberPulse members.";if(s.contains("what is cyberchat"))return "CyberChat is CyberPulse's private collaboration and communication app.";if(s.contains("thank"))return "You're welcome!";if(s.contains("help"))return "I can answer basic questions offline. More AI features are planned.";return "I'm still a basic offline assistant. I can't answer that yet, but my future version will support more advanced AI features.";}
 void settings(){section("Settings");tile("◉  "+profile,"CyberPulse Member · Profile",()->show("Personalization"));tile("🎨  Customization","Theme, accent colors, interface",()->show("Appearance"));tile("✦  AI Assistant","Name, personality and capabilities",()->show("AI Settings"));tile("♧  Chats","Local conversation settings",()->show("Chats"));tile("▣  Calls & Meetings","Meeting preferences",()->show("Meetings"));tile("▱  Data & Storage","Device files",()->show("Files"));tile("ⓘ  About","CyberChat 0.1 · Android prototype",()->toast("Built for CyberPulse by DarthWolf"));}
 void appearance(){section("Appearance");add(body,text("Choose a theme",13,MUTED,false));gap(body,8);
 tile("◉  Dark","Active neon blue theme",()->{theme="Dark";toast("Dark theme selected");});
 tile("◯  Light","Planned for future version",()->toast("Light theme is not available yet"));
 section("Accent color");int[] colors={0xff008cff,0xff963aff,0xff00d6ef,0xff16c784,0xffe44767};String[] names={"Blue","Purple","Cyan","Green","Rose"};
 for(int i=0;i<colors.length;i++){final int c=colors[i];final String n=names[i];tile("●  "+n,"Choose "+n+" accent",()->{accent=c;toast(n+" selected. Full theme switching is planned.");});}}
 void personalization(){section("Profile");tile("◉  Display name",profile,()->edit("Display name",profile,v->{profile=v;prefs.edit().putString("profile",v).apply();show("Personalization");}));tile("▣  Cover photo","Custom profile images planned",()->toast("Coming later"));tile("◉  Profile picture","Custom avatars planned",()->toast("Coming later"));tile("✦  Status","CyberPulse Member",()->{});}
 void aiSettings(){section("AI Assistant");tile("✎  Assistant name",aiName,()->edit("Rename AI",aiName,v->{aiName=v;prefs.edit().putString("ai",v).apply();show("AI Settings");}));tile("✦  Personality","Balanced and helpful",()->toast("More personalities are planned"));tile("▣  AI model","Offline basic responses",()->toast("Gemini API integration is not connected yet"));tile("◉  Live voice","Planned feature",()->toast("Live voice is not implemented"));tile("▱  Image generation","Planned feature",()->toast("Image generation needs an AI service"));tile("♧  Chat with "+aiName,"Open assistant",()->show("AI"));}
 interface Saved{void accept(String value);}
 void edit(String title,String value,Saved done){EditText field=new EditText(this);field.setSingleLine(true);field.setText(value);new AlertDialog.Builder(this).setTitle(title).setView(field).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->done.accept(field.getText().toString().trim())).show();}
 @Override public void onBackPressed(){show("Chats");}
}
