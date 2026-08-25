(function(){
'use strict';
function byId(id){return document.getElementById(id)}
function installLock(){
  if(byId('tvGameLock'))return;
  const style=document.createElement('style');
  style.textContent=`#tvGameLock{position:fixed;right:18px;bottom:18px;z-index:80;width:64px;height:64px;border-radius:50%;border:1px solid rgba(255,255,255,.35);background:rgba(5,11,18,.88);color:#fff;font-size:28px;font-weight:900;display:flex;align-items:center;justify-content:center;box-shadow:0 6px 28px rgba(0,0,0,.45);user-select:none;-webkit-user-select:none}body.tv-locked #hud{opacity:.10;pointer-events:none}body.tv-locked #hud button,body.tv-locked #hud select{pointer-events:none}body.tv-locked #promoOverlay{display:none!important}body.tv-locked #status{opacity:.35}body.tv-locked #score{opacity:.96}body.tv-locked #tvGameLock{opacity:.88;pointer-events:auto}#tvLockHint{position:fixed;right:92px;bottom:32px;z-index:79;background:rgba(3,8,13,.72);border:1px solid rgba(255,255,255,.15);border-radius:10px;padding:8px 10px;font:700 11px Arial;color:white;opacity:0;transition:opacity .2s;pointer-events:none}`;
  document.head.appendChild(style);
  const b=document.createElement('div');b.id='tvGameLock';b.textContent='🔒';b.setAttribute('role','button');b.setAttribute('aria-label','TV oyun kilidi');
  const h=document.createElement('div');h.id='tvLockHint';h.textContent='Açmak için 2 sn basılı tut';
  document.body.appendChild(h);document.body.appendChild(b);
  let locked=true,timer=null;
  function apply(){document.body.classList.toggle('tv-locked',locked);b.textContent=locked?'🔒':'🔓';h.style.opacity=locked?'1':'0';setTimeout(()=>h.style.opacity='0',2200)}
  function longStart(e){e.preventDefault();if(timer)clearTimeout(timer);timer=setTimeout(()=>{locked=!locked;apply();try{navigator.vibrate&&navigator.vibrate(35)}catch(x){}},1200)}
  function cancel(){if(timer){clearTimeout(timer);timer=null}}
  ['pointerdown','touchstart','mousedown'].forEach(ev=>b.addEventListener(ev,longStart,{passive:false}));
  ['pointerup','pointercancel','touchend','touchcancel','mouseup','mouseleave'].forEach(ev=>b.addEventListener(ev,cancel,{passive:false}));
  apply();
}
function makeGuaranteedOpponent(){
  try{
    if(!window.THREE||!window.scene)return;
    let existing=null;scene.traverse(o=>{if(existing)return;if(o&&o.name&&(o.name==='ULTRA_REALISTIC_TENNIS_OPPONENT'||o.name==='REAL_TENNIS_OPPONENT'))existing=o});
    if(existing&&existing.visible)return;
    const g=new THREE.Group();g.name='GUARANTEED_HUMAN_OPPONENT';
    const skin=new THREE.MeshStandardMaterial({color:0xc98f6b,roughness:.68,metalness:0});
    const shirt=new THREE.MeshStandardMaterial({color:0xf3f5f7,roughness:.72,metalness:0});
    const shorts=new THREE.MeshStandardMaterial({color:0xe8edf2,roughness:.74,metalness:0});
    const dark=new THREE.MeshStandardMaterial({color:0x20242a,roughness:.58,metalness:.08});
    function mesh(geo,mat,x,y,z){const m=new THREE.Mesh(geo,mat);m.position.set(x,y,z);m.castShadow=true;m.receiveShadow=true;g.add(m);return m}
    mesh(new THREE.CapsuleGeometry(.25,.58,8,16),shirt,0,1.18,0);
    mesh(new THREE.SphereGeometry(.19,20,16),skin,0,1.82,0);
    const hair=mesh(new THREE.SphereGeometry(.195,20,12,0,Math.PI*2,0,Math.PI*.46),dark,0,1.91,-.005);
    const hip=mesh(new THREE.BoxGeometry(.48,.28,.28),shorts,0,.78,0);
    const legGeo=new THREE.CapsuleGeometry(.09,.55,6,12);const armGeo=new THREE.CapsuleGeometry(.07,.48,6,12);
    const l1=mesh(legGeo,skin,-.13,.36,0);const l2=mesh(legGeo,skin,.13,.36,0);l1.rotation.z=.05;l2.rotation.z=-.05;
    const a1=mesh(armGeo,skin,-.32,1.2,0);const a2=mesh(armGeo,skin,.32,1.2,0);a1.rotation.z=.35;a2.rotation.z=-.65;
    const shoeGeo=new THREE.BoxGeometry(.18,.09,.34);mesh(shoeGeo,dark,-.13,.055,.06);mesh(shoeGeo,dark,.13,.055,.06);
    if(typeof createRacket==='function'){const r=createRacket(0x202020);r.scale.setScalar(.92);r.position.set(.56,1.02,-.02);r.rotation.set(0,.2,-.55);g.add(r)}
    g.position.set(0,0,-8.7);g.rotation.y=Math.PI;scene.add(g);
    window.MGGuaranteedOpponent=g;
    if(typeof opponent==='undefined'||!opponent)window.opponent=g;
    if(typeof opponentX!=='undefined')opponentX=0;if(typeof opponentZ!=='undefined')opponentZ=-8.7;
  }catch(e){console.error('guaranteed opponent',e)}
}
function watchdog(){
  installLock();
  setTimeout(makeGuaranteedOpponent,2500);
  setInterval(()=>{try{let visible=false;scene&&scene.traverse(o=>{if(o&&o.visible&&o.name&&(o.name==='ULTRA_REALISTIC_TENNIS_OPPONENT'||o.name==='REAL_TENNIS_OPPONENT'||o.name==='GUARANTEED_HUMAN_OPPONENT'))visible=true});if(!visible)makeGuaranteedOpponent()}catch(e){}},3000);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',watchdog);else watchdog();
})();
