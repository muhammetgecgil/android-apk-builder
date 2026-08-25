(function(){
'use strict';
let ultra=null, mixer=null, handBone=null, racket=null, lastHit='', contactUntil=0, prevBallVz=0, loaded=false;
const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
function bone(root, pats){let f=null;root.traverse(o=>{if(f||!o.isBone)return;const n=(o.name||'').toLowerCase();if(pats.some(p=>n.includes(p)))f=o});return f}
function hideOld(){try{scene.traverse(o=>{if(!o||!o.name)return;if(/GUARANTEED_HUMAN_OPPONENT|ULTRA_REALISTIC_TENNIS_OPPONENT|REAL_TENNIS_OPPONENT/.test(o.name)&&o!==ultra)o.visible=false})}catch(e){}}
function tune(root){root.traverse(o=>{if(!(o.isMesh||o.isSkinnedMesh))return;o.castShadow=true;o.receiveShadow=true;o.frustumCulled=false;if(o.material){o.material=o.material.clone();o.material.side=THREE.FrontSide;o.material.transparent=false;o.material.opacity=1;o.material.roughness=clamp(o.material.roughness==null?.55:o.material.roughness,.32,.82);o.material.metalness=clamp(o.material.metalness||0,0,.08);o.material.needsUpdate=true}})}
function loadPart(url){return new Promise((res,rej)=>loader.load(url,g=>res(g),undefined,rej))}
function scaleHuman(root){root.updateMatrixWorld(true);let b=new THREE.Box3().setFromObject(root),h=b.max.y-b.min.y;if(!isFinite(h)||h<.2)h=1.8;root.scale.setScalar(1.84/h);root.updateMatrixWorld(true);b=new THREE.Box3().setFromObject(root);root.position.y-=b.min.y}
async function build(){if(loaded||!window.THREE||!window.scene||!window.loader)return;loaded=true;try{
 const parts=await Promise.all([loadPart('v33_body.glb'),loadPart('v33_head.glb'),loadPart('v33_hair.glb')]);
 const g=new THREE.Group();g.name='MG_ULTRA_PHOTOREAL_OPPONENT_V33';
 const body=parts[0].scene,head=parts[1].scene,hair=parts[2].scene;tune(body);tune(head);tune(hair);g.add(body,head,hair);scaleHuman(g);g.position.set(0,0,-8.7);g.rotation.y=Math.PI;scene.add(g);ultra=g;window.opponent=g;window.MGGuaranteedOpponent=g;
 handBone=bone(body,['righthand','right_hand','hand_r','mixamorigrighthand']);
 if(typeof createRacket==='function'){racket=createRacket(0x151515);racket.name='MG_REAL_RACKET_V33';racket.scale.setScalar(.72);scene.add(racket);window.opponentRacket=racket}
 mixer=new THREE.AnimationMixer(body);const clips=parts[0].animations||[];const idle=clips.find(a=>/idle|sway/i.test(a.name))||clips[0];if(idle)mixer.clipAction(idle).play();
 hideOld();const st=document.getElementById('status');if(st)st.textContent='ULTRA GERÇEKÇİ RAKİP HAZIR';
 }catch(e){console.error('v33 human',e);loaded=false;setTimeout(build,2500)}}
function racketPose(){if(!ultra||!racket)return null;const p=new THREE.Vector3(),q=new THREE.Quaternion();try{if(handBone){handBone.getWorldPosition(p);handBone.getWorldQuaternion(q);racket.position.copy(p);racket.quaternion.copy(q);racket.rotateZ(-.55);racket.translateY(-.32);racket.translateX(.12)}else{ultra.getWorldPosition(p);racket.position.set(p.x+.52,1.03,p.z+.08);racket.rotation.set(0,.15,-.55)}return p}catch(e){return null}}
function racketContactPoint(){if(!racket)return null;const p=new THREE.Vector3();try{const b=new THREE.Box3().setFromObject(racket);b.getCenter(p);p.y=clamp(p.y,.55,1.8);return p}catch(e){return null}}
function lockBallToRacket(){try{if(!ball||!ballVel||!ultra)return;const now=performance.now(),cur=String(window.lastHitter||'');const switched=cur==='opponent'&&lastHit!=='opponent';const reversed=prevBallVz<-.15&&ballVel.z>.15;if((switched||reversed)&&now>contactUntil){const cp=racketContactPoint();if(cp){ball.position.copy(cp);if(ballShadow)ballShadow.position.set(cp.x,.02,cp.z);if(trailLine&&trailLine.geometry)trailLine.geometry.setFromPoints([cp.clone(),cp.clone()]);contactUntil=now+180}}
 if(now<contactUntil){const cp=racketContactPoint();if(cp)ball.position.lerp(cp,.92)}
 lastHit=cur;prevBallVz=ballVel.z}catch(e){}}
function moveOpponent(dt){try{if(!ultra||!ball||!ballVel)return;let tx=0,tz=-8.7;if(ballVel.z<-.2){const eta=(-8.25-ball.position.z)/ballVel.z;if(eta>0&&eta<4){tx=clamp(ball.position.x+ballVel.x*eta,-4.3,4.3);tz=clamp(-8.7+(1.2-eta)*.45,-10.2,-6.7)}}const k=1-Math.exp(-4.8*dt);ultra.position.x+=(tx-ultra.position.x)*k;ultra.position.z+=(tz-ultra.position.z)*k;ultra.rotation.y=Math.PI;if(typeof opponentX!=='undefined')opponentX=ultra.position.x;if(typeof opponentZ!=='undefined')opponentZ=ultra.position.z}catch(e){}}
let t0=performance.now();function tick(t){const dt=Math.min(.04,(t-t0)/1000||.016);t0=t;if(!ultra)build();else{if(mixer)mixer.update(dt);moveOpponent(dt);racketPose();lockBallToRacket();hideOld()}requestAnimationFrame(tick)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>requestAnimationFrame(tick));else requestAnimationFrame(tick);
})();
