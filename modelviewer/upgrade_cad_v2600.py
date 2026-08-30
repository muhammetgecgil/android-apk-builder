from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2600.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2600.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
const BLUE=0x42a5ff,GREEN=0x55d98a;
let mode='',stage='OFF',p1=null,ref=null,geom=[];
const V=()=>new THREE.Vector3();
function span(){return Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1,1)}
function unit(v){const f=typeof unitFactor==='function'?unitFactor():1,u=document.getElementById('unit')?.value||'mm';return (v*f).toFixed(Math.abs(v*f)>=100?2:3)+' '+u}
function dispose(o){if(!o)return;scene.remove(o);if(o.geometry)o.geometry.dispose();if(o.material){(Array.isArray(o.material)?o.material:[o.material]).forEach(m=>{if(m&&m.map)m.map.dispose();if(m&&m.dispose)m.dispose()})}}
function clearGeom(){geom.forEach(dispose);geom=[]}
function add(o){o.renderOrder=1900;scene.add(o);geom.push(o);return o}
function point(p,c){const r=span()*.014,o=new THREE.Mesh(new THREE.SphereGeometry(r,18,12),new THREE.MeshBasicMaterial({color:c,depthTest:false}));o.position.copy(p);return add(o)}
function line(a,b,c){return add(new THREE.Line(new THREE.BufferGeometry().setFromPoints([a,b]),new THREE.LineBasicMaterial({color:c,depthTest:false})))}
function label(p,text,c){const cv=document.createElement('canvas');cv.width=1200;cv.height=280;const x=cv.getContext('2d');x.fillStyle='rgba(3,10,20,.95)';x.fillRect(16,24,1168,232);x.strokeStyle=c===GREEN?'#55d98a':'#42a5ff';x.lineWidth=12;x.strokeRect(16,24,1168,232);x.fillStyle='#fff';x.font='bold 72px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(text,600,140);const tex=new THREE.CanvasTexture(cv),m=new THREE.SpriteMaterial({map:tex,depthTest:false,transparent:true}),s=new THREE.Sprite(m);s.position.copy(p);const z=span()*.18;s.scale.set(z*3.6,z*.84,1);return add(s)}
function hitAt(ev){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function faceVerts(h){if(!h||!h.face||!h.object?.geometry?.attributes?.position)return null;const p=h.object.geometry.attributes.position,f=h.face;return [V().fromBufferAttribute(p,f.a).applyMatrix4(h.object.matrixWorld),V().fromBufferAttribute(p,f.b).applyMatrix4(h.object.matrixWorld),V().fromBufferAttribute(p,f.c).applyMatrix4(h.object.matrixWorld)]}
function closestEdge(h){const a=faceVerts(h);if(!a)return null;function ds(P,A,B){const d=B.clone().sub(A),l2=d.lengthSq();let t=l2?P.clone().sub(A).dot(d)/l2:0;t=Math.max(0,Math.min(1,t));const q=A.clone().addScaledVector(d,t);return{d:P.distanceTo(q),dir:d.normalize(),q,len:A.distanceTo(B)}}const es=[ds(h.point,a[0],a[1]),ds(h.point,a[1],a[2]),ds(h.point,a[2],a[0])].sort((x,y)=>x.d-y.d),e=es[0],mx=Math.max(...es.map(x=>x.len),1e-6);return e.d<mx*.12?e:null}
function makeRef(h){const z=h.face?h.face.normal.clone().transformDirection(h.object.matrixWorld).normalize():new THREE.Vector3(0,0,1),e=closestEdge(h);let x=e?e.dir.clone():null;if(x){x.sub(z.clone().multiplyScalar(x.dot(z)));if(x.lengthSq()<1e-12)x=null}if(!x){let w=Math.abs(z.y)<.92?new THREE.Vector3(0,1,0):new THREE.Vector3(1,0,0);x=w.sub(z.clone().multiplyScalar(w.dot(z))).normalize()}x.normalize();let y=z.clone().cross(x).normalize();x=y.clone().cross(z).normalize();return{o:h.point.clone(),x,y,z,type:e?'KENAR':'YÜZEY',edge:e}}
function showRef(h,r){if(r.edge){const len=span()*.30,s=h.point.clone().addScaledVector(r.x,-len*.5),ar=new THREE.ArrowHelper(r.x,s,len,BLUE,len*.12,len*.06);ar.line.material.depthTest=false;ar.cone.material.depthTest=false;add(ar);point(h.point,BLUE)}else{point(h.point,BLUE);const len=span()*.18,ar=new THREE.ArrowHelper(r.z,h.point.clone(),len,BLUE,len*.12,len*.06);ar.line.material.depthTest=false;ar.cone.material.depthTest=false;add(ar)}}
function result(html,cls){const e=document.getElementById('mgLiveResult2600');if(e){e.className='mg-live-result '+(cls||'');e.innerHTML=html}}
function activate(which){mode=which;stage=which==='point'?'P1':'REF';p1=null;ref=null;clearGeom();document.getElementById('mgPoint2560')?.classList.toggle('mgm-active',which==='point');document.getElementById('mgRef2560')?.classList.toggle('mgm-active',which==='ref');document.getElementById('mgThick2560')?.classList.remove('mgm-active');result(which==='point'?'🟢 <b>1. NOKTAYI SEÇ</b>':'🔷 <b>REFERANS KENAR / YÜZEY SEÇ</b>',which)}
function finishPoint(h){point(h.point,GREEN);line(p1,h.point,GREEN);const d=p1.distanceTo(h.point),mid=p1.clone().add(h.point).multiplyScalar(.5);label(mid,'MESAFE  '+unit(d),GREEN);result('🟢 <b>MESAFE: '+unit(d)+'</b><br><small>Değer parça üzerinde yeşil ölçü çizgisiyle gösteriliyor.</small>','point');stage='DONE';if(navigator.vibrate)navigator.vibrate([20,35,20])}
function finishRef(h){point(h.point,BLUE);line(p1,h.point,BLUE);const d=h.point.clone().sub(p1),d3=d.length(),dx=d.dot(ref.x),dy=d.dot(ref.y),dz=d.dot(ref.z),along=Math.abs(dx),perp=Math.sqrt(Math.max(0,d3*d3-dx*dx)),mid=p1.clone().add(h.point).multiplyScalar(.5);label(mid,'3D '+unit(d3)+' | REF '+unit(along),BLUE);result('🔷 <b>3D: '+unit(d3)+'</b><br>Referans boyunca: <b>'+unit(along)+'</b> • Dik: <b>'+unit(perp)+'</b><br><small>ΔX '+unit(dx)+' • ΔY '+unit(dy)+' • ΔZ '+unit(dz)+'</small>','ref');stage='DONE';if(navigator.vibrate)navigator.vibrate([20,35,20])}
function pick(ev){if((mode!=='point'&&mode!=='ref')||stage==='DONE'||stage==='OFF'||ev.clientY<58)return;const h=hitAt(ev);if(!h)return;ev.preventDefault();ev.stopPropagation();if(mode==='point'){if(stage==='P1'){clearGeom();point(h.point,GREEN);p1=h.point.clone();stage='P2';result('🟢 <b>1. NOKTA SEÇİLDİ</b><br>Şimdi 2. noktayı seç.','point')}else if(stage==='P2')finishPoint(h);return}if(stage==='REF'){clearGeom();ref=makeRef(h);showRef(h,ref);stage='P1';result('🔷 <b>REFERANS '+ref.type+' SEÇİLDİ</b><br>Şimdi 1. noktayı seç.','ref')}else if(stage==='P1'){point(h.point,BLUE);p1=h.point.clone();stage='P2';result('🔷 <b>1. NOKTA SEÇİLDİ</b><br>Şimdi 2. noktayı seç.','ref')}else if(stage==='P2')finishRef(h)}
function txt(b){return (b.textContent||'').replace(/\s+/g,' ').trim().toLocaleUpperCase('tr-TR')}
function findButton(re){return [...document.querySelectorAll('#tools button')].find(b=>re.test(txt(b)))}
function removeEmpty(holder){if(!holder)return;const visible=[...holder.querySelectorAll('button')].some(b=>b.style.display!=='none'&&b.parentElement===holder);if(!visible&&holder.classList.contains('row'))holder.style.display='none'}
function reorganize(){const tools=document.getElementById('tools'),hub=document.getElementById('mgMeasureHub2570'),core=document.getElementById('mgMeasureModes2560');if(!tools||!core)return;
 // Remove the obsolete Engineering Tools title and Smart Measure shortcut.
 [...tools.querySelectorAll('.head')].forEach(h=>{if(txt(h)==='MÜHENDİSLİK ARAÇLARI')h.style.display='none'});
 const smart=findButton(/^AKILLI ÖLÇ$/);if(smart){const p=smart.parentElement;smart.style.display='none';removeEmpty(p)}
 // Place PROBE, HOLES/DIAMETER and 2-POINT together at the top of the measurement area.
 let quick=document.getElementById('mgQuickMeasure2600');if(!quick){quick=document.createElement('div');quick.id='mgQuickMeasure2600';quick.innerHTML='<div class="mgQuickTitle">HIZLI ÖLÇÜM / İNCELEME</div><div class="mgQuickRow"></div>';const host=hub?.querySelector('#mgCore2570')||core.parentElement;host.insertBefore(quick,core);}
 const row=quick.querySelector('.mgQuickRow');
 const hole=findButton(/DELİKLER\s*\/\s*ÇAP|DELİK.*ÇAP/),probe=findButton(/^PROB$/),two=findButton(/^2 NOKTA$/);
 [two,hole,probe].forEach(b=>{if(!b)return;const old=b.parentElement;row.appendChild(b);b.style.display='';removeEmpty(old)});
 if(two){two.textContent='🟢 2 NOKTA';two.onclick=e=>{e.preventDefault();e.stopPropagation();activate('point')}}
 // Live result is always directly below clear button.
 const clear=document.getElementById('mgClear2560');if(clear&&!document.getElementById('mgLiveResult2600')){const r=document.createElement('div');r.id='mgLiveResult2600';r.className='mg-live-result';r.innerHTML='Bir ölçüm seç. Sonuç burada ve parça üzerinde gösterilecek.';const cr=clear.closest('.row');cr.parentNode.insertBefore(r,cr.nextSibling)}
 // Remove old helper text that only said "calculated" without a numeric value.
 const guide=document.getElementById('mgOnModelGuide2570');if(guide)guide.style.display='none';
}
function install(){const s=document.createElement('style');s.textContent=`#mgQuickMeasure2600{margin:0 0 9px;border-bottom:1px solid #245582;padding-bottom:8px}.mgQuickTitle{font-weight:800;color:#8bd8ff;font-size:12px;margin:2px 0 6px}.mgQuickRow{display:grid;grid-template-columns:1fr 1fr;gap:6px}.mgQuickRow button{min-height:48px}.mgQuickRow button:first-child{grid-column:1/-1;border-color:#55d98a;color:#d2f8df}.mg-live-result{margin-top:7px;padding:9px;border:1px solid #35536e;border-radius:9px;background:rgba(3,10,20,.88);font-size:13px;line-height:1.45}.mg-live-result.point{border-color:#55d98a;color:#d2f8df}.mg-live-result.ref{border-color:#42a5ff;color:#cbeaff}`;document.head.appendChild(s);reorganize();const pb=document.getElementById('mgPoint2560'),rb=document.getElementById('mgRef2560'),cb=document.getElementById('mgClear2560'),tb=document.getElementById('mgThick2560');if(pb)pb.onclick=e=>{e.preventDefault();e.stopPropagation();activate('point')};if(rb)rb.onclick=e=>{e.preventDefault();e.stopPropagation();activate('ref')};if(cb)cb.addEventListener('click',()=>{mode='';stage='OFF';p1=null;ref=null;clearGeom();result('Ölçümler temizlendi.','')},true);if(tb)tb.addEventListener('click',()=>{mode='';stage='OFF';clearGeom()},true);canvas.addEventListener('pointerup',pick,true);window.MG_CAD_V2600={version:'2.6.0',baseline:'2.5.5',engineeringToolsRemoved:true,smartMeasureShortcutRemoved:true,probeAndHoleMovedToMeasurementHub:true,twoPointMovedUp:true,greenNumericResultPanel:true,greenOnModelDimension:true,blueReferenceEngineIndependentFromProbe:true}}
ready(install)
})();'''
(AS/'cad-v2600.js').write_text(js,encoding='utf-8')
print('v2.6.0 reorganized measurement hub + guaranteed green/blue numeric on-model results')
