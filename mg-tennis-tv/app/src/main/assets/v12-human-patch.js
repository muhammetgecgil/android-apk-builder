(function(){
'use strict';
let bootReleased=false, opponentLoaded=false;
function ready(){try{return !!(window.THREE&&scene&&loader&&ball)}catch(e){return false}}
function releaseGame(statusText){
  if(bootReleased)return; bootReleased=true;
  const loading=document.getElementById('loading'); if(loading)loading.style.display='none';
  const fill=document.getElementById('loadfill'); if(fill)fill.style.width='100%';
  const status=document.getElementById('status'); if(status)status.textContent=statusText||'Hazır • rakip arka planda hazırlanıyor';
}
function removeAllCrowd(){if(!ready())return;const gone=[];scene.traverse(o=>{if(!o||!o.geometry)return;const p=o.geometry.parameters||{};const body=o.isInstancedMesh&&o.geometry.type==='CylinderGeometry'&&p.height>0.28&&p.height<0.65;const head=o.isInstancedMesh&&o.geometry.type==='SphereGeometry'&&(p.radius||0)>.045&&(p.radius||0)<.16;if(body||head)gone.push(o)});gone.forEach(o=>{if(o.parent)o.parent.remove(o)})}
function cleanOldOpponent(){try{if(opponent&&opponent.parent)opponent.parent.remove(opponent)}catch(e){} try{if(opponentRacket&&opponentRacket.parent)opponentRacket.parent.remove(opponentRacket)}catch(e){} opponent=null;opponentRacket=null;opponentMixer=null;opponentBones={}}
function findBone(root,terms,exclude){let f=null;exclude=exclude||[];root.traverse(o=>{if(f||!o.isBone)return;const n=(o.name||'').toLowerCase();if(terms.some(t=>n.includes(t))&&!exclude.some(t=>n.includes(t)))f=o});return f}
function loadOpponent(url,fallback){
  const msg=document.getElementById('loadmsg'),fill=document.getElementById('loadfill');
  if(msg)msg.textContent='Kort açılıyor • rakip arka planda hazırlanıyor…'; if(fill)fill.style.width='72%';
  setTimeout(()=>releaseGame('Hazır • rakip arka planda hazırlanıyor'),900);
  let settled=false;
  const watchdog=setTimeout(()=>{if(settled)return;settled=true;releaseGame('Hazır • rakip modeli gecikiyor'); if(fallback)loadOpponent(fallback,null)},5500);
  loader.load(url,g=>{
    if(settled)return; settled=true; clearTimeout(watchdog);
    try{
      cleanOldOpponent(); removeAllCrowd();
      const model=g.scene; model.visible=true;
      model.traverse(o=>{o.visible=true;o.frustumCulled=false;if(o.isMesh||o.isSkinnedMesh){o.castShadow=true;o.receiveShadow=true;if(o.material){o.material=o.material.clone();o.material.transparent=false;o.material.opacity=1;o.material.side=THREE.FrontSide;o.material.needsUpdate=true}}});
      model.updateMatrixWorld(true); let box=new THREE.Box3().setFromObject(model),h=box.max.y-box.min.y;if(!isFinite(h)||h<.05||h>100)h=1.8;model.scale.setScalar(1.82/h);model.updateMatrixWorld(true);box=new THREE.Box3().setFromObject(model);model.position.y-=box.min.y;
      const wrap=new THREE.Group();wrap.name='REAL_TENNIS_OPPONENT';wrap.add(model);opponentX=0;opponentZ=-8.85;targetOpponentX=0;targetOpponentZ=-8.85;wrap.position.set(0,0,-8.85);wrap.visible=true;scene.add(wrap);opponent=wrap;opponentLoaded=true;
      opponentMixer=new THREE.AnimationMixer(model);
      const b={upperR:findBone(model,['rightarm','upperarm_r','r_upperarm','upperarm.r'],['fore']),lowerR:findBone(model,['rightforearm','lowerarm_r','r_forearm','forearm.r']),handR:findBone(model,['righthand','hand_r','r_hand','hand.r']),upperL:findBone(model,['leftarm','upperarm_l','l_upperarm','upperarm.l'],['fore']),lowerL:findBone(model,['leftforearm','lowerarm_l','l_forearm','forearm.l']),handL:findBone(model,['lefthand','hand_l','l_hand','hand.l']),thighR:findBone(model,['rightupleg','thigh_r','r_thigh','upperleg.r']),shinR:findBone(model,['rightleg','calf_r','r_calf','lowerleg.r']),thighL:findBone(model,['leftupleg','thigh_l','l_thigh','upperleg.l']),shinL:findBone(model,['leftleg','calf_l','l_calf','lowerleg.l']),spine:findBone(model,['spine2','spine_03','spine1','spine']),pelvis:findBone(model,['hips','pelvis']),neck:findBone(model,['neck']),head:findBone(model,['head'])};opponentBones=b;
      const rest={};Object.keys(b).forEach(k=>{const x=b[k];if(x)rest[k]={x:x.rotation.x,y:x.rotation.y,z:x.rotation.z}});
      if(typeof createRacket==='function'){opponentRacket=createRacket(0x202020);scene.add(opponentRacket)}
      let phase=0,swing=0,lastIncoming=false,dt0=.016;
      const damp=(a,z,k,d)=>a+(z-a)*(1-Math.exp(-k*d)); const clamp=(v,a,z)=>Math.max(a,Math.min(z,v));
      function pose(k,x,y,z){const q=b[k],r=rest[k];if(!q||!r)return;q.rotation.x=damp(q.rotation.x,r.x+x,10,dt0);q.rotation.y=damp(q.rotation.y,r.y+y,10,dt0);q.rotation.z=damp(q.rotation.z,r.z+z,10,dt0)}
      window.updateOpponentMotion=function(dt){if(!opponent||!ball)return;dt0=Math.min(.05,Math.max(.001,dt));phase+=dt0;const incoming=ballVel&&ballVel.z<-.45;let eta=9,predX=0;if(incoming){eta=(-8.25-ball.position.z)/ballVel.z;if(eta>0&&eta<4)predX=clamp(ball.position.x+ballVel.x*eta,-4.1,4.1)}targetOpponentX=incoming&&eta<4?predX*.96:0;targetOpponentZ=-8.85;const dx=targetOpponentX-opponentX,dz=targetOpponentZ-opponentZ,dist=Math.hypot(dx,dz),step=Math.min(dist,5.4*dt0);if(dist>.001){opponentX+=dx/dist*step;opponentZ+=dz/dist*step}opponent.position.set(opponentX,0,opponentZ);const speed=step/Math.max(dt0,.001),s=Math.sin(phase*(5.2+speed*.5)),walk=clamp(speed/4,0,1);pose('thighR',s*.45*walk-.08,0,0);pose('thighL',-s*.45*walk-.08,0,0);pose('shinR',Math.max(0,-s)*.45*walk,0,0);pose('shinL',Math.max(0,s)*.45*walk,0,0);const bd=ball.position.x-opponentX;if(incoming&&eta<.5){swing+=dt0;const p=clamp(swing/.48,0,1),swingA=Math.sin(p*Math.PI);if(bd>=0){pose('upperR',-.45+.95*swingA,0,-.15);pose('lowerR',-.45+.4*swingA,0,0)}else{pose('upperL',-.4+.85*swingA,0,.15);pose('lowerL',-.4+.35*swingA,0,0)}}else swing=0;pose('spine',-.03*walk,clamp(bd*.04,-.16,.16),0);pose('neck',0,-clamp(bd*.03,-.1,.1),0);pose('head',0,-clamp(bd*.02,-.07,.07),0);if(opponentRacket&&b.handR){const wp=new THREE.Vector3(),wq=new THREE.Quaternion();b.handR.getWorldPosition(wp);b.handR.getWorldQuaternion(wq);opponentRacket.position.copy(wp);opponentRacket.quaternion.copy(wq)}lastIncoming=incoming};
      releaseGame('Hazır • gerçek 3D rakip aktif');
    }catch(e){console.error('opponent setup',e);releaseGame('Hazır • rakip kurulumu atlandı');if(fallback)loadOpponent(fallback,null)}
  },x=>{if(fill&&x&&x.total)fill.style.width=(72+Math.min(24,Math.round(x.loaded/x.total*24)))+'%'},e=>{clearTimeout(watchdog);if(settled)return;settled=true;console.error('opponent load',e);releaseGame('Hazır • rakip modeli yeniden deneniyor');if(fallback)loadOpponent(fallback,null)});
}
function boot(){if(!ready()){setTimeout(boot,80);return}removeAllCrowd();releaseGame('Hazır • rakip arka planda hazırlanıyor');setTimeout(()=>loadOpponent('opponent.glb','tennis-player.glb'),60);setTimeout(removeAllCrowd,800)}
setTimeout(()=>releaseGame('Hazır • oyun başlatıldı'),1800);boot();
})();
