from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v188.js' not in s:
    s=s.replace('</body>','<script src="/cad-v188.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
function E(id){return document.getElementById(id)}

// 1) Compact automatic diameter labels and tap-to-dismiss.
function compactDiaLabels(){
 const arr=window.autoDiaSprites||[];
 arr.forEach(sp=>{
   if(!sp||sp.userData&&sp.userData.mg188)return;
   sp.userData=sp.userData||{};sp.userData.mg188=true;
   // Diameter callouts are intentionally half the visual size of the main X/Y/Z dimensions.
   sp.scale.multiplyScalar(.52);
 });
}
function dismissNearestDiameter(ev){
 const arr=window.autoDiaSprites||[];if(!arr.length)return;
 const r=canvas.getBoundingClientRect(),x=ev.clientX-r.left,y=ev.clientY-r.top;
 let best=null,bestD=34;
 arr.forEach(sp=>{if(!sp||sp.visible===false)return;const p=sp.position.clone().project(camera);const sx=(p.x*.5+.5)*r.width,sy=(-p.y*.5+.5)*r.height,d=Math.hypot(sx-x,sy-y);if(d<bestD){bestD=d;best=sp}});
 if(best){best.visible=false;ev.preventDefault();ev.stopImmediatePropagation()}
}
function installDiaBehavior(){
 const old=window.autoDiameters;
 if(typeof old==='function'&&!old.__mg188){const f=function(){const z=old.apply(this,arguments);setTimeout(compactDiaLabels,30);return z};f.__mg188=true;window.autoDiameters=f}
 canvas.addEventListener('click',dismissNearestDiameter,true);
 setInterval(compactDiaLabels,700);
}

// 2) Analysis restore button must remain in the bottom command row without covering neighbours.
function fixAnalysisRestoreLayout(){
 const r=E('mgAnalysisRestore'),d=E('autoDimB');if(!r||!d)return false;
 const row=d.parentElement;if(!row)return false;
 row.style.display='flex';row.style.alignItems='center';row.style.gap='6px';row.style.flexWrap='nowrap';
 r.style.position='static';r.style.left='auto';r.style.right='auto';r.style.bottom='auto';r.style.transform='none';r.style.zIndex='8';
 r.style.flex='0 0 92px';r.style.minWidth='92px';r.style.maxWidth='92px';r.style.padding='7px 6px';r.style.fontSize='13px';
 d.style.flex='1 1 150px';d.style.minWidth='138px';
 // Keep the section and axis controls in their own space and prevent visual overlap on narrow landscape screens.
 Array.from(row.children).forEach(x=>{if(x!==d&&x!==r){x.style.position='static';x.style.transform='none';x.style.zIndex='8'}});
 return true;
}

// 3) Always-available navigation: finger pinch zoom and S-Pen orbit, even while measurement tools are active.
let penDrag=null;
function orbitBy(dx,dy){
 try{
  const target=controls.target.clone(),off=camera.position.clone().sub(target),sph=new THREE.Spherical().setFromVector3(off);
  sph.theta-=dx*.008;sph.phi=Math.max(.03,Math.min(Math.PI-.03,sph.phi-dy*.008));
  off.setFromSpherical(sph);camera.position.copy(target).add(off);camera.lookAt(target);controls.update();
 }catch(e){}
}
function installPersistentNavigation(){
 try{controls.enableZoom=true;controls.enablePan=true;if(THREE.TOUCH){controls.touches.ONE=THREE.TOUCH.ROTATE;controls.touches.TWO=THREE.TOUCH.DOLLY_PAN}canvas.style.touchAction='none'}catch(e){}
 canvas.addEventListener('pointerdown',e=>{if(e.pointerType==='pen'){penDrag={id:e.pointerId,x:e.clientX,y:e.clientY};try{canvas.setPointerCapture(e.pointerId)}catch(_){}}},true);
 canvas.addEventListener('pointermove',e=>{if(!penDrag||e.pointerType!=='pen'||e.pointerId!==penDrag.id)return;const dx=e.clientX-penDrag.x,dy=e.clientY-penDrag.y;penDrag.x=e.clientX;penDrag.y=e.clientY;if(Math.abs(dx)+Math.abs(dy)>0){orbitBy(dx,dy);e.preventDefault();e.stopImmediatePropagation()}},true);
 const end=e=>{if(penDrag&&e.pointerId===penDrag.id)penDrag=null};canvas.addEventListener('pointerup',end,true);canvas.addEventListener('pointercancel',end,true);
 // Other tools historically disable rotate/zoom. Zoom must never be disabled.
 setInterval(()=>{try{controls.enableZoom=true;controls.enablePan=true;if(THREE.TOUCH)controls.touches.TWO=THREE.TOUCH.DOLLY_PAN}catch(e){}},350);
}

function boot(){installDiaBehavior();fixAnalysisRestoreLayout();installPersistentNavigation();let n=0;const t=setInterval(()=>{n++;if(fixAnalysisRestoreLayout()||n>20)clearInterval(t)},250);window.MG_CAD_V188={version:'1.8.8',compactDiameterLabels:true,tapDismissDiameter:true,analysisNoOverlap:true,pinchZoomAlways:true,penOrbitAlways:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,700));else setTimeout(boot,700);
})();'''
(AS/'cad-v188.js').write_text(js,encoding='utf-8')
print('v1.8.8 compact diameter labels + tap dismiss + analysis no-overlap + persistent touch navigation applied')
