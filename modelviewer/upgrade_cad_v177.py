from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v177.js' not in s:
    s=s.replace('</body>','<script src="/cad-v177.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
let activePart=null;
let prevSelected=null;
function buttonsByText(txt){return Array.from(document.querySelectorAll('button')).filter(b=>b.textContent.trim().toUpperCase()===txt)}
function meshList(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function setActive(m){
  if(prevSelected&&prevSelected.material&&prevSelected.material.emissive)prevSelected.material.emissive.setHex(0x000000);
  activePart=m||null;prevSelected=activePart;
  if(activePart&&activePart.material&&activePart.material.emissive)activePart.material.emissive.setHex(0x15324a);
}
function ensureActive(){const a=meshList();if(!activePart||!a.includes(activePart))setActive(a[0]||null);return activePart}
function pickPart(ev){
  try{
    if(window.markupOn)return;
    const r=canvas.getBoundingClientRect();
    mouse.x=((ev.clientX-r.left)/r.width)*2-1;mouse.y=-((ev.clientY-r.top)/r.height)*2+1;
    ray.setFromCamera(mouse,camera);const h=ray.intersectObjects(meshList().filter(x=>x.visible),false)[0];if(h)setActive(h.object);
  }catch(e){}
}
function installPartControls(){
  const giz=buttonsByText('GİZLE')[0],izo=buttonsByText('İZOLE')[0],all=buttonsByText('TÜMÜ')[0];
  if(giz)giz.onclick=()=>{const m=ensureActive();if(m){m.visible=false;const rest=meshList().filter(x=>x.visible);setActive(rest[0]||null)}};
  if(izo)izo.onclick=()=>{const m=ensureActive();if(!m)return;meshList().forEach(x=>x.visible=(x===m));setActive(m)};
  if(all)all.onclick=()=>{meshList().forEach(x=>x.visible=true);setActive(meshList()[0]||null)};
  if(typeof canvas!=='undefined'){canvas.addEventListener('pointerup',pickPart,true);}
  setTimeout(()=>ensureActive(),300);
}

function installPenFix(){
  const mc=document.getElementById('markupCanvas'),mark=document.getElementById('markB'),clear=document.getElementById('markClear');
  if(!mc||!mark)return;
  mc.style.zIndex='4';mc.style.touchAction='none';
  let down=false,last=null,ctx=mc.getContext('2d');
  function prep(){ctx=mc.getContext('2d');ctx.lineCap='round';ctx.lineJoin='round'}
  prep();
  function point(e){return{x:e.clientX,y:e.clientY,p:(e.pressure&&e.pressure>0)?e.pressure:0.55,t:e.pointerType||'touch'}}
  function drawPoint(q){
    if(!last){last=q;ctx.beginPath();ctx.moveTo(q.x,q.y);return}
    const mx=(last.x+q.x)/2,my=(last.y+q.y)/2;
    ctx.strokeStyle='#ff3b30';ctx.lineWidth=q.t==='pen'?(1.6+3.2*q.p):3.2;
    ctx.quadraticCurveTo(last.x,last.y,mx,my);ctx.stroke();last=q;
  }
  function feed(e){const arr=(e.getCoalescedEvents?e.getCoalescedEvents():null)||[e];for(const x of arr)drawPoint(point(x))}
  mc.onpointerdown=e=>{if(mc.style.pointerEvents==='none')return;down=true;last=null;try{mc.setPointerCapture(e.pointerId)}catch(_){ }e.preventDefault();feed(e)};
  mc.onpointermove=e=>{if(!down)return;e.preventDefault();feed(e)};
  mc.onpointerrawupdate=e=>{if(!down)return;e.preventDefault();feed(e)};
  const end=e=>{if(!down)return;feed(e);down=false;last=null;try{mc.releasePointerCapture(e.pointerId)}catch(_){ }};
  mc.onpointerup=end;mc.onpointercancel=()=>{down=false;last=null};
  const old=mark.onclick;
  mark.onclick=function(e){if(old)old.call(this,e);const on=this.classList.contains('on');mc.style.pointerEvents=on?'auto':'none';try{controls.enabled=!on}catch(_){} };
  if(clear)clear.onclick=()=>{ctx.clearRect(0,0,mc.width,mc.height)};
  window.addEventListener('resize',()=>setTimeout(prep,30));
}
function boot(){installPartControls();installPenFix();window.MG_CAD_V177={version:'1.7.7',partControls:true,continuousSPen:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,80));else setTimeout(boot,80);
})();
'''
(AS/'cad-v177.js').write_text(js,encoding='utf-8')
print('v1.7.7 part controls + S-Pen patch applied')
