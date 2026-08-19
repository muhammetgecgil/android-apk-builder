from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v197.js' not in s:
    s=s.replace('</body>','<script src="/cad-v197.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
const E=id=>document.getElementById(id);
let active=false,panelVisible=false,oldCam=null,ortho=null,edgeGroup=null,meshState=[],oldClear=0x07111f,currentView='front';
const EPS_FACTOR=1e-5, CREASE_DEG=32;
function modelMeshes(){try{return group.children.filter(o=>o&&o.isMesh&&!o.userData?.mgTechAux)}catch(e){return[]}}
function bounds(){try{return new THREE.Box3().setFromObject(group)}catch(e){return null}}
function clearEdges(){if(!edgeGroup)return;try{scene.remove(edgeGroup);edgeGroup.traverse(o=>{if(o.geometry)o.geometry.dispose();if(o.material)o.material.dispose()})}catch(e){}edgeGroup=null}
function qkey(v,eps){return Math.round(v.x/eps)+','+Math.round(v.y/eps)+','+Math.round(v.z/eps)}
function geomEdges(mesh,viewDir){
 const g=mesh.geometry,p=g&&g.attributes&&g.attributes.position;if(!p)return {vis:[],hid:[]};mesh.updateMatrixWorld(true);
 const b=bounds(),sz=new THREE.Vector3();if(b)b.getSize(sz);const eps=Math.max(sz.x,sz.y,sz.z,1)*EPS_FACTOR;
 const idx=g.index,tri=idx?Math.floor(idx.count/3):Math.floor(p.count/3),vi=(t,k)=>idx?idx.getX(t*3+k):t*3+k;
 const vertCache=new Map();function wp(i){let v=vertCache.get(i);if(!v){v=new THREE.Vector3().fromBufferAttribute(p,i).applyMatrix4(mesh.matrixWorld);vertCache.set(i,v)}return v}
 const em=new Map();
 for(let t=0;t<tri;t++){
  const ia=vi(t,0),ib=vi(t,1),ic=vi(t,2),A=wp(ia),B=wp(ib),C=wp(ic);
  const n=new THREE.Vector3().crossVectors(B.clone().sub(A),C.clone().sub(A));if(n.lengthSq()<1e-18)continue;n.normalize();
  const front=n.dot(viewDir)<0, vv=[[ia,ib],[ib,ic],[ic,ia]];
  vv.forEach(pair=>{const P=wp(pair[0]),Q=wp(pair[1]),ka=qkey(P,eps),kb=qkey(Q,eps),k=ka<kb?ka+'|'+kb:kb+'|'+ka;let e=em.get(k);if(!e){e={A:P.clone(),B:Q.clone(),faces:[]};em.set(k,e)}e.faces.push({n:n.clone(),front})});
 }
 const vis=[],hid=[],th=Math.cos(CREASE_DEG*Math.PI/180);
 em.forEach(e=>{
   let keep=false,isSil=false;
   if(e.faces.length===1){keep=true;isSil=true}
   else{
     const f0=e.faces[0],f1=e.faces[1];isSil=f0.front!==f1.front;
     const crease=f0.n.dot(f1.n)<th;
     keep=isSil||crease;
   }
   if(!keep)return;
   const arr=(isSil||e.faces.some(f=>f.front))?vis:hid;
   arr.push(e.A.x,e.A.y,e.A.z,e.B.x,e.B.y,e.B.z);
 });
 return {vis,hid};
}
function addSegments(arr,hidden){if(!arr.length)return;const gg=new THREE.BufferGeometry();gg.setAttribute('position',new THREE.Float32BufferAttribute(arr,3));let mm;if(hidden){mm=new THREE.LineDashedMaterial({color:0x777777,dashSize:3,gapSize:2,transparent:true,opacity:.75,depthTest:true,depthWrite:false});mm.depthFunc=THREE.GreaterDepth}else{mm=new THREE.LineBasicMaterial({color:0x111111,depthTest:true,depthWrite:false});mm.depthFunc=THREE.LessEqualDepth}const ls=new THREE.LineSegments(gg,mm);if(hidden)ls.computeLineDistances();ls.renderOrder=90;edgeGroup.add(ls)}
function buildEdges(){clearEdges();edgeGroup=new THREE.Group();edgeGroup.userData.mgTechAux=true;scene.add(edgeGroup);const vd=new THREE.Vector3();camera.getWorldDirection(vd).normalize();modelMeshes().forEach(m=>{try{const r=geomEdges(m,vd);addSegments(r.vis,false);addSegments(r.hid,true)}catch(e){}})}
function setDepthOnly(on){if(on){meshState=modelMeshes().map(m=>({m,mat:m.material,vis:m.visible}));meshState.forEach(s=>{const base=Array.isArray(s.m.material)?s.m.material[0]:s.m.material;const dm=new THREE.MeshBasicMaterial({color:0xffffff,side:THREE.DoubleSide,depthWrite:true,depthTest:true});dm.colorWrite=false;s.m.material=dm;s.m.visible=s.vis})}else{meshState.forEach(s=>{try{if(s.m.material&&s.m.material!==s.mat)s.m.material.dispose();s.m.material=s.mat;s.m.visible=s.vis}catch(e){}});meshState=[]}}
function setCamera(v){const b=bounds();if(!b||b.isEmpty())return;const c=new THREE.Vector3(),sz=new THREE.Vector3();b.getCenter(c);b.getSize(sz);const aspect=Math.max(innerWidth/innerHeight,.25);let w,h;if(v==='front'||v==='back'){w=sz.x;h=sz.y}else if(v==='right'||v==='left'){w=sz.z;h=sz.y}else if(v==='top'||v==='bottom'){w=sz.x;h=sz.z}else{w=Math.max(sz.x,sz.z)*1.3;h=Math.max(sz.y,sz.z)*1.3}w=Math.max(w,1)*1.15;h=Math.max(h,1)*1.15;if(w/h>aspect)h=w/aspect;else w=h*aspect;const max=Math.max(sz.x,sz.y,sz.z,1),d=max*6+10;ortho=new THREE.OrthographicCamera(-w/2,w/2,h/2,-h/2,.001,max*50+1000);let dir,up=new THREE.Vector3(0,1,0);if(v==='front')dir=new THREE.Vector3(0,0,1);else if(v==='back')dir=new THREE.Vector3(0,0,-1);else if(v==='right')dir=new THREE.Vector3(1,0,0);else if(v==='left')dir=new THREE.Vector3(-1,0,0);else if(v==='top'){dir=new THREE.Vector3(0,1,0);up.set(0,0,-1)}else if(v==='bottom'){dir=new THREE.Vector3(0,-1,0);up.set(0,0,1)}else dir=new THREE.Vector3(1,1,1).normalize();ortho.position.copy(c).add(dir.multiplyScalar(d));ortho.up.copy(up);ortho.lookAt(c);ortho.updateProjectionMatrix();camera=ortho;try{controls.object=camera;controls.target.copy(c);controls.enableRotate=false;controls.enablePan=true;controls.enableZoom=true;controls.update()}catch(e){}}
function setView(v){if(!active)return;currentView=v;setDepthOnly(false);setCamera(v);setDepthOnly(true);try{grid.visible=false;axes.visible=false}catch(e){}try{renderer.setClearColor(0xf7f7f4,1)}catch(e){}setTimeout(()=>{buildEdges();try{if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}catch(e){}},60)}
function showPanel(){let p=E('mgDrawing197');if(!p){p=document.createElement('div');p.id='mgDrawing197';p.style.cssText='position:fixed;left:10px;top:68px;z-index:60;width:330px;padding:10px;background:rgba(250,250,248,.98);color:#111;border:1px solid #777;border-radius:6px;box-shadow:0 4px 16px rgba(0,0,0,.25)';p.innerHTML='<div style="font-weight:800;margin-bottom:8px">TEKNİK RESİM</div><div class="row"><button data-v="front">ÖN</button><button data-v="top">ÜST</button><button data-v="right">SAĞ</button><button data-v="left">SOL</button><button data-v="back">ARKA</button></div><div class="row"><button data-v="bottom">ALT</button><button data-v="iso">İZO</button></div><div class="row"><button id="d197Dim">ÖLÇÜLENDİR</button><button id="d197Rad">RADYÜS R</button></div><div style="font-size:12px;line-height:1.35;margin:8px 0">Klasik teknik resim: görünür kenarlar düz siyah; arkada kalan kenarlar kesikli gri; üçgen/mesh çizgileri gösterilmez. KAPAT yalnız bu menüyü kapatır.</div><div class="row"><button id="d197MenuClose">MENÜYÜ KAPAT</button></div>';document.body.appendChild(p);p.querySelectorAll('[data-v]').forEach(b=>b.onclick=()=>setView(b.dataset.v));E('d197Dim').onclick=()=>{try{const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}catch(e){}};E('d197Rad').onclick=()=>{try{const f=typeof detectCircularFeatures==='function'?(detectCircularFeatures()||[]):[];const vals=[...new Set(f.filter(q=>isFinite(q.r)&&q.r>0).map(q=>(q.r).toFixed(3)))];const r=E('proResult');if(r)r.innerHTML='<b>RADYÜS</b><br>'+(vals.length?vals.map(x=>'R '+x+' mm').join(' • '):'Radyüs algılanmadı.')}catch(e){}};E('d197MenuClose').onclick=()=>hidePanel()}p.style.display='block';panelVisible=true}
function hidePanel(){const p=E('mgDrawing197');if(p)p.style.display='none';panelVisible=false}
function enter(){if(active)return;active=true;if(!oldCam)oldCam=camera;const p195=E('mgDrawing195');if(p195)p195.remove();showPanel();setView('front')}
function exit(){if(!active)return;active=false;hidePanel();clearEdges();setDepthOnly(false);if(oldCam){camera=oldCam;oldCam=null}try{controls.object=camera;controls.enableRotate=true;controls.enablePan=true;controls.enableZoom=true;controls.update()}catch(e){}try{grid.visible=true;axes.visible=true}catch(e){}try{renderer.setClearColor(0x07111f,1)}catch(e){}}
function hook(){const b=[...document.querySelectorAll('button')].find(x=>/^TEKNİK\s*RESİM$/i.test((x.textContent||'').trim()));if(!b||b.dataset.mg197)return false;b.dataset.mg197='1';b.onclick=(ev)=>{try{ev&&ev.stopPropagation()}catch(e){}active?exit():enter()};return true}
const mo=new MutationObserver(()=>{hook();if(E('mgDrawing195')){try{E('mgDrawing195').remove()}catch(e){}}});mo.observe(document.documentElement,{subtree:true,childList:true});let n=0,t=setInterval(()=>{n++;if(hook()||n>30)clearInterval(t)},300);window.MG_CAD_V197={version:'1.9.7',classicTechnicalDrawing:true,weldedFeatureEdges:true,noTriangleMeshLines:true,visibleSolidHiddenDashed:true,menuCloseOnly:true,exitViaTechnicalDrawingButton:true};
})();'''
(AS/'cad-v197.js').write_text(js,encoding='utf-8')
print('v1.9.7 classic technical drawing: solid visible + dashed hidden, no mesh triangles, menu-only close')