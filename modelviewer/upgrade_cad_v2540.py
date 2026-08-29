from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2540.js' not in h: h=h.replace('</body>','<script src="/cad-v2540.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
const EPS2=1e-12;
let axisMode='GLOBAL',axisPick=false,axisFrame=null,capGroup=null;
function makeTriad(){
 const g=new THREE.Group();g.name='MG_SMART_AXIS_2540';
 const s=()=>Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1)*0.16;
 function arr(dir,col){let a=new THREE.ArrowHelper(dir,new THREE.Vector3(),s(),col,s()*.24,s()*.12);g.add(a)}
 arr(new THREE.Vector3(1,0,0),0xff4d4d);arr(new THREE.Vector3(0,1,0),0x4dff88);arr(new THREE.Vector3(0,0,1),0x4da6ff);
 scene.add(g);g.visible=false;return g
}
function setFrame(origin,x,y,z){
 if(!axisFrame)axisFrame=makeTriad();
 const M=new THREE.Matrix4();M.makeBasis(x.clone().normalize(),y.clone().normalize(),z.clone().normalize());
 axisFrame.position.copy(origin);axisFrame.quaternion.setFromRotationMatrix(M);axisFrame.visible=true;
 const info=document.getElementById('mgAxisInfo2540');if(info)info.textContent='LOCAL • X kenar/tanjant • Z yüzey normali';
}
function globalFrame(){if(!bbox)return;let c=new THREE.Vector3();bbox.getCenter(c);setFrame(c,new THREE.Vector3(1,0,0),new THREE.Vector3(0,1,0),new THREE.Vector3(0,0,1));const i=document.getElementById('mgAxisInfo2540');if(i)i.textContent='GLOBAL • Model X/Y/Z'}
function closestEdge(hit){
 const o=hit.object,g=o.geometry,face=hit.face;if(!face||!g||!g.attributes.position)return null;
 const p=g.attributes.position,A=new THREE.Vector3().fromBufferAttribute(p,face.a).applyMatrix4(o.matrixWorld),B=new THREE.Vector3().fromBufferAttribute(p,face.b).applyMatrix4(o.matrixWorld),C=new THREE.Vector3().fromBufferAttribute(p,face.c).applyMatrix4(o.matrixWorld);
 function distSeg(P,U,V){let uv=V.clone().sub(U),t=uv.lengthSq()?P.clone().sub(U).dot(uv)/uv.lengthSq():0;t=Math.max(0,Math.min(1,t));let q=U.clone().addScaledVector(uv,t);return {d:P.distanceTo(q),dir:uv.normalize(),len:U.distanceTo(V)}}
 let es=[distSeg(hit.point,A,B),distSeg(hit.point,B,C),distSeg(hit.point,C,A)].sort((a,b)=>a.d-b.d),e=es[0],scale=Math.max(es[0].len,es[1].len,es[2].len,1e-6);return e.d<scale*.08?e:null
}
function smartFrameFromHit(hit){
 let z=hit.face.normal.clone().transformDirection(hit.object.matrixWorld).normalize(),edge=closestEdge(hit),x;
 if(edge){x=edge.dir.clone().sub(z.clone().multiplyScalar(edge.dir.dot(z)));if(x.lengthSq()<EPS2)x=null}
 if(!x){let w=Math.abs(z.dot(new THREE.Vector3(0,1,0)))<.92?new THREE.Vector3(0,1,0):new THREE.Vector3(1,0,0);x=w.clone().sub(z.clone().multiplyScalar(w.dot(z))).normalize()}
 x.normalize();let y=z.clone().cross(x).normalize();x=y.clone().cross(z).normalize();setFrame(hit.point,x,y,z)
}
function hitAt(ev){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function installAxisUI(){
 if(document.getElementById('mgSmartAxis2540'))return;let host=document.getElementById('tools');if(!host)return;
 let box=document.createElement('div');box.innerHTML='<div class="sep"></div><div class="head">AKILLI EKSEN</div><div class="row"><button id="mgSmartAxis2540">AKILLI EKSEN</button><button id="mgAxisMode2540">GLOBAL</button></div><div class="small" id="mgAxisInfo2540">Model X/Y/Z</div>';host.appendChild(box);
 let b=document.getElementById('mgSmartAxis2540'),m=document.getElementById('mgAxisMode2540');
 b.onclick=e=>{e.preventDefault();axisPick=!axisPick;b.classList.toggle('on',axisPick);b.textContent=axisPick?'YÜZ/KENAR SEÇ':'AKILLI EKSEN';if(axisMode==='GLOBAL')globalFrame()};
 m.onclick=e=>{e.preventDefault();axisMode=axisMode==='GLOBAL'?'LOCAL':'GLOBAL';m.textContent=axisMode;if(axisMode==='GLOBAL'){axisPick=false;b.classList.remove('on');b.textContent='AKILLI EKSEN';globalFrame()}else{axisPick=true;b.classList.add('on');b.textContent='YÜZ/KENAR SEÇ'}};
 canvas.addEventListener('click',ev=>{if(!axisPick||axisMode!=='LOCAL')return;let h=hitAt(ev);if(!h)return;smartFrameFromHit(h);ev.stopPropagation()},true)
}
function meshWatertight(mesh){
 const g=mesh.geometry,p=g&&g.attributes&&g.attributes.position;if(!p)return false;const idx=g.index;let counts=new Map(),q=v=>Math.round(v*1e6)/1e6,key=(a,b)=>{let A=q(a.x)+','+q(a.y)+','+q(a.z),B=q(b.x)+','+q(b.y)+','+q(b.z);return A<B?A+'|'+B:B+'|'+A};
 const A=new THREE.Vector3(),B=new THREE.Vector3(),C=new THREE.Vector3(),n=idx?idx.count/3:p.count/3;for(let k=0;k<n;k++){let ia=idx?idx.getX(k*3):k*3,ib=idx?idx.getX(k*3+1):k*3+1,ic=idx?idx.getX(k*3+2):k*3+2;A.fromBufferAttribute(p,ia);B.fromBufferAttribute(p,ib);C.fromBufferAttribute(p,ic);for(const e of [[A,B],[B,C],[C,A]]){let K=key(e[0],e[1]);counts.set(K,(counts.get(K)||0)+1)}}for(const c of counts.values())if(c!==2)return false;return counts.size>0
}
function axisData(){const a=document.getElementById('secAxis').value,t=+document.getElementById('section').value/100,min=a==='X'?bbox.min.x:a==='Y'?bbox.min.y:bbox.min.z,max=a==='X'?bbox.max.x:a==='Y'?bbox.max.y:bbox.max.z;return{a,pos:min+(max-min)*t}}
function project(v,a){return a==='X'?new THREE.Vector2(v.y,v.z):a==='Y'?new THREE.Vector2(v.x,v.z):new THREE.Vector2(v.x,v.y)}
function area2(poly){let s=0;for(let i=0,j=poly.length-1;i<poly.length;j=i++)s+=(poly[j].x*poly[i].y-poly[i].x*poly[j].y);return s/2}
function pointIn(p,poly){let c=false;for(let i=0,j=poly.length-1;i<poly.length;j=i++){let a=poly[i],b=poly[j];if(((a.y>p.y)!=(b.y>p.y))&&(p.x<(b.x-a.x)*(p.y-a.y)/(b.y-a.y)+a.x))c=!c}return c}
function clearCaps(){if(!capGroup)return;while(capGroup.children.length){let o=capGroup.children.pop();o.geometry&&o.geometry.dispose();o.material&&o.material.dispose()}}
function buildLoops(src,d,eps){
 const g=src.geometry,p=g.attributes.position,idx=g.index,A=new THREE.Vector3(),B=new THREE.Vector3(),C=new THREE.Vector3(),segs=[];function coord(v){return d.a==='X'?v.x:d.a==='Y'?v.y:v.z}function edge(u,v,out){let du=coord(u)-d.pos,dv=coord(v)-d.pos;if(Math.abs(du)<eps)du=0;if(Math.abs(dv)<eps)dv=0;if(du*dv<0)out.push(u.clone().lerp(v,du/(du-dv)))}
 const n=idx?idx.count/3:p.count/3;for(let k=0;k<n;k++){let ia=idx?idx.getX(k*3):k*3,ib=idx?idx.getX(k*3+1):k*3+1,ic=idx?idx.getX(k*3+2):k*3+2;A.fromBufferAttribute(p,ia).applyMatrix4(src.matrixWorld);B.fromBufferAttribute(p,ib).applyMatrix4(src.matrixWorld);C.fromBufferAttribute(p,ic).applyMatrix4(src.matrixWorld);let q=[];edge(A,B,q);edge(B,C,q);edge(C,A,q);if(q.length>=2)segs.push([q[0],q[1]])}
 let loops=[];while(segs.length){let s=segs.pop(),loop=[s[0],s[1]],guard=0;while(guard++<20000){let end=loop[loop.length-1],j=segs.findIndex(e=>e[0].distanceToSquared(end)<eps*eps*25||e[1].distanceToSquared(end)<eps*eps*25);if(j<0)break;let e=segs.splice(j,1)[0],next=e[0].distanceToSquared(end)<e[1].distanceToSquared(end)?e[1]:e[0];if(next.distanceToSquared(loop[0])<eps*eps*25)break;loop.push(next)}if(loop.length>=3)loops.push(loop)}return loops
}
function rebuildCaps(){
 clearCaps();if(!sectionOn||!bbox)return;const d=axisData(),eps=Math.max(baseDims.x,baseDims.y,baseDims.z,1)*1e-5;
 group.children.forEach(src=>{if(!src.isMesh||!src.visible||!meshWatertight(src))return;let loops=buildLoops(src,d,eps);if(!loops.length)return;let rec=loops.map(L=>({L,P:L.map(v=>project(v,d.a))}));rec.forEach(r=>r.abs=Math.abs(area2(r.P)));rec.sort((a,b)=>b.abs-a.abs);
 while(rec.length){let outer=rec.shift(),holes=[],remain=[];for(const r of rec){if(pointIn(r.P[0],outer.P))holes.push(r);else remain.push(r)}rec=remain;let op=outer.P.slice();if(area2(op)<0)op.reverse();let hp=holes.map(h=>{let p=h.P.slice();if(area2(p)>0)p.reverse();return p});let faces=THREE.ShapeUtils.triangulateShape(op,hp);let all=[outer].concat(holes),flat=[];all.forEach(r=>flat.push(...r.L));let contourCount=outer.L.length,offsets=[0];for(let i=0,s=contourCount;i<holes.length;i++){offsets.push(s);s+=holes[i].L.length}let arr=[];faces.forEach(f=>f.forEach(ind=>{let v=flat[ind];arr.push(v.x,v.y,v.z)}));if(!arr.length)continue;let cg=new THREE.BufferGeometry();cg.setAttribute('position',new THREE.Float32BufferAttribute(arr,3));cg.computeVertexNormals();let cm=new THREE.MeshBasicMaterial({color:src.material&&src.material.color?src.material.color:0x69aee8,side:THREE.DoubleSide,depthTest:true,polygonOffset:true,polygonOffsetFactor:-1,polygonOffsetUnits:-1});capGroup.add(new THREE.Mesh(cg,cm))}
 })
}
function installAutoSection(){
 capGroup=new THREE.Group();capGroup.name='MG_AUTO_SOLID_CAP_2540';scene.add(capGroup);
 if(window.applySection&&!window.applySection._mg2540){let old=window.applySection;window.applySection=function(){old();requestAnimationFrame(rebuildCaps)};window.applySection._mg2540=true}
 if(window.toggleSection&&!window.toggleSection._mg2540){let old=window.toggleSection;window.toggleSection=function(){old();requestAnimationFrame(rebuildCaps)};window.toggleSection._mg2540=true}
}
function init(){installAxisUI();installAutoSection();window.MG_CAD_V2540={version:'2.5.4',functionalSmartAxis:true,faceNormalAxis:true,edgeAwareAxis:true,automaticSolidSection:true,openShellNotFilled:true,holeAwareCap:true,baseline:'2.5.2'}}
ready(init)
})();'''
(AS/'cad-v2540.js').write_text(js,encoding='utf-8')
print('v2.5.4 functional smart axis + automatic solid-aware section')
