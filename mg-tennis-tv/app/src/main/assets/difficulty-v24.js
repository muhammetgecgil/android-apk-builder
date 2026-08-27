(function(){'use strict';
const profiles={
 easy:{key:'easy',label:'KOLAY',flightTime:2.75,strikeWindow:1.45,reaction:900,runSpeed:2.7,accuracy:.78,errorRate:.10,power:.62},
 normal:{key:'normal',label:'NORMAL',flightTime:1.95,strikeWindow:1.05,reaction:500,runSpeed:4.0,accuracy:.70,errorRate:.12,power:.88},
 hard:{key:'hard',label:'ZOR',flightTime:1.40,strikeWindow:.78,reaction:300,runSpeed:5.0,accuracy:.82,errorRate:.07,power:1.03},
 pro:{key:'pro',label:'PROFESYONEL',flightTime:1.15,strikeWindow:.62,reaction:180,runSpeed:6.0,accuracy:.92,errorRate:.035,power:1.16}
};let current=profiles.easy;window.MGDifficulty={profiles,get current(){return current},set(key){current=profiles[key]||profiles.easy;update();return current}};
function update(){const e=document.getElementById('difficultySelect');if(e)e.value=current.key;const s=document.getElementById('status');if(s)s.textContent='Zorluk: '+current.label+' • uçuş '+current.flightTime.toFixed(2)+' sn • vuruş penceresi '+current.strikeWindow.toFixed(2)+' sn'}
function mount(){const hud=document.getElementById('hud');if(!hud||document.getElementById('difficultySelect'))return;const sel=document.createElement('select');sel.id='difficultySelect';sel.style.cssText='pointer-events:auto;border:1px solid rgba(255,255,255,.35);border-radius:10px;background:#07111a;color:#fff;padding:9px 10px;font-weight:800;font-size:12px';[['easy','KOLAY'],['normal','NORMAL'],['hard','ZOR'],['pro','PRO']].forEach(x=>{const o=document.createElement('option');o.value=x[0];o.textContent=x[1];sel.appendChild(o)});sel.value=current.key;sel.onchange=()=>{current=profiles[sel.value]||profiles.easy;update()};const brand=document.getElementById('brand');hud.insertBefore(sel,brand||null)}setTimeout(mount,400);setInterval(mount,1500);
})();