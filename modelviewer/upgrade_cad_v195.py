from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v195.js' not in s:
    s=s.replace('</body>','<script src="/cad-v195.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
const E=id=>document.getElementById(id);
let outlineGroup=null,savedBg=null,saved=[];
function meshes(){try{return group.children.filter(x=>x&&x.isMesh&&!x.userData?.mg195Outline)}catch(e){return[]}}
function clearOutline(){if(outlineGroup){try{scene.remove(outlineGroup);outlineGroup.traverse(o=>{if(o.geometry)o.geometry.dispose();if(o.material)o.material.dispose()})}catch(e){}outlineGroup=null}}
function faceN(m,g,t){const p=g.attributes.position,idx=g.index,vi=k=>idx?idx.getX(t*3+k):t*3+k;const A=new THREE.Vector3().fromBufferAttribute(p,vi(0)).applyMatrix4(m.matrixWorld),B=new THREE.Vector3().fromBufferAttribute(p,vi(1)).applyMatrix4(m.matrixWorld),C=new THREE.Vector3().fromBufferAttribute(p,vi(2)).applyMatrix4(m.matrixWorld);return new THREE.Vector3().crossVectors(B.clone().sub(A),C.clone().sub(A)).normalize()}
function worldV(m,g,i){return new THREE.Vector3().fromBufferAttribute(g.attributes.position,i).applyMatrix4(m.matrixWorld)}
function makeVisibleContours(){
 clearOutline();outlineGroup=new THREE.Group();outlineGroup.userData.mg195Outline=true;scene.add(outlineGroup);const vd=new THREE.Vector3();camera.getWorldDirection(vd).normalize();
 meshes().forEach(m=>{try{const g=m.geometry,p=g.attributes.position,idx=g.index;if(!p)return;m.updateMatrixWorld(true);const tc=idx?idx.count/3:p.count/3,edge=new Map(),vi=(t,k)=>idx?idx.getX(t*3+k):t*3+k;
 for(let t=0;t<tc;t++){const n=faceN(m,g,t),front=n.dot(vd)<0,ids=[vi(t,0),vi(t,1),vi(t,2)];[[ids[0],ids[1]],[ids[1],ids[2]],[ids[2],ids[0]]].forEach(([a,b])=>{let x=a,y=b;if(x>y){const q=x;x=y;y=q}const k=x+','+y;if(!edge.has(k))edge.set(k,{a:x,b:y,faces:[]});edge.get(k).faces.push({front,n})})}
 const arr=[];edge.forEach(e=>{let draw=e.faces.length===1?e.faces[0].front:false;if(e.faces.length>=2)draw=e.faces[0].front!==e.faces[1].front;if(!draw)return;const A=worldV(m,g,e.a),B=worldV(m,g,e.b);arr.push(A.x,A.y,A.z,B.x,B.y,B.z)});if(arr.length){const gg=new THREE.BufferGeometry();gg.setAttribute('position',new THREE.Float32BufferAttribute(arr,3));const mm=new THREE.LineBasicMaterial({color:0x111111,depthTest:false});const ls=new THREE.LineSegments(gg,mm);ls.renderOrder=95;outlineGroup.add(ls)}}catch(e){}})
}
function addDims(){try{const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}catch(e){}try{if(typeof autoDiameters==='function')autoDiameters()}catch(e){}}
function radiusList(){try{const f=typeof detectCircularFeatures==='function'?(detectCircularFeatures()||[]):[];const vals=[];f.forEach(q=>{if(isFinite(q.r)&&q.r>0)vals.push(q.r)});return [...new Set(vals.map(x=>Number(x).toFixed(3)))].slice(0,24)}catch(e){return[]}}
function showRadius(){const rs=radiusList(),r=E('proResult');if(r)r.innerHTML='<b>RADYÜS ÖLÇÜLERİ</b><br>'+(rs.length?rs.map(x=>'R '+x+' mm').join(' • '):'Radyüs algılanmadı.');try{if(window.MG_CAD_V194&&typeof addRadiusCallouts==='function')addRadiusCallouts()}catch(e){}}
function paperMode(on){if(on){if(savedBg===null)savedBg=scene.background;saved=meshes().map(m=>({m,v:m.visible}));saved.forEach(x=>x.m.visible=false);scene.background=new THREE.Color(0xf7f7f4);try{renderer.setClearColor(0xf7f7f4,1)}catch(e){}makeVisibleContours()}else{clearOutline();saved.forEach(x=>{try{x.m.visible=x.v}catch(e){}});saved=[];if(savedBg!==null)scene.background=savedBg;savedBg=null;try{renderer.setClearColor(0x07111f,1)}catch(e){}}}
function setView(v){paperMode(false);try{if(typeof viewDir==='function')viewDir(v);if(typeof fit==='function')fit()}catch(e){}setTimeout(()=>{paperMode(true);addDims();const r=E('proResult');if(r)r.innerHTML='<b>TEKNİK RESİM</b><br>'+String(v).toUpperCase()+' görünüş • yalnız görünür dış/siluet çizgileri • temel ölçüler • radyüs desteği';},120)}
function install(){const old=[...document.querySelectorAll('button')].find(x=>/TEKNİK\s*RESİM/i.test((x.textContent||'').trim()));if(!old||old.dataset.mg195)return false;old.dataset.mg195='1';old.textContent='TEKNİK RESİM';old.onclick=()=>{let p=E('mgDrawing195');if(p){paperMode(false);p.remove();return}p=document.createElement('div');p.id='mgDrawing195';p.style.cssText='position:fixed;left:10px;top:68px;z-index:45;width:330px;padding:10px;background:rgba(250,250,248,.98);color:#111;border:1px solid #777;border-radius:6px;box-shadow:0 4px 16px rgba(0,0,0,.25)';p.innerHTML='<div style="font-weight:800;margin-bottom:8px">TEKNİK RESİM</div><div class="row"><button id="d195Front">ÖN</button><button id="d195Top">ÜST</button><button id="d195Right">SAĞ</button><button id="d195Iso">İZO</button></div><div class="row"><button id="d195Dim">ÖLÇÜLENDİR</button><button id="d195Rad">RADYÜS R</button></div><div style="font-size:12px;line-height:1.35;margin:8px 0">Tam teknik resim görünüşü: beyaz zemin, siyah görünür dış/siluet çizgileri, gölgeleme yok, iç mesh/tel çizgileri yok. Sadece temel görünüşler ve temel ölçülendirme. Radyüsler R olarak gösterilir.</div><div class="row"><button id="d195Close">KAPAT</button></div>';document.body.appendChild(p);E('d195Front').onclick=()=>setView('front');E('d195Top').onclick=()=>setView('top');E('d195Right').onclick=()=>setView('right');E('d195Iso').onclick=()=>setView('iso');E('d195Dim').onclick=()=>addDims();E('d195Rad').onclick=()=>showRadius();E('d195Close').onclick=()=>{paperMode(false);p.remove()};setView('front')};return true}
function boot(){install();let n=0;const t=setInterval(()=>{n++;if(install()||n>20)clearInterval(t)},300);window.MG_CAD_V195={version:'1.9.5',trueTechnicalDrawing:true,whitePaper:true,visibleSilhouetteOnly:true,basicViewsOnly:true,basicDimensions:true,radiusDimensions:true};}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,1400));else setTimeout(boot,1400);
})();'''
(AS/'cad-v195.js').write_text(js,encoding='utf-8')
print('v1.9.5 true technical drawing: white paper + silhouette only + basic views + radius')
