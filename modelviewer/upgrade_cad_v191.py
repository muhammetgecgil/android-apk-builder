from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v191.js' not in s:
    s=s.replace('</body>','<script src="/cad-v191.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
function E(id){return document.getElementById(id)}
function rightPanel(){
  return E('tools')||E('industryModes')?.closest('.panel')||Array.from(document.querySelectorAll('.panel')).find(p=>{const r=p.getBoundingClientRect();return r.width>240&&r.left>innerWidth*.55&&r.height>250})||null;
}
let lock=false;
function alignBottomBar(){
  if(lock)return false;
  const d=E('autoDimB');if(!d||!d.parentElement)return false;
  const row=d.parentElement,p=rightPanel();if(!p)return false;
  lock=true;
  try{
    row.style.transform='none';row.style.position='fixed';row.style.bottom='10px';row.style.right='auto';row.style.zIndex='30';
    row.style.display='flex';row.style.flexWrap='nowrap';row.style.gap='6px';row.style.maxWidth='none';row.style.width='max-content';
    // The right edge is the invariant anchor. If ANALIZ appears, all added width grows LEFT only.
    const panelLeft=p.getBoundingClientRect().left;
    const anchor=Math.round(panelLeft-12);
    const width=Math.ceil(row.getBoundingClientRect().width);
    row.style.left=Math.max(8,anchor-width)+'px';
    row.dataset.mgRightAnchor=String(anchor);
    return true;
  } finally { lock=false; }
}
function boot(){
  const d=E('autoDimB');if(!d||!d.parentElement)return;
  const row=d.parentElement,p=rightPanel();
  alignBottomBar();
  // React immediately to ANALIZ restore button appearing/disappearing or any button text/size change.
  if(window.ResizeObserver){
    const ro=new ResizeObserver(()=>requestAnimationFrame(alignBottomBar));
    ro.observe(row);if(p)ro.observe(p);
  }
  if(window.MutationObserver){
    const mo=new MutationObserver(()=>requestAnimationFrame(alignBottomBar));
    mo.observe(row,{childList:true,subtree:true,attributes:true,characterData:true,attributeFilter:['style','class']});
  }
  addEventListener('resize',()=>requestAnimationFrame(alignBottomBar));
  setInterval(alignBottomBar,1200);
  window.MG_CAD_V191={version:'1.9.1',bottomRightAnchorPersistent:true,analysisExpandsLeftOnly:true};
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,900));else setTimeout(boot,900);
})();'''
(AS/'cad-v191.js').write_text(js,encoding='utf-8')
print('v1.9.1 bottom bar right anchor persistent; analysis expands left only')
