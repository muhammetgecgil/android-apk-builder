(function(){'use strict';
window.MGPC1=window.MGPC1||{};
const $=id=>document.getElementById(id);let waiting=null,entered=false,lastLink='DISCONNECTED',tvConnected=false;
function call(n){try{if(window.Android&&typeof Android[n]==='function')return Android[n]()}catch(e){}return null}
function mode(){return (window.MGProductMode&&MGProductMode.mode)||''}
function selected(sel,attr){const b=document.querySelector(sel+' .mg41opt.sel');return b?b.dataset[attr]||null:null}
function stopBall(){try{if(window.ballVel)ballVel.set(0,0,0);if(window.MGTennisRules&&MGTennisRules.match)MGTennisRules.match.pointLive=false}catch(e){}}
function setStatus(t){const e=$('status');if(e)e.textContent=t}
function applyChoices(m){const d=selected('#mg41diff','v'),s=selected('#mg41serve','v'),c=selected('#mg67camera','c');try{if(window.MGDifficulty&&MGDifficulty.set&&d)MGDifficulty.set(d);window.MGSelectedDifficulty=d;window.MGSelectedServe=s;window.MGSelectedMode='match';window.MGSelectedCamera=c}catch(e){}return {d,s,c,m}}
function enter(m){if(entered)return;entered=true;waiting=null;const root=$('mg41');if(root)root.remove();document.body.dataset.mgProductMode=m;try{if(window.MGTennisRules&&MGTennisRules.reset)MGTennisRules.reset()}catch(e){}MGPC1.setupGate='PASS';MGPC1.connectionGate='PASS';setStatus(m==='tv'?'TV BAĞLI ✓ • MAÇ HAZIR':m==='host'?'RAKET BAĞLI ✓ • MAÇ HAZIR':'MAÇ HAZIR')}
function waitUi(kind){const b=$('mg41start');if(!b)return;b.disabled=false;b.style.opacity='1';b.textContent=kind==='tv'?'TV BAĞLANTISI BEKLENİYOR…':'RAKET TELEFONU BEKLENİYOR…';let c=$('mg71cancel');if(!c){c=document.createElement('button');c.id='mg71cancel';c.className='mg41opt';c.textContent='← MODLARA DÖN';c.style.marginTop='10px';c.onclick=e=>{e.preventDefault();e.stopPropagation();waiting=null;entered=false;try{call('stopVision')}catch(x){};location.reload()};const setup=$('mg41setup');if(setup)setup.appendChild(c)}setStatus(kind==='tv'?'TV bağlantısını tamamla • oyun bağlantı kurulmadan başlamaz':'Bluetooth raket telefonunu bağla • oyun bağlantı kurulmadan başlamaz')}
function startTv(){const ch=applyChoices('tv');if(!ch.d||!ch.s||!ch.c)return;waiting='tv';entered=false;MGPC1.connectionGate='WAIT_TV';MGPC1.tvCamera=ch.c==='camera'?'CAMERA_SELECTED':'NO_CAMERA';MGPC1.sensorProfile='TV_LOCAL_RACKET';stopBall();call('startAi');call('openCastSettings');waitUi('tv');try{tvConnected=!!call('isTvConnected')}catch(e){tvConnected=false}if(tvConnected)setTimeout(()=>enter('tv'),180)}
function startHost(){const ch=applyChoices('host');if(!ch.d||!ch.s)return;waiting='host';entered=false;MGPC1.connectionGate='WAIT_RACKET';MGPC1.sensorProfile='REMOTE_RACKET';stopBall();call('startHost');waitUi('host')}
function intercept(e){const t=e.target&&e.target.closest?e.target.closest('#mg41start'):null;if(!t||waiting)return;const m=mode();if(m!=='tv'&&m!=='host')return;e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();if(m==='tv')startTv();else startHost()}
window.addEventListener('click',intercept,true);
const oldTv=window.tvConnectionChanged;window.tvConnectionChanged=function(ok){tvConnected=!!ok;try{if(oldTv)oldTv(ok)}catch(e){}if(waiting==='tv'){MGPC1.tvConnected=tvConnected?'YES':'NO';if(tvConnected)enter('tv');else waitUi('tv')}};
const oldLink=window.racketLinkState;window.racketLinkState=function(state,lat){lastLink=String(state||'');try{if(oldLink)oldLink(state,lat)}catch(e){}if(waiting==='host'){MGPC1.racketLink=lastLink;MGPC1.racketLatencyMs=Number(lat)||0;if(lastLink==='CONNECTED')enter('host');else waitUi('host')}};
function productionCleanup(){document.querySelectorAll('button').forEach(b=>{if(/OTOMATİK QA TESTİ/i.test(b.textContent||''))b.style.display='none'});if(waiting)stopBall()}
setInterval(productionCleanup,250);
window.MGProductBackToModes=function(){waiting=null;entered=false;location.reload()};
MGPC1.version='7.1.0';MGPC1.productTrack='PRODUCT_STABILIZATION_CONNECTION_GATES';MGPC1.qaButton='HIDDEN_PRODUCTION';
})();