(()=>{'use strict';
const SVG='<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="2.1"/><path d="M4 9.2c2.1-2 4.1-2 6.1 0s4.1 2 6.1 0 3.3-1.7 3.8-1.3M4 14.8c2.1 2 4.1 2 6.1 0s4.1-2 6.1 0 3.3 1.7 3.8 1.3"/><path d="M6.4 12h11.2"/></svg>';
function style(){if(document.getElementById('p2SlowIcon261Css'))return;const s=document.createElement('style');s.id='p2SlowIcon261Css';s.textContent=`
.profile2-active #v12Smart{align-items:center!important;justify-content:flex-start!important}
.profile2-active #v12Smart>b{
 position:relative!important;left:auto!important;right:auto!important;top:auto!important;bottom:auto!important;transform:none!important;
 display:grid!important;place-items:center!important;box-sizing:border-box!important;
 width:64px!important;height:64px!important;min-width:64px!important;min-height:64px!important;max-width:64px!important;max-height:64px!important;
 margin:0 0 15px!important;padding:0!important;overflow:hidden!important;border-radius:50%!important;
 border:1.25px solid color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 84%,#fff 6%)!important;
 background:radial-gradient(circle at 42% 34%,color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 22%,#2b1821) 0%,#111019 43%,#06070b 76%)!important;
 color:#fff7f8!important;box-shadow:inset 0 0 20px #0009,inset 0 0 0 1px #ffffff0d,0 0 0 4px color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 8%,transparent),0 0 22px color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 31%,transparent)!important;
 text-shadow:none!important;font-size:0!important;line-height:1!important;
}
.profile2-active #v12Smart>b svg{display:block!important;width:38px!important;height:38px!important;max-width:38px!important;max-height:38px!important;margin:0!important;transform:none!important;fill:none!important;stroke:currentColor!important;stroke-width:1.7!important;stroke-linecap:round!important;stroke-linejoin:round!important;filter:drop-shadow(0 0 7px color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 55%,transparent))!important}
.profile2-active #v12Smart>b svg circle{fill:currentColor!important;stroke:none!important;filter:drop-shadow(0 0 4px currentColor)}
@media(max-width:430px){.profile2-active #v12Smart>b{width:59px!important;height:59px!important;min-width:59px!important;min-height:59px!important;max-width:59px!important;max-height:59px!important;margin-bottom:14px!important}.profile2-active #v12Smart>b svg{width:34px!important;height:34px!important;max-width:34px!important;max-height:34px!important}}
`;document.head.appendChild(s)}
function apply(){style();const b=document.querySelector('#v12Smart');if(!b)return false;const icon=b.querySelector(':scope>b')||b.querySelector('b');if(!icon)return false;icon.dataset.p261Slow='waves';icon.innerHTML=SVG;return true}
function mount(){apply();setTimeout(apply,350);setTimeout(apply,1000);setTimeout(apply,2300);setTimeout(apply,3600)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();
