const $=id=>document.getElementById(id);const notice=msg=>$('notice').textContent=msg;let access='',user=null,selected=null,refreshTimer=null,refreshToken='',expiresAt=0;
const base=window.CYBERCHAT_CONFIG?.url;const key=window.CYBERCHAT_CONFIG?.publishableKey;
async function api(path,method='GET',payload){if(!base||!key)throw Error('Supabase configuration missing.');const headers={apikey:key,'Content-Type':'application/json'};if(access)headers.Authorization='Bearer '+access;const r=await fetch(base+path,{method,headers,body:payload?JSON.stringify(payload):undefined});const raw=await r.text();let result;try{result=JSON.parse(raw)}catch{result=raw}if(!r.ok)throw Error(result?.message||result?.msg||result?.error_description||'Request failed');return result}
function storeSession(data){access=data.access_token;refreshToken=data.refresh_token||refreshToken;expiresAt=Date.now()+(data.expires_in||3600)*1000;user=data.user||user;localStorage.setItem('cyberchat_session',JSON.stringify({access,refreshToken,expiresAt,user}))}
async function renewSession(){if(!refreshToken)return false;try{const r=await api('/auth/v1/token?grant_type=refresh_token','POST',{refresh_token:refreshToken});storeSession(r);return true}catch{localStorage.removeItem('cyberchat_session');return false}}
async function showApp(){ $('auth').hidden=true;showTab('chats');let role;try{role=await api('/rest/v1/rpc/cyberchat_my_role','POST',{})}catch(e){notice(e.message);return}if(role==='pending'){$('codeGate').hidden=false;$('app').hidden=true;notice('Enter your CyberPulse access code.');return}$('codeGate').hidden=true;$('app').hidden=false;$('adminPanel').hidden=role!=='admin';notice('Connected as '+(user.email||user.id));if(role==='admin')loadAccounts();directory();rooms();if(refreshTimer)clearInterval(refreshTimer);refreshTimer=setInterval(()=>{if(selected)messages()},4000)}
async function auth(signup){try{const email=$('email').value.trim(),password=$('password').value;if(!email||password.length<6)throw Error('Enter an email and a password of at least 6 characters.');const body={email,password};if(signup){body.data={display_name:$('nickname').value.trim()||'CyberChat Member'}}const result=await api(signup?'/auth/v1/signup':'/auth/v1/token?grant_type=password','POST',body);if(!result.access_token){notice('Check your email to confirm your registration, then sign in.');return}storeSession(result);showApp()}catch(e){notice(e.message)}}
$('login').onclick=()=>auth(false);$('signup').onclick=()=>auth(true);
$('logout').onclick=async()=>{try{await api('/auth/v1/logout','POST')}catch{}access='';refreshToken='';user=null;selected=null;localStorage.removeItem('cyberchat_session');if(refreshTimer)clearInterval(refreshTimer);$('auth').hidden=false;$('app').hidden=true;notice('Signed out')};
async function directory(){try{const members=await api('/rest/v1/profiles?approved=eq.true&select=id,display_name&order=display_name.asc');$('directory').replaceChildren();for(const m of members){const p=document.createElement('p');p.textContent=m.display_name+' ';if(m.id!==user.id){const b=document.createElement('button');b.textContent='Message';b.onclick=async()=>{try{const id=await api('/rest/v1/rpc/start_direct_chat','POST',{other_user:m.id});selected={id,title:m.display_name};$('chat').hidden=false;$('chatname').textContent=m.display_name;await rooms();await messages()}catch(e){notice(e.message)}};p.appendChild(b)}$('directory').appendChild(p)}if(!members.length)$('directory').textContent='No approved members yet.'}catch(e){notice(e.message)}}
async function rooms(){try{const list=await api('/rest/v1/conversations?select=id,title&order=created_at.desc');$('rooms').replaceChildren();for(const c of list){const b=document.createElement('button');b.textContent=c.title||'Conversation';b.onclick=()=>{selected=c;$('chat').hidden=false;$('chatname').textContent=c.title||'Chat';messages()};$('rooms').appendChild(b)}if(!list.length)$('rooms').textContent='No chats yet.';const home=$('chatListHome');home.replaceChildren();for(const item of $('rooms').children){const copy=item.cloneNode(true);if(item.tagName==='BUTTON')copy.onclick=item.onclick;home.appendChild(copy)}if(!list.length)home.textContent='No conversations yet. Tap New chat to begin.'}catch(e){$('chatConnection').textContent=e.message;notice(e.message)}}
$('create').onclick=async()=>{try{await api('/rest/v1/conversations','POST',{title:$('title').value.trim()||'New chat',is_group:true,created_by:user.id});$('title').value='';await rooms()}catch(e){notice(e.message)}};
$('addMember').onclick=async()=>{if(!selected)return notice('Choose a group first.');const id=$('memberId').value.trim();if(!id)return notice('Choose a member from the directory or enter a member ID.');try{await api('/rest/v1/rpc/add_group_member','POST',{group_id:selected.id,new_member:id});$('memberId').value='';notice('Member added')}catch(e){notice(e.message)}};
async function messages(){if(!selected)return;try{const rows=await api('/rest/v1/messages?conversation_id=eq.'+encodeURIComponent(selected.id)+'&select=sender_id,body,created_at&order=created_at.asc&limit=200');$('messages').replaceChildren();for(const m of rows){const p=document.createElement('p');p.textContent=(m.sender_id===user.id?'You: ':'Member: ')+m.body;$('messages').appendChild(p)}}catch(e){notice(e.message)}}
$('send').onclick=async()=>{if(!selected)return notice('Choose a conversation first.');const body=$('message').value.trim();if(!body)return;try{await api('/rest/v1/messages','POST',{conversation_id:selected.id,sender_id:user.id,body});$('message').value='';await messages()}catch(e){notice(e.message)}};
if(!base||!key)notice('Online service configuration is required before sign-in can work.');
(async()=>{try{const saved=JSON.parse(localStorage.getItem('cyberchat_session')||'null');if(saved){access=saved.access;refreshToken=saved.refreshToken;expiresAt=saved.expiresAt;user=saved.user;if(expiresAt-Date.now()<120000){if(!await renewSession())return}if(user)showApp()}}catch(e){localStorage.removeItem('cyberchat_session')}})();
setInterval(async()=>{if(refreshToken&&expiresAt-Date.now()<180000)await renewSession()},60000);

$('redeem').onclick=async()=>{try{const role=await api('/rest/v1/rpc/cyberchat_redeem_code','POST',{p_code:$('accessCode').value.trim()});$('accessCode').value='';notice(role==='admin'?'Administrator access enabled':'Member access enabled');await showApp()}catch(e){notice(e.message)}};
$('changeCode').onclick=async()=>{try{await api('/rest/v1/rpc/cyberchat_change_code','POST',{p_kind:$('codeKind').value,p_new_code:$('newCode').value});$('newCode').value='';notice('Access code updated')}catch(e){notice(e.message)}};
$('gateLogout').onclick=()=>{$('logout').click();$('codeGate').hidden=true};

$('addPerson').onclick=async()=>{const email=$('personEmail').value.trim();if(!email)return notice('Enter a member email address.');try{const id=await api('/rest/v1/rpc/cyberchat_start_chat_by_email','POST',{p_email:email});selected={id,title:email};$('chat').hidden=false;$('chatname').textContent=email;$('personEmail').value='';await rooms();await messages();notice('Chat ready')}catch(e){notice(e.message)}};
async function loadAccounts(){try{const rows=await api('/rest/v1/rpc/cyberchat_admin_accounts','POST',{});const panel=$('newAccounts');panel.replaceChildren();for(const m of rows){const line=document.createElement('p');const label=document.createElement('span');label.textContent=(m.display_name||'Member')+' — '+m.email+' — '+(m.approved?'Approved':'Awaiting access')+' — '+new Date(m.created_at).toLocaleDateString()+' ';line.appendChild(label);if(m.id!==user.id){const b=document.createElement('button');b.textContent=m.approved?'Suspend':'Approve';b.onclick=async()=>{try{await api('/rest/v1/rpc/cyberchat_admin_set_approval','POST',{p_user:m.id,p_approved:!m.approved});await loadAccounts()}catch(e){notice(e.message)}};line.appendChild(b)}panel.appendChild(line)}if(!rows.length)panel.textContent='No registered accounts yet.'}catch(e){notice(e.message)}}
$('refreshAccounts').onclick=loadAccounts;

if(window.CyberAuth){
$('login').onclick=()=>CyberAuth.login($('email').value.trim(),$('password').value);
$('signup').onclick=()=>CyberAuth.register($('email').value.trim(),$('password').value);
$('resetPassword').onclick=()=>CyberAuth.reset($('email').value.trim());
$('verifyStatus').onclick=()=>CyberAuth.status();
$('firebaseLogout').onclick=()=>CyberAuth.logout();
$('logout').onclick=()=>CyberAuth.logout();
function showFirebaseHome(e){
 user={id:e.uid,email:e.email||'',firebase:true};
 $('auth').hidden=true;$('firebaseStatus').hidden=true;$('codeGate').hidden=true;$('app').hidden=false;
 $('adminPanel').hidden=true;
 $('directory').textContent='Connecting member directory…';
 $('rooms').textContent='Connecting conversations…';
 $('chat').hidden=true;
 showTab('chats');
 notice('Welcome to CyberChat · '+(e.email||'Member'));
 $('backendStatus').textContent='Connecting messaging…';
 if(window.CyberAuth)CyberAuth.token();
}
window.onCyberAuth=async e=>{
 if(e.type==='token'){
  window.firebaseIdToken=e.token;
  if(!user?.firebase)return;
  try{
   const claims=JSON.parse(atob(e.token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/')));
   if(claims.role!=='authenticated')throw Error('Messaging setup incomplete: Firebase authenticated role claim is missing.');
   access=e.token;
   const response=await api('/rest/v1/rpc/cyberchat_my_role','POST',{});
   if(response==='pending')throw Error('Messaging profile is not provisioned in Supabase yet.');
   $('directory').textContent='';$('rooms').textContent='';$('chatConnection').textContent='Connected securely';
   directory();rooms();
   if(refreshTimer)clearInterval(refreshTimer);
   refreshTimer=setInterval(()=>{if(selected)messages()},4000);
   notice('CyberChat messaging connected');
  }catch(err){
   access='';
   $('directory').textContent='Messaging unavailable until Firebase–Supabase account integration is configured.';
   $('rooms').textContent=err.message;$('chatConnection').textContent='Messaging unavailable: '+err.message;
   notice('Signed in · Messaging connection pending');
  }
  return;
 }
 if(e.type==='logout'){
  access='';user=null;selected=null;window.firebaseIdToken=null;
  if(refreshTimer)clearInterval(refreshTimer);
  $('auth').hidden=false;$('firebaseStatus').hidden=true;$('app').hidden=true;
  $('credentials').hidden=true;$('auth').querySelector('.welcome').hidden=false;
  notice('Signed out');return;
 }
 if(e.type==='error'){notice(e.message||'Sign-in failed');return;}
 if(e.uid){showFirebaseHome(e);return;}
 notice(e.message||'Sign in to CyberChat');
};
CyberAuth.status();
}

let wallpaperUrl=null;
const wallpaperVideo=$('chatWallpaper');
$('wallpaperFile').addEventListener('change',e=>{
 const file=e.target.files&&e.target.files[0];
 if(!file)return;
 if(!file.type.startsWith('video/'))return notice('Choose a video file.');
 if(wallpaperUrl)URL.revokeObjectURL(wallpaperUrl);
 wallpaperUrl=URL.createObjectURL(file);
 wallpaperVideo.src=wallpaperUrl;
 wallpaperVideo.style.display='block';
 wallpaperVideo.muted=true;
 $('wallpaperSound').checked=false;
 wallpaperVideo.play().catch(()=>notice('Tap the video to play wallpaper.'));
 notice('Video wallpaper applied. Sound is muted by default.');
});
$('wallpaperSound').addEventListener('change',e=>{
 wallpaperVideo.muted=!e.target.checked;
 notice(e.target.checked?'Wallpaper sound enabled':'Wallpaper sound muted');
});
$('clearWallpaper').addEventListener('click',()=>{
 wallpaperVideo.pause();wallpaperVideo.removeAttribute('src');wallpaperVideo.load();
 wallpaperVideo.style.display='none';
 if(wallpaperUrl)URL.revokeObjectURL(wallpaperUrl);
 wallpaperUrl=null;$('wallpaperSound').checked=false;
 notice('Video wallpaper removed.');
});

$('applyIcon').onclick=()=>{if(!window.CyberAuth)return notice('Android icon settings unavailable');CyberAuth.setIcon($('iconChoice').value);};

const welcome=$('auth').querySelector('.welcome');
const credentials=$('credentials');
function openCredentials(mode){welcome.hidden=true;credentials.hidden=false;$('authHeading').textContent=mode==='register'?'Create CyberPulse Account':'Sign in to CyberChat';$('nickname').hidden=mode!=='register';$('signup').hidden=mode!=='register';$('login').hidden=mode==='register';}
$('welcomeLogin').onclick=()=>openCredentials('login');
$('welcomeRegister').onclick=()=>openCredentials('register');
$('emailLogin').onclick=()=>openCredentials('login');
$('backWelcome').onclick=()=>{credentials.hidden=true;welcome.hidden=false;};
$('googleLogin').onclick=()=>{if(window.CyberAuth)CyberAuth.google();else notice('Google sign-in requires the Android app.');};
$('resendVerification').onclick=()=>{if(window.CyberAuth)CyberAuth.resendVerification();};


function showTab(tab){
 const tabs={chats:'chatsHome',new:'newChatPanel',settings:'settingsPanel'};
 for(const [name,id] of Object.entries(tabs))$(id).hidden=name!==tab;
 for(const [name,id] of Object.entries({chats:'tabChats',new:'tabNew',settings:'tabSettings'}))$(id).classList.toggle('active',name===tab);
 if(tab!=='chats')$('chat').hidden=true;
}
$('tabChats').onclick=()=>showTab('chats');
$('tabNew').onclick=()=>showTab('new');
$('tabSettings').onclick=()=>showTab('settings');
$('newChatShortcut').onclick=()=>showTab('new');
