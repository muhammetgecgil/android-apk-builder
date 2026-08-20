from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v216.js' not in h:
    h=h.replace('</body>','<script src="/cad-v216.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function findXYZ(){
  return [...document.querySelectorAll('div,span,p')].find(e=>{const t=tr(e.textContent).replace(/\s+/g,' ').trim();return t==='X Y Z';})||null;
}
function findPen(){
 const els=[...document.querySelectorAll('button,div')].filter(e=>e.id!=='mgPenStatus');
 let best=null;
 for(const e of els){
   const r=e.getBoundingClientRect();
   if(r.left>320||r.top>230||r.width<45||r.width>130||r.height<45||r.height>130) continue;
   const t=tr(e.textContent);
   const hasIcon=!!e.querySelector('svg,img')||t.includes('✎')||t.includes('PEN');
   if(!hasIcon) continue;
   if(!best || r.left<best.getBoundingClientRect().left) best=e;
 }
 return best;
}
function styleXYZ(){
 const l=findXYZ(); if(!l) return;
 l.style.position='fixed';
 l.style.left='12px';
 l.style.top='138px';
 l.style.zIndex='42';
 l.style.display='flex';
 l.style.gap='14px';
 l.style.alignItems='center';
 l.style.background='transparent';
 l.style.border='0';
 l.style.boxShadow='none';
 l.style.padding='0';
 l.style.margin='0';
 l.style.font='900 22px Arial,sans-serif';
 l.style.pointerEvents='none';
 const spans=l.querySelectorAll('span');
 if(spans.length>=3){spans[0].style.color='#ff3b48';spans[1].style.color='#55ef6d';spans[2].style.color='#46a6ff';}
}
function makePenDraggable(){
 const p=findPen(); if(!p||p.dataset.mgDrag216==='1') return;
 p.dataset.mgDrag216='1';
 const r=p.getBoundingClientRect();
 p.style.position='fixed';
 p.style.left=Math.max(12,r.left)+'px';
 p.style.top=Math.max(68,Math.min(r.top,92))+'px';
 p.style.zIndex='44';
 p.style.touchAction='none';
 p.style.userSelect='none';
 p.style.cursor='grab';
 let drag=false,sx=0,sy=0,sl=0,st=0;
 p.addEventListener('pointerdown',e=>{
   drag=true; sx=e.clientX; sy=e.clientY;
   const q=p.getBoundingClientRect(); sl=q.left; st=q.top;
   try{p.setPointerCapture(e.pointerId)}catch(_){}
   p.style.cursor='grabbing'; e.preventDefault(); e.stopPropagation();
 },true);
 p.addEventListener('pointermove',e=>{
   if(!drag)return;
   const maxL=Math.max(0,innerWidth-p.offsetWidth),maxT=Math.max(62,innerHeight-p.offsetHeight);
   p.style.left=Math.max(0,Math.min(maxL,sl+e.clientX-sx))+'px';
   p.style.top=Math.max(62,Math.min(maxT,st+e.clientY-sy))+'px';
   e.preventDefault(); e.stopPropagation();
 },true);
 const up=()=>{drag=false;p.style.cursor='grab'};
 p.addEventListener('pointerup',up,true);p.addEventListener('pointercancel',up,true);
}
function setPenActive(on){
 const p=findPen(); if(!p)return;
 p.style.transition='color .15s,filter .15s';
 p.style.color=on?'#4dff72':'#ffffff';
 p.style.filter=on?'drop-shadow(0 0 7px #4dff72)':'none';
 p.dataset.mgReady=on?'1':'0';
 p.querySelectorAll('svg').forEach(svg=>{
   svg.style.color=on?'#4dff72':'';
   svg.querySelectorAll('*').forEach(n=>{
     if(n.getAttribute('stroke')&&n.getAttribute('stroke')!=='none')n.style.stroke=on?'#4dff72':'';
     if(n.getAttribute('fill')&&n.getAttribute('fill')!=='none')n.style.fill=on?'#4dff72':'';
   });
 });
}
function init(){
 const extra=document.getElementById('mgPenStatus'); if(extra)extra.remove();
 styleXYZ();
 makePenDraggable();
 setPenActive(false);
 document.addEventListener('click',e=>{
   const b=e.target&&e.target.closest?e.target.closest('button,[role=button]'):null;if(!b)return;
   const t=tr(b.textContent).trim();
   if(t==='ÇİZ'||t.includes('S-PEN'))setPenActive(true);
   if(t==='SİL'||t.includes('TEMİZLE')||t.includes('TEKNİK RESİM'))setPenActive(false);
 },true);
 window.MG_CAD_V216={version:'2.0.16',xyzSmallLeft:true,penDraggable:true,penSeparatedFromXYZ:true,onlyPenIconGreenWhenReady:true,noPolling:true};
}
ready(init)
})();'''
(AS/'cad-v216.js').write_text(js,encoding='utf-8')
print('v2.0.16: smaller left-aligned XYZ + draggable S-Pen separated from labels')
