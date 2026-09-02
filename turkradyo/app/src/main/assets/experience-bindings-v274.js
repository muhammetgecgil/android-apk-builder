(()=>{'use strict';
const $=s=>document.querySelector(s);
const BRIDGE='tr274FunctionBridge';
const proxies=['trSlow273Card','trTracks273Card','trSmart273Card','trGenre273Card'];
const oldCards=['v12Smart','p2Tracks','p2DNA','v12Genres'];
function installCss(){if($('#tr274BindingsCss'))return;const s=document.createElement('style');s.id='tr274BindingsCss';s.textContent=`
#${BRIDGE}{display:none!important}
#trSlow273Card,#trTracks273Card,#trSmart273Card,#trGenre273Card{display:none!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr273LegacyHidden,
body.profile2-active #p2UnifiedPremiumGrid263>#p2Tracks.tr273LegacyHidden,
body.profile2-active #p2UnifiedPremiumGrid263>#p2DNA.tr273LegacyHidden,
body.profile2-active #p2UnifiedPremiumGrid263>#v12Genres.tr273LegacyHidden{display:flex!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart{transform:translateZ(0)!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.16),inset 0 -24px 34px rgba(0,0,0,.34),0 11px 24px rgba(0,0,0,.38),0 0 20px color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 22%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon{background:radial-gradient(circle at 31% 25%,rgba(255,255,255,.95) 0 2%,color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 65%,#fff) 4% 11%,color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 33%,#12121a) 36%,#080910 72%)!important;box-shadow:inset -10px -11px 15px rgba(0,0,0,.52),inset 7px 7px 12px rgba(255,255,255,.12),0 8px 15px rgba(0,0,0,.34),0 0 24px color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 38%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr274SlowOn{border-color:color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 88%,#fff 10%)!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.18),inset 0 -24px 34px rgba(0,0,0,.32),0 11px 24px rgba(0,0,0,.38),0 0 28px color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 36%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr274SlowOn .p263Icon{animation:tr274SlowPulse 2.5s ease-in-out infinite}@keyframes tr274SlowPulse{50%{filter:brightness(1.16);transform:translateY(-1px) scale(1.025)}}
`;document.head.appendChild(s)}
function bridge(){let b=$('#'+BRIDGE);if(!b){b=document.createElement('div');b.id=BRIDGE;b.setAttribute('aria-hidden','true');document.body.appendChild(b)}return b}
function cleanup(){installCss();oldCards.forEach(id=>$('#'+id)?.classList.remove('tr273LegacyHidden'));const b=bridge();proxies.forEach(id=>{const p=$('#'+id);if(p&&p.parentNode!==b)b.appendChild(p)});const slow=$('#v12Smart');if(slow){let on=false;try{on=!!JSON.parse(localStorage.getItem('trSlow273')||'{}').on}catch(e){}slow.classList.toggle('tr274SlowOn',on)}}
function fire(id,attempt=0){const p=$('#'+id);if(p&&typeof p.click==='function'){p.click();return}if(attempt<20)setTimeout(()=>fire(id,attempt+1),100)}
function target(e){const t=e.target?.closest?.('button');if(!t)return'';if(t.id==='v12Smart')return'trSlow273Card';if(t.id==='p2Tracks')return'trTracks273Card';if(t.matches('.tr-product-quick button[data-q="smart"]'))return'trSmart273Card';if(t.matches('.tr-product-quick button[data-q="genre"]'))return'trGenre273Card';return''}
function intercept(e){const id=target(e);if(!id)return;e.preventDefault();e.stopImmediatePropagation();fire(id)}
function watch(){cleanup();const mo=new MutationObserver(()=>cleanup());mo.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['class']});[0,100,250,500,900,1500,2500,4000,5200,6500].forEach(ms=>setTimeout(cleanup,ms));setInterval(cleanup,5000)}
function boot(){window.addEventListener('click',intercept,true);watch();window.addEventListener('turkradyo-theme-synced',()=>setTimeout(cleanup,0));window.addEventListener('pageshow',cleanup)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();