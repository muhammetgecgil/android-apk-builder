(function(){
'use strict';
function ready(){try{return !!(window.THREE&&scene&&loader&&ball)}catch(e){return false}}
function clamp2(v,a,b){return Math.max(a,Math.min(b,v))}
function damp(a,b,k,dt){return a+(b-a)*(1-Math.exp(-k*dt))}
function removeAllCrowd(){
  if(!ready())return;
  const gone=[];
  scene.traverse(o=>{
    if(!o||!o.geometry)return;
    const p=o.geometry.parameters||{};
    const primitiveBody=o.isInstancedMesh&&o.geometry.type==='CylinderGeometry'&&p.height>0.28&&p.height<0.65;
    const primitiveHead=o.isInstancedMesh&&o.geometry.type==='SphereGeometry'&&(p.radius||0)>.045&&(p.radius||0)<.16;
    if(primitiveBody||primitiveHead)gone.push(o);
  });
  gone.forEach(o=>{if(o.parent)o.parent.remove(o)});
}
function cleanOldOpponent(){
  try{if(opponent&&opponent.parent)opponent.parent.remove(opponent)}catch(e){}
  try{if(opponentRacket&&opponentRacket.parent)opponentRacket.parent.remove(opponentRacket)}catch(e){}
  opponent=null;opponentRacket=null;opponentMixer=null;opponentBones={};
}
function findBone(root,terms,exclude){let f=null;exclude=exclude||[];root.traverse(o=>{if(f||!o.isBone)return;const n=(o.name||'').toLowerCase();if(terms.some(t=>n.includes(t))&&!exclude.some(t=>n.includes(t)))f=o});return f}
function restOf(b){const r={};Object.keys(b).forEach(k=>{const x=b[k];if(x)r[k]={x:x.rotation.x,y:x.rotation.y,z:x.rotation.z}});return r}
function loadOpponent(url,fallback){
  const msg=document.getElementById('loadmsg'),fill=document.getElementById('loadfill');
  if(msg)msg.textContent='Gerçek 3D tenis rakibi hazırlanıyor…';if(fill)fill.style.width='28%';
  loader.load(url,g=>{
    cleanOldOpponent();removeAllCrowd();
    const model=g.scene;
    model.visible=true;model.updateMatrixWorld(true);
    model.traverse(o=>{
      o.visible=true;o.frustumCulled=false;
      if(o.isMesh||o.isSkinnedMesh){
        o.castShadow=true;o.receiveShadow=true;
        if(o.material){
          o.material=o.material.clone();o.material.visible=true;o.material.transparent=false;o.material.opacity=1;o.material.side=THREE.FrontSide;
          if(o.material.roughness!==undefined)o.material.roughness=Math.max(.28,Math.min(.56,o.material.roughness));
          if(o.material.metalness!==undefined)o.material.metalness=Math.min(.035,o.material.metalness);
          if(o.material.map){o.material.map.anisotropy=Math.min(8,renderer.capabilities.getMaxAnisotropy());o.material.map.needsUpdate=true}
          o.material.needsUpdate=true;
        }
      }
    });
    model.updateMatrixWorld(true);
    let box=new THREE.Box3().setFromObject(model),h=box.max.y-box.min.y;
    if(!isFinite(h)||h<.05||h>100)h=1.8;
    model.scale.setScalar(1.82/h);model.updateMatrixWorld(true);
    box=new THREE.Box3().setFromObject(model);model.position.y-=box.min.y;model.updateMatrixWorld(true);
    const wrap=new THREE.Group();wrap.name='REAL_TENNIS_OPPONENT';wrap.add(model);
    opponentX=0;opponentZ=-8.85;targetOpponentX=0;targetOpponentZ=-8.85;
    wrap.position.set(opponentX,0,opponentZ);wrap.rotation.set(0,0,0);wrap.visible=true;scene.add(wrap);opponent=wrap;
    opponentMixer=new THREE.AnimationMixer(model);
    const b={
      upperR:findBone(model,['rightarm','upperarm_r','r_upperarm','upperarm.r'],['fore']),lowerR:findBone(model,['rightforearm','lowerarm_r','r_forearm','forearm.r']),handR:findBone(model,['righthand','hand_r','r_hand','hand.r']),
      upperL:findBone(model,['leftarm','upperarm_l','l_upperarm','upperarm.l'],['fore']),lowerL:findBone(model,['leftforearm','lowerarm_l','l_forearm','forearm.l']),handL:findBone(model,['lefthand','hand_l','l_hand','hand.l']),
      thighR:findBone(model,['rightupleg','thigh_r','r_thigh','upperleg.r']),shinR:findBone(model,['rightleg','calf_r','r_calf','lowerleg.r']),footR:findBone(model,['rightfoot','foot_r','r_foot','foot.r']),
      thighL:findBone(model,['leftupleg','thigh_l','l_thigh','upperleg.l']),shinL:findBone(model,['leftleg','calf_l','l_calf','lowerleg.l']),footL:findBone(model,['leftfoot','foot_l','l_foot','foot.l']),
      spine:findBone(model,['spine2','spine_03','spine1','spine']),pelvis:findBone(model,['hips','pelvis']),neck:findBone(model,['neck']),head:findBone(model,['head'])
    };opponentBones=b;const rest=restOf(b);
    if(typeof createRacket==='function'){opponentRacket=createRacket(0x202020);scene.add(opponentRacket)}
    let state='READY',stateT=0,runPhase=0,swingT=0,lastIncoming=false,frameDt=.016;
    function poseBone(name,ax,ay,az,blend){const x=b[name],r=rest[name];if(!x||!r)return;blend=blend||1;x.rotation.x=damp(x.rotation.x,r.x+ax,10*blend,frameDt);x.rotation.y=damp(x.rotation.y,r.y+ay,10*blend,frameDt);x.rotation.z=damp(x.rotation.z,r.z+az,10*blend,frameDt)}
    function setState(n){if(n!==state){state=n;stateT=0}}
    window.updateOpponentMotion=function(dt){
      if(!opponent||!ball)return;frameDt=Math.min(.05,Math.max(.001,dt));stateT+=frameDt;runPhase+=frameDt;
      const incoming=ballVel&&ballVel.z<-.45;let eta=9,predX=0,predY=1,interceptZ=-8.25;
      if(incoming){eta=(interceptZ-ball.position.z)/ballVel.z;if(eta>0&&eta<4){predX=clamp2(ball.position.x+ballVel.x*eta,-4.15,4.15);predY=ball.position.y+ballVel.y*eta-4.905*eta*eta}else eta=9}
      const shortBall=incoming&&eta<2.1&&predY<.82;targetOpponentX=incoming&&eta<4?predX*.96:0;targetOpponentZ=shortBall?-7.2:-8.85;
      const dx=targetOpponentX-opponentX,dz=targetOpponentZ-opponentZ,dist=Math.hypot(dx,dz),maxSpeed=state==='RUN'?5.8:4.0,step=Math.min(dist,maxSpeed*frameDt);
      if(dist>.001){opponentX+=dx/dist*step;opponentZ+=dz/dist*step}opponent.position.x=opponentX;opponent.position.z=opponentZ;oppSpeed=step/Math.max(frameDt,.001);
      const ballDx=ball.position.x-opponentX,ballDz=ball.position.z-opponentZ,ballDist=Math.hypot(ballDx,ballDz);
      if(incoming&&!lastIncoming&&ball.position.z<1.5)setState('SPLIT');if(state==='READY'&&incoming&&eta<2.4)setState('SPLIT');if(state==='SPLIT'&&stateT>.16)setState(dist>.48?'RUN':'SET');
      if((state==='RUN'||state==='SET')&&incoming&&eta<.42&&ballDist<2.0){swingT=0;setState(ballDx>=0?'FOREHAND':'BACKHAND')}if(state==='RUN'&&dist<.30)setState('SET');if(state==='SET'&&dist>.62)setState('RUN');
      if(state==='FOREHAND'||state==='BACKHAND'){swingT+=frameDt;if(swingT>.48)setState('RECOVER')}if(state==='RECOVER'&&stateT>.42)setState(incoming?'SET':'READY');if(!incoming&&state!=='FOREHAND'&&state!=='BACKHAND'&&state!=='RECOVER')setState(dist>.55?'RUN':'READY');lastIncoming=incoming;
      const speed=Math.min(6,oppSpeed||0),walk=clamp2(speed/4.2,0,1),ph=runPhase*(5.2+speed*.55),s=Math.sin(ph),split=state==='SPLIT'?Math.sin(Math.min(1,stateT/.16)*Math.PI):0;
      let knee=.10+.12*walk+.12*split,torsoY=clamp2(ballDx*.045,-.18,.18),torsoZ=clamp2(-dx*.018,-.08,.08),rUx=-s*.24*walk,lUx=s*.24*walk,rLx=-.15,lLx=-.12,rUz=0,lUz=0;
      if(state==='FOREHAND'||state==='BACKHAND'){const p=clamp2(swingT/.48,0,1),hit=clamp2((p-.28)/.32,0,1),follow=clamp2((p-.60)/.40,0,1),side=state==='FOREHAND'?1:-1;torsoY=side*(-.45+.98*hit-.28*follow)*.58;torsoZ=-side*.10*Math.sin(p*Math.PI);if(side>0){rUx=-.52+.95*hit-.25*follow;rLx=-.52+.46*hit;rUz=-.18+.38*hit}else{lUx=-.42+.82*hit-.20*follow;lLx=-.48+.40*hit;lUz=.16-.32*hit}knee=.20+.08*Math.sin(p*Math.PI)}
      poseBone('thighR',s*.48*walk-knee*.25,0,0);poseBone('thighL',-s*.48*walk-knee*.25,0,0);poseBone('shinR',Math.max(0,-s)*.50*walk+knee*.45,0,0);poseBone('shinL',Math.max(0,s)*.50*walk+knee*.45,0,0);
      poseBone('upperR',rUx,0,rUz);poseBone('lowerR',rLx,0,0);poseBone('upperL',lUx,0,lUz);poseBone('lowerL',lLx,0,0);poseBone('spine',-.035*walk,torsoY,torsoZ);poseBone('pelvis',0,torsoY*.25,0);
      const lookX=clamp2(ballDx*.035,-.12,.12),lookY=clamp2((ball.position.y-1.45)*.07,-.08,.10);poseBone('neck',lookY,-lookX,0);poseBone('head',lookY*.45,-lookX*.55,0);
      const facing=clamp2(ballDx*.035,-.14,.14);opponent.rotation.x=damp(opponent.rotation.x,-Math.min(.04,speed*.003),8,frameDt);opponent.rotation.z=damp(opponent.rotation.z,-clamp2(dx*.025,-.07,.07),9,frameDt);opponent.rotation.y=damp(opponent.rotation.y,facing,9,frameDt);
      if(opponentRacket&&b.handR){const wp=new THREE.Vector3(),wq=new THREE.Quaternion();b.handR.getWorldPosition(wp);b.handR.getWorldQuaternion(wq);opponentRacket.position.copy(wp);opponentRacket.quaternion.copy(wq);opponentRacket.rotateX(-.22);opponentRacket.rotateZ(-.12)}
    };
    if(fill)fill.style.width='100%';const loading=document.getElementById('loading');if(loading)loading.style.display='none';const status=document.getElementById('status');if(status)status.textContent='Hazır • gerçek 3D rakip • seyircisiz salon';
  },x=>{if(fill&&x&&x.total)fill.style.width=(28+Math.min(68,Math.round(x.loaded/x.total*68)))+'%'},e=>{console.error('opponent load',e);if(fallback)loadOpponent(fallback,null);else{const status=document.getElementById('status');if(status)status.textContent='Rakip modeli yüklenemedi'}});
}
function boot(){if(!ready()){setTimeout(boot,80);return}removeAllCrowd();loadOpponent('opponent.glb','tennis-player.glb');setTimeout(removeAllCrowd,500);setTimeout(removeAllCrowd,1600)}
boot();
})();
