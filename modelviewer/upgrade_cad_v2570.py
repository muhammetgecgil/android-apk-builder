from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2570.js' not in h: h=h.replace('</body>','<script src="/cad-v2570.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
let thickOn=false, thickMarks=[], thickLine=null;
const V=()=>new THREE.Vector3();
function hideOld(){
 ['mgMeasureHub2560'].forEach(id=>{const e=document.getElementById(id);if(e)e.style.display='none'});
 const s=document.getElementById('mgSmartMeasure2550');if(s){let b=s.closest('div');while(b&&b.parentElement&&b.parentElement.id!=='tools')b=b.parentElement;if(b)b.style.display='none'}
 const m=document.getElementById('mB');if(m){const r=m.closest('.row');if(r)r.style.display='none';const h=r&&r.previousElementSibling;if(h&&h.classList.contains('head')&&/ÖLÇÜM/.test(h.textContent))h.style.display='none';const sm=r&&r.nextElementSibling;if(sm&&sm.classList.contains('small'))sm.style.display='none'}
}
function smartOff(){const b=document.getElementById('mgSmartMeasure2550');if(b&&b.classList.contains('on'))b.click()}
function simpleOff(){if(window.measureOn&&window.toggleMeasure)window.toggleMeasure()}
function stopModes(){smartOff();simpleOff();thickOn=false;const t=document.getElementById('mgThickness2570');if(t)t.classList.remove('on');const s=document.getElementById('mgMeasure2570');if(s)s.classList.remove('on')}
function clearThick(){thickMarks.forEach(o=>{scene.remove(o);o.geometry&&o.geometry.dispose();o.material&&o.material.dispose()});thickMarks=[];if(thickLine){scene.remove(thickLine);thickLine.geometry.dispose();thickLine.material.dispose();thickLine=null}}
function mark(p,c){let s=Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1)*.012,o=new THREE.Mesh(new THREE.SphereGeometry(s,14,10),new THREE.MeshBasicMaterial({color:c,depthTest:false}));o.position.copy(p);o.renderOrder=999;scene.add(o);thickMarks.push(o)}
function line(a,b){if(thickLine){scene.remove(thickLine);thickLine.geometry.dispose();thickLine.material.dispose()}thickLine=new THREE.Line(new THREE.BufferGeometry().setFromPoints([a,b]),new THREE.LineBasicMaterial({color:0xffd45c,depthTest:false}));thickLine.renderOrder=998;scene.add(thickLine)}
function unit(v){const f=typeof unitFactor==='function'?unitFactor():1,u=document.getElementById('unit')?.value||'mm';return (v*f).toFixed(Math.abs(v*f)>=100?2:3)+' '+u}
function info(t){const e=document.getElementById('mgSimpleMeasureInfo2570');if(e)e.innerHTML=t}
function hitAt(ev){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function opposite(hit){
 if(!hit.face)return null;
 const n=hit.face.normal.clone().transformDirection(hit.object.matrixWorld).normalize();
 const span=Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1,1),eps=span*1e-5;
 function shoot(dir){
   const rr=new THREE.Raycaster(hit.point.clone().addScaledVector(dir,eps*4),dir,eps,span*4);
   const hs=rr.intersectObjects(group.children.filter(o=>o.visible),true).filter(x=>x.distance>eps*2);
   return hs.length?hs[0]:null;
 }
 const a=shoot(n.clone().negate()),b=shoot(n.clone());
 if(a&&b)return a.distance<=b.distance?a:b;
 return a||b;
}
function install(){
 const tools=document.getElementById('tools');if(!tools||document.getElementById('mgSimpleMeasure2570'))return;hideOld();
 const w=document.createElement('div');w.id='mgSimpleMeasure2570';w.innerHTML='<div class="sep"></div><div class="head">ÖLÇÜM</div><div class="row"><button id="mgMeasure2570">ÖLÇ</button><button id="mgThickness2570">KALINLIK</button><button id="mgClear2570">TEMİZLE</button></div><div class="small" id="mgSimpleMeasureInfo2570"><b>ÖLÇ:</b> referans → başlangıç → bitiş &nbsp; • &nbsp; <b>KALINLIK:</b> yüzeye bir kez dokun</div>';
 tools.appendChild(w);
 const mb=document.getElementById('mgMeasure2570'),tb=document.getElementById('mgThickness2570'),cb=document.getElementById('mgClear2570');
 mb.onclick=e=>{e.preventDefault();clearThick();thickOn=false;tb.classList.remove('on');simpleOff();const b=document.getElementById('mgSmartMeasure2550');if(b&&!b.classList.contains('on'))b.click();mb.classList.add('on');info('<b>ÖLÇÜM:</b> 1) Referans kenar/yüzey seç &nbsp; 2) Başlangıç &nbsp; 3) Bitiş')};
 tb.onclick=e=>{e.preventDefault();smartOff();simpleOff();clearThick();thickOn=!thickOn;tb.classList.toggle('on',thickOn);mb.classList.remove('on');info(thickOn?'<b>KALINLIK:</b> Kumpas gibi ölçmek istediğin yüzeye bir kez dokun. Karşı yüz otomatik bulunur.':'KALINLIK kapalı.')};
 cb.onclick=e=>{e.preventDefault();stopModes();clearThick();if(window.clearMeasure)window.clearMeasure();info('<b>ÖLÇ:</b> referans → başlangıç → bitiş &nbsp; • &nbsp; <b>KALINLIK:</b> yüzeye bir kez dokun')};
 canvas.addEventListener('click',ev=>{if(!thickOn||ev.clientY<58)return;const h=hitAt(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();const o=opposite(h);clearThick();if(!o){mark(h.point,0xff7766);info('<b>Karşı yüz bulunamadı.</b> Kapalı/katı bir bölgenin dış yüzeyine dokun.');return}mark(h.point,0x66ddff);mark(o.point,0xffd45c);line(h.point,o.point);const d=h.point.distanceTo(o.point);info('<b>KALINLIK: '+unit(d)+'</b><br><small>Dokunduğun noktadan yüzey normalinde karşı yüze ölçüldü.</small>');if(navigator.vibrate)navigator.vibrate([20,30,20])},true);
}
function init(){install();window.MG_CAD_V2570={version:'2.5.7',simpleMeasurementUI:true,oneTapThickness:true,caliperLikeThickness:true,normalRayOppositeSurface:true,baseline:'2.5.2'}}
ready(init)
})();'''
(AS/'cad-v2570.js').write_text(js,encoding='utf-8')
print('v2.5.7 simple measure + caliper thickness')
