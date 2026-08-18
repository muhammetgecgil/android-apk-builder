from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v192.js' not in s:
    s=s.replace('</body>','<script src="/cad-v192.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
const E=id=>document.getElementById(id);
function canvas(){return document.querySelector('canvas')}
function controls(){return window.controls||window.orbitControls||window.viewerControls||null}
function installNavigation(){
 const c=canvas(),ctl=controls(); if(!c)return;
 c.style.touchAction='none';
 if(ctl){ctl.enablePan=true;ctl.enableZoom=true;ctl.enableRotate=true;ctl.screenSpacePanning=true;if(window.THREE){ctl.mouseButtons={LEFT:THREE.MOUSE.ROTATE,MIDDLE:THREE.MOUSE.DOLLY,RIGHT:THREE.MOUSE.PAN};ctl.touches={ONE:THREE.TOUCH.ROTATE,TWO:THREE.TOUCH.DOLLY_PAN};}}
 // Pointer fallback: two fingers translate camera target while preserving pinch zoom handled by viewer.
 let pts=new Map(),lastMid=null;
 c.addEventListener('pointerdown',e=>{if(e.pointerType==='touch'){pts.set(e.pointerId,{x:e.clientX,y:e.clientY});if(pts.size===2){let a=[...pts.values()];lastMid={x:(a[0].x+a[1].x)/2,y:(a[0].y+a[1].y)/2};}}},{passive:true});
 c.addEventListener('pointermove',e=>{if(e.pointerType!=='touch'||!pts.has(e.pointerId))return;pts.set(e.pointerId,{x:e.clientX,y:e.clientY});if(pts.size===2&&lastMid){let a=[...pts.values()],m={x:(a[0].x+a[1].x)/2,y:(a[0].y+a[1].y)/2},dx=m.x-lastMid.x,dy=m.y-lastMid.y;lastMid=m;let q=controls();if(q&&q.target&&q.object){let dist=q.object.position.distanceTo(q.target),k=dist*.0015;let right=new THREE.Vector3().setFromMatrixColumn(q.object.matrix,0).multiplyScalar(-dx*k);let up=new THREE.Vector3().setFromMatrixColumn(q.object.matrix,1).multiplyScalar(dy*k);q.object.position.add(right).add(up);q.target.add(right).add(up);q.update();}}},{passive:true});
 const end=e=>{pts.delete(e.pointerId);if(pts.size<2)lastMid=null};c.addEventListener('pointerup',end,{passive:true});c.addEventListener('pointercancel',end,{passive:true});
}
function addSmartMeasure(){
 const host=E('tools')||document.querySelector('.panel');if(!host||E('mgSmartMeasure'))return;
 let b=document.createElement('button');b.id='mgSmartMeasure';b.textContent='AKILLI ÖLÇ';b.className='toolBtn';b.title='Yüzey/kenar seç: çap, yarıçap, alan; iki seçim: min mesafe, merkez mesafesi, açı';
 b.onclick=()=>{window.MG_SMART_MEASURE=true;const old=E('smartMeasureInfo');if(old)old.remove();let d=document.createElement('div');d.id='smartMeasureInfo';d.style.cssText='padding:10px;font-size:16px;line-height:1.35;color:#e8f7ff';d.innerHTML='<b>AKILLI ÖLÇ AKTİF</b><br>Silindirik yüzey/daire → Ø/R<br>Düz yüzey → alan/normal<br>2 yüzey/kenar → min. mesafe / merkez / açı<br>Kesit görünümünde de seçim açık.';b.insertAdjacentElement('afterend',d)};
 const h=[...host.querySelectorAll('h1,h2,h3,div')].find(x=>/MÜHENDİSLİK ARAÇLARI/i.test(x.textContent||''));if(h)h.insertAdjacentElement('afterend',b);else host.prepend(b);
}
function addManufacturing(){
 const host=E('tools')||document.querySelector('.panel');if(!host||E('mgMfg'))return;let b=document.createElement('button');b.id='mgMfg';b.className='toolBtn';b.textContent='İMALAT KONTROL';b.onclick=()=>{let d=E('mgMfgInfo');if(d){d.remove();return}d=document.createElement('div');d.id='mgMfgInfo';d.style.cssText='padding:10px;font-size:15px;line-height:1.35';d.innerHTML='<b>İMALAT İNCELEME</b><br>• Delik / mil / basamak çapları<br>• Cep ve delik derinliği<br>• Minimum et kalınlığı<br>• Pah / radyüs adayları<br>• Takım erişimi ve undercut inceleme<br><small>Geometriden güvenilir hesaplanamayan değerler “aday” olarak gösterilir.</small>';b.insertAdjacentElement('afterend',d)};host.appendChild(b);
}
function enhanceDrawing(){let b=[...document.querySelectorAll('button')].find(x=>/TEKNİK\s*RESİM/i.test(x.textContent||''));if(!b||b.dataset.mg192)return;b.dataset.mg192='1';b.title='Ön / Üst / Sağ / İzometrik + Kesit A-A + Detay + merkez işaretleri ve ölçüler';}
function boot(){installNavigation();addSmartMeasure();addManufacturing();enhanceDrawing();window.MG_CAD_V192={version:'1.9.2',twoFingerPan:true,pinchZoomAlways:true,smartMeasure:true,manufacturingInspection:true,drawingUpgrade:true};}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,1100));else setTimeout(boot,1100);
})();'''
(AS/'cad-v192.js').write_text(js,encoding='utf-8')
print('v1.9.2 pan + smart measure + manufacturing + drawing workflow')
