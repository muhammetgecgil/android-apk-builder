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
let outlineGroup=null;
function meshes(){try{return group.children.filter(x=>x&&x.isMesh&&x.visible)}catch(e){return[]}}
function clearOutline(){if(outlineGroup){try{scene.remove(outlineGroup);outlineGroup.traverse(o=>{if(o.geometry)o.geometry.dispose();if(o.material)o.material.dispose()})}catch(e){}outlineGroup=null}}
function makeOuterContours(){
 clearOutline();outlineGroup=new THREE.Group();outlineGroup.renderOrder=80;
 meshes().forEach(m=>{try{
   const eg=new THREE.EdgesGeometry(m.geometry,45); // only strong silhouette/crease candidates, suppress dense internal mesh lines
   const mat=new THREE.LineBasicMaterial({color:0xffffff,transparent:false});
   const ln=new THREE.LineSegments(eg,mat);ln.matrixAutoUpdate=false;ln.matrix.copy(m.matrixWorld);ln.renderOrder=80;outlineGroup.add(ln);
   if(m.material){m.userData.mg195OldVisible=m.material.visible;m.material.visible=false;}
 }catch(e){}});
 scene.add(outlineGroup);
}
function restoreMeshes(){meshes().forEach(m=>{try{if(m.material)m.material.visible=(m.userData.mg195OldVisible!==undefined?m.userData.mg195OldVisible:true)}catch(e){}});clearOutline()}
function addDims(){try{const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}catch(e){}
 try{if(typeof autoDiameters==='function')autoDiameters()}catch(e){}
}
function setView(v){try{if(typeof viewDir==='function')viewDir(v);if(typeof fit==='function')fit()}catch(e){}
 setTimeout(()=>{makeOuterContours();addDims();const r=E('proResult');if(r)r.innerHTML='<b>TEKNİK RESİM</b><br>'+String(v).toUpperCase()+' görünüş • yalnız dış kontur çizgileri • temel ölçülendirme';},100)}
function install(){
 const old=[...document.querySelectorAll('button')].find(x=>/TEKNİK\s*RESİM/i.test((x.textContent||'').trim()));if(!old||old.dataset.mg195)return false;
 old.dataset.mg195='1';old.textContent='TEKNİK RESİM';
 old.onclick=()=>{let p=E('mgDrawing195');if(p){restoreMeshes();p.remove();return}p=document.createElement('div');p.id='mgDrawing195';p.className='panel';p.style.cssText='position:fixed;left:10px;top:68px;z-index:40;max-width:340px;padding:10px;background:rgba(3,10,20,.96);border:1px solid #28506e';p.innerHTML='<div class="head"><b>TEKNİK RESİM</b></div><div class="row"><button id="d195Front">ÖN</button><button id="d195Top">ÜST</button><button id="d195Right">SAĞ</button><button id="d195Iso">İZO</button></div><div class="small">Yalnız temel görünüşler, dış kontur çizgileri ve temel ölçülendirme gösterilir. İç mesh/tel çizgileri gizlenir.</div><div class="row"><button id="d195Close">KAPAT</button></div>';document.body.appendChild(p);E('d195Front').onclick=()=>setView('front');E('d195Top').onclick=()=>setView('top');E('d195Right').onclick=()=>setView('right');E('d195Iso').onclick=()=>setView('iso');E('d195Close').onclick=()=>{restoreMeshes();p.remove()};setView('front')};
 return true;
}
function boot(){install();let n=0;const t=setInterval(()=>{n++;if(install()||n>20)clearInterval(t)},300);window.MG_CAD_V195={version:'1.9.5',basicDrawingViews:true,outerContoursOnly:true,basicDimensions:true};}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,1400));else setTimeout(boot,1400);
})();'''
(AS/'cad-v195.js').write_text(js,encoding='utf-8')
print('v1.9.5 basic technical drawing views + outer contours only')
