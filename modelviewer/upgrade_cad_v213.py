from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v213.js' not in h:
    h=h.replace('</body>','<script src="/cad-v213.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function textHas(el,s){return ((el&&el.textContent)||'').toLocaleUpperCase('tr-TR').indexOf(s)>=0;}
function findAxisLegend(){
  const all=[...document.querySelectorAll('div,span,p')];
  return all.find(e=>textHas(e,'X KIRMIZI')&&textHas(e,'Y YEŞİL')&&textHas(e,'Z MAVİ'))||null;
}
function init(){
  const info=document.getElementById('info');
  if(info){
    info.style.position='fixed';
    info.style.left='16px';
    info.style.bottom='92px';
    info.style.top='auto';
    info.style.width='min(46vw,700px)';
    info.style.maxHeight='38vh';
    info.style.overflowY='auto';
    info.style.overflowX='hidden';
    info.style.touchAction='pan-y';
    info.style.zIndex='40';
    info.style.boxSizing='border-box';
    if(!document.getElementById('mgInfoDrag')){
      const hd=document.createElement('div');
      hd.id='mgInfoDrag';
      hd.textContent='MODEL ANALİZİ  ↕  SÜRÜKLE';
      hd.style.cssText='position:sticky;top:0;z-index:3;margin:-8px -8px 8px -8px;padding:9px 14px;background:rgba(5,20,35,.96);border-bottom:1px solid #24567a;color:#78d7ff;font-weight:800;cursor:grab;touch-action:none;user-select:none';
      info.insertBefore(hd,info.firstChild);
      let drag=false,sx=0,sy=0,sl=0,st=0;
      hd.addEventListener('pointerdown',e=>{drag=true;hd.setPointerCapture(e.pointerId);const r=info.getBoundingClientRect();sx=e.clientX;sy=e.clientY;sl=r.left;st=r.top;info.style.bottom='auto';hd.style.cursor='grabbing';e.preventDefault();});
      hd.addEventListener('pointermove',e=>{if(!drag)return;const maxL=Math.max(0,innerWidth-info.offsetWidth),maxT=Math.max(62,innerHeight-info.offsetHeight);info.style.left=Math.max(0,Math.min(maxL,sl+e.clientX-sx))+'px';info.style.top=Math.max(62,Math.min(maxT,st+e.clientY-sy))+'px';e.preventDefault();});
      const up=()=>{drag=false;hd.style.cursor='grab'};hd.addEventListener('pointerup',up);hd.addEventListener('pointercancel',up);
    }
  }

  const legend=findAxisLegend();
  if(legend){
    legend.style.position='fixed';
    legend.style.left='28px';
    legend.style.top='116px';
    legend.style.zIndex='42';
    legend.style.margin='0';
    legend.style.pointerEvents='none';
  }
  if(!document.getElementById('mgPenStatus')){
    const p=document.createElement('div');p.id='mgPenStatus';p.textContent='✎';p.title='S-Pen';
    p.style.cssText='position:fixed;left:116px;top:66px;width:62px;height:62px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:#17202c;border:2px solid #2c6c96;color:white;font-size:34px;z-index:43;box-shadow:0 3px 12px #0008;transition:.15s';
    document.body.appendChild(p);
  }
  const pen=document.getElementById('mgPenStatus');
  function setPen(on){if(!pen)return;pen.style.background=on?'#117a39':'#17202c';pen.style.borderColor=on?'#48ef79':'#2c6c96';pen.style.boxShadow=on?'0 0 18px #48ef79':'0 3px 12px #0008';pen.dataset.ready=on?'1':'0';}
  setPen(false);
  document.addEventListener('click',e=>{
    const b=e.target&&e.target.closest?e.target.closest('button,[role=button]'):null;if(!b)return;
    const t=(b.textContent||'').trim().toLocaleUpperCase('tr-TR');
    if(t==='ÇİZ'||t.includes('S-PEN')) setPen(true);
    if(t==='SİL'||t.includes('TEMİZLE')||t.includes('TEKNİK RESİM')) setPen(false);
  },true);
  window.addEventListener('resize',()=>{if(info&&!info.style.bottom){const r=info.getBoundingClientRect();info.style.left=Math.min(r.left,innerWidth-info.offsetWidth)+'px';}}, {passive:true});
  window.MG_CAD_V213={version:'2.0.13',draggableAnalysis:true,scrollableAnalysis:true,analysisStartsAboveToolbar:true,penAboveAxisLegend:true,penReadyGreen:true,noPolling:true};
}
ready(init);
})();'''
(AS/'cad-v213.js').write_text(js,encoding='utf-8')
print('v2.0.13: draggable/scrollable analysis panel + S-Pen ready status over XYZ legend')
