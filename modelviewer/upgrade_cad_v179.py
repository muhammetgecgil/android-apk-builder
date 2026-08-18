from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
# Remove the legacy top-left back button next to MG CAD PRO.
s=s.replace('<div id="top"><button onclick="closeApp()">←</button><span id="badge">MG CAD PRO</span>', '<div id="top"><span id="badge">MG CAD PRO</span>', 1)
if '/cad-v179.js' not in s:
    s=s.replace('</body>','<script src="/cad-v179.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
let undoStack=[];
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function B(txt){return Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()===txt)}
function pushUndo(label,fn){undoStack.push({label,fn});if(undoStack.length>30)undoStack.shift();updateUndo()}
function updateUndo(){const b=document.getElementById('mgUndo');if(b){b.disabled=!undoStack.length;b.textContent=undoStack.length?'GERİ AL':'GERİ AL'}}
function restoreVisible(state){state.forEach((v,i)=>{if(meshes()[i])meshes()[i].visible=v})}
function installHideToggle(){
 const hide=B('GİZLE'); if(!hide)return;
 let hiddenPart=null;
 hide.onclick=()=>{
   const ms=meshes(); if(!ms.length)return;
   if(hiddenPart && ms.includes(hiddenPart) && !hiddenPart.visible){
     const target=hiddenPart; const before=ms.map(m=>m.visible); target.visible=true; hiddenPart=null;
     pushUndo('Gizlemeyi geri al',()=>{restoreVisible(before);hiddenPart=target});
     if(typeof selectMesh==='function')selectMesh(target);
     hide.textContent='GİZLE';
     return;
   }
   let target=(typeof selected!=='undefined'&&selected&&ms.includes(selected))?selected:null;
   if(!target)target=ms.find(m=>m.visible)||ms[0];
   const before=ms.map(m=>m.visible); target.visible=false; hiddenPart=target;
   pushUndo('Parçayı gizle',()=>{restoreVisible(before);hiddenPart=null;if(typeof selectMesh==='function')selectMesh(target)});
   if(typeof selectMesh==='function')selectMesh(null);
   hide.textContent='GERİ GETİR';
 };
 const all=B('TÜMÜ'); if(all){const old=all.onclick;all.onclick=()=>{const ms=meshes(),before=ms.map(m=>m.visible);if(old)old();else ms.forEach(m=>m.visible=true);hiddenPart=null;hide.textContent='GİZLE';pushUndo('Tümünü göster',()=>restoreVisible(before))}}
 const iso=B('İZOLE'); if(iso){const old=iso.onclick;iso.onclick=()=>{const ms=meshes(),before=ms.map(m=>m.visible);if(old)old();pushUndo('İzole',()=>restoreVisible(before))}}
}
function installUndo(){
 const top=document.getElementById('top');if(!top||document.getElementById('mgUndo'))return;
 const b=document.createElement('button');b.id='mgUndo';b.textContent='GERİ AL';b.disabled=true;
 const name=document.getElementById('name'); if(name&&name.nextSibling)top.insertBefore(b,name.nextSibling); else top.appendChild(b);
 b.onclick=()=>{const a=undoStack.pop();if(a){try{a.fn()}catch(e){}updateUndo()}};
}
function installExplodeUndo(){
 const r=document.getElementById('explodeRange')||document.getElementById('explode');if(!r)return;
 let startVal=+r.value;
 r.addEventListener('pointerdown',()=>{startVal=+r.value});
 r.addEventListener('change',()=>{const endVal=+r.value;if(endVal===startVal)return;pushUndo('Patlatılmış görünüm',()=>{r.value=startVal;r.dispatchEvent(new Event('input',{bubbles:true}))});startVal=endVal});
}
function boot(){installUndo();installHideToggle();installExplodeUndo();window.MG_CAD_V179={version:'1.7.9',hideToggle:true,undo:true,noBackButton:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,180));else setTimeout(boot,180);
})();
'''
(AS/'cad-v179.js').write_text(js,encoding='utf-8')
print('v1.7.9 hide toggle + undo + top-left cleanup applied')
