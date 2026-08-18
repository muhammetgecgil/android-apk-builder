from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v189.js' not in s:
    s=s.replace('</body>','<script src="/cad-v189.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
function E(id){return document.getElementById(id)}
function isUiTarget(t){return !!(t&&t.closest&&t.closest('button,select,input,.panel,#top,#tools,#info,#mgDrawingBar'))}

// 1) Make the bottom command group move left by approximately one +/- button width.
function shiftBottomCommandBar(){
 const d=E('autoDimB');if(!d||!d.parentElement)return false;
 const row=d.parentElement;row.style.transform='translateX(-52px)';row.style.transformOrigin='center bottom';row.style.transition='transform .15s ease';
 row.style.maxWidth='calc(100vw - 250px)';
 return true;
}

// 2) Readable local thickness result. Keep the side result large and add one clear in-view label.
let thicknessBadge=null,lastPointer={x:innerWidth*.5,y:innerHeight*.5};
function removeThicknessBadge(){if(thicknessBadge){thicknessBadge.remove();thicknessBadge=null}}
function showThicknessBadge(text,x,y){
 removeThicknessBadge();const b=document.createElement('div');thicknessBadge=b;b.id='mgThicknessBadge';b.textContent=text;
 b.style.cssText='position:fixed;z-index:40;pointer-events:none;background:rgba(3,10,20,.96);color:#ffe477;border:2px solid #ffd84d;border-radius:9px;padding:7px 10px;font:bold 18px Arial;line-height:1.1;white-space:nowrap;box-shadow:0 3px 12px rgba(0,0,0,.55)';
 document.body.appendChild(b);const nx=Math.min(innerWidth-b.offsetWidth-12,Math.max(12,x+14)),ny=Math.min(innerHeight-b.offsetHeight-12,Math.max(70,y+14));b.style.left=nx+'px';b.style.top=ny+'px';
}
function installReadableThickness(){
 window.addEventListener('pointerdown',e=>{lastPointer={x:e.clientX,y:e.clientY}},true);
 const out=E('proResult');if(out){const mo=new MutationObserver(()=>{const txt=(out.textContent||'').trim();if(/KALINLIK/i.test(txt)){out.style.fontSize='16px';out.style.lineHeight='1.45';out.style.fontWeight='700';const m=txt.match(/KALINLIK\s*[≈=:]?\s*([0-9.,]+\s*(?:mm|cm|m|inch|ft)?)/i);if(m)showThicknessBadge('KALINLIK ≈ '+m[1],lastPointer.x,lastPointer.y)}});mo.observe(out,{subtree:true,childList:true,characterData:true})}
 const tb=E('thickB');if(tb){const old=tb.onclick;tb.onclick=function(e){removeThicknessBadge();if(old)old.call(this,e)}}
}

// 3) Too many dimensions: double tap/double click any individual dimension to hide it.
function hideDomDimensionTarget(t){const n=t&&t.closest?t.closest('.mgDimLabel,[data-auto-hole],.mg-dim-label'):null;if(n){n.style.display='none';return true}return false}
let taps=[];
function nearestSpriteAt(x,y,maxPx){
 const arr=[];try{(window.autoDiaSprites||[]).forEach(s=>arr.push(s));(window.diaLabels||[]).forEach(s=>arr.push(s))}catch(e){}
 const r=canvas.getBoundingClientRect();let best=null,bd=maxPx||42;arr.forEach(sp=>{if(!sp||sp.visible===false)return;const p=sp.position.clone().project(camera),sx=r.left+(p.x*.5+.5)*r.width,sy=r.top+(-p.y*.5+.5)*r.height,d=Math.hypot(sx-x,sy-y);if(d<bd){bd=d;best=sp}});return best
}
function installDimensionDismiss(){
 document.addEventListener('dblclick',e=>{if(hideDomDimensionTarget(e.target)){e.preventDefault();e.stopPropagation();return}const sp=nearestSpriteAt(e.clientX,e.clientY,50);if(sp){sp.visible=false;e.preventDefault();e.stopPropagation()}},true);
 document.addEventListener('pointerup',e=>{if(e.pointerType!=='touch'&&e.pointerType!=='pen')return;const now=performance.now();taps=taps.filter(a=>now-a.t<420);const prev=taps.find(a=>Math.hypot(a.x-e.clientX,a.y-e.clientY)<28);if(prev){if(hideDomDimensionTarget(e.target)){taps=[];return}const sp=nearestSpriteAt(e.clientX,e.clientY,55);if(sp){sp.visible=false;taps=[];e.preventDefault();e.stopImmediatePropagation();return}}taps.push({t:now,x:e.clientX,y:e.clientY})},true);
}

// 4) Pinch zoom must work even if another measurement tool disables OrbitControls.
const touches=new Map();let pinchDist=null;
function cameraDolly(scale){try{const t=controls.target.clone(),off=camera.position.clone().sub(t),len=off.length();if(!isFinite(len)||len<=0)return;const nl=Math.max(camera.near*8,Math.min(camera.far*.25,len*scale));off.setLength(nl);camera.position.copy(t).add(off);camera.lookAt(t);controls.update()}catch(e){}}
function installAbsolutePinchZoom(){
 window.addEventListener('pointerdown',e=>{if(e.pointerType==='touch'){touches.set(e.pointerId,{x:e.clientX,y:e.clientY});if(touches.size===2){const a=[...touches.values()];pinchDist=Math.hypot(a[0].x-a[1].x,a[0].y-a[1].y)}}},true);
 window.addEventListener('pointermove',e=>{if(e.pointerType!=='touch'||!touches.has(e.pointerId))return;touches.set(e.pointerId,{x:e.clientX,y:e.clientY});if(touches.size>=2){const a=[...touches.values()].slice(0,2),d=Math.hypot(a[0].x-a[1].x,a[0].y-a[1].y);if(pinchDist&&d>4){cameraDolly(pinchDist/d);e.preventDefault()}pinchDist=d}},true);
 const end=e=>{if(e.pointerType==='touch'){touches.delete(e.pointerId);if(touches.size<2)pinchDist=null}};window.addEventListener('pointerup',end,true);window.addEventListener('pointercancel',end,true);
 setInterval(()=>{try{controls.enableZoom=true;if(THREE.TOUCH)controls.touches.TWO=THREE.TOUCH.DOLLY_PAN}catch(e){}},250)
}

// 5) S-Pen can orbit the model even when engineering measurement modes are active.
let pen=null;
function penOrbit(dx,dy){try{const t=controls.target.clone(),off=camera.position.clone().sub(t),s=new THREE.Spherical().setFromVector3(off);s.theta-=dx*.0075;s.phi=Math.max(.035,Math.min(Math.PI-.035,s.phi-dy*.0075));off.setFromSpherical(s);camera.position.copy(t).add(off);camera.lookAt(t);controls.update()}catch(e){}}
function installPenOrbit(){
 window.addEventListener('pointerdown',e=>{if(e.pointerType==='pen'&&!isUiTarget(e.target)){pen={id:e.pointerId,x:e.clientX,y:e.clientY}}},true);
 window.addEventListener('pointermove',e=>{if(!pen||e.pointerType!=='pen'||e.pointerId!==pen.id)return;const dx=e.clientX-pen.x,dy=e.clientY-pen.y;pen.x=e.clientX;pen.y=e.clientY;if(Math.abs(dx)+Math.abs(dy)>1){penOrbit(dx,dy);e.preventDefault();e.stopImmediatePropagation()}},true);
 const end=e=>{if(pen&&e.pointerId===pen.id)pen=null};window.addEventListener('pointerup',end,true);window.addEventListener('pointercancel',end,true)
}

// 6) CAM/CNC is removed; replace it with dedicated ZOOM - / ZOOM + controls.
function installZoomButtons(){
 const cam=E('modeCam');if(!cam)return false;const parent=cam.parentElement;if(!parent)return false;
 const minus=document.createElement('button'),plus=document.createElement('button');minus.id='mgZoomMinus';plus.id='mgZoomPlus';minus.textContent='ZOOM −';plus.textContent='ZOOM +';
 minus.onclick=()=>cameraDolly(1.18);plus.onclick=()=>cameraDolly(.84);
 parent.insertBefore(minus,cam);parent.insertBefore(plus,cam);cam.remove();
 // if the hole-table button shares the row, keep it on a separate full row so zoom buttons remain readable.
 const hole=E('modeHole');if(hole&&hole.parentElement===parent){const r=document.createElement('div');r.className='row';parent.parentElement.insertBefore(r,parent.nextSibling);r.appendChild(hole)}
 return true
}

function boot(){shiftBottomCommandBar();installReadableThickness();installDimensionDismiss();installAbsolutePinchZoom();installPenOrbit();installZoomButtons();let n=0;const t=setInterval(()=>{n++;shiftBottomCommandBar();if(installZoomButtons()||n>16)clearInterval(t)},300);window.MG_CAD_V189={version:'1.8.9',readableThickness:true,bottomShiftLeft:true,doubleTapHideDimension:true,pinchZoomAbsolute:true,penOrbitAbsolute:true,camRemovedZoomButtons:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,780));else setTimeout(boot,780);
})();'''
(AS/'cad-v189.js').write_text(js,encoding='utf-8')
print('v1.8.9 readable thickness + left command bar + double-tap dismiss + absolute pinch/pen navigation + zoom buttons applied')
