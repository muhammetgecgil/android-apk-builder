(function(){
'use strict';
const profiles={
  easy:{key:'easy',label:'KOLAY',shotTime:2.90,serveTime:2.20,reaction:1050,runSpeed:2.65,accuracy:.38,errorRate:.24,power:.64},
  normal:{key:'normal',label:'NORMAL',shotTime:1.82,serveTime:1.42,reaction:470,runSpeed:4.35,accuracy:.68,errorRate:.10,power:.94},
  hard:{key:'hard',label:'ZOR',shotTime:1.53,serveTime:1.24,reaction:290,runSpeed:5.25,accuracy:.84,errorRate:.055,power:1.08},
  pro:{key:'pro',label:'PROFESYONEL',shotTime:1.32,serveTime:1.10,reaction:175,runSpeed:6.15,accuracy:.94,errorRate:.025,power:1.18}
};
let current=profiles.easy;
window.MGDifficulty={profiles,get current(){return current},set(key){current=profiles[key]||profiles.easy;update();return current}};
function update(){
  const e=document.getElementById('difficultySelect');if(e)e.value=current.key;
  const s=document.getElementById('status');if(s)s.textContent='Zorluk: '+current.label+' • top hızı, rakip reaksiyonu ve hata payı ayarlandı';
}
function mount(){
  const hud=document.getElementById('hud');if(!hud||document.getElementById('difficultySelect'))return;
  const sel=document.createElement('select');sel.id='difficultySelect';sel.setAttribute('aria-label','Zorluk seviyesi');
  sel.style.cssText='pointer-events:auto;border:1px solid rgba(255,255,255,.28);border-radius:10px;background:rgba(12,20,28,.94);color:#fff;padding:9px 10px;font-weight:800;font-size:12px';
  [['easy','KOLAY'],['normal','NORMAL'],['hard','ZOR'],['pro','PRO']].forEach(x=>{const o=document.createElement('option');o.value=x[0];o.textContent=x[1];sel.appendChild(o)});
  sel.value=current.key;sel.addEventListener('change',()=>{current=profiles[sel.value]||profiles.easy;update()});
  const brand=document.getElementById('brand');hud.insertBefore(sel,brand||null);
}
setTimeout(mount,400);setInterval(mount,1500);
})();
