from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v206.js' not in h:
    h=h.replace('</body>','<script src="/cad-v206.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function t(el){return ((el&&el.textContent)||'').trim().toUpperCase()}
function visible(el){if(!el)return false;const s=getComputedStyle(el);return s.display!=='none'&&s.visibility!=='hidden'}
function buttons(label){return Array.from(document.querySelectorAll('button')).filter(b=>t(b)===label)}
function fix(){
  try{
    const secAxis=document.getElementById('secAxis');
    if(secAxis){
      const row=secAxis.parentElement;
      if(row){
        // Right-side section row: axis selector, minus, plus.
        let minus=document.getElementById('mgSectionMinus') || buttons('-').find(visible);
        let plus=document.getElementById('mgSectionPlus') || buttons('+').find(visible);
        if(minus){minus.id='mgSectionMinus';minus.style.minWidth='52px';minus.style.flex='0 0 52px';if(minus.parentElement!==row)row.appendChild(minus);}
        if(plus){plus.id='mgSectionPlus';plus.style.minWidth='52px';plus.style.flex='0 0 52px';if(plus.parentElement!==row)row.appendChild(plus);}
        row.style.display='flex';row.style.alignItems='center';row.style.gap='6px';row.style.flexWrap='nowrap';
        secAxis.style.flex='1 1 auto';
      }
    }
    // Bottom measurement controls: remove any leftover +/- and shift remaining group right.
    const meas=buttons('ÖLÇÜLENDİR').find(visible) || buttons('ÖLÇÜLERİ KAPAT').find(visible);
    if(meas&&meas.parentElement){
      const bar=meas.parentElement;
      Array.from(bar.querySelectorAll('button')).forEach(b=>{if((t(b)==='-'||t(b)==='+')&&b.id!=='mgSectionMinus'&&b.id!=='mgSectionPlus')b.style.display='none';});
      Array.from(bar.querySelectorAll('select')).forEach(s=>{if(s.id!=='unit')s.style.display='none';});
      bar.style.display='flex';bar.style.justifyContent='flex-end';bar.style.alignItems='center';bar.style.gap='8px';bar.style.paddingRight='8px';
      bar.style.left='auto';bar.style.right='calc(220px + 24px)';
    }
  }catch(e){}
}
new MutationObserver(fix).observe(document.documentElement,{subtree:true,childList:true,attributes:true,attributeFilter:['style','class']});
setInterval(fix,700);fix();
window.MG_CAD_V206={version:'2.0.6',sectionMinusBesidePlus:true,bottomToolbarShiftRight:true};
})();'''
(AS/'cad-v206.js').write_text(js,encoding='utf-8')
print('v2.0.6: section minus moved beside plus; bottom measurement toolbar shifted right')
