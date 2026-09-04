(()=>{'use strict';
let wrapped=false;
function sync(){
  try{if(typeof stations!=='undefined'&&Array.isArray(stations))window.stations=stations}catch(e){}
  try{if(typeof index!=='undefined')window.index=index}catch(e){}
  try{window.trGetStations=()=>{try{return Array.isArray(stations)?stations:[]}catch(e){return[]}}}catch(e){}
  try{window.trGetIndex=()=>{try{return typeof index==='number'?index:-1}catch(e){return-1}}}catch(e){}
}
function wrap(){
  sync();
  if(wrapped)return;
  const original=typeof window.select==='function'?window.select:null;
  if(!original)return;
  const fn=function(i,auto){const r=original.call(window,i,auto);sync();return r};
  fn.__tr281=true;window.select=fn;wrapped=true;
}
function boot(){
  [0,80,220,500,1000,2200].forEach(ms=>setTimeout(()=>{wrap();sync()},ms));
  document.addEventListener('click',e=>{if(e.target.closest?.('#prev,#next,#prev2,#next2,.mode,[data-p2play],[data-tr273genre],[data-g280],.trBazCard,.tr273Choice'))setTimeout(sync,0)},true);
  window.addEventListener('pageshow',sync);
  document.addEventListener('visibilitychange',()=>{if(!document.hidden)sync()});
  window.addEventListener('turkradyo-theme-synced',()=>setTimeout(sync,0));
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();