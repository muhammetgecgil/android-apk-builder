(function(){
'use strict';
function ready(){return window.THREE&&window.scene&&window.loader}
function removePrimitiveCrowd(){
  if(!window.scene)return;
  const gone=[];
  scene.children.forEach(o=>{
    if(!o||!o.isInstancedMesh||!o.geometry)return;
    const p=o.geometry.parameters||{};
    const isBody=o.geometry.type==='CylinderGeometry'&&p.height>0.38&&p.height<0.46&&(p.radiusTop||0)>.07;
    const isHead=o.geometry.type==='SphereGeometry'&&(p.radius||0)>.06&&(p.radius||0)<.11;
    if(isBody||isHead)gone.push(o);
  });
  gone.forEach(o=>scene.remove(o));
}
function cleanOldOpponent(){
  try{if(window.opponent&&opponent.parent)opponent.parent.remove(opponent)}catch(e){}
  try{if(window.opponentRacket&&opponentRacket.parent)opponentRacket.parent.remove(opponentRacket)}catch(e){}
  window.opponent=null;window.opponentRacket=null;window.opponentMixer=null;window.opponentBones={};
}
const actions={};let current=null;
function setAction(name,fade){const n=actions[name];if(!n||n===current)return;if(current)current.fadeOut(fade||.18);n.reset().fadeIn(fade||.18).play();current=n}
function findBone(root,terms,exclude){let f=null;exclude=exclude||[];root.traverse(o=>{if(f||!o.isBone)return;const n=(o.name||'').toLowerCase();if(terms.some(t=>n.includes(t))&&!exclude.some(t=>n.includes(t)))f=o});return f}
function loadOpponent(){
  const msg=document.getElementById('loadmsg'),fill=document.getElementById('loadfill');
  if(msg)msg.textContent='Gerçek rakip tenisçi yükleniyor…';if(fill)fill.style.width='35%';
  loader.load('opponent.glb',g=>{
    cleanOldOpponent();
    const model=g.scene;
    model.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;if(o.material){o.material=o.material.clone();o.material.side=THREE.FrontSide;o.material.roughness=Math.min(.76,o.material.roughness===undefined?.62:o.material.roughness);o.material.needsUpdate=true}}});
    let box=new THREE.Box3().setFromObject(model),h=Math.max(.001,box.max.y-box.min.y);model.scale.setScalar(1.82/h);box=new THREE.Box3().setFromObject(model);model.position.y-=box.min.y;
    const wrap=new THREE.Group();wrap.add(model);wrap.position.set(window.opponentX||0,0,window.opponentZ||-9.55);scene.add(wrap);window.opponent=wrap;
    window.opponentMixer=new THREE.AnimationMixer(model);(g.animations||[]).forEach(c=>actions[c.name]=opponentMixer.clipAction(c));setAction(actions.Idle?'Idle':Object.keys(actions)[0],0);
    window.opponentBones={upperR:findBone(model,['rightarm','upperarm_r','r_upperarm'],['fore']),lowerR:findBone(model,['rightforearm','lowerarm_r','r_forearm']),handR:findBone(model,['righthand','hand_r','r_hand']),clavR:findBone(model,['rightshoulder','clavicle_r','r_clavicle']),spine:findBone(model,['spine2','spine_03','spine1','spine']),pelvis:findBone(model,['hips','pelvis'])};
    if(typeof window.createRacket==='function'){window.opponentRacket=createRacket(0x91a915);scene.add(opponentRacket)}
    window.updateOpponentMotion=function(dt){if(!window.opponent)return;const dx=(window.targetOpponentX||0)-(window.opponentX||0),dz=(window.targetOpponentZ||-9.55)-(window.opponentZ||-9.55);window.oppSpeed=Math.sqrt(dx*dx+dz*dz)/Math.max(dt,.001);if(oppSpeed>3.2&&actions.Run)setAction('Run');else if(oppSpeed>.45&&actions.Walk)setAction('Walk');else if(actions.Idle)setAction('Idle');if(current)current.timeScale=Math.max(.72,Math.min(1.55,.7+oppSpeed*.11));const turn=Math.max(-.20,Math.min(.20,dx*.09));opponent.rotation.x+=( -Math.min(.045,oppSpeed*.002)-opponent.rotation.x)*(1-Math.exp(-dt*7));opponent.rotation.z+=(-turn*.35-opponent.rotation.z)*(1-Math.exp(-dt*8));opponent.rotation.y+=(turn*.20-opponent.rotation.y)*(1-Math.exp(-dt*8));};
    if(fill)fill.style.width='100%';const loading=document.getElementById('loading');if(loading)loading.style.display='none';const status=document.getElementById('status');if(status)status.textContent='Hazır • rigged gerçek rakip • gerçek insan seyirciler';
    loadCrowd();
  },x=>{if(fill&&x&&x.total)fill.style.width=(35+Math.min(55,Math.round(x.loaded/x.total*55)))+'%'},e=>{console.error('opponent load',e);const status=document.getElementById('status');if(status)status.textContent='Rakip modeli yüklenemedi';});
}
function loadCrowd(){
  loader.load('spectator.glb',g=>{
    const base=g.scene;base.traverse(o=>{if(o.isMesh){o.castShadow=false;o.receiveShadow=true;if(o.material)o.material=o.material.clone()}});
    let n=0;
    for(let side=-1;side<=1;side+=2)for(let row=0;row<4;row++)for(let i=0;i<10;i++){
      const c=base.clone(true);let b=new THREE.Box3().setFromObject(c),h=Math.max(.001,b.max.y-b.min.y);c.scale.setScalar((1.60+(n%6)*.04)/h);c.traverse(o=>{if(o.isMesh&&o.material){o.material=o.material.clone();o.material.color.multiplyScalar(.88+(n%5)*.025)}});b=new THREE.Box3().setFromObject(c);c.position.y-=b.min.y;
      const w=new THREE.Group();w.add(c);w.position.set(side*(7.45+row*.72),row*.38,-10.4+i*2.3);w.rotation.y=side>0?-Math.PI/2:Math.PI/2;w.rotation.z=((n%3)-1)*.02;scene.add(w);n++;
    }
  },undefined,e=>console.warn('crowd load',e));
}
function boot(){if(!ready()){setTimeout(boot,120);return}removePrimitiveCrowd();loadOpponent()}
boot();
})();
