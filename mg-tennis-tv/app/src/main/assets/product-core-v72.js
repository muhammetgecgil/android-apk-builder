(function(){'use strict';
window.MGPC1=window.MGPC1||{};const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));let dressed=false,menuBtn=null;
function opponent(){return window.MGGuaranteedOpponent||window.opponent||null}
function mat(color,rough=.72){return new THREE.MeshStandardMaterial({color,roughness:rough,metalness:.02,side:THREE.DoubleSide})}
function addKit(){if(dressed||!window.THREE||!window.scene)return;const o=opponent();if(!o)return;dressed=true;const kit=new THREE.Group();kit.name='MG_COLOR_TENNIS_KIT_V72';
 const shirt=mat(0x16b8ff,.58),accent=mat(0xff3b78,.55),shorts=mat(0x1726b8,.62),white=mat(0xf5f7ff,.66),shoe=mat(0xffd52a,.58);
 const torso=new THREE.Mesh(new THREE.CylinderGeometry(.31,.38,.64,28),shirt);torso.position.set(0,1.23,0);torso.scale.z=.62;kit.add(torso);
 const collar=new THREE.Mesh(new THREE.TorusGeometry(.13,.025,10,24),accent);collar.position.set(0,1.55,-.13);collar.rotation.x=Math.PI/2;kit.add(collar);
 const stripe=new THREE.Mesh(new THREE.BoxGeometry(.075,.54,.025),accent);stripe.position.set(.18,1.26,-.205);kit.add(stripe);
 const shortsL=new THREE.Mesh(new THREE.BoxGeometry(.27,.34,.27),shorts),shortsR=shortsL.clone();shortsL.position.set(-.15,.82,0);shortsR.position.set(.15,.82,0);kit.add(shortsL,shortsR);
 const sockGeo=new THREE.CylinderGeometry(.09,.095,.22,16);const sl=new THREE.Mesh(sockGeo,white),sr=sl.clone();sl.position.set(-.15,.31,0);sr.position.set(.15,.31,0);kit.add(sl,sr);
 const shoeGeo=new THREE.BoxGeometry(.20,.11,.34);const shl=new THREE.Mesh(shoeGeo,shoe),shr=shl.clone();shl.position.set(-.15,.13,-.06);shr.position.set(.15,.13,-.06);kit.add(shl,shr);
 const band=new THREE.Mesh(new THREE.TorusGeometry(.12,.035,8,22),accent);band.position.set(0,1.78,0);band.rotation.x=Math.PI/2;kit.add(band);
 kit.traverse(x=>{if(x.isMesh){x.castShadow=true;x.receiveShadow=true}});o.add(kit);MGPC1.opponentKit='CYAN_PINK_NAVY_YELLOW_TENNIS';}
function menuButton(){if(document.getElementById('mg72menu')||document.getElementById('mg41'))return;const b=document.createElement('button');b.id='mg72menu';b.textContent='← MODLAR';Object.assign(b.style,{position:'fixed',left:'12px',bottom:'14px',zIndex:9999,padding:'10px 14px',borderRadius:'12px',border:'1px solid rgba(255,255,255,.35)',background:'rgba(3,9,16,.82)',color:'#fff',fontWeight:'800'});b.onclick=()=>{try{if(window.MGProductBackToModes)MGProductBackToModes();else location.reload()}catch(e){location.reload()}};document.body.appendChild(b);menuBtn=b}
function health(){try{addKit();menuButton();if(window.ball&&window.ball.position&&window.ballVel){const p=ball.position,v=ballVel;if(![p.x,p.y,p.z,v.x,v.y,v.z].every(Number.isFinite)){v.set(0,0,0);p.set(0,1.05,0);MGPC1.v72Recovery=(MGPC1.v72Recovery||0)+1}const sp=v.length();if(sp>38){v.multiplyScalar(38/sp);MGPC1.v72SpeedClamp=(MGPC1.v72SpeedClamp||0)+1}}if(document.hidden&&window.ballVel)MGPC1.backgroundSeen='YES'}catch(e){MGPC1.v72Error=String(e)}}
setInterval(health,250);document.addEventListener('visibilitychange',health);
MGPC1.version='7.2.0';MGPC1.productTrack='COLOR_TENNIS_KIT_AND_PRODUCT_POLISH';
})();