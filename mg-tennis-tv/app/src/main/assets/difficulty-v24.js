(function(){
'use strict';
const profiles={
  easy:{key:'easy',label:'KOLAY',shotTime:3.25,serveTime:3.00,reaction:1250,runSpeed:2.45,accuracy:.34,errorRate:.28,power:.58},
  normal:{key:'normal',label:'NORMAL',shotTime:2.05,serveTime:1.65,reaction:560,runSpeed:4.10,accuracy:.66,errorRate:.11,power:.90},
  hard:{key:'hard',label:'ZOR',shotTime:1.62,serveTime:1.30,reaction:310,runSpeed:5.10,accuracy:.83,errorRate:.06,power:1.05},
  pro:{key:'pro',label:'PROFESYONEL',shotTime:1.36,serveTime:1.12,reaction:180,runSpeed:6.10,accuracy:.94,errorRate:.025,power:1.17}
};
let current=profiles.easy;
window.MGDifficulty={profiles,get current(){return current},set(key){current=profiles[key]||profiles.easy;update();return current}};
function update(){const e=document.getElementById('difficultySelect');if(e)e.value=current.key;const s=document.getElementById('status');if(s)s.textContent='Zorluk: '+current.label+' • top hızı, reaksiyon ve hata payı ayarlandı'}
function mount(){const hud=document.getElementById('hud');if(!hud||document.getElementById('difficultySelect'))return;const sel=document.createElement('select');sel.id='difficultySelect';sel.setAttribute('aria-label','Zorluk seviyesi');sel.style.cssText='pointer-events:auto;border:1px solid rgba(255,255,255,.28);border-radius:10px;background:rgba(12,20,28,.94);color:#fff;padding:9px 10px;font-weight:800;font-size:12px';[['easy','KOLAY'],['normal','NORMAL'],['hard','ZOR'],['pro','PRO']].forEach(x=>{const o=document.createElement('option');o.value=x[0];o.textContent=x[1];sel.appendChild(o)});sel.value=current.key;sel.addEventListener('change',()=>{current=profiles[sel.value]||profiles.easy;update()});const brand=document.getElementById('brand');hud.insertBefore(sel,brand||null)}
setTimeout(mount,400);setInterval(mount,1500);
})();
