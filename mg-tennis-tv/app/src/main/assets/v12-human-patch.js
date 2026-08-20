(function(){
'use strict';
function ready(){try{return !!(window.THREE&&scene&&loader)}catch(e){return false}}
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
function loadOpponent(){
  const msg=document.getElementById('loadmsg'),fill=document.getElementById('loadfill');
  if(msg)msg.textContent='Yüksek detaylı gerçek rakip hazırlanıyor…';if(fill)fill.style.width='28%';
  loader.load('opponent.glb',g=>{
    cleanOldOpponent();removeAllCrowd();
    const model=g.scene;
    model.traverse(o=>{
      if(o.isMesh){
        o.castShadow=true;o.receiveShadow=true;
        if(o.material){
          o.material=o.material.clone();o.material.side=THREE.FrontSide;
          if(o.material.roughness!==undefined)o.material.roughness=Math.max(.30,Math.min(.58,o.material.roughness));
          if(o.material.metalness!==undefined)o.material.metalness=Math.min(.04,o.material.metalness);
          if(o.material.map){o.material.map.anisotropy=Math.min(8,renderer.capabilities.getMaxAnisotropy());o.material.map.needsUpdate=true}
          o.material.needsUpdate=true;
        }
      }
    });
    let box=new THREE.Box3().setFromObject(model),h=Math.max(.001,box.max.y-box.min.y);
    model.scale.setScalar(1.82/h);box=new THREE.Box3().setFromObject(model);model.position.y-=box.min.y;
    const wrap=new THREE.Group();wrap.add(model);wrap.position.set(opponentX||0,0,opponentZ||-9.55);wrap.rotation.y=Math.PI;scene.add(wrap);opponent=wrap;
    opponentMixer=new THREE.AnimationMixer(model);
    const actions={};(g.animations||[]).forEach(c=>actions[c.name]=opponentMixer.clipAction(c));
    const b={
      upperR:findBone(model,['rightarm','upperarm_r','r_upperarm','upperarm.r'],['fore']),
      lowerR:findBone(model,['rightforearm','lowerarm_r','r_forearm','forearm.r']),
      handR:findBone(model,['righthand','hand_r','r_hand','hand.r']),
      upperL:findBone(model,['leftarm','upperarm_l','l_upperarm','upperarm.l'],['fore']),
      lowerL:findBone(model,['leftforearm','lowerarm_l','l_forearm','forearm.l']),
      thighR:findBone(model,['rightupleg','thigh_r','r_thigh','upperleg.r']),
      shinR:findBone(model,['rightleg','calf_r','r_calf','lowerleg.r']),
      thighL:findBone(model,['leftupleg','thigh_l','l_thigh','upperleg.l']),
      shinL:findBone(model,['leftleg','calf_l','l_calf','lowerleg.l']),
      spine:findBone(model,['spine2','spine_03','spine1','spine']),
      pelvis:findBone(model,['hips','pelvis']),
      neck:findBone(model,['neck'])
    };opponentBones=b;
    if(typeof createRacket==='function'){opponentRacket=createRacket(0x202020);scene.add(opponentRacket)}
    let t=0;
    window.updateOpponentMotion=function(dt){
      if(!opponent)return;t+=dt;
      const dx=(targetOpponentX||0)-(opponentX||0),dz=(targetOpponentZ||-9.55)-(opponentZ||-9.55);oppSpeed=Math.sqrt(dx*dx+dz*dz)/Math.max(dt,.001);
      const speed=Math.min(7,oppSpeed||0),walk=Math.min(1,speed/3.6),phase=t*(4.3+speed*.42),s=Math.sin(phase),ease=1-Math.exp(-dt*10);
      function rx(x,v){if(x)x.rotation.x+=(v-x.rotation.x)*ease}function ry(x,v){if(x)x.rotation.y+=(v-x.rotation.y)*ease}function rz(x,v){if(x)x.rotation.z+=(v-x.rotation.z)*ease}
      rx(b.thighR,s*.42*walk);rx(b.thighL,-s*.42*walk);rx(b.shinR,Math.max(0,-s)*.44*walk);rx(b.shinL,Math.max(0,s)*.44*walk);
      rx(b.upperR,-s*.18*walk);rx(b.upperL,s*.18*walk);rx(b.lowerR,-.15-.08*walk);rx(b.lowerL,-.10-.06*walk);
      ry(b.spine,Math.max(-.14,Math.min(.14,dx*.04)));rz(b.spine,-Math.max(-.06,Math.min(.06,dx*.014)));ry(b.neck,-Math.max(-.08,Math.min(.08,dx*.018)));
      const turn=Math.max(-.16,Math.min(.16,dx*.06));opponent.rotation.x+=(-Math.min(.04,speed*.003)-opponent.rotation.x)*(1-Math.exp(-dt*7));opponent.rotation.z+=(-turn*.20-opponent.rotation.z)*(1-Math.exp(-dt*8));
      opponent.rotation.y+=(Math.PI+turn*.12-opponent.rotation.y)*(1-Math.exp(-dt*8));
    };
    if(fill)fill.style.width='100%';const loading=document.getElementById('loading');if(loading)loading.style.display='none';const status=document.getElementById('status');if(status)status.textContent='Hazır • boş tribün • gerçek insan rakip';
  },x=>{if(fill&&x&&x.total)fill.style.width=(28+Math.min(68,Math.round(x.loaded/x.total*68)))+'%'},e=>{console.error('opponent load',e);const status=document.getElementById('status');if(status)status.textContent='Rakip modeli yüklenemedi';});
}
function boot(){if(!ready()){setTimeout(boot,100);return}removeAllCrowd();loadOpponent();setTimeout(removeAllCrowd,800);setTimeout(removeAllCrowd,2200)}
boot();
})();
