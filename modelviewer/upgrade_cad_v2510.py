from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2510.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2510.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
let smartPick=false;
function toast(msg){let t=document.getElementById('mgSmartPivotToast2510');if(!t){t=document.createElement('div');t.id='mgSmartPivotToast2510';t.style.cssText='position:fixed;left:50%;top:74px;transform:translateX(-50%);z-index:190;background:rgba(4,18,31,.97);border:1px solid #39b8ff;border-radius:10px;padding:10px 16px;color:#eef9ff;font-weight:800;box-shadow:0 6px 22px #0009;pointer-events:none;transition:.2s';document.body.appendChild(t)}t.textContent=msg;t.style.opacity='1';clearTimeout(t._tm);t._tm=setTimeout(()=>t.style.opacity='0',1600)}
function hide(id){const e=document.getElementById(id);if(e)e.style.display='none'}
function closestOnSegment(p,a,b){const ab=b.clone().sub(a),d=ab.lengthSq();if(d<1e-16)return a.clone();const t=Math.max(0,Math.min(1,p.clone().sub(a).dot(ab)/d));return a.clone().addScaledVector(ab,t)}
function triWorld(hit){try{const o=hit.object,g=o.geometry,f=hit.face,p=g&&g.attributes&&g.attributes.position;if(!o||!p||!f)return null;const a=new THREE.Vector3().fromBufferAttribute(p,f.a),b=new THREE.Vector3().fromBufferAttribute(p,f.b),c=new THREE.Vector3().fromBufferAttribute(p,f.c);o.localToWorld(a);o.localToWorld(b);o.localToWorld(c);return[a,b,c]}catch(e){return null}}
function smartPoint(hit){const p=hit.point.clone(),t=triWorld(hit);if(!t)return {p,kind:'YÜZEY NOKTASI'};const a=t[0],b=t[1],c=t[2],scale=Math.max(a.distanceTo(b),b.distanceTo(c),c.distanceTo(a),1e-9);const verts=[a,b,c];let best=verts[0],vd=p.distanceTo(best);for(let i=1;i<3;i++){const d=p.distanceTo(verts[i]);if(d<vd){vd=d;best=verts[i]}}if(vd<scale*.085)return {p:best.clone(),kind:'KÖŞE'};const edges=[[a,b],[b,c],[c,a]];let ep=null,ed=Infinity;for(const e of edges){const q=closestOnSegment(p,e[0],e[1]),d=p.distanceTo(q);if(d<ed){ed=d;ep=q}}if(ep&&ed<scale*.055)return {p:ep,kind:'KENAR'};return {p,kind:'YÜZEY NOKTASI'};}
function isPivotButton(b){if(!b)return false;const id=(b.id||'').toLowerCase(),txt=(b.textContent||'').toUpperCase();return id.includes('pivot')||txt.includes('PİVOT')||txt.includes('PIVOT');}
function unlockViewer(){try{if(typeof pivotPick!=='undefined')pivotPick=false}catch(e){}try{if(window.controls){controls.enabled=true;controls.enableRotate=true;controls.enablePan=true;controls.enableZoom=true}}catch(e){}}
function simplify(){const pb=document.getElementById('pivotB');if(!pb)return false;['mgPivotSnap2440','mgPivotAdvanced2500','mgPivotPart2430','mgPivotFace2430','mgPivotEdge2440','mgPivotVertex2440','mgPivotCenter2440','mgPivotUndo2500','mgPivotRedo2500','mgPivotFill2500','mgPivotApply2500'].forEach(hide);const row=pb.parentElement;if(row)[...row.querySelectorAll('button')].forEach(b=>{if(b!==pb&&isPivotButton(b))b.style.display='none'});pb.removeAttribute('onclick');pb.title='Akıllı pivot: model üzerinde bir nokta seç';if(!pb.dataset.mg2510){pb.dataset.mg2510='1';pb.onclick=function(e){e.preventDefault();e.stopPropagation();unlockViewer();smartPick=true;pb.textContent='MODELDE NOKTA SEÇ…';pb.classList.add('on');const info=document.getElementById('pivotInfo');if(info)info.textContent='Model üzerinde dönme merkezi olacak noktaya bir kez dokun.';toast('Akıllı pivot: modelde nokta seç')};}if(!smartPick)pb.textContent='AKILLI PİVOT';return true;}
function hitFromEvent(ev){try{const c=document.getElementById('c'),r=c.getBoundingClientRect();const x=('clientX'in ev)?ev.clientX:(ev.changedTouches&&ev.changedTouches[0]?ev.changedTouches[0].clientX:0);const y=('clientY'in ev)?ev.clientY:(ev.changedTouches&&ev.changedTouches[0]?ev.changedTouches[0].clientY:0);const m=new THREE.Vector2(((x-r.left)/r.width)*2-1,-((y-r.top)/r.height)*2+1),rc=new THREE.Raycaster();rc.setFromCamera(m,camera);return rc.intersectObjects(group.children.filter(x=>x.visible),false)}catch(e){return []}}
function pickHandler(ev){if(!smartPick)return;const hits=hitFromEvent(ev);if(!hits.length)return;smartPick=false;unlockViewer();const s=smartPoint(hits[0]);try{if(typeof window.setPivot==='function')window.setPivot(s.p)}catch(e){console.warn('smart pivot',e)}const pb=document.getElementById('pivotB');if(pb){pb.textContent='✓ AKILLI PİVOT';pb.classList.add('on')}const info=document.getElementById('pivotInfo');if(info)info.innerHTML='<b>✓ Akıllı pivot seçildi</b><br>'+s.kind+' yakalandı.<br><span style="color:#8bd8ff">Yeni nokta için AKILLI PİVOT düğmesine tekrar bas.</span>';toast('✓ Akıllı pivot aktif');}
function installCapture(){const c=document.getElementById('c');if(!c||c.dataset.mgSmartPivot2510)return false;c.dataset.mgSmartPivot2510='1';c.addEventListener('pointerup',pickHandler,true);return true;}
function init(){unlockViewer();simplify();installCapture();setTimeout(()=>{unlockViewer();simplify();installCapture()},300);setTimeout(unlockViewer,900);window.MG_CAD_V2510={version:'2.5.2',smartPivot:true,singlePivotButton:true,legacyPivotPickDisabled:true,viewerNeverLocked:true,nonPivotToolsPreserved:true};}
ready(init);
})();'''
(AS/'cad-v2510.js').write_text(js,encoding='utf-8')
print('v2.5.2: smart pivot isolated from legacy pivot picker; viewer stays unlocked')
