from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v193.js' not in s:
    s=s.replace('</body>','<script src="/cad-v193.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
const E=id=>document.getElementById(id);
const selectedDom=new Set(),selectedSprites=new Set();
let faceOverlay=null,downPos=null;
function C(){return document.querySelector('canvas')}
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function updateCloseText(){const b=E('mgCloseMeasures');if(b)b.textContent='ÖLÇÜ KAPAT'+((selectedDom.size+selectedSprites.size)?' ('+(selectedDom.size+selectedSprites.size)+')':'')}
function selectDom(el){if(!el)return false;if(selectedDom.has(el)){selectedDom.delete(el);el.classList.remove('mg193SelectedMeasure');el.style.outline='';el.style.filter='';}else{selectedDom.add(el);el.classList.add('mg193SelectedMeasure');el.style.outline='2px solid #ff5a5a';el.style.filter='drop-shadow(0 0 5px #ff5a5a)';}updateCloseText();return true}
function candidateDom(t){return t&&t.closest?t.closest('.mgDimLabel,[data-auto-hole],.mg-dim-label,.dimLabel,.measureLabel,[data-measure-label]'):null}
function sceneSprites(){const a=[];try{scene.traverse(o=>{if(o&&o.isSprite&&o.visible!==false&&o.renderOrder>=45)a.push(o)})}catch(e){}return a}
function nearestSprite(x,y,maxPx){const c=C();if(!c)return null;const r=c.getBoundingClientRect();let best=null,bd=maxPx||55;sceneSprites().forEach(sp=>{try{const p=sp.getWorldPosition(new THREE.Vector3()).project(camera),sx=r.left+(p.x*.5+.5)*r.width,sy=r.top+(-p.y*.5+.5)*r.height,d=Math.hypot(sx-x,sy-y);if(d<bd){bd=d;best=sp}}catch(e){}});return best}
function selectSprite(sp){if(!sp)return false;sp.userData=sp.userData||{};if(selectedSprites.has(sp)){selectedSprites.delete(sp);if(sp.material){if(sp.userData.mg193Color!=null&&sp.material.color)sp.material.color.setHex(sp.userData.mg193Color);if(sp.userData.mg193Opacity!=null)sp.material.opacity=sp.userData.mg193Opacity}}else{selectedSprites.add(sp);if(sp.material){if(sp.material.color&&!('mg193Color' in sp.userData))sp.userData.mg193Color=sp.material.color.getHex();if(!('mg193Opacity' in sp.userData))sp.userData.mg193Opacity=sp.material.opacity; if(sp.material.color)sp.material.color.setHex(0xff5a5a);sp.material.opacity=.72;sp.material.transparent=true}}updateCloseText();return true}
function hideSelectedMeasures(){selectedDom.forEach(el=>{el.style.display='none';el.style.outline='';el.style.filter=''});selectedSprites.forEach(sp=>{sp.visible=false});selectedDom.clear();selectedSprites.clear();updateCloseText()}
function installMeasureClose(){const d=E('autoDimB');if(!d||!d.parentElement)return false;let b=E('mgCloseMeasures');if(!b){b=document.createElement('button');b.id='mgCloseMeasures';b.textContent='ÖLÇÜ KAPAT';b.title='Kapatmak istediğin ölçülere dokun; seçilenleri bu düğmeyle gizle.';d.parentElement.insertBefore(b,d.nextSibling)}
 b.onclick=e=>{e.preventDefault();e.stopPropagation();hideSelectedMeasures()};
 document.addEventListener('pointerup',e=>{if(e.pointerType==='touch'||e.pointerType==='pen'||e.pointerType==='mouse'){const dom=candidateDom(e.target);if(dom){selectDom(dom);e.preventDefault();e.stopImmediatePropagation();return}if(e.target===C()){const sp=nearestSprite(e.clientX,e.clientY,58);if(sp){selectSprite(sp);e.preventDefault();e.stopImmediatePropagation()}}}},true);
 updateCloseText();return true}
function triNormal(g,ti){const p=g.attributes.position,idx=g.index,ia=idx?idx.getX(ti*3):ti*3,ib=idx?idx.getX(ti*3+1):ti*3+1,ic=idx?idx.getX(ti*3+2):ti*3+2,A=new THREE.Vector3().fromBufferAttribute(p,ia),B=new THREE.Vector3().fromBufferAttribute(p,ib),C=new THREE.Vector3().fromBufferAttribute(p,ic);return new THREE.Vector3().crossVectors(B.clone().sub(A),C.clone().sub(A)).normalize()}
function facePatch(mesh,faceIndex){const g=mesh.geometry,idx=g.index,p=g.attributes.position;if(faceIndex==null||!p)return[];const tc=idx?idx.count/3:p.count/3,n0=triNormal(g,faceIndex),edge=new Map(),verts=[];const vi=(t,k)=>idx?idx.getX(t*3+k):t*3+k;for(let t=0;t<tc;t++){const a=vi(t,0),b=vi(t,1),c=vi(t,2);verts[t]=[a,b,c];[[a,b],[b,c],[c,a]].forEach(([x,y])=>{if(x>y){const q=x;x=y;y=q}const k=x+','+y;if(!edge.has(k))edge.set(k,[]);edge.get(k).push(t)})}const adj=Array.from({length:tc},()=>[]);for(const a of edge.values())if(a.length===2){adj[a[0]].push(a[1]);adj[a[1]].push(a[0])}const q=[faceIndex],seen=new Set();while(q.length){const t=q.pop();if(seen.has(t))continue;const n=triNormal(g,t);if(n.dot(n0)<.94)continue;seen.add(t);adj[t].forEach(k=>{if(!seen.has(k))q.push(k)})}return Array.from(seen)}
function clearFaceOverlay(){if(faceOverlay){try{faceOverlay.parent.remove(faceOverlay);faceOverlay.geometry.dispose();faceOverlay.material.dispose()}catch(e){}faceOverlay=null}}
function paintFace(hit){clearFaceOverlay();const mesh=hit.object,g=mesh.geometry,p=g.attributes.position,idx=g.index,patch=facePatch(mesh,hit.faceIndex);if(!patch.length)return;const arr=[];const vi=(t,k)=>idx?idx.getX(t*3+k):t*3+k;patch.forEach(t=>{for(let k=0;k<3;k++){const i=vi(t,k);arr.push(p.getX(i),p.getY(i),p.getZ(i))}});const pg=new THREE.BufferGeometry();pg.setAttribute('position',new THREE.Float32BufferAttribute(arr,3));pg.computeVertexNormals();const mat=new THREE.MeshBasicMaterial({color:0xffd84d,transparent:true,opacity:.58,side:THREE.DoubleSide,depthTest:true,polygonOffset:true,polygonOffsetFactor:-2,polygonOffsetUnits:-2});faceOverlay=new THREE.Mesh(pg,mat);faceOverlay.renderOrder=70;faceOverlay.userData.mgFaceHighlight=true;mesh.add(faceOverlay);const r=E('proResult');if(r)r.innerHTML='<b>YÜZEY SEÇİLDİ</b><br>Assembly içindeki seçili yüzey sarı renkle vurgulandı.'}
function hitAt(x,y){const c=C();if(!c)return null;try{const r=c.getBoundingClientRect();mouse.x=((x-r.left)/r.width)*2-1;mouse.y=-((y-r.top)/r.height)*2+1;ray.setFromCamera(mouse,camera);return ray.intersectObjects(meshes().filter(m=>m.visible),false)[0]||null}catch(e){return null}}
function installAssemblyFacePaint(){const c=C();if(!c)return; c.addEventListener('pointerdown',e=>{if(e.pointerType==='touch'||e.pointerType==='pen'||e.pointerType==='mouse')downPos={id:e.pointerId,x:e.clientX,y:e.clientY}},true);c.addEventListener('pointerup',e=>{if(!downPos||e.pointerId!==downPos.id)return;const move=Math.hypot(e.clientX-downPos.x,e.clientY-downPos.y);downPos=null;if(move>8||meshes().length<2)return;const h=hitAt(e.clientX,e.clientY);if(h&&h.object&&!h.object.userData?.mgFaceHighlight)paintFace(h)},false)}
function boot(){installMeasureClose();installAssemblyFacePaint();let n=0;const t=setInterval(()=>{n++;if(installMeasureClose()||n>20)clearInterval(t)},300);window.MG_CAD_V193={version:'1.9.3',multiMeasureSelectClose:true,assemblyFacePaint:true};}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,1200));else setTimeout(boot,1200);
})();'''
(AS/'cad-v193.js').write_text(js,encoding='utf-8')
print('v1.9.3 multi measure close + assembly face paint')
