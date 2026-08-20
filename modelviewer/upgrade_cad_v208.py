from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v208.js' not in h:
    h=h.replace('</body>','<script src="/cad-v208.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function txt(el){return ((el&&el.textContent)||'').trim().toUpperCase()}
const originalExit=(typeof window.exitTechDrawing==='function')?window.exitTechDrawing:null;
function pdfAction(){
  try{
    if(window.AndroidHost&&typeof AndroidHost.printTechnicalDrawing==='function'){AndroidHost.printTechnicalDrawing();return;}
    if(typeof window.mgTechPdf==='function'){window.mgTechPdf();return;}
    window.print();
  }catch(e){try{window.print()}catch(_){}}
}
function leaveTech(){
  try{
    if(originalExit){originalExit();return;}
    const toggle=Array.from(document.querySelectorAll('button')).find(b=>txt(b)==='TEKNİK RESİM');
    if(toggle){toggle.click();return;}
    document.body.classList.remove('mg-tech-sheet');
    document.querySelectorAll('[data-tech-sheet],#techSheet,#technicalDrawingSheet,.tech-sheet').forEach(e=>e.remove());
    if(typeof window.fit==='function')window.fit();
  }catch(e){}
}
function fix(){
  try{
    Array.from(document.querySelectorAll('button')).filter(b=>txt(b)==='MENÜ'||txt(b)==='MENU').forEach(b=>b.remove());
    let pdfs=Array.from(document.querySelectorAll('button')).filter(b=>txt(b)==='PDF');
    if(document.body.classList.contains('mg-tech-sheet')&&pdfs.length===0){
      const exit=Array.from(document.querySelectorAll('button')).find(b=>txt(b)==='TEKNİK RESİMDEN ÇIK');
      if(exit&&exit.parentElement){const p=document.createElement('button');p.textContent='PDF';p.className=exit.className;p.onclick=pdfAction;exit.parentElement.insertBefore(p,exit);pdfs=[p];}
    }
    pdfs.forEach((b,i)=>{if(i===0){b.style.display='';b.onclick=pdfAction;}else b.remove();});
    const exits=Array.from(document.querySelectorAll('button')).filter(b=>txt(b)==='TEKNİK RESİMDEN ÇIK');
    exits.forEach((b,i)=>{if(i===0){b.onclick=leaveTech;}else b.remove();});
  }catch(e){}
}
new MutationObserver(fix).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class']});
setInterval(fix,600);fix();
window.MG_CAD_V208={version:'2.0.8',singlePdfButton:true,pdfWorks:true,techExitReturnsToCad:true};
})();'''
(AS/'cad-v208.js').write_text(js,encoding='utf-8')
print('v2.0.8: single working PDF; technical drawing exit returns to CAD screen')
