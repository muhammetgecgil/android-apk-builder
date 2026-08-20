(function(){
'use strict';
function ready(){return window.THREE&&window.scene&&window.loader}
function removeAllCrowd(){
  if(!window.scene)return;
  const gone=[];
  scene.traverse(o=>{
    if(!o||!o.geometry)return;
    const p=o.geometry.parameters||{};
    const primitiveBody=o.isInstancedMesh&&o.geometry.type==='CylinderGeometry'&&p.height>0.30&&p.height<0.60;
    const primitiveHead=o.isInstancedMesh&&o.geometry.type==='SphereGeometry'&&(p.radius||0)>.05&&(p.radius||0)<.14;
    if(primitiveBody||primitiveHead)gone.push(o);
  });
  gone.forEach(o=>{if(o.parent)o.parent.remove(o)});
}
function cleanOldOpponent(){
  try{if(window.opponent&&opponent.parent)opponent.parent.remove(opponent)}catch(e){}
  try{if(window.opponentRacket&&opponentRacket.parent)opponentRacket.parent.remove(opponentRacket)}catch(e){}
  window.opponent=null;window.opponentRacket=null;window.opponentMixer=null;window.opponentBones={};
}
function findBone(root,terms,exclude){let f=null;exclude=exclude||[];root.traverse(o=>{if(f||!o.isBone)return;const n=(o.name||'').toLowerCase();if(terms.some(t=>n.includes(t))&&!exclude.some(t=>n.includes(t)))f=o});return f}
function loadOpponent(){
  const msg=document.getElementById('loadmsg'),fill=document.getElementById('loadfill');
  if(msg)msg.textContent='Fotogerçekçi rakip tenisçi yükleniyor…';if(fill)fill.style.width='30%';
  loader.load('opponent.glb',g=>{
    cleanOldOpponent();removeAllCrowd();
    const model=g.scene;
    model.traverse(o=>{
      if(o.isMesh){
        o.castShadow=true;o.receiveShadow=true;
        if(o.material){
          o.material=o.material.clone();o.material.side=THREE.FrontSide;
          if(o.material.roughness!==undefined)o.material.roughness=Math.max(.28,Math.min(.62,o.material.roughness));
          if(o.material.metalness!==undefined)o.material.metalness=Math.min(.08,o.material.metalness);
          if(o.material.map){o.material.map.anisotropy=4;o.material.map.needsUpdate=true}
          o.material.needsUpdate=true;
        }
      }
    });
    let box=new THREE.Box3().setFromObject(model),h=Math.max(.001,box.max.y-box.min.y);model.scale.setScalar(1.82/h);box=new THREE.Box3().setFromObject(model);model.position.y-=box.min.y;
    const wrap=new THREE.Group();wrap.add(model);wrap.position.set(window.opponentX||0,0,window.opponentZ||-9.55);scene.add(wrap);window.opponent=wrap;
    window.opponentMixer=new THREE.AnimationMixer(model);const actions={};(g.animations||[]).forEach(c=>actions[c.name]=opponentMixer.clipAction(c));
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
    };window.opponentBones=b;
    if(typeof window.createRacket==='function'){window.opponentRacket=createRacket(0x262626);scene.add(opponentRacket)}
    let t=0;
    window.updateOpponentMotion=function(dt){
      if(!window.opponent)return;t+=dt;
      const dx=(window.targetOpponentX||0)-(window.opponentX||0),dz=(window.targetOpponentZ||-9.55)-(window.opponentZ||-9.55);window.oppSpeed=Math.sqrt(dx*dx+dz*dz)/Math.max(dt,.001);
      const speed=Math.min(7,window.oppSpeed||0),walk=Math.min(1,speed/3.6),phase=t*(4.6+speed*.45),s=Math.sin(phase),c=Math.cos(phase);
      const ease=1-Math.exp(-dt*10);
      function rz(x,v){if(x)x.rotation.z+=(v-x.rotation.z)*ease}function rx(x,v){if(x)x.rotation.x+=(v-x.rotation.x)*ease}function ry(x,v){if(x)x.rotation.y+=(v-x.rotation.y)*ease}
      rx(b.thighR,s*.46*walk);rx(b.thighL,-s*.46*walk);rx(b.shinR,Math.max(0,-s)*.48*walk);rx(b.shinL,Math.max(0,s)*.48*walk);
      rx(b.upperR,-s*.22*walk);rx(b.upperL,s*.22*walk);rx(b.lowerR,-.18-.10*walk);rx(b.lowerL,-.12-.08*walk);
      ry(b.spine,Math.max(-.18,Math.min(.18,dx*.045)));rz(b.spine,-Math.max(-.08,Math.min(.08,dx*.018)));ry(b.neck,-Math.max(-.10,Math.min(.10,dx*.02)));
      const turn=Math.max(-.22,Math.min(.22,dx*.085));opponent.rotation.x+=(-Math.min(.055,speed*.004)-opponent.rotation.x)*(1-Math.exp(-dt*7));opponent.rotation.z+=(-turn*.25-opponent.rotation.z)*(1-Math.exp(-dt*8));opponent.rotation.y+=(turn*.18-opponent.rotation.y)*(1-Math.exp(-dt*8));
    };
    if(fill)fill.style.width='100%';const loading=document.getElementById('loading');if(loading)loading.style.display='none';const status=document.getElementById('status');if(status)status.textContent='Hazır • boş tribün • yüksek detaylı gerçek insan rakip';
  },x=>{if(fill&&x&&x.total)fill.style.width=(30+Math.min(65,Math.round(x.loaded/x.total*65)))+'%'},e=>{console.error('opponent load',e);const status=document.getElementById('status');if(status)status.textContent='Rakip modeli yüklenemedi';});
}
function boot(){if(!ready()){setTimeout(boot,120);return}removeAllCrowd();loadOpponent()}
boot();
})();
