from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v205.js' not in h:
    h=h.replace('</body>','<script src="/cad-v205.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function txt(el){return ((el&&el.textContent)||'').trim().toUpperCase()}
function allButtons(t){return Array.from(document.querySelectorAll('button')).filter(b=>txt(b)===t)}
function visible(el){if(!el)return false;const s=getComputedStyle(el);return s.display!=='none'&&s.visibility!=='hidden'}
function cleanupTechToolbar(){
  try{
    if(!document.body.classList.contains('mg-tech-sheet')) return;
    allButtons('MENÜ').forEach(b=>b.remove());
    const pdfs=allButtons('PDF');
    pdfs.forEach((b,i)=>{if(i===0){b.style.display='';b.onclick=function(){window.mgTechPdf&&window.mgTechPdf();};}else b.remove();});
    const exits=allButtons('TEKNİK RESİMDEN ÇIK');
    exits.forEach((b,i)=>{if(i>0)b.remove();});
  }catch(e){}
}
function relocateSectionControls(){
  try{
    const secAxis=document.getElementById('secAxis');
    const section=document.getElementById('section');
    if(!secAxis||!section) return;
    let row=secAxis.parentElement;
    if(!row) return;
    // Keep the right-side section axis selector; remove duplicate bottom X/Y/Z selector instead.
    const selects=Array.from(document.querySelectorAll('select')).filter(s=>s!==secAxis&&s.id!=='unit'&&['X','Y','Z'].includes((s.value||'').toUpperCase()));
    selects.forEach(s=>s.style.display='none');
    // Move the visible bottom - / + controls into the section row.
    const minus=allButtons('-').find(visible);
    const plus=allButtons('+').find(visible);
    if(minus&&minus.parentElement!==row){minus.id='mgSectionMinus';minus.style.minWidth='52px';row.appendChild(minus);}
    if(plus&&plus.parentElement!==row){plus.id='mgSectionPlus';plus.style.minWidth='52px';row.appendChild(plus);}
    row.style.display='flex';row.style.alignItems='center';row.style.gap='6px';row.style.flexWrap='nowrap';
    // The former bottom command bar now contains only measurement commands and can expand into freed space.
    allButtons('ÖLÇÜLENDİR').forEach(b=>{if(b.parentElement){b.parentElement.style.justifyContent='flex-start';b.parentElement.style.gap='8px';}});
  }catch(e){}
}
function dockAnalysis(){try{const i=document.getElementById('info');if(i){i.style.bottom='8px';i.style.maxHeight='36vh';i.style.overflowY='auto';i.style.touchAction='pan-y';}}catch(e){}}
function apply(){cleanupTechToolbar();relocateSectionControls();dockAnalysis();}
new MutationObserver(apply).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['class','style']});
setInterval(apply,900);apply();
window.MG_CAD_V205={version:'2.0.5',singlePdfButton:true,techExitPreserved:true,sectionPlusMinusMoved:true,bottomAxisSelectorRemoved:true,measurementButtonsExpanded:true};
})();'''
(AS/'cad-v205.js').write_text(js,encoding='utf-8')
print('v2.0.5: technical toolbar deduped, section +/- moved, bottom axis selector removed')
