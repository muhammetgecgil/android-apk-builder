from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v187.js' not in s:
    s=s.replace('</body>','<script src="/cad-v187.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
let pickedFace=null, hiddenFaceStates=[], autoDiaSprites=[];
function E(id){return document.getElementById(id)}
function M(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function U(v){try{return fmt(v)+' '+E('unit').value}catch(e){return Number(v).toFixed(3)+' mm'}}
function msg(h){const a=E('industryOut'),b=E('proResult');if(a)a.innerHTML=h;if(b)b.innerHTML=h}

// ANALYSIS PANEL: when collapsed, leave a restore button beside the bottom dimension controls.
function installAnalysisRestore(){
 const info=E('info'), panelB=E('mgAnalysisPanelB'), dimB=E('autoDimB'); if(!info||!panelB||!dimB)return;
 let r=E('mgAnalysisRestore'); if(!r){r=document.createElement('button');r.id='mgAnalysisRestore';r.textContent='ANALİZ ▶';r.style.display='none';dimB.parentElement.insertBefore(r,dimB.nextSibling)}
 function isOpen(){return Array.from(info.children).some(x=>x!==panelB&&x.style.display!=='none')}
 function sync(){const open=isOpen();r.style.display=open?'none':'';panelB.style.display=open?'':'none'}
 const old=panelB.onclick;panelB.onclick=function(e){if(old)old.call(this,e);setTimeout(sync,0)};
 r.onclick=()=>{Array.from(info.children).filter(x=>x!==panelB).forEach(x=>x.style.display='');info.style.minWidth='245px';info.style.maxWidth='330px';info.style.width='';info.style.padding='38px 9px 9px';info.style.background='rgba(3,10,20,.92)';info.style.border='1px solid #173c60';panelB.textContent='ANALİZ ◀';panelB.style.display='';r.style.display='none'};
 sync();
}

// FACE PICKING + HIDE: remove a connected surface patch from the selected mesh index buffer.
function triNormal(g,ti){const p=g.attributes.position,idx=g.index;const ia=idx?idx.getX(ti*3):ti*3,ib=idx?idx.getX(ti*3+1):ti*3+1,ic=idx?idx.getX(ti*3+2):ti*3+2;const A=new THREE.Vector3().fromBufferAttribute(p,ia),B=new THREE.Vector3().fromBufferAttribute(p,ib),C=new THREE.Vector3().fromBufferAttribute(p,ic);return new THREE.Vector3().crossVectors(B.clone().sub(A),C.clone().sub(A)).normalize()}
function selectedPatch(mesh,faceIndex){const g=mesh.geometry,idx=g.index,p=g.attributes.position;if(faceIndex==null||!p)return[];const tc=idx?idx.count/3:p.count/3,n0=triNormal(g,faceIndex),verts=[],edgeMap=new Map();function vi(t,k){return idx?idx.getX(t*3+k):t*3+k}for(let t=0;t<tc;t++){const a=vi(t,0),b=vi(t,1),c=vi(t,2);verts[t]=[a,b,c];[[a,b],[b,c],[c,a]].forEach(([x,y])=>{if(x>y){const z=x;x=y;y=z}const key=x+','+y;if(!edgeMap.has(key))edgeMap.set(key,[]);edgeMap.get(key).push(t)})}const adj=Array.from({length:tc},()=>[]);for(const ts of edgeMap.values())if(ts.length===2){adj[ts[0]].push(ts[1]);adj[ts[1]].push(ts[0])}const q=[faceIndex],seen=new Set();while(q.length){const t=q.pop();if(seen.has(t))continue;const n=triNormal(g,t);if(n.dot(n0)<0.965)continue;seen.add(t);adj[t].forEach(k=>{if(!seen.has(k))q.push(k)})}return Array.from(seen)}
function hidePickedFace(){if(!pickedFace)return false;const mesh=pickedFace.mesh,g=mesh.geometry,patch=selectedPatch(mesh,pickedFace.faceIndex);if(!patch.length)return false;const idx=g.index,tc=idx?idx.count/3:g.attributes.position.count/3,remove=new Set(patch),newIdx=[];for(let t=0;t<tc;t++)if(!remove.has(t)){if(idx){newIdx.push(idx.getX(t*3),idx.getX(t*3+1),idx.getX(t*3+2))}else newIdx.push(t*3,t*3+1,t*3+2)}hiddenFaceStates.push({mesh,oldIndex:idx?Array.from(idx.array):null});g.setIndex(newIdx);g.computeBoundingSphere();g.computeBoundingBox();pickedFace=null;msg('<b>YÜZEY GİZLENDİ</b><br>TÜMÜ ile gizlenen yüzeyleri geri getirebilirsin.');return true}
function restoreFaces(){hiddenFaceStates.forEach(s=>{if(s.oldIndex)s.mesh.geometry.setIndex(s.oldIndex);else s.mesh.geometry.setIndex(null);s.mesh.geometry.computeBoundingSphere();s.mesh.geometry.computeBoundingBox()});hiddenFaceStates=[]}
function installFaceHide(){
 canvas.addEventListener('click',ev=>{if(ev.clientY<58)return;try{const r=canvas.getBoundingClientRect();mouse.x=((ev.clientX-r.left)/r.width)*2-1;mouse.y=-((ev.clientY-r.top)/r.height)*2+1;ray.setFromCamera(mouse,camera);const h=ray.intersectObjects(M().filter(x=>x.visible),false)[0];if(h)pickedFace={mesh:h.object,faceIndex:h.faceIndex}}catch(e){}},false);
 const hide=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()==='GİZLE'||b.textContent.trim().toUpperCase()==='GERİ GETİR');if(hide){const old=hide.onclick;hide.onclick=function(e){if(pickedFace&&hidePickedFace()){hide.textContent='GİZLE';return}if(old)old.call(this,e)}}
 const all=Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()==='TÜMÜ');if(all){const old=all.onclick;all.onclick=function(e){restoreFaces();M().forEach(m=>m.visible=true);if(old)try{old.call(this,e)}catch(_){}}}
}

// Automatic diameter detection: hole loops + axisymmetric standalone meshes (shafts/bosses).
function clearAutoDia(){autoDiaSprites.forEach(s=>{scene.remove(s);if(s.material&&s.material.map)s.material.map.dispose();if(s.material)s.material.dispose()});autoDiaSprites=[]}
function sprite(text,p){const c=document.createElement('canvas');c.width=460;c.height=92;const x=c.getContext('2d');x.fillStyle='rgba(3,10,20,.95)';x.strokeStyle='#ffd84d';x.lineWidth=4;x.beginPath();x.roundRect(4,4,452,84,16);x.fill();x.stroke();x.fillStyle='#ffe477';x.font='bold 32px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(text,230,46);const t=new THREE.CanvasTexture(c),m=new THREE.SpriteMaterial({map:t,depthTest:false,transparent:true}),s=new THREE.Sprite(m),sc=Math.max(baseDims.x,baseDims.y,baseDims.z,1)*.17;s.scale.set(sc,sc*.20,1);s.position.copy(p);s.renderOrder=60;scene.add(s);autoDiaSprites.push(s)}
function shaftCandidates(){const out=[],tol=.035;M().forEach(mesh=>{const b=new THREE.Box3().setFromObject(mesh),s=new THREE.Vector3();b.getSize(s);const c=new THREE.Vector3();b.getCenter(c);const ds=[s.x,s.y,s.z],pairs=[[0,1,2],[0,2,1],[1,2,0]];let best=null;pairs.forEach(([a,bx,ax])=>{const d=(ds[a]+ds[bx])/2;if(d>0&&Math.abs(ds[a]-ds[bx])/d<tol&&ds[ax]>d*.25){const score=Math.abs(ds[a]-ds[bx])/d;if(!best||score<best.score)best={diam:d,axis:['Z','Y','X'][pairs.findIndex(p=>p[0]===a&&p[1]===bx)],center:c.clone(),score}}});if(best)out.push(best)});return out}
function autoDiameters(){clearAutoDia();let features=[];try{if(typeof detectCircularFeatures==='function')features=detectCircularFeatures()||[]}catch(e){}const scale=Math.max(baseDims.x,baseDims.y,baseDims.z,1),seen=[];features.forEach((f,i)=>{const d=f.r*2;if(d<scale*.002)return;const key=Math.round(d*1000)+'-'+f.axis;if(seen.includes(key))return;seen.push(key);sprite('DELİK Ø '+U(d),f.center.clone())});const shafts=shaftCandidates();shafts.slice(0,12).forEach((s,i)=>sprite('MİL/DIŞ Ø '+U(s.diam),s.center.clone()));msg('<b>OTOMATİK ÇAP ÖLÇÜLENDİRME</b><br>'+seen.length+' delik/dairesel açıklık • '+shafts.length+' mil/dış çap adayı model üzerinde gösterildi.')}
function installAutoDiameter(){const b=E('autoDimB');if(!b)return;const old=b.onclick;b.onclick=function(e){if(old)old.call(this,e);setTimeout(()=>{if(this.classList.contains('on'))autoDiameters();else clearAutoDia()},140)}}

function boot(){installAnalysisRestore();installFaceHide();installAutoDiameter();window.MG_CAD_V187={version:'1.8.7',analysisRestore:true,faceHide:true,autoHoleDiameter:true,autoShaftDiameter:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,650));else setTimeout(boot,650);
})();'''
(AS/'cad-v187.js').write_text(js,encoding='utf-8')
print('v1.8.7 analysis restore + selected surface hide + automatic hole/shaft diameters applied')
