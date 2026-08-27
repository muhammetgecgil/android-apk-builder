(function(){'use strict';
/* v3.7: legacy opponent loader and global left-seat rotation intentionally disabled.
   Ultra human + v37 clean core are the single authorities. */
let released=false;
function release(){if(released)return;released=true;const l=document.getElementById('loading');if(l)l.style.display='none';const f=document.getElementById('loadfill');if(f)f.style.width='100%';const s=document.getElementById('status');if(s)s.textContent='Kort hazır • rakip yükleniyor'}
setTimeout(release,1000);
})();