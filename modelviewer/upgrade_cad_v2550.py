from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2550.js' not in h: h=h.replace('</body>','<script src="/cad-v2550.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
let mode='OFF',ref=null,p1=null,marks=[],line=null;
const V=()=>new THREE.Vector3();
function hitAt(ev){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function closestEdge(hit){const o=hit.object,g=o.geometry,f=hit.face;if(!f||!g||!g.attributes.position)return null;const p=g.attributes.position,A=V().fromBufferAttribute(p,f.a).applyMatrix4(o.matrixWorld),B=V().fromBufferAttribute(p,f.b).applyMatrix4(o.matrixWorld),C=V().fromBufferAttribute(p,f.c).applyMatrix4(o.matrixWorld);function dseg(P,U,W){let d=W.clone().sub(U),t=d.lengthSq()?P.clone().sub(U).dot(d)/d.lengthSq():0;t=Math.max(0,Math.min(1,t));let q=U.clone().addScaledVector(d,t);return{d:P.distanceTo(q),dir:d.normalize(),len:U.distanceTo(W)}}let es=[dseg(hit.point,A,B),dseg(hit.point,B,C),dseg(hit.point,C,A)].sort((a,b)=>a.d-b.d),e=es[0],s=Math.max(es[0].len,es[1].len,es[2].len,1e-6);return e.d<s*.10?e:null}
function makeRef(hit){let z=hit.face&&hit.face.normal?hit.face.normal.clone().transformDirection(hit.object.matrixWorld).normalize():new THREE.Vector3(0,0,1),e=closestEdge(hit),x=e?e.dir.clone():null;if(x){x.sub(z.clone().multiplyScalar(x.dot(z)));if(x.lengthSq()<1e-12)x=null}if(!x){let w=Math.abs(z.y)<.92?new THREE.Vector3(0,1,0):new THREE.Vector3(1,0,0);x=w.sub(z.clone().multiplyScalar(w.dot(z))).normalize()}x.normalize();let y=z.clone().cross(x).normalize();x=y.clone().cross(z).normalize();return{o:hit.point.clone(),x,y,z,type:e?'KENAR':'YÜZEY'}}
function clearGeom(){marks.forEach(o=>{scene.remove(o);o.geometry&&o.geometry.dispose();o.material&&o.material.dispose()});marks=[];if(line){scene.remove(line);line.geometry.dispose();line.material.dispose();line=null}}
function mark(p,col){let s=Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1)*.012,g=new THREE.SphereGeometry(s,14,10),m=new THREE.MeshBasicMaterial({color:col,depthTest:false}),o=new THREE.Mesh(g,m);o.position.copy(p);o.renderOrder=999;scene.add(o);marks.push(o)}
function drawLine(a,b){if(line){scene.remove(line);line.geometry.dispose();line.material.dispose()}let g=new THREE.BufferGeometry().setFromPoints([a,b]),m=new THREE.LineBasicMaterial({color:0xffdf66,depthTest:false});line=new THREE.Line(g,m);line.renderOrder=998;scene.add(line)}
function unit(v){const f=typeof unitFactor==='function'?unitFactor():1,u=document.getElementById('unit')?.value||'mm';return (v*f).toFixed(Math.abs(v*f)>=100?2:3)+' '+u}
function status(t){let e=document.getElementById('mgSmartMeasureInfo2550');if(e)e.innerHTML=t}
function button(t,on){let b=document.getElementById('mgSmartMeasure2550');if(b){b.textContent=t;b.classList.toggle('on',!!on)}}
function start(){clearGeom();ref=null;p1=null;mode='REF';button('REFERANS SEÇ',true);status('1/3 • Ölçüm yönü için <b>kenar veya yüzey</b> seç.');if(window.measureOn&&window.toggleMeasure)window.toggleMeasure()}
function stop(){mode='OFF';button('AKILLI ÖLÇÜM',false);status('Tek buton: Referans → 1. nokta → 2. nokta');p1=null}
function setRef(h){ref=makeRef(h);mode='P1';button('1. NOKTA',true);status('2/3 • Referans: <b>'+ref.type+'</b> • Şimdi ilk noktayı seç.');if(navigator.vibrate)navigator.vibrate(25)}
function setP1(h){clearGeom();p1=h.point.clone();mark(p1,0x66ddff);mode='P2';button('2. NOKTA',true);status('3/3 • İkinci noktayı seç.');if(navigator.vibrate)navigator.vibrate(20)}
function setP2(h){let p2=h.point.clone();mark(p2,0xffdd66);drawLine(p1,p2);let d=p2.clone().sub(p1),d3=d.length(),dx=d.dot(ref.x),dy=d.dot(ref.y),dz=d.dot(ref.z),along=dx,perp=Math.sqrt(Math.max(0,d3*d3-along*along));status('<b>SONUÇ</b><br>3D: <b>'+unit(d3)+'</b><br>Referans boyunca: <b>'+unit(Math.abs(along))+'</b><br>Referansa dik: <b>'+unit(perp)+'</b><br>Local ΔX '+unit(dx)+' • ΔY '+unit(dy)+' • ΔZ '+unit(dz)+'<br><small>Aynı referansla yeni ölçüm için ilk noktayı seç.</small>');p1=null;mode='P1';button('1. NOKTA',true);if(navigator.vibrate)navigator.vibrate([20,35,20])}
function install(){let host=document.getElementById('tools');if(!host||document.getElementById('mgSmartMeasure2550'))return;let box=document.createElement('div');box.innerHTML='<div class="sep"></div><div class="head">AKILLI ÖLÇÜM</div><div class="row"><button id="mgSmartMeasure2550">AKILLI ÖLÇÜM</button><button id="mgSmartMeasureRef2550">REF DEĞİŞTİR</button></div><div class="small" id="mgSmartMeasureInfo2550">Tek buton: Referans → 1. nokta → 2. nokta</div>';host.appendChild(box);document.getElementById('mgSmartMeasure2550').onclick=e=>{e.preventDefault();mode==='OFF'?start():stop()};document.getElementById('mgSmartMeasureRef2550').onclick=e=>{e.preventDefault();if(mode==='OFF')start();else{clearGeom();ref=null;p1=null;mode='REF';button('REFERANS SEÇ',true);status('Referansı değiştir: kenar veya yüzey seç.')}};canvas.addEventListener('click',ev=>{if(mode==='OFF'||ev.clientY<58)return;let h=hitAt(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();if(mode==='REF')setRef(h);else if(mode==='P1')setP1(h);else if(mode==='P2')setP2(h)},true)}
function init(){install();window.MG_CAD_V2550={version:'2.5.5',smartReferenceMeasurement:true,edgeOrFaceReference:true,localXYZ:true,alongPerpendicular:true,easyThreeStepFlow:true,baseline:'2.5.2'}}ready(init)
})();'''
(AS/'cad-v2550.js').write_text(js,encoding='utf-8')
print('v2.5.5 easy smart reference measurement')
