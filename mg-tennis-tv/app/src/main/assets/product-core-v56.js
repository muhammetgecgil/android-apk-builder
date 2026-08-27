(function(){'use strict';
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
window.MGPC1=window.MGPC1||{};
let patched=false,lastAccepted=0,dirEma=0,powerEma=1,calibrationSamples=0;
function mode(){return (window.MGProductMode&&MGProductMode.mode)||document.body.dataset.mgProductMode||''}
function solo(){return mode()==='solo'}
function profile(){return (window.MGDifficulty&&MGDifficulty.current)||{key:'easy'}
function incoming(){try{if(!ball||!ballVel)return false;const px=typeof playerX!=='undefined'?playerX:0,pz=typeof playerZ!=='undefined'?playerZ:9.55;const eta=ballVel.z>.15?(pz-ball.position.z)/ballVel.z:99;const dx=Math.abs(ball.position.x-px);return ballVel.z>.15&&eta>-.18&&eta<(profile().key==='easy'?1.55:1.05)&&dx<(profile().key==='easy'?2.5:2.0)}catch(e){return false}}
function inferDirection(raw){const r=clamp(Number(raw)||0,-1,1);const tilt=clamp(Number(window.playerTilt||0)/6,-1,1);let d=r*.58+tilt*.42;if(Math.abs(d)<.10)d=0;dirEma=dirEma*.55+d*.45;return clamp(dirEma,-1,1)}
function inferPower(raw){let p=clamp(Number(raw)||1,.68,2.45);p=Math.pow(p,1.08);powerEma=powerEma*.42+p*.58;return clamp(powerEma,.72,2.5)}
function patch(){if(patched||typeof window.nativeSwing!=='function')return;const old=window.nativeSwing;window.nativeSwing=function(power,dir,remote){if(!solo()||remote)return old.apply(this,arguments);const now=performance.now();if(now-lastAccepted<210)return;lastAccepted=now;const p=inferPower(power),d=inferDirection(dir);window.MGSoloSensorIntent={power:p,direction:d,stroke:d>.14?'FOREHAND':d<-.14?'BACKHAND':'CENTER',at:now};MGPC1.soloSensor='PASS';MGPC1.soloStroke=window.MGSoloSensorIntent.stroke;MGPC1.soloPower=Number(p.toFixed(2));MGPC1.soloDirection=Number(d.toFixed(2));return old.call(this,p,d,false)};window.nativeSwing.__mg56=true;patched=true}
function tuneStrike(){try{if(!solo()||!window.MGV37||MGV37.__mg56)return;const old=MGV37.canPlayerSwing.bind(MGV37);MGV37.canPlayerSwing=function(){if(old())return true;if(!incoming())return false;return true};MGV37.__mg56=true}catch(e){}}
function calibrateHint(){if(!solo())return;calibrationSamples++;if(calibrationSamples<180){MGPC1.soloCalibration='WARMUP'}else MGPC1.soloCalibration='READY'}
function status(){try{if(!solo())return;const s=document.getElementById('status');const q=window.MGSoloSensorIntent;if(s&&q&&performance.now()-q.at<500)s.textContent='TEK TELEFON • '+q.stroke+' • güç '+q.power.toFixed(1)+' • yön '+(q.direction>0.12?'SAĞ':q.direction<-0.12?'SOL':'ORTA')}catch(e){}}
function tick(){patch();tuneStrike();calibrateHint();status();requestAnimationFrame(tick)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(tick));else requestAnimationFrame(tick);
})();
