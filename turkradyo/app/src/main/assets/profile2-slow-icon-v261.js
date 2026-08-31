(()=>{'use strict';
const SVG='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M17.5 15.8A7.4 7.4 0 0 1 8.2 6.5a7.5 7.5 0 1 0 9.3 9.3Z"/></svg>';
function style(){if(document.getElementById('p2SlowIcon261Css'))return;const s=document.createElement('style');s.id='p2SlowIcon261Css';s.textContent=`
.profile2-active #v12Smart{align-items:center!important;justify-content:flex-start!important}
.profile2-active #v12Smart>b{
 position:relative!important;left:auto!important;right:auto!important;top:auto!important;bottom:auto!important;transform:none!important;
 display:grid!important;place-items:center!important;box-sizing:border-box!important;
 width:64px!important;height:64px!important;min-width:64px!important;min-height:64px!important;max-width:64px!important;max-height:64px!important;
 margin:0 0 15px!important;padding:0!important;overflow:hidden!important;border-radius:50%!important;
 border:1.25px solid color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 84%,#fff 6%)!important;
 background:radial-gradient(circle at 42% 34%,color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 16%,#2b1821) 0%,#111019 43%,#06070b 76%)!important;
 color:#fff7f8!important;box-shadow:inset 0 0 20px #0009,inset 0 0 0 1px #ffffff0d,0 0 0 4px color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 8%,transparent),0 0 22px color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 31%,transparent)!important;
 text-shadow:none!important;font-size:0!important;line-height:1!important;
}
.profile2-active #v12Smart>b svg{display:block!important;width:34px!important;height:34px!important;max-width:34px!important;max-height:34px!important;margin:0!important;transform:none!important;fill:#fff7f8!important;stroke:none!important;filter:drop-shadow(0 0 7px color-mix(in srgb,var(--p260a,var(--accent,var(--red,#ff3647))) 55%,transparent))!important}
@media(max-width:430px){.profile2-active #v12Smart>b{width:59px!important;height:59px!important;min-width:59px!important;min-height:59px!important;max-width:59px!important;max-height:59px!important;margin-bottom:14px!important}.profile2-active #v12Smart>b svg{width:31px!important;height:31px!important;max-width:31px!important;max-height:31px!important}}
`;document.head.appendChild(s)}
function apply(){style();const b=document.querySelector('#v12Smart');if(!b)return false;const icon=b.querySelector(':scope>b')||b.querySelector('b');if(!icon)return false;if(icon.dataset.p261Slow!=='1'){icon.dataset.p261Slow='1';icon.innerHTML=SVG}return true}
function mount(){apply();setTimeout(apply,350);setTimeout(apply,1000);setTimeout(apply,2300)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();
