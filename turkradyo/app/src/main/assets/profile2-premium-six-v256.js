(()=>{'use strict';
const SVG={
DNA:'<svg viewBox="0 0 24 24"><path d="M8 3c0 4.5 8 4.5 8 9s-8 4.5-8 9M16 3c0 4.5-8 4.5-8 9s8 4.5 8 9"/><path d="M9 6h6M8.4 10h7.2M8.4 14h7.2M9 18h6"/></svg>',
TRACKS:'<svg viewBox="0 0 24 24"><path d="M4 13h2l2-5 3 10 3-13 3 8h3"/></svg>',
ALARM:'<svg viewBox="0 0 24 24"><path d="M6 16h12l-1.5-2v-3.5a4.5 4.5 0 0 0-9 0V14z"/><path d="M10 19h4M7 5 5 7M17 5l2 2"/></svg>',
SLEEP:'<svg viewBox="0 0 24 24"><path d="M7 3h10M7 21h10M8.5 4.5c0 3.3 3.5 4.6 3.5 7.5s-3.5 4.2-3.5 7.5M15.5 4.5c0 3.3-3.5 4.6-3.5 7.5s3.5 4.2 3.5 7.5"/></svg>',
ZAP:'<svg viewBox="0 0 24 24"><path d="M13 2 6 13h5l-1 9 8-12h-5z"/></svg>',
TR:'<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="7"/><path d="M5 12h14M12 5c2 2.2 3 4.5 3 7s-1 4.8-3 7c-2-2.2-3-4.5-3-7s1-4.8 3-7"/></svg>'
};
const CARDS=[
 ['p2DNA','DNA','İSTASYON DNA','İstasyonların kararlılık ve bağlantı analizleri','KEŞFET'],
 ['p2Tracks','TRACKS','SON 50','Son dinlediğin 50 şarkı ve geçmiş kayıtların','GÖRÜNTÜLE'],
 ['p2Alarm','ALARM','RADYO İLE UYAN','Favori radyonla güne enerjik başla','AYARLA'],
 ['p2Sleep','SLEEP','ZAMANLAYICI','Otomatik kapanma zamanlayıcısı','BAŞLAT'],
 ['p2Zap','ZAP','AKILLI ZAPPING','Beğendiğin tarza uygun radyoları keşfet','BAŞLAT'],
 ['p2Genres','TR','TÜRKİYE GRUPLARI','Türkçe, yabancı, haber ve daha fazlası','KEŞFET']
];
function build(id,k,title,sub,action){const el=document.getElementById(id);if(!el||el.dataset.p256==='1')return false;el.dataset.p256='1';el.classList.add('p256PremiumCard');el.replaceChildren();
 const crown=document.createElement('span');crown.className='p256Crown';crown.textContent='♛';
 const icon=document.createElement('span');icon.className='p256Icon';icon.innerHTML=SVG[k];
 const badge=document.createElement('span');badge.className='p256Badge';badge.textContent='PREMIUM';
 const t=document.createElement('strong');t.className='p256Title';t.textContent=title;
 const s=document.createElement('small');s.className='p256Sub';s.textContent=sub;
 const a=document.createElement('span');a.className='p256Action';a.innerHTML='<b>'+action+'</b><i>›</i>';
 el.append(crown,icon,badge,t,s,a);return true}
function apply(){let n=0;CARDS.forEach(x=>{if(build(...x))n++});return n}
function mount(){apply();setTimeout(apply,260);setTimeout(apply,850);setTimeout(apply,1800)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();
