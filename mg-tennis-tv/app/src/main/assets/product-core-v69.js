(function(){'use strict';
window.MGPC1=window.MGPC1||{};const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
let gx=0,gy=0,gz=9.81,lastMotion=0,lastServe=0,serveArmedAt=0,armPeak=0,ready=false;
function mode(){return (window.MGProductMode&&MGProductMode.mode)||document.body.dataset.mgProductMode||''}
function solo(){return mode()==='solo'}
function match(){try{return window.MGTennisRules&&MGTennisRules.match?MGTennisRules.match:null}catch(e){return null}}
function status(t){const e=document.getElementById('status');if(e)e.textContent=t}
function servingMe(){const m=match();return !!(solo()&&m&&m.server==='player'&&!m.pointLive&&!m.matchOver)}
function doServe(power,dir){try{const m=match();if(!servingMe()||!window.ball||!window.ballVel)return false;const now=performance.now();if(now-lastServe<650)return false;lastServe=now;const p=clamp(Number(power)||1,.80,1.75),d=clamp(Number(dir)||0,-.72,.72),T=clamp(1.34-(p-1)*.08,1.16,1.38),tx=clamp(d*2.0,-2.35,2.35),tz=-5.35;ball.position.y=Math.max(ball.position.y,1.56);const sy=ball.position.y;ballVel.x=(tx-ball.position.x)/T;ballVel.z=(tz-ball.position.z)/T;ballVel.y=((.34-sy)+.5*9.81*T*T)/T+.40;if(window.ballSpin){ballSpin.x=-13.5-p*2;ballSpin.y=d*1.8}if(typeof lastHitter!=='undefined')lastHitter='player';if(typeof serving!=='undefined')serving=false;m.pointLive=true;m.serveNumber=1;serveArmedAt=0;MGPC1.soloServe='PASS';MGPC1.soloServeProfile='TURN_THEN_SCREEN_PUSH';MGPC1.soloServeIntent='CONFIRMED';status('SERVİS ✓ • hazırlık + ileri itiş algılandı');return true}catch(e){MGPC1.lastError=String(e);return false}}
const prior=window.localSwingV61;
window.localSwingV61=function(p,d,s){if(servingMe()){
  // Serving is intentionally NOT fired by a generic/native swing anymore.
  // It must pass the explicit two-stage turn + screen-forward push gesture below.
  MGPC1.soloServeBlockedGeneric=(Number(MGPC1.soloServeBlockedGeneric)||0)+1;return false;
}return prior?prior(p,d,s):false};
function motion(e){if(!solo()||document.getElementById('mg41'))return;const a=e.accelerationIncludingGravity||e.acceleration;if(!a)return;const ax=Number(a.x)||0,ay=Number(a.y)||0,az=Number(a.z)||0;gx=gx*.94+ax*.06;gy=gy*.94+ay*.06;gz=gz*.94+az*.06;const dx=ax-gx,dy=ay-gy,dz=az-gz,lin=Math.sqrt(dx*dx+dy*dy+dz*dz),rr=e.rotationRate||{},rot=Math.max(Math.abs(Number(rr.alpha)||0),Math.abs(Number(rr.beta)||0),Math.abs(Number(rr.gamma)||0)),now=performance.now();
 if(servingMe()){
   // Stage 1: deliberate phone turn/tilt. Idle sensor noise cannot arm the serve.
   if(!serveArmedAt&&rot>18){serveArmedAt=now;armPeak=rot;MGPC1.soloServeIntent='ARMED';status('SERVİS HAZIR • şimdi ekranı ileri doğru it')}
   if(serveArmedAt){armPeak=Math.max(armPeak,rot);if(now-serveArmedAt>1200){serveArmedAt=0;MGPC1.soloServeIntent='TIMEOUT';status('SERVİS SENDE • telefonu çevir, sonra ekranı ileri it');return}
     // Stage 2: screen-normal thrust. Require a real impulse, not gravity drift.
     const push=Math.abs(dz);if(now-serveArmedAt>70&&push>1.55&&lin>1.65&&now-lastMotion>420){lastMotion=now;const dir=clamp(dx/Math.max(1.4,lin),-.72,.72),power=clamp(.88+push*.15+lin*.06+armPeak*.002,.88,1.70);doServe(power,dir);return}}
   return;
 }
 // Rally stays gentle: film-view grip with a small intentional movement.
 if(lin>1.35&&now-lastMotion>300){lastMotion=now;const dir=clamp(dx/Math.max(1.2,lin),-1,1),power=clamp(.80+lin*.13,.80,1.90);if(prior)prior(power,dir,Math.abs(dir)<.16?'CENTER':dir>0?'FOREHAND':'BACKHAND')}
}
function enable(){if(ready)return;ready=true;window.addEventListener('devicemotion',motion,{passive:true});MGPC1.soloControl='FILM_VIEW_TWO_STAGE_SERVE';}
function hint(){if(!servingMe())return;if(!serveArmedAt)status('SERVİS SENDE • telefonu hafifçe çevir/dikleştir • sonra ekranı ileri doğru it');}
setInterval(hint,1400);enable();MGPC1.version='6.9.0';MGPC1.productTrack='INTENTIONAL_TWO_STAGE_SOLO_SERVE';})();
