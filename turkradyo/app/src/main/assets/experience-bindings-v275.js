(()=>{'use strict';
const $=s=>document.querySelector(s);
const BRIDGE='tr275FunctionBridge';
const proxies=['trSlow273Card','trTracks273Card','trSmart273Card','trGenre273Card'];
let reconcileQueued=false;
function installCss(){if($('#tr275BindingsCss'))return;const s=document.createElement('style');s.id='tr275BindingsCss';s.textContent=`
#${BRIDGE}{display:none!important}
#trSlow273Card,#trTracks273Card,#trSmart273Card,#trGenre273Card{display:none!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr273LegacyHidden,
body.profile2-active #p2UnifiedPremiumGrid263>#p2Tracks.tr273LegacyHidden,
body.profile2-active #p2UnifiedPremiumGrid263>#p2DNA.tr273LegacyHidden,
body.profile2-active #p2UnifiedPremiumGrid263>#v12Genres.tr273LegacyHidden{display:flex!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart{transform:translateZ(0)!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.16),inset 0 -24px 34px rgba(0,0,0,.34),0 11px 24px rgba(0,0,0,.38),0 0 20px color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 22%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon{background:radial-gradient(circle at 31% 25%,rgba(255,255,255,.95) 0 2%,color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 65%,#fff) 4% 11%,color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 33%,#12121a) 36%,#080910 72%)!important;box-shadow:inset -10px -11px 15px rgba(0,0,0,.52),inset 7px 7px 12px rgba(255,255,255,.12),0 8px 15px rgba(0,0,0,.34),0 0 24px color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 38%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn{border-color:color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 88%,#fff 10%)!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.18),inset 0 -24px 34px rgba(0,0,0,.32),0 11px 24px rgba(0,0,0,.38),0 0 28px color-mix(in srgb,var(--p263a,var(--accent,#ff3647)) 36%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn .p263Icon{animation:tr275SlowPulse 2.5s ease-in-out infinite}@keyframes tr275SlowPulse{50%{filter:brightness(1.16);transform:translateY(-1px) scale(1.025)}}
`;document.head.appendChild(s)}
function bridge(){let b=$('#'+BRIDGE);if(!b){b=document.createElement('div');b.id=BRIDGE;b.setAttribute('aria-hidden','true');document.body.appendChild(b)}return b}
function syncSlowVisual(){const slow=$('#v12Smart');if(!slow)return;let on=false;try{on=!!JSON.parse(localStorage.getItem('trSlow273')||'{}').on}catch(e){}if(slow.classList.contains('tr275SlowOn')!==on)slow.classList.toggle('tr275SlowOn',on)}
function reconcile(){reconcileQueued=false;installCss();const b=bridge();for(const id of proxies){const p=$('#'+id);if(p&&p.parentNode!==b)b.appendChild(p)}syncSlowVisual()}
function queueReconcile(delay=0){if(delay){setTimeout(()=>queueReconcile(0),delay);return}if(reconcileQueued)return;reconcileQueued=true;requestAnimationFrame(reconcile)}
function fire(id,attempt=0){const direct=id==='trSlow273Card'?window.trOpenSlow279:id==='trTracks273Card'?window.trOpenTracks279:id==='trSmart273Card'?window.trOpenSmart279:id==='trGenre273Card'?window.trOpenGenre279:null;if(typeof direct==='function'){direct();queueReconcile(40);return}const p=$('#'+id);if(p&&typeof p.click==='function'){p.click();queueReconcile(40);return}if(attempt<18)setTimeout(()=>fire(id,attempt+1),110)}
function target(e){const t=e.target?.closest?.('button');if(!t)return'';if(t.id==='v12Smart')return'trSlow273Card';if(t.id==='p2Tracks')return'trTracks273Card';if(t.matches('.tr-product-quick button[data-q="smart"]'))return'trSmart273Card';if(t.matches('.tr-product-quick button[data-q="genre"]'))return'trGenre273Card';return''}
function intercept(e){const id=target(e);if(!id)return;e.preventDefault();e.stopImmediatePropagation();fire(id)}
function boot(){window.addEventListener('click',intercept,true);[0,120,350,800,1600,3000,5200].forEach(ms=>queueReconcile(ms));document.addEventListener('click',e=>{if(e.target.closest?.('[data-prof],[data-use],[data-mode="themes"],.nature-reset')){queueReconcile(60);queueReconcile(500)}},true);window.addEventListener('turkradyo-theme-synced',()=>queueReconcile(0));window.addEventListener('pageshow',()=>queueReconcile(0));document.addEventListener('visibilitychange',()=>{if(!document.hidden)queueReconcile(0)});window.addEventListener('storage',e=>{if(e.key==='trSlow273')queueReconcile(0)})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();