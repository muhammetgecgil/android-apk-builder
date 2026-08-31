(()=>{'use strict';
const IDS=['v12Similar','v12Genres','v12Smart','p2DNA','p2Tracks','p2Alarm','p2Sleep','p2Zap','p2Genres'];
const LABELS={v12Similar:'MÜZİK TÜRÜ',v12Genres:'TÜRLER',v12Smart:'SLOW MOD'};
const MOON='<svg viewBox="0 0 24 24" aria-hidden="true"><path class="moonFill" d="M15.8 3.3a8.6 8.6 0 1 0 4.9 15.2 7.2 7.2 0 0 1-4.9-15.2Z"/></svg>';
function defaultProfile(){try{if(localStorage.getItem('p2Active')===null)localStorage.setItem('p2Active','1')}catch(e){}if(document.body)document.body.classList.add('profile2-active')}
function cleanDiscovery(el){if(!el)return;const title=LABELS[el.id];if(title){let s=el.querySelector(':scope>span');if(!s){s=document.createElement('span');el.appendChild(s)}s.textContent=title}const sm=el.querySelector(':scope>small');if(sm)sm.style.display='none';if(el.id==='v12Smart'){let b=el.querySelector(':scope>b');if(!b){b=document.createElement('b');el.prepend(b)}b.innerHTML=MOON;b.dataset.p262Moon='1'}}
function cleanUtility(el){if(!el)return;el.classList.add('p256PremiumCard');const sub=el.querySelector('.p256Sub');if(sub)sub.style.display='none';el.querySelectorAll('.p256Crown,.p256Badge,.p256Action').forEach(x=>x.style.display='none')}
function mount(){defaultProfile();const nodes=IDS.map(id=>document.getElementById(id));if(nodes.some(x=>!x))return false;let grid=document.getElementById('p2UnifiedPremiumGrid262');if(!grid){grid=document.createElement('div');grid.id='p2UnifiedPremiumGrid262';const anchor=document.getElementById('p2DiscoveryMenu')||document.querySelector('.p2Hub');if(anchor?.parentNode)anchor.parentNode.insertBefore(grid,anchor);else return false}
 nodes.forEach((el,i)=>{el.classList.add('p262UnifiedCard');if(i<3)cleanDiscovery(el);else cleanUtility(el);if(el.parentNode!==grid)grid.appendChild(el)});
 const old=document.getElementById('p2DiscoveryMenu');if(old)old.classList.add('p262LegacyShell');const hub=document.querySelector('.p2Hub');if(hub)hub.classList.add('p262LegacyShell');document.body?.classList.add('p262-ready');return true}
function boot(){defaultProfile();let n=0;const go=()=>{if(mount()||++n>18){clearInterval(t)}};const t=setInterval(go,180);go();setTimeout(mount,650);setTimeout(mount,1400)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
