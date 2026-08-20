from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v217.js' not in h: h=h.replace('</body>','<script src="/cad-v217.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){'use strict';
function ready(f){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',f,{once:true});else f()}
function xyz(){let e=[...document.querySelectorAll('div,span,p')].find(x=>String(x.textContent||'').replace(/\s+/g,' ').trim()==='X Y Z');if(!e)return;e.style.cssText='position:fixed;left:72px;top:112px;z-index:43;display:flex;gap:18px;background:transparent;border:0;box-shadow:none;padding:0;margin:0;font:900 18px Arial,sans-serif;pointer-events:none';let s=e.querySelectorAll('span');if(s.length>=3){s[0].style.color='#ff3b48';s[1].style.color='#55ef6d';s[2].style.color='#46a6ff'}}
function hideAxisLetters(){document.querySelectorAll('canvas').forEach(c=>c.dataset.mgAxisLabels='off');window.MG_AXIS_LABELS=false}
function init(){xyz();hideAxisLetters();window.MG_CAD_V217={version:'2.0.17',compactXYZ:true,axisLetters:false,axisColorsOnly:true,penDefaultAboveXYZ:true}}
ready(init)})();'''
(AS/'cad-v217.js').write_text(js,encoding='utf-8')
print('v2.0.17 compact XYZ + axis colors only')
