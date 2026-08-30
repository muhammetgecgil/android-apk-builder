(()=>{'use strict';
function patchUtility(){const map=[['#p2DNA','İSTASYON DNA','Kararlılık • bağlantı • tür'],['#p2Tracks','SON 50','Son çalan şarkılar'],['#p2Alarm','RADYO İLE UYAN','Saat seç • radyo ile uyan'],['#p2Sleep','ZAMANLAYICI','15–120 dk • otomatik kapatma'],['#p2Zap','AKILLI ZAPPING','Tek dokunuş • akıllı seçim'],['#p2Genres','TÜRKİYE GRUPLARI','Türkiye kataloğu • tür grupları']];map.forEach(([sel,t,s])=>{const el=document.querySelector(sel);if(!el)return;const st=el.querySelector('.p2CardCopy strong'),sm=el.querySelector('.p2CardCopy small');if(st&&st.textContent!==t)st.textContent=t;if(sm&&sm.textContent!==s)sm.textContent=s;});}
function mount(){patchUtility();setTimeout(patchUtility,350);setTimeout(patchUtility,1000);setTimeout(patchUtility,2200)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();
