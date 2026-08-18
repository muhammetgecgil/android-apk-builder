from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v190.js' not in s:
    s=s.replace('</body>','<script src="/cad-v190.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
function E(id){return document.getElementById(id)}
function rightPanel(){
  return E('tools')||E('industryModes')?.closest('.panel')||Array.from(document.querySelectorAll('.panel')).find(p=>{const r=p.getBoundingClientRect();return r.width>240&&r.left>innerWidth*.55&&r.height>250})||null;
}
function alignBottomBar(){
  const d=E('autoDimB');if(!d||!d.parentElement)return false;
  const row=d.parentElement,p=rightPanel();if(!p)return false;
  row.style.transform='none';row.style.position='fixed';row.style.bottom='10px';row.style.right='auto';row.style.zIndex='30';
  row.style.display='flex';row.style.flexWrap='nowrap';row.style.gap='6px';row.style.maxWidth='none';
  const pr=p.getBoundingClientRect();
  // User requested the command group's RIGHT EDGE to finish exactly at the left edge of the right CAD panel.
  const rr=row.getBoundingClientRect();
  const gap=10;
  const left=Math.max(8,Math.round(pr.left-gap-rr.width));
  row.style.left=left+'px';
  return true;
}
function boot(){
  alignBottomBar();
  let n=0;const t=setInterval(()=>{n++;alignBottomBar();if(n>24)clearInterval(t)},250);
  addEventListener('resize',()=>setTimeout(alignBottomBar,80));
  if(window.ResizeObserver){const p=rightPanel();if(p)new ResizeObserver(()=>alignBottomBar()).observe(p)}
  window.MG_CAD_V190={version:'1.9.0',bottomRightAlignedToPanel:true};
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,850));else setTimeout(boot,850);
})();'''
(AS/'cad-v190.js').write_text(js,encoding='utf-8')
print('v1.9.0 bottom command bar dynamically aligned to right panel edge')
