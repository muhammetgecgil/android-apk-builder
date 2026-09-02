(()=>{'use strict';
const GENRE_KEY='trGenre273',SLOW_KEY='trSlow273';
const $=s=>document.querySelector(s);
function parse(k,d){try{return Object.assign({},d,JSON.parse(localStorage.getItem(k)||'{}'))}catch(e){return{...d}}}
function urlOf(s){return s?._url||s?.url_resolved||s?.url||''}
function nameOf(s){return s?.name||'Türk Radyo'}
function baseStations(){try{return Array.isArray(window.stations)?window.stations.filter(s=>urlOf(s)):[]}catch(e){return[]}}
function currentUrl(){try{return urlOf(window.stations?.[window.index])}catch(e){return''}}
function compact(a){return a.filter(s=>urlOf(s)).map(s=>({name:nameOf(s),url:urlOf(s)}))}
function nativeQueue(a){if(!a.length)return;const cur=currentUrl();let i=Math.max(0,a.findIndex(s=>urlOf(s)===cur));try{window.RadioNative?.setQueue?.(JSON.stringify(compact(a)),i)}catch(e){}}
function genreState(){return parse(GENRE_KEY,{active:'',pos:0,pool:[]})}
function restoreMain(){const a=baseStations();if(a.length)nativeQueue(a)}
function syncGenreQueue(){const g=genreState();if(!g.active){restoreMain();return}const pool=Array.isArray(g.pool)?g.pool.filter(s=>urlOf(s)):[];if(pool.length)nativeQueue(pool);else restoreMain()}
function slowCfg(){return parse(SLOW_KEY,{on:false,cap:.68,intensity:'balanced'})}
function slowCap(x){return Math.min(Number(x.cap||.68),x.intensity==='light'?.82:x.intensity==='deep'?.50:.67)}
function clampVolume(e){const v=e?.target;if(!v||v.id!=='volume')return;const x=slowCfg();if(!x.on)return;const c=slowCap(x),n=Number(v.value||0);if(n>c)v.value=String(c)}
function enforceSlow(){const v=$('#volume'),x=slowCfg();if(!v||!x.on)return;const c=slowCap(x);if(Number(v.value||0)>c){v.value=String(c);v.dispatchEvent(new Event('input',{bubbles:true}))}}
function profileChanged(){setTimeout(()=>{syncGenreQueue();enforceSlow()},80);setTimeout(syncGenreQueue,450)}
function click(e){const b=e.target?.closest?.('button,[data-prof]');if(!b)return;if(b.matches('[data-tr273genre]')||b.id==='tr273GenreOff'||b.id==='tr273GenreNext')setTimeout(syncGenreQueue,30);if(b.matches('[data-prof]')||b.closest?.('[data-prof]'))profileChanged();if(b.id==='tr273SlowStart'||b.id==='tr273SlowAuto'||b.matches('[data-slow-int],[data-slow-min]'))setTimeout(enforceSlow,40)}
function boot(){window.addEventListener('input',clampVolume,true);window.addEventListener('change',clampVolume,true);document.addEventListener('click',click,false);[0,250,900,1800].forEach(ms=>setTimeout(()=>{syncGenreQueue();enforceSlow()},ms));window.addEventListener('pageshow',()=>{syncGenreQueue();enforceSlow()});document.addEventListener('visibilitychange',()=>{if(!document.hidden){syncGenreQueue();enforceSlow()}})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();