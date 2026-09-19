(()=>{'use strict';
const $=s=>document.querySelector(s),paths={
 menu:'<path d="M4 6h16M4 12h16M4 18h16"/>',
 settings:'<path d="m9 3-1 3-3 1-2 3 2 2-1 3 2 3 3-1 3 2 3-2 3 1 2-3-1-3 2-2-2-3-3-1-1-3z"/><circle cx="12" cy="11" r="3"/>',
 prev:'<path d="M5 5v14M19 5 8 12l11 7z"/>',next:'<path d="M19 5v14M5 5l11 7-11 7z"/>',
 play:'<path d="m8 4 13 8-13 8z" fill="currentColor" stroke="none"/>',pause:'<path d="M8 5v14M16 5v14" stroke-width="4"/>',
 low:'<path d="M4 9h4l5-4v14l-5-4H4zM17 9a5 5 0 0 1 0 6"/>',high:'<path d="M3 9h4l5-4v14l-5-4H3zM16 8a6 6 0 0 1 0 8M19 5a10 10 0 0 1 0 14"/>',
 home:'<path d="m3 11 9-8 9 8M5 10v11h5v-7h4v7h5V10"/>',discover:'<circle cx="12" cy="12" r="9"/><path d="m16 8-3 5-5 3 3-5z"/>',favorites:'<path d="M20 5c-3-3-6-1-8 1-2-2-5-4-8-1-5 5 8 15 8 15S25 10 20 5z"/>'};
const svg=n=>'<svg class="tr-detail-icon" viewBox="0 0 24 24" aria-hidden="true">'+paths[n]+'</svg>';
function attr(el,k,v){if(el&&el.getAttribute(k)!==v)el.setAttribute(k,v)}
function icon(el,n,label){if(!el)return;const html=svg(n);if(el.innerHTML!==html)el.innerHTML=html;if(label)attr(el,'aria-label',label)}
function text(el,value){if(el&&el.textContent!==value)el.textContent=value}
function readState(){try{return JSON.parse(window.RadioNative?.getTelemetry?.()||'{}')}catch{return{}}}
let graceUntil=0,lastNativeVolume=null;
function decorate(){
 icon($('#menuBtn'),'menu','Radyo listesini aç');icon($('#settingsBtn'),'settings','Ayarları aç');
 icon($('#prev'),'prev','Önceki radyo');icon($('#next'),'next','Sonraki radyo');
 icon($('.vol>span:first-child'),'low');icon($('.vol>span:last-child'),'high');
 document.querySelectorAll('.bottom [data-nav]').forEach(b=>icon(b.querySelector('b'),paths[b.dataset.nav]?b.dataset.nav:'settings'));
 attr($('#volume'),'aria-label','Ses seviyesi');attr($('#search'),'aria-label','Radyolarda ara');
 const live=$('.live');if(live&&!live.querySelector('.tr-live-text'))live.innerHTML='<i class="tr-live-mark" aria-hidden="true"></i><span class="tr-live-text">HAZIR</span>';
 attr($('#toast'),'role','status');attr($('#toast'),'aria-live','polite');
 document.querySelectorAll('.sheet .panel,.nature-shell').forEach(e=>{attr(e,'role','dialog');attr(e,'aria-modal','true')});
 attr($('.sheet .panel'),'aria-labelledby','sheetTitle');
 update();
}
function update(){
 if(document.hidden)return;
 const t=readState();let state='idle';let isPlaying=false;
 try{isPlaying=!!playing;state=paused?'paused':isPlaying?'playing':'idle'}catch{}
 if(Date.now()>graceUntil&&('isPlaying' in t||'serviceActive' in t)){
  isPlaying=!!t.isPlaying&&t.serviceActive!==false&&!t.manualPause;
  state=t.serviceActive===false?'idle':t.manualPause?'paused':t.buffering?'buffering':isPlaying?'playing':t.networkType==='offline'?'offline':'idle';
  try{paused=!!t.manualPause;if(playing!==isPlaying&&typeof setPlayUI==='function')setPlayUI(isPlaying)}catch{}
  if(t.serviceActive!==false&&t.primaryUrl){try{const all=window.trGetStations?.()||window.stations||[],i=all.findIndex(s=>(s._url||s.url_resolved||s.url)===t.primaryUrl);if(i>=0&&i!==(window.trGetIndex?.()??window.index))window.select?.(i,false)}catch{}}
  if(Number.isFinite(t.volume)&&t.volume!==lastNativeVolume){lastNativeVolume=t.volume;const v=$('#volume');if(v)v.value=String(t.volume)}
 }
 if(document.body.dataset.playback!==state)document.body.dataset.playback=state;
 const labels={idle:'HAZIR',paused:'DURAKLATILDI',playing:'CANLI YAYIN',buffering:'BAĞLANIYOR',offline:'İNTERNET BEKLENİYOR'};
 text($('.tr-live-text'),labels[state]);
 text($('.mini>div:nth-child(2)>small'),isPlaying?'Şu anda çalıyor':'Seçili istasyon');
 icon($('#play'),isPlaying?'pause':'play',isPlaying?'Duraklat':'Oynat');icon($('#miniPlay'),isPlaying?'pause':'play',isPlaying?'Duraklat':'Oynat');
 const v=$('#volume');if(v){const fill=String(Math.round(Number(v.value)*100))+'%';if(v.style.getPropertyValue('--range-fill')!==fill)v.style.setProperty('--range-fill',fill);attr(v,'aria-valuetext',fill+' ses')}
 syncOverlays();
}
function syncOverlays(){const open=!!document.querySelector('.sheet.show,.nature-modal.show');if(document.body.classList.contains('tr-overlay-open')!==open)document.body.classList.toggle('tr-overlay-open',open)}
window.trCloseTopOverlay=()=>{const open=[...document.querySelectorAll('.sheet.show,.nature-modal.show')];const top=open.at(-1);if(!top)return false;top.classList.remove('show');syncOverlays();return true};
function boot(){
 document.body.classList.add('tr-refined');
 const css=document.createElement('link');css.rel='stylesheet';css.href='https://appassets.androidplatform.net/assets/refinement-v2872.css?v=2872';document.head.appendChild(css);
 [0,300,1100,2600,5000].forEach(ms=>setTimeout(decorate,ms));
 document.addEventListener('click',e=>{if(e.target.closest?.('#play,#miniPlay,#prev,#next,#prev2,#next2,.item,[data-p2play],.tr273Choice'))graceUntil=Date.now()+1500;setTimeout(update,40);if(e.target.closest?.('[data-prof],[data-use],.nature-reset'))setTimeout(decorate,500)},true);
 document.addEventListener('input',e=>{if(e.target.id==='volume'){graceUntil=Date.now()+1500;lastNativeVolume=null;update()}});
 document.addEventListener('keydown',e=>{if(e.key==='Escape'&&window.trCloseTopOverlay())e.preventDefault()});
 document.addEventListener('visibilitychange',()=>{document.body.classList.toggle('tr-background',document.hidden);if(!document.hidden)update()});
 window.addEventListener('turkradyo-theme-synced',()=>setTimeout(decorate,0));
 const watch=new MutationObserver(syncOverlays);document.querySelectorAll('#sheet,.nature-modal').forEach(e=>watch.observe(e,{attributes:true,attributeFilter:['class']}));
 setInterval(update,1000);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
