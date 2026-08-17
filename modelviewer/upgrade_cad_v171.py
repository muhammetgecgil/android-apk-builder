from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
AS.mkdir(parents=True, exist_ok=True)
js=r'''(function(){
'use strict';
const style=document.createElement('style');
style.textContent=`
:root{--mg-safe-r:max(18px,env(safe-area-inset-right,0px));--mg-safe-l:max(8px,env(safe-area-inset-left,0px));--mg-safe-b:max(10px,env(safe-area-inset-bottom,0px));}
#tools{right:var(--mg-safe-r)!important;width:min(220px,calc(100vw - var(--mg-safe-r) - var(--mg-safe-l) - 16px))!important;box-sizing:border-box!important;overflow-x:hidden!important;}
#tools .row{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:6px!important;}
#tools .row button,#tools .row select{min-width:0!important;width:100%!important;max-width:100%!important;box-sizing:border-box!important;white-space:normal!important;overflow-wrap:anywhere!important;}
#tools .row button:only-child{grid-column:1/-1!important;}
#parts{max-width:100%!important;overflow-x:hidden!important;}
.part{max-width:100%!important;box-sizing:border-box!important;}
#cadQuick{bottom:var(--mg-safe-b)!important;max-width:calc(100vw - var(--mg-safe-l) - var(--mg-safe-r) - 12px)!important;box-sizing:border-box!important;}
#mgPanelTab{position:absolute;z-index:14;right:var(--mg-safe-r);top:70px;background:#0b1b2d;color:#eaf5ff;border:1px solid #245582;border-radius:9px;padding:9px 10px;font-weight:800;box-shadow:0 4px 14px rgba(0,0,0,.35)}
body.mgToolsCollapsed #tools{display:none!important}body.mgToolsCollapsed #mgPanelTab{display:block!important}
@media (max-width:900px){#tools{width:min(205px,42vw)!important}.panel{font-size:92%}#tools button,#tools select{padding:6px 7px!important;font-size:12px!important}.head{font-size:15px!important}#info{max-width:min(330px,46vw)!important}}
@media (max-width:700px){#tools{width:min(190px,46vw)!important}#top{padding-right:var(--mg-safe-r)!important}#name{max-width:120px!important}}
`;
document.head.appendChild(style);

const tools=document.getElementById('tools');
if(tools){
  const tab=document.createElement('button');
  tab.id='mgPanelTab';
  tab.textContent='CAD ◀';
  tab.setAttribute('aria-label','CAD araç panelini aç/kapat');
  document.body.appendChild(tab);
  let collapsed=false;
  function apply(){document.body.classList.toggle('mgToolsCollapsed',collapsed);tab.textContent=collapsed?'CAD ◀':'CAD ▶';tab.style.display='block';}
  tab.onclick=()=>{collapsed=!collapsed;apply()};
  apply();
}

function clampTools(){
  if(!tools)return;
  const r=tools.getBoundingClientRect();
  if(r.right>innerWidth-8){tools.style.right='max(18px, env(safe-area-inset-right, 0px))';}
}
addEventListener('resize',()=>setTimeout(clampTools,50));
addEventListener('orientationchange',()=>setTimeout(clampTools,250));
setTimeout(clampTools,200);
window.MGMobileSafeArea={version:'1.7.1',clamp:clampTools};
})();'''
(AS/'cad-v171.js').write_text(js,encoding='utf-8')

p=AS/'index.html'
s=p.read_text(encoding='utf-8')
if '/cad-v171.js' not in s:
    s=s.replace('</body>','<script src="/cad-v171.js"></script></body>')
p.write_text(s,encoding='utf-8')
print('CAD v1.7.1 mobile safe-area patch applied')
