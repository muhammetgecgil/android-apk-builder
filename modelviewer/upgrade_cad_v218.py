from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v218.js' not in h:
    h=h.replace('</body>','<script src="/cad-v218.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function findXYZ(){return [...document.querySelectorAll('div,span,p')].find(e=>String(e.textContent||'').replace(/\s+/g,' ').trim()==='X Y Z')||null}
function findPen(){
 const els=[...document.querySelectorAll('button,div')].filter(e=>e.id!=='mgPenStatus');
 let best=null;
 for(const e of els){const r=e.getBoundingClientRect();if(r.left>330||r.top>250||r.width<45||r.width>135||r.height<45||r.height>135)continue;const t=tr(e.textContent);if(!(e.querySelector('svg,img')||t.includes('✎')||t.includes('PEN')))continue;if(!best||r.left<best.getBoundingClientRect().left)best=e}
 return best;
}
function placeXYZ(){
 const e=findXYZ();if(!e)return;
 e.style.cssText='position:fixed;left:138px;top:153px;z-index:43;display:flex;gap:20px;align-items:center;background:transparent;border:0;box-shadow:none;padding:0;margin:0;font:900 18px Arial,sans-serif;pointer-events:none;line-height:1';
 const s=e.querySelectorAll('span');if(s.length>=3){s[0].style.color='#ff3b48';s[1].style.color='#55ef6d';s[2].style.color='#46a6ff'}
}
function placePen(){
 const p=findPen();if(!p)return;
 p.style.position='fixed';p.style.left='14px';p.style.top='126px';p.style.zIndex='44';p.style.touchAction='none';p.style.userSelect='none';
}
function removeSceneAxisLetters(){
 try{
   if(typeof scene==='undefined'||!scene||!scene.children)return false;
   const kill=[];
   scene.traverse(o=>{if(o&&o.userData&&o.userData.mgAxisLabel)kill.push(o)});
   kill.forEach(o=>{if(o.parent)o.parent.remove(o)});
   window.MG_AXIS_LABELS=false;
   return true;
 }catch(e){return false}
}
function suppressFutureAxisLetters(){
 try{
   if(typeof scene==='undefined'||!scene||scene.userData&&scene.userData.mgNoAxisLetters218)return;
   scene.userData=scene.userData||{};scene.userData.mgNoAxisLetters218=true;
   const originalAdd=scene.add;
   scene.add=function(){
     const args=[...arguments].filter(o=>!(o&&o.userData&&o.userData.mgAxisLabel));
     if(args.length) return originalAdd.apply(this,args);
     return this;
   };
 }catch(e){}
}
function ensureAxisClean(){removeSceneAxisLetters();suppressFutureAxisLetters();}
function init(){
 const extra=document.getElementById('mgPenStatus');if(extra)extra.remove();
 placePen();placeXYZ();
 ensureAxisClean();
 setTimeout(ensureAxisClean,0);setTimeout(ensureAxisClean,250);setTimeout(ensureAxisClean,800);setTimeout(ensureAxisClean,1800);
 document.addEventListener('click',()=>setTimeout(ensureAxisClean,0),true);
 window.MG_CAD_V218={version:'2.0.18',penStartupPosition:true,xyzCompactNextToPen:true,centerAxisLettersRemoved:true,axisColorsOnly:true,noContinuousPolling:true};
}
ready(init);
})();'''
(AS/'cad-v218.js').write_text(js,encoding='utf-8')
print('v2.0.18: startup pen+compact XYZ at left, remove all center axis letter sprites')
