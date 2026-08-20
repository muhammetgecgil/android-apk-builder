from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v209.js' not in h:
    h=h.replace('</body>','<script src="/cad-v209.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function txt(el){return ((el&&el.textContent)||'').trim().toUpperCase()}
const savedExit=(typeof window.exitTechDrawing==='function')?window.exitTechDrawing:null;
function leaveTech(){
  try{if(savedExit){savedExit();return;}}catch(e){}
  try{
    const toggle=Array.from(document.querySelectorAll('button')).find(function(b){return txt(b)==='TEKNİK RESİM';});
    if(toggle){toggle.click();return;}
  }catch(e){}
  try{
    document.body.classList.remove('mg-tech-sheet');
    document.querySelectorAll('[data-tech-sheet],#techSheet,#technicalDrawingSheet,.tech-sheet').forEach(function(e){e.remove();});
    if(typeof window.fit==='function')window.fit();
  }catch(e){}
}
function fix(){
  try{
    Array.from(document.querySelectorAll('button')).forEach(function(b){
      const t=txt(b);
      if(t==='MENÜ'||t==='MENU'||t==='PDF') b.remove();
    });
    const exits=Array.from(document.querySelectorAll('button')).filter(function(b){return txt(b)==='TEKNİK RESİMDEN ÇIK';});
    exits.forEach(function(b,i){
      if(i>0){b.remove();return;}
      b.style.display='';
      b.onclick=function(ev){if(ev){ev.preventDefault();ev.stopPropagation();}leaveTech();};
    });
  }catch(e){}
}
new MutationObserver(fix).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style']});
setInterval(fix,400);fix();
window.MG_CAD_V209={version:'2.0.9',techMenuRemoved:true,allPdfButtonsRemoved:true,techExitWorks:true};
})();'''
(AS/'cad-v209.js').write_text(js,encoding='utf-8')
print('v2.0.9: removed MENU and all PDF buttons; technical drawing exit fixed')
