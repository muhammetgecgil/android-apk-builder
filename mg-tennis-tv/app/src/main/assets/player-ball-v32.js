(function(){'use strict';
let last=performance.now(),prevVelZ=0,contactLock=0;
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
function opp(){try{return (window.opponent&&opponent.visible&&opponent)||(window.MGGuaranteedOpponent&&MGGuaranteedOpponent.visible&&MGGuaranteedOpponent)||null}catch(e){return null}}
function normalizeRacket(){try{const o=opp();if(!o)return;let racket=null;o.traverse(x=>{if(!racket&&x&&x.name&&/racket|racquet/i.test(x.name))racket=x});if(!racket&&window.opponentRacket)racket=opponentRacket;if(racket){const box=new THREE.Box3().setFromObject(racket),sz=new THREE.Vector3();box.getSize(sz);const h=Math.max(sz.x,sz.y,sz.z);if(isFinite(h)&&h>1.05){const k=.72/h;racket.scale.multiplyScalar(k)}}}catch(e){}}
function handOrigin(o){const p=new THREE.Vector3();try{if(window.opponentBones&&opponentBones.handR){opponentBones.handR.getWorldPosition(p);return p}o.getWorldPosition(p);p.x+=(ball&&ball.position.x<p.x?-.48:.48);p.y+=1.05;p.z+=.12}catch(e){}return p}
function syncOpponentContact(dt){try{const o=opp();if(!o||!ball||!ballVel)return;const now=performance.now();const approaching=prevVelZ<-.15,returned=ballVel.z>.15;if(approaching&&returned&&now>contactLock){const p=handOrigin(o);const dx=ball.position.x-p.x,dz=ball.position.z-p.z;if(Math.hypot(dx,dz)<2.5){ball.position.copy(p);ball.position.y=clamp(p.y,.55,1.75);contactLock=now+220;try{if(ballShadow)ballShadow.position.set(ball.position.x,.02,ball.position.z)}catch(e){}}}prevVelZ=ballVel.z}catch(e){}}
function improveFallback(){try{const o=window.MGGuaranteedOpponent;if(!o)return;o.traverse(x=>{if(!x.isMesh)return;x.castShadow=true;x.receiveShadow=true;if(x.material){x.material.roughness=Math.max(.45,Math.min(.82,x.material.roughness||.65));x.material.metalness=0}})}catch(e){}}
function tick(t){const dt=Math.min(.04,(t-last)/1000||.016);last=t;normalizeRacket();improveFallback();syncOpponentContact(dt);requestAnimationFrame(tick)}
requestAnimationFrame(tick);
})();