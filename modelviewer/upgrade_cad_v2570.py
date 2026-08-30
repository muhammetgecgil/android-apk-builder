from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2570.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2570.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
const BLUE=0x42a5ff,GREEN=0x55d98a,YELLOW=0xffd34d;
let guideGeom=[],pointStep=0,lastMode='';
const V=()=>new THREE.Vector3();
function dispose(o){if(!o)return;scene.remove(o);if(o.geometry)o.geometry.dispose();if(o.material){const a=Array.isArray(o.material)?o.material:[o.material];a.forEach(m=>m&&m.dispose&&m.dispose())}}
function clearGuides(){guideGeom.forEach(dispose);guideGeom=[];pointStep=0}
function add(o){o.renderOrder=1200;scene.add(o);guideGeom.push(o);return o}
function span(){return Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1,1)}
function hitAt(ev){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function faceVerts(h){if(!h||!h.face||!h.object||!h.object.geometry||!h.object.geometry.attributes.position)return null;const p=h.object.geometry.attributes.position,f=h.face;return [V().fromBufferAttribute(p,f.a).applyMatrix4(h.object.matrixWorld),V().fromBufferAttribute(p,f.b).applyMatrix4(h.object.matrixWorld),V().fromBufferAttribute(p,f.c).applyMatrix4(h.object.matrixWorld)]}
function faceNormal(h){return h&&h.face?h.face.normal.clone().transformDirection(h.object.matrixWorld).normalize():new THREE.Vector3(0,0,1)}
function showFace(h,color){const a=faceVerts(h);if(!a)return;const g=new THREE.BufferGeometry().setFromPoints([a[0],a[1],a[2]]);g.setIndex([0,1,2]);g.computeVertexNormals();const m=new THREE.MeshBasicMaterial({color,transparent:true,opacity:.40,side:THREE.DoubleSide,depthTest:false,depthWrite:false});add(new THREE.Mesh(g,m));const edges=new THREE.BufferGeometry().setFromPoints([a[0],a[1],a[1],a[2],a[2],a[0]]);add(new THREE.LineSegments(edges,new THREE.LineBasicMaterial({color,depthTest:false})))}
function showPoint(p,color){const r=span()*.013;const s=new THREE.Mesh(new THREE.SphereGeometry(r,18,12),new THREE.MeshBasicMaterial({color,depthTest:false}));s.position.copy(p);add(s)}
function closestEdge(h){const a=faceVerts(h);if(!a)return null;function dseg(P,A,B){const d=B.clone().sub(A),l2=d.lengthSq();let t=l2?P.clone().sub(A).dot(d)/l2:0;t=Math.max(0,Math.min(1,t));const q=A.clone().addScaledVector(d,t);return{d:P.distanceTo(q),dir:d.normalize(),len:A.distanceTo(B),q}}const es=[dseg(h.point,a[0],a[1]),dseg(h.point,a[1],a[2]),dseg(h.point,a[2],a[0])].sort((x,y)=>x.d-y.d),e=es[0],mx=Math.max(es[0].len,es[1].len,es[2].len,1e-6);return e.d<mx*.10?e:null}
function showAxis(h,color){const e=closestEdge(h);if(!e)return false;const len=span()*.28,dir=e.dir.clone(),start=h.point.clone().addScaledVector(dir,-len*.50);const ar=new THREE.ArrowHelper(dir,start,len,color,len*.12,len*.06);ar.line.material.depthTest=false;ar.cone.material.depthTest=false;add(ar);showPoint(h.point,color);return true}
function showFaceReference(h,color){showFace(h,color);showPoint(h.point,color);const n=faceNormal(h),len=span()*.18,ar=new THREE.ArrowHelper(n,h.point.clone(),len,color,len*.12,len*.06);ar.line.material.depthTest=false;ar.cone.material.depthTest=false;add(ar)}
function guideText(t,colorClass){let e=document.getElementById('mgOnModelGuide2570');if(e){e.className='mg-guide '+(colorClass||'');e.innerHTML=t}}
function mode(){if(document.getElementById('mgRef2560')?.classList.contains('mgm-active'))return'ref';if(document.getElementById('mgPoint2560')?.classList.contains('mgm-active'))return'point';if(document.getElementById('mgThick2560')?.classList.contains('mgm-active'))return'thick';return''}
function smartStage(){const b=document.getElementById('mgSmartMeasure2550');return (b&&b.textContent||'').trim().toLocaleUpperCase('tr-TR')}
function onPointer(ev){if(ev.clientY<58)return;const md=mode();if(md!==lastMode){clearGuides();lastMode=md}if(md!=='ref'&&md!=='point')return;const h=hitAt(ev);if(!h)return;if(md==='ref'){
 const st=smartStage();
 if(st.includes('REFERANS')){clearGuides();if(showAxis(h,BLUE))guideText('🔷 <b>EKSEN SEÇİLDİ</b> — mavi ok seçilen referans yönünü gösteriyor. Şimdi 1. noktayı seç.','blue');else{showFaceReference(h,BLUE);guideText('🔷 <b>YÜZEY REFERANSI SEÇİLDİ</b> — mavi yüzey ve normal oku aktif referansı gösteriyor. Şimdi 1. noktayı seç.','blue')}}
 else if(st.includes('1. NOKTA')){showFace(h,BLUE);showPoint(h.point,BLUE);guideText('🔷 <b>1. NOKTA / YÜZEY SEÇİLDİ</b> — şimdi ikinci yüzey veya noktayı seç.','blue')}
 else if(st.includes('2. NOKTA')){showFace(h,BLUE);showPoint(h.point,BLUE);guideText('🔷 <b>2. NOKTA / YÜZEY SEÇİLDİ</b> — ölçüm sonucu hesaplanıyor.','blue')}
 }else if(md==='point'){
  if(pointStep===0){clearGuides();showFace(h,GREEN);showPoint(h.point,GREEN);pointStep=1;guideText('🟢 <b>1. YÜZEY / NOKTA SEÇİLDİ</b> — yeşil alan seçimi gösteriyor. Şimdi ikinci yüzeyi seç.','green')}
  else{showFace(h,GREEN);showPoint(h.point,GREEN);pointStep=0;guideText('🟢 <b>2. YÜZEY / NOKTA SEÇİLDİ</b> — mesafe hesaplandı.','green')}
 }
}
function gatherMeasurements(){const tools=document.getElementById('tools'),core=document.getElementById('mgMeasureModes2560');if(!tools||!core||document.getElementById('mgMeasureHub2570'))return;const hub=document.createElement('section');hub.id='mgMeasureHub2570';hub.innerHTML='<div class="head mgHubTitle">📐 TÜM ÖLÇÜMLER</div><div id="mgCore2570"></div><div id="mgOtherWrap2570" style="display:none"><div class="sep"></div><div class="head">DİĞER / OTOMATİK ÖLÇÜMLER</div><div id="mgOther2570" class="mg-other"></div></div><div id="mgOnModelGuide2570" class="mg-guide">Bir ölçüm modu seç. Seçtiğin eksen, yüzey ve noktalar model üzerinde renkle gösterilecek.</div>';
 tools.insertBefore(hub,tools.firstChild);hub.querySelector('#mgCore2570').appendChild(core);
 const other=hub.querySelector('#mgOther2570'),wrap=hub.querySelector('#mgOtherWrap2570'),exclude=new Set(['mgRef2560','mgPoint2560','mgThick2560','mgClear2560','mgSmartMeasure2550','mgSmartMeasureRef2550']);
 const duplicate=/^(AKILLI ÖLÇ|AKILLI ÖLÇÜ|AKILLI ÖLÇÜM|ÖZELLİK ÖLÇ)$/;
 const measure=/ÖLÇ|MESAFE|KALINLIK|KUMPAS|ÇAP|YARIÇAP|AÇI|ALAN|OTOMATİK/;
 [...tools.querySelectorAll('button')].forEach(b=>{if(hub.contains(b)||exclude.has(b.id))return;const t=(b.textContent||'').replace(/\s+/g,' ').trim().toLocaleUpperCase('tr-TR');if(!measure.test(t))return;if(duplicate.test(t)){b.style.display='none';return}const holder=b.closest('.row')||b.parentElement;if(holder&&holder!==tools&&!hub.contains(holder)){const cloneHolder=document.createElement('div');cloneHolder.className='mg-other-row';holder.querySelectorAll('button').forEach(x=>{const tx=(x.textContent||'').toLocaleUpperCase('tr-TR');if(measure.test(tx)&&!exclude.has(x.id))cloneHolder.appendChild(x)});if(cloneHolder.children.length){other.appendChild(cloneHolder);wrap.style.display='block'}}});
 // hide now-empty legacy measurement headings/rows
 [...tools.querySelectorAll('.head')].forEach(h=>{if(hub.contains(h))return;const t=(h.textContent||'').trim().toLocaleUpperCase('tr-TR');if(t==='ÖLÇÜM'||t==='MÜHENDİSLİK ARAÇLARI'){const n=h.nextElementSibling;if(!n||!n.querySelector||!n.querySelector('button:not([style*="display: none"])'))h.style.display='none'}});
}
function installStyle(){const s=document.createElement('style');s.textContent=`#mgMeasureHub2570{border:1px solid #245582;border-radius:12px;padding:8px;margin-bottom:10px;background:rgba(4,15,28,.72)}#mgMeasureHub2570 #mgMeasureModes2560{margin-top:0}#mgMeasureHub2570 .mgHubTitle{font-size:14px;color:#eaf5ff;border-bottom:1px solid #245582;padding-bottom:6px}.mg-guide{margin-top:8px;padding:8px;border-radius:9px;border:1px solid #35536e;background:rgba(255,255,255,.035);font-size:12px;line-height:1.4}.mg-guide.blue{border-color:#42a5ff;color:#cbeaff}.mg-guide.green{border-color:#55d98a;color:#d2f8df}.mg-other{display:grid;gap:6px}.mg-other-row{display:flex;gap:6px;flex-wrap:wrap}.mg-other-row button{flex:1;min-width:90px}.mg-other-row button[id*="auto" i],.mg-other-row button{border-width:2px}`;document.head.appendChild(s)}
function bindButtons(){['mgRef2560','mgPoint2560','mgThick2560','mgClear2560'].forEach(id=>{const b=document.getElementById(id);if(!b)return;b.addEventListener('click',()=>{clearGuides();pointStep=0;setTimeout(()=>{lastMode=mode();if(lastMode==='ref')guideText('🔷 Referans için <b>kenar/eksen veya yüzey</b> seç. Seçimin model üzerinde mavi gösterilecek.','blue');else if(lastMode==='point')guideText('🟢 İlk <b>yüzey veya noktayı</b> seç. Seçim model üzerinde yeşil gösterilecek.','green');else if(lastMode==='thick')guideText('🟧 Kumpas karşı iki yüzü turuncu çenelerle gösterecek.','');else guideText('Bir ölçüm modu seç.','')},0)})})}
function init(){installStyle();gatherMeasurements();bindButtons();canvas.addEventListener('pointerdown',onPointer,true);window.MG_CAD_V2570={version:'2.5.7',baseline:'2.5.5',measurementHubRight:true,onModelGuidance:true,axisVisualization:true,faceSelectionHighlight:true,pointSelectionHighlight:true,smartBlue:true,pointGreen:true,caliperOrange:true,automaticYellow:true}}
ready(init)
})();'''
(AS/'cad-v2570.js').write_text(js,encoding='utf-8')
print('v2.5.7 right-side measurement hub + guided axis/face/point selection')
