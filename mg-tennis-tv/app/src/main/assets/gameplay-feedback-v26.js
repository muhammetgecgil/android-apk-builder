(function(){
'use strict';
let overlay=null, marker=null, prevY=99, prevVy=0, lastHit='';
function mount(){
 if(!overlay){overlay=document.createElement('div');overlay.id='matchFeedback';overlay.style.cssText='position:fixed;left:50%;top:24%;transform:translate(-50%,-50%);z-index:99;pointer-events:none;padding:14px 22px;border-radius:14px;background:rgba(0,0,0,.72);color:white;font:900 28px system-ui;text-align:center;opacity:0;transition:opacity .15s;text-shadow:0 2px 4px #000';document.body.appendChild(overlay)}
}
function flash(t,ms){mount();overlay.textContent=t;overlay.style.opacity='1';clearTimeout(overlay._t);overlay._t=setTimeout(()=>overlay.style.opacity='0',ms||1100)}
function mark(x,z){try{if(!window.THREE||!scene)return;if(marker&&marker.parent)marker.parent.remove(marker);const g=new THREE.RingGeometry(.24,.38,40);const m=new THREE.MeshBasicMaterial({color:0xffd500,side:THREE.DoubleSide,transparent:true,opacity:.95});marker=new THREE.Mesh(g,m);marker.rotation.x=-Math.PI/2;marker.position.set(x,.012,z);scene.add(marker);setTimeout(()=>{if(marker&&marker.parent)marker.parent.remove(marker)},1400)}catch(e){}}
setInterval(()=>{
 try{
  if(!ball||!ballVel)return;
  const landed=prevY>BALL_R+.03&&ball.position.y<=BALL_R+.07&&prevVy<0;
  if(landed){mark(ball.position.x,ball.position.z);flash('TOP DÜŞTÜ • '+(ball.position.x<-.6?'SOL':ball.position.x>.6?'SAĞ':'ORTA'),850)}
  if(lastHit&&lastHitter&&lastHitter!==lastHit){if(lastHitter==='opponent')flash('RAKİP KARŞILADI',850);else flash('VURUŞUN ALINDI',700)}
  lastHit=lastHitter||lastHit;prevY=ball.position.y;prevVy=ballVel.y;
 }catch(e){}
},30);
setInterval(()=>{
 const s=document.getElementById('status');if(!s)return;const t=s.textContent||'';if(s._last===t)return;s._last=t;
 if(/Sayı senin|Game senin|MAÇ SENİN/.test(t))flash('SAYI SENİN'+(t.includes('OUT')?' • OUT':''),1500);
 else if(/Sayı rakibin|Game rakibin|MAÇ RAKİBİN/.test(t))flash('SAYI RAKİBİN',1500);
 else if(/Çift hata/.test(t))flash('ÇİFT HATA',1400);
 else if(/OUT/.test(t))flash('OUT',1200);
 else if(/NET/.test(t))flash('NET',1200);
},80);
})();
