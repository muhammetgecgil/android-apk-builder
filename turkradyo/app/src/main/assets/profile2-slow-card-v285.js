(()=>{'use strict';
const $=s=>document.querySelector(s);
const PULSE=`<svg class="p285CalmPulse" viewBox="0 0 64 64" aria-hidden="true"><circle class="p285Ring" cx="32" cy="32" r="22"/><path class="p285Pulse" d="M11 33h8l3-7 5 15 5-22 5 25 5-14 4 7h7"/><circle class="p285Dot" cx="32" cy="32" r="2.8"/></svg>`;
function css(){if($('#p285SlowThemeCss'))return;const s=document.createElement('style');s.id='p285SlowThemeCss';s.textContent=`
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart{
 --p285a:var(--p270accent,var(--p263a,var(--accent,#ff3647)));
 --p285surface:var(--p270surface,var(--card,#15121a));
 --p285surface2:var(--p270surface2,var(--card2,#21131c));
 --p285text:var(--p270text,var(--text,#fff));
 border-color:color-mix(in srgb,var(--p285a) 46%,rgba(255,255,255,.12))!important;
 background:radial-gradient(circle at 50% 0%,color-mix(in srgb,var(--p285a) 18%,transparent),transparent 46%),linear-gradient(160deg,color-mix(in srgb,var(--p285surface2) 94%,var(--p285a) 6%),color-mix(in srgb,var(--p285surface) 98%,#000))!important;
 box-shadow:inset 0 1px 0 #ffffff18,inset 0 -20px 36px #0002,0 8px 22px #0005,0 0 14px color-mix(in srgb,var(--p285a) 10%,transparent)!important;
 color:var(--p285text)!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart::before{background:linear-gradient(120deg,#ffffff12,transparent 28%,transparent 72%,color-mix(in srgb,var(--p285a) 12%,transparent))!important;opacity:.72!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart::after{content:"CALM"!important;display:block!important;position:absolute!important;right:7px!important;top:7px!important;z-index:3!important;padding:3px 5px!important;border-radius:999px!important;border:1px solid color-mix(in srgb,var(--p285a) 42%,transparent)!important;background:color-mix(in srgb,var(--p285surface) 86%,transparent)!important;color:color-mix(in srgb,var(--p285a) 72%,#fff 28%)!important;font-size:6px!important;font-weight:900!important;letter-spacing:.75px!important;line-height:1!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon{border-color:color-mix(in srgb,var(--p285a) 72%,#fff 8%)!important;background:radial-gradient(circle at 44% 34%,color-mix(in srgb,var(--p285a) 16%,var(--p285surface2)),color-mix(in srgb,var(--p285surface) 96%,#02060b) 72%)!important;box-shadow:inset 0 0 18px #0006,0 0 14px color-mix(in srgb,var(--p285a) 22%,transparent)!important;color:#f7fbff!important;overflow:visible!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285CalmPulse{width:31px!important;height:31px!important;max-width:31px!important;max-height:31px!important;display:block!important;fill:none!important;stroke:none!important;filter:none!important;overflow:visible!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285Ring{fill:none!important;stroke:color-mix(in srgb,var(--p285a) 38%,rgba(255,255,255,.22))!important;stroke-width:1.2!important;stroke-dasharray:2.2 3.7!important;transform-origin:32px 32px;animation:p285ring 12s linear infinite}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285Pulse{fill:none!important;stroke:color-mix(in srgb,var(--p285a) 72%,#fff 28%)!important;stroke-width:3!important;stroke-linecap:round!important;stroke-linejoin:round!important;filter:drop-shadow(0 0 5px color-mix(in srgb,var(--p285a) 42%,transparent))!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285Dot{fill:#fff!important;stroke:color-mix(in srgb,var(--p285a) 70%,#fff)!important;stroke-width:1!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn,body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr274SlowOn{border-color:color-mix(in srgb,var(--p285a) 72%,#fff 8%)!important;box-shadow:inset 0 1px 0 #ffffff20,inset 0 -20px 36px #0002,0 8px 22px #0005,0 0 22px color-mix(in srgb,var(--p285a) 24%,transparent)!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn::after,body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr274SlowOn::after{content:"ACTIVE"!important;background:color-mix(in srgb,var(--p285a) 18%,var(--p285surface))!important;color:#fff!important}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn .p285Pulse,body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr274SlowOn .p285Pulse{animation:p285pulse 2.1s ease-in-out infinite}
@keyframes p285ring{to{transform:rotate(360deg)}}@keyframes p285pulse{50%{transform:scaleY(.9);filter:drop-shadow(0 0 9px color-mix(in srgb,var(--p285a) 66%,transparent))}}
@media(max-width:430px){body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285CalmPulse{width:28px!important;height:28px!important;max-width:28px!important;max-height:28px!important}body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart::after{right:5px!important;top:5px!important;font-size:5.4px!important}}
@media(prefers-reduced-motion:reduce){body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285Ring,body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p285Pulse{animation:none!important}}
`;document.head.appendChild(s)}
function apply(){css();const card=$('#v12Smart');if(!card)return false;const icon=card.querySelector('.p263Icon')||card.querySelector(':scope>b')||card.querySelector('b');if(!icon)return false;if(icon.dataset.p285Slow!=='theme-pulse'||!icon.querySelector('.p285CalmPulse')){icon.innerHTML=PULSE;icon.dataset.p285Slow='theme-pulse'}return true}
function boot(){[0,100,250,600,1200,2400].forEach(ms=>setTimeout(apply,ms));new MutationObserver(()=>{const i=$('#v12Smart .p263Icon');if(i&&!i.querySelector('.p285CalmPulse'))queueMicrotask(apply)}).observe(document.documentElement,{subtree:true,childList:true});window.addEventListener('turkradyo-theme-synced',()=>setTimeout(apply,0));document.addEventListener('visibilitychange',()=>{if(!document.hidden)setTimeout(apply,0)})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();