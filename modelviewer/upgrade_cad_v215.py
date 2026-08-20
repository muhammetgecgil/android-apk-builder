from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v215.js' not in h:
    h=h.replace('</body>','<script src="/cad-v215.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function findLegend(){return [...document.querySelectorAll('div,span,p')].find(e=>{const t=tr(e.textContent);return t.includes('X KIRMIZI')&&t.includes('Y YEŞİL')&&t.includes('Z MAVİ')})||null}
function findPen(){
 const els=[...document.querySelectorAll('button,div')].filter(e=>e.id!=='mgPenStatus');
 let hit=null;
 for(const e of els){const r=e.getBoundingClientRect();if(r.left>280||r.top>210||r.width<45||r.width>125||r.height<45||r.height>125)continue;const t=tr(e.textContent);if(e.querySelector('svg,img')||t.includes('✎')||t.includes('PEN')){hit=e;break}}
 return hit;
}
function cleanLegend(){
 const l=findLegend();if(!l)return;
 l.innerHTML='<span style="color:#ff3b48">X</span><span style="color:#55ef6d">Y</span><span style="color:#46a6ff">Z</span>';
 l.style.cssText='position:fixed;left:72px;top:104px;z-index:42;display:flex;gap:22px;align-items:center;background:transparent;border:0;box-shadow:none;padding:0;margin:0;font:900 30px Arial,sans-serif;pointer-events:none;letter-spacing:0';
}
function setPenActive(on){
 const p=findPen();if(!p)return;
 p.style.background='';p.style.borderColor='';p.style.boxShadow='';
 p.style.transition='color .15s,filter .15s';
 p.style.color=on?'#4dff72':'#ffffff';
 p.style.filter=on?'drop-shadow(0 0 7px #4dff72)':'none';
 p.dataset.mgReady=on?'1':'0';
 p.querySelectorAll('svg').forEach(svg=>{svg.style.color=on?'#4dff72':'';svg.querySelectorAll('*').forEach(n=>{if(n.getAttribute('stroke')&&n.getAttribute('stroke')!=='none')n.style.stroke=on?'#4dff72':'';if(n.getAttribute('fill')&&n.getAttribute('fill')!=='none')n.style.fill=on?'#4dff72':''})});
}
function init(){
 const extra=document.getElementById('mgPenStatus');if(extra)extra.remove();
 cleanLegend();setPenActive(false);
 document.addEventListener('click',e=>{const b=e.target&&e.target.closest?e.target.closest('button,[role=button]'):null;if(!b)return;const t=tr(b.textContent).trim();if(t==='ÇİZ'||t.includes('S-PEN'))setPenActive(true);if(t==='SİL'||t.includes('TEMİZLE')||t.includes('TEKNİK RESİM'))setPenActive(false)},true);
 window.MG_CAD_V215={version:'2.0.15',plainXYZOnly:true,legendBoxRemoved:true,axisWordsRemoved:true,onlyPenIconGreenWhenReady:true,extraPenRemoved:true,noPolling:true};
}
ready(init)
})();'''
(AS/'cad-v215.js').write_text(js,encoding='utf-8')
print('v2.0.15: clean colored XYZ only + single S-Pen icon green when active')
