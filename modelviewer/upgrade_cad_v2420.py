from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2420.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2420.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').trim().toLocaleUpperCase('tr-TR')}
function init(){
 const info=document.getElementById('info');
 if(!info)return;
 info.style.display='none';
 info.style.position='fixed';
 info.style.left='16px';
 info.style.bottom='96px';
 info.style.top='auto';
 info.style.width='min(42vw,650px)';
 info.style.maxWidth='calc(100vw - 48px)';
 info.style.maxHeight='42vh';
 info.style.overflowY='auto';
 info.style.overflowX='hidden';
 info.style.zIndex='44';
 info.style.boxSizing='border-box';
 info.style.margin='0';

 const buttons=[...document.querySelectorAll('button')];
 const cad=buttons.find(b=>/^CAD\b/.test(tr(b.textContent)));
 let analysis=buttons.find(b=>tr(b.textContent).startsWith('ANALİZ'));
 if(analysis){
   const clean=analysis.cloneNode(true);
   clean.id='mgAnalysis2420';
   clean.textContent='ANALİZ';
   analysis.replaceWith(clean);
   analysis=clean;
 }else{
   analysis=document.createElement('button');
   analysis.id='mgAnalysis2420';
   analysis.textContent='ANALİZ';
 }
 analysis.style.whiteSpace='nowrap';
 analysis.onclick=function(e){
   e.preventDefault();e.stopPropagation();
   const open=info.style.display==='none';
   info.style.display=open?'block':'none';
   analysis.textContent=open?'ANALİZ ◀':'ANALİZ';
 };
 if(cad&&cad.parentElement){
   const parent=cad.parentElement;
   if(getComputedStyle(parent).display!=='flex'){
     parent.style.display='flex';
     parent.style.gap='8px';
     parent.style.alignItems='center';
   }
   if(analysis.parentElement!==parent) parent.insertBefore(analysis,cad.nextSibling);
 }
 const drag=document.getElementById('mgInfoDrag');
 if(drag){
   drag.style.cursor='grab';
   drag.textContent='MODEL ANALİZİ ↕ SÜRÜKLE';
 }
 window.addEventListener('resize',()=>{
   info.style.left='16px';
   info.style.bottom='96px';
   info.style.top='auto';
 },{passive:true});
 window.MG_CAD_V2420={version:'2.4.2',analysisNextToCad:true,analysisClosedOnStart:true,analysisBottomLeft:true,toolbarSafeGap:true};
}
ready(init);
})();'''
(AS/'cad-v2420.js').write_text(js,encoding='utf-8')
print('v2.4.2: ANALIZ button beside CAD, panel closed by default, bottom-left safe placement')
