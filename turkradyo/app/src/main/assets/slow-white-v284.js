(()=>{'use strict';
const ID='trSlowWhite284Css';
function install(){
  if(document.getElementById(ID))return;
  const s=document.createElement('style');
  s.id=ID;
  s.textContent=`
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart{
  border-color:rgba(255,255,255,.88)!important;
  background:radial-gradient(circle at 50% 0%,rgba(255,255,255,.095),transparent 47%),linear-gradient(160deg,#19191e 0%,#0d0d12 58%,#07070a 100%)!important;
  box-shadow:inset 0 1px 0 rgba(255,255,255,.22),inset 0 -20px 34px rgba(0,0,0,.34),0 9px 22px rgba(0,0,0,.44),0 0 18px rgba(255,255,255,.16)!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon,
body.profile2-active #v12Smart>b{
  border-color:rgba(255,255,255,.94)!important;
  background:radial-gradient(circle at 38% 28%,rgba(255,255,255,.16) 0%,rgba(28,28,34,.96) 35%,#0c0d11 72%,#050609 100%)!important;
  color:#fff!important;
  box-shadow:inset 0 1px 0 rgba(255,255,255,.18),inset 0 -10px 18px rgba(0,0,0,.45),0 0 0 2px rgba(255,255,255,.05),0 0 20px rgba(255,255,255,.22)!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon .p282SlowWave,
body.profile2-active #v12Smart>b svg{
  color:#fff!important;
  stroke:#fff!important;
  filter:drop-shadow(0 0 5px rgba(255,255,255,.70))!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Label{
  color:#fff!important;
  text-shadow:0 0 7px rgba(255,255,255,.18)!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn{
  border-color:#fff!important;
  box-shadow:inset 0 1px 0 rgba(255,255,255,.26),inset 0 -22px 34px rgba(0,0,0,.32),0 10px 24px rgba(0,0,0,.42),0 0 28px rgba(255,255,255,.32)!important;
}
body.profile2-active [data-slow-pro="1"].v187SlowOn:before{
  color:#fff!important;
  border-color:rgba(255,255,255,.72)!important;
  background:rgba(16,16,20,.92)!important;
  box-shadow:0 0 12px rgba(255,255,255,.16)!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart:active{
  border-color:#fff!important;
}
@keyframes tr284SlowWhitePulse{50%{filter:brightness(1.10);transform:translateY(-1px) scale(1.025)}}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn .p263Icon .p282SlowWave{animation:tr284SlowWhitePulse 2.2s ease-in-out infinite!important}
@media(prefers-reduced-motion:reduce){body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart.tr275SlowOn .p263Icon .p282SlowWave{animation:none!important}}
`;
  document.head.appendChild(s);
}
function boot(){install();window.addEventListener('turkradyo-theme-synced',install);window.addEventListener('pageshow',install);document.addEventListener('visibilitychange',()=>{if(!document.hidden)install()})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
