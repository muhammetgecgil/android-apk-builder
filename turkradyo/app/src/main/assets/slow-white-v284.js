(()=>{'use strict';
const ID='trSlowInnerWhite285Css';
function install(){
  if(document.getElementById(ID))return;
  const s=document.createElement('style');
  s.id=ID;
  s.textContent=`
/* v2.8.5: Slow Mod stays in the shared premium/theme system.
   Only the symbol INSIDE the circular dial is white. */
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon .p282SlowWave,
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon .p282SlowWave path,
body.profile2-active #v12Smart>b svg,
body.profile2-active #v12Smart>b svg path{
  color:#fff!important;
  stroke:#fff!important;
}
body.profile2-active #p2UnifiedPremiumGrid263>#v12Smart .p263Icon .p282SlowWave{
  filter:drop-shadow(0 0 5px rgba(255,255,255,.58))!important;
}
/* Deliberately no card/background/border/ring/label/badge overrides here.
   Those continue to follow the active theme exactly like the other premium cards. */
`;
  document.head.appendChild(s);
}
function boot(){install();window.addEventListener('turkradyo-theme-synced',install);window.addEventListener('pageshow',install);document.addEventListener('visibilitychange',()=>{if(!document.hidden)install()})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
