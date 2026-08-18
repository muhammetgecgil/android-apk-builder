from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v184.js' not in s:
    s=s.replace('</body>','<script src="/cad-v184.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
const undo=[];
let edgeOn=true;
let edgeInstalled=false;
let hiddenToggle=null;
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function B(txt){return Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()===txt)}
function cloneV(v){return v?{x:v.x,y:v.y,z:v.z}:null}
function snapshot(label){
 const ms=meshes();
 return {label:label||'İşlem',vis:ms.map(m=>m.visible),pos:ms.map(m=>cloneV(m.position)),
  cameraPos:cloneV(camera.position),cameraQuat:{x:camera.quaternion.x,y:camera.quaternion.y,z:camera.quaternion.z,w:camera.quaternion.w},target:cloneV(controls.target),
  explode:(document.getElementById('explodeRange')||document.getElementById('explode'))?.value,
  section:document.getElementById('section')?.value,sectionOn:typeof sectionOn!=='undefined'?sectionOn:false,
  wire:typeof wire!=='undefined'?wire:false,xray:typeof xray!=='undefined'?xray:false,grid:grid?grid.visible:true,
  edgeOn:edgeOn,dimOn:!!document.getElementById('autoDimB')?.classList.contains('on')};
}
function push(label){undo.push(snapshot(label));if(undo.length>40)undo.shift();updateUndo()}
function updateUndo(){const b=document.getElementById('mgUndo');if(b){b.disabled=!undo.length;b.textContent=undo.length?'GERİ AL ('+undo.length+')':'GERİ AL'}}
function restore(s){
 const ms=meshes();ms.forEach((m,i)=>{if(i<s.vis.length)m.visible=s.vis[i];if(s.pos[i])m.position.set(s.pos[i].x,s.pos[i].y,s.pos[i].z)});
 if(s.cameraPos)camera.position.set(s.cameraPos.x,s.cameraPos.y,s.cameraPos.z);if(s.cameraQuat)camera.quaternion.set(s.cameraQuat.x,s.cameraQuat.y,s.cameraQuat.z,s.cameraQuat.w);if(s.target)controls.target.set(s.target.x,s.target.y,s.target.z);
 const ex=document.getElementById('explodeRange')||document.getElementById('explode');if(ex&&s.explode!==undefined){ex.value=s.explode;ex.dispatchEvent(new Event('input',{bubbles:true}))}
 const sec=document.getElementById('section');if(sec&&s.section!==undefined){sec.value=s.section;try{if(typeof sectionOn!=='undefined'&&sectionOn!==s.sectionOn&&typeof toggleSection==='function')toggleSection();if(typeof applySection==='function')applySection()}catch(e){}}
 try{if(typeof wire!=='undefined'&&wire!==s.wire&&typeof toggleWire==='function')toggleWire();if(typeof xray!=='undefined'&&xray!==s.xray&&typeof toggleXray==='function')toggleXray();if(grid&&grid.visible!==s.grid&&typeof toggleGrid==='function')toggleGrid()}catch(e){}
 setEdgeMode(s.edgeOn,false);controls.update();updateUndo();
}
function installUndo(){
 let b=document.getElementById('mgUndo');const top=document.getElementById('top');if(!b&&top){b=document.createElement('button');b.id='mgUndo';b.textContent='GERİ AL';const n=document.getElementById('name');top.insertBefore(b,n?n.nextSibling:null)}
 if(!b)return;b.onclick=()=>{const s=undo.pop();if(s)restore(s)};updateUndo();
 // Replace part operations with snapshot-backed behavior.
 const hide=B('GİZLE')||B('GERİ GETİR');if(hide)hide.onclick=()=>{const ms=meshes();if(!ms.length)return;push('Gizle/Geri getir');if(hiddenToggle&&ms.includes(hiddenToggle)&&!hiddenToggle.visible){hiddenToggle.visible=true;try{selectMesh(hiddenToggle)}catch(e){};hiddenToggle=null;hide.textContent='GİZLE';return}let t=(typeof selected!=='undefined'&&selected&&ms.includes(selected))?selected:ms.find(m=>m.visible);if(!t)return;t.visible=false;hiddenToggle=t;try{selectMesh(null)}catch(e){};hide.textContent='GERİ GETİR'};
 const iso=B('İZOLE');if(iso)iso.onclick=()=>{const ms=meshes();let t=(typeof selected!=='undefined'&&selected&&ms.includes(selected))?selected:ms.find(m=>m.visible);if(!t)return;push('İzole');ms.forEach(m=>m.visible=m===t)};
 const all=B('TÜMÜ');if(all)all.onclick=()=>{push('Tümünü göster');meshes().forEach(m=>m.visible=true);hiddenToggle=null;if(hide)hide.textContent='GİZLE'};
 // Wrap common global view state functions.
 [['toggleWire','Tel görünüm'],['toggleXray','X-Ray'],['toggleGrid','Grid'],['toggleSection','Kesit'],['resetAll','Sıfırla']].forEach(([name,label])=>{const old=window[name];if(typeof old==='function'&&!old.__mg184){const f=function(){push(label);return old.apply(this,arguments)};f.__mg184=true;window[name]=f}});
 const exp=document.getElementById('explodeRange')||document.getElementById('explode');if(exp){let start=null;exp.addEventListener('pointerdown',()=>{start=snapshot('Patlatılmış görünüm')},{capture:true});exp.addEventListener('change',()=>{if(start){undo.push(start);if(undo.length>40)undo.shift();start=null;updateUndo()}},{capture:true})}
 const sec=document.getElementById('section');if(sec){let start=null;sec.addEventListener('pointerdown',()=>{start=snapshot('Kesit konumu')},{capture:true});sec.addEventListener('change',()=>{if(start){undo.push(start);if(undo.length>40)undo.shift();start=null;updateUndo()}},{capture:true})}
}
function installTouch(){
 try{controls.enableZoom=true;controls.enablePan=true;controls.zoomSpeed=1.05;controls.panSpeed=.75;controls.rotateSpeed=.7;if(THREE.TOUCH){controls.touches.ONE=THREE.TOUCH.ROTATE;controls.touches.TWO=THREE.TOUCH.DOLLY_PAN}canvas.style.touchAction='none';document.body.style.overscrollBehavior='none'}catch(e){}
}
function edgeChildren(){const a=[];meshes().forEach(m=>m.children.forEach(c=>{if(c.userData&&c.userData.mgEdge)a.push(c)}));return a}
function installEdges(){
 const ms=meshes();if(!ms.length)return false;let totalTri=0;ms.forEach(m=>{const g=m.geometry,p=g&&g.attributes&&g.attributes.position;if(g&&p)totalTri+=g.index?g.index.count/3:p.count/3});
 // Adaptive renderer load for large models.
 try{const target=totalTri>700000?1.25:totalTri>300000?1.5:1.75;renderer.setPixelRatio(Math.min(devicePixelRatio,target));resize()}catch(e){}
 if(edgeInstalled)return true;
 ms.forEach(m=>{try{const g=m.geometry,p=g&&g.attributes&&g.attributes.position;if(!p)return;const tri=g.index?g.index.count/3:p.count/3;if(tri>350000)return;const eg=new THREE.EdgesGeometry(g,18);const em=new THREE.LineBasicMaterial({color:0x07131f,transparent:true,opacity:.92,depthTest:true,depthWrite:false});const lines=new THREE.LineSegments(eg,em);lines.userData.mgEdge=true;lines.renderOrder=3;m.add(lines)}catch(e){}});
 edgeInstalled=true;setEdgeMode(edgeOn,false);return true;
}
function setEdgeMode(on,record=true){if(record)push(on?'Kenar görünüm aç':'Kenar görünüm kapat');edgeOn=!!on;edgeChildren().forEach(e=>e.visible=edgeOn);const a=document.getElementById('mgEdgeOn'),b=document.getElementById('mgEdgeOff');if(a)a.classList.toggle('on',edgeOn);if(b)b.classList.toggle('on',!edgeOn)}
function installDisplayButtons(){
 const tools=document.getElementById('tools');if(!tools||document.getElementById('mgEdgeOn'))return;const heads=Array.from(tools.querySelectorAll('.head'));const h=heads.find(x=>x.textContent.trim().toUpperCase()==='GÖRÜNÜM');if(!h)return;const row=document.createElement('div');row.className='row';row.innerHTML='<button id="mgEdgeOn">KENAR + GÖLGE</button><button id="mgEdgeOff">SADE GÖLGE</button>';h.insertAdjacentElement('afterend',row);row.querySelector('#mgEdgeOn').onclick=()=>{installEdges();setEdgeMode(true)};row.querySelector('#mgEdgeOff').onclick=()=>setEdgeMode(false);const cad=['step','stp','iges','igs','brep','brp'].includes((window.fileType||'').toLowerCase());edgeOn=cad;setEdgeMode(edgeOn,false)
}
function boot(){installTouch();installUndo();installDisplayButtons();let tries=0;const t=setInterval(()=>{tries++;if(installEdges()||tries>24)clearInterval(t)},250);window.MG_CAD_V184={version:'1.8.4',reliableUndo:true,pinchZoom:true,shadedEdges:true,adaptiveDpr:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,320));else setTimeout(boot,320);
})();
'''
(AS/'cad-v184.js').write_text(js,encoding='utf-8')
print('v1.8.4 reliable undo + pinch zoom + shaded edges + adaptive performance applied')
