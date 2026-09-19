/* One classification/catalog source for Turkey Groups and the lower transport row. */
(()=>{'use strict';
const CACHE='v201CatalogCache',API=['https://de1.api.radio-browser.info','https://fi1.api.radio-browser.info','https://nl1.api.radio-browser.info'];
const groups=['Pop','Rock','Arabesk','Türkü','Klasik','Haber','Spor','Dini','Jazz','Slow','Nostalji','Elektronik','Diğer'];
const url=s=>s?._url||s?.url_resolved||s?.url||'';
const key=s=>s?.stationuuid||url(s)||s?.name||'';
const fold=t=>String(t||'').toLocaleLowerCase('tr-TR').normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/ı/g,'i');
function genre(s){
 const t=fold([s?.name,s?.tags,s?.genre].join(' '));
 if(/haber|news|gundem|guncel|\bntv\b|haberturk|cnn turk|bloomberg/.test(t))return'Haber';
 if(/spor|sports/.test(t))return'Spor';
 if(/arabesk|fantezi|fantazi|damar/.test(t))return'Arabesk';
 if(/rock|metal|alternative|indie/.test(t))return'Rock';
 if(/turku|folk|halk/.test(t))return'Türkü';
 if(/dini|ilah|kur.?an|tasavvuf/.test(t))return'Dini';
 if(/klasik|classical|opera|senfoni/.test(t))return'Klasik';
 if(/jazz|blues/.test(t))return'Jazz';
 if(/slow|soft|chill|lounge|love|romantik|acoustic|akustik|relax|easy/.test(t))return'Slow';
 if(/nostalji|nostalgia|80.?ler|90.?lar|oldies/.test(t))return'Nostalji';
 if(/electro|elektronik|techno|trance|house|edm|dance/.test(t))return'Elektronik';
 if(/pop|hit|top\s?40/.test(t))return'Pop';
 return'Diğer';
}
function clean(input){
 const result=[],ids=new Set(),urls=new Set();
 for(const s of Array.isArray(input)?input:[]){
  const u=url(s),id=s?.stationuuid;
  if(!s||!/^https?:\/\//i.test(u)||(s.countrycode&&String(s.countrycode).toUpperCase()!=='TR'))continue;
  if(/kurd|kürt|kurdish|kurdi|zaza|zazaki|sorani|kurman|\bdinamo(?:\.fm)?\b/i.test([s.name,s.language,s.languagecodes,s.tags,u].join(' ')))continue;
  if(urls.has(u)||(id&&ids.has(id)))continue;
  urls.add(u);if(id)ids.add(id);result.push({...s,_url:u});
 }
 return result;
}
function cached(){try{return clean(JSON.parse(localStorage.getItem(CACHE)||'{}').data)}catch{return[]}}
let pending=null;
async function request(endpoint){
 if(typeof window.radioFetchJson==='function')return window.radioFetchJson(endpoint,8000);
 const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),8000);
 try{const r=await fetch(endpoint,{signal:controller.signal,cache:'no-store'});if(!r.ok)throw new Error('catalog');return await r.json()}finally{clearTimeout(timer)}
}
function ensure(){
 const stored=cached();if(stored.length)return Promise.resolve(stored);
 if(pending)return pending;
 pending=(async()=>{for(const endpoint of API){try{const data=clean(await request(endpoint+'/json/stations/bycountrycodeexact/TR?hidebroken=true&order=clickcount&reverse=true&limit=3000'));if(data.length){try{localStorage.setItem(CACHE,JSON.stringify({at:Date.now(),data}))}catch{}return data}}catch{}}return cached()})().finally(()=>pending=null);
 return pending;
}
function score(s){return(Number(s.lastcheckok)===1?40:0)+Math.min(16,(Number(s.bitrate)||0)/24)+Math.min(12,Math.log10(1+Math.max(0,Number(s.clickcount)||0))*3)}
function pool(g,data){return clean(data).filter(s=>genre(s)===g&&s.lastcheckok!==0&&s.lastcheckok!=='0').sort((a,b)=>score(b)-score(a)||fold(a.name).localeCompare(fold(b.name),'tr')||key(a).localeCompare(key(b)))}
window.trStationGroups={genres:groups,genre,clean,cached,ensure,pool};

let requestId=0,selectionVersion=0,lastMainKey='';
const $=s=>document.querySelector(s);
function stationList(){try{return window.trGetStations?.()||(typeof stations!=='undefined'?stations:window.stations)||[]}catch{return[]}}
function current(){try{return stationList()[window.trGetIndex?.()??(typeof index==='number'?index:window.index)]||null}catch{return null}}
function mainStations(){
 const all=stationList();let keys=null;try{keys=JSON.parse(localStorage.getItem('v5MainKeys')||'null')}catch{}
 if(Array.isArray(keys)){
  const map=new Map([...cached(),...all].map(s=>[key(s),s]));return keys.map(k=>map.get(k)).filter(s=>url(s));
 }
 return all.slice(0,80).filter(s=>url(s));
}
function notify(s){try{window.toast?.(s)}catch{}}
function busy(on){for(const id of ['prev2','next2']){const b=$('#'+id);if(b)b.setAttribute('aria-busy',String(on))}}
function labels(){
 const s=current(),g=s?genre(s):'',label=g&&g!=='Diğer'?g.toLocaleUpperCase('tr-TR')+' GRUBU':'AYNI TÜR';
 for(const [id,dir] of [['prev2','önceki'],['next2','sonraki']]){
  const b=$('#'+id),small=b?.querySelector('span');if(!b)continue;
  if(small&&small.textContent!==label)small.textContent=label;
  b.setAttribute('aria-label',(g&&g!=='Diğer'?g+' grubunda':'Aynı türde')+' '+dir+' radyo');
  b.title='Türkiye Grupları içindeki '+dir+' radyo';
 }
 const list=mainStations();if(s&&list.some(x=>key(x)===key(s)||url(x)===url(s)))lastMainKey=key(s);
}
function playStation(s){
 const list=stationList();let i=list.findIndex(x=>key(x)===key(s)||url(x)===url(s));
 if(i<0){list.push({...s,_url:url(s)});i=list.length-1}else list[i]={...list[i],...s,_url:url(s)}
 window.dispatchEvent(new CustomEvent('turkradyo-user-playback'));
 if(typeof window.select==='function')window.select(i,true);
 labels();
}
function mainStep(direction){
 ++requestId;busy(false);const list=mainStations(),s=current();if(!list.length)return notify('Ana radyo listesi henüz hazır değil.');
 let i=list.findIndex(x=>key(x)===key(s)||url(x)===url(s));
 if(i<0)i=list.findIndex(x=>key(x)===lastMainKey);
 const next=i<0?(direction>0?0:list.length-1):(i+direction+list.length)%list.length;
 try{localStorage.removeItem('v208GenreMode');localStorage.setItem('trGenre273',JSON.stringify({active:'',pos:0,pool:[]}))}catch{}
 playStation(list[next]);
}
async function groupStep(direction){
 const s=current();if(!s)return notify('Önce bir radyo seçin.');
 const g=genre(s);if(g==='Diğer')return notify('Bu radyonun türü belirlenemedi. Türkiye Grupları’ndan bir radyo seçin.');
 const id=++requestId,version=selectionVersion,start=url(s);busy(true);
 try{
  const catalog=await ensure();
  if(id!==requestId||version!==selectionVersion||url(current())!==start)return;
  // Include the current station as the navigation anchor, including its working alternate URL.
  const candidates=pool(g,[s,...catalog]);
  if(candidates.length<2)return notify(g+' grubunda başka uygun radyo bulunamadı.');
  const found=candidates.findIndex(x=>key(x)===key(s)||url(x)===start);
  const i=found<0?(direction>0?0:candidates.length-1):(found+direction+candidates.length)%candidates.length;
  const chosen=candidates[i];
  if(key(chosen)===key(s)||url(chosen)===start)return notify(g+' grubunda başka uygun radyo bulunamadı.');
  // Do not keep a stale explicitly selected genre pointing at a different radio group.
  try{const st=JSON.parse(localStorage.getItem('trGenre273')||'{}');if(st.active)localStorage.setItem('trGenre273',JSON.stringify({active:g,pos:i,pool:candidates}))}catch{}
  playStation(chosen);notify(g+' • '+(i+1)+'/'+candidates.length+' • '+chosen.name);
 }catch{if(id===requestId)notify('Tür listesi alınamadı. Mevcut yayın korunuyor.')}finally{if(id===requestId)busy(false)}
}
function intercept(e){
 const id=e.target.closest?.('button')?.id;
 if(id==='play'||id==='miniPlay'){++requestId;busy(false);return}
 if(!['prev','next','prev2','next2'].includes(id))return;
 e.preventDefault();e.stopImmediatePropagation();
 const direction=id==='prev'||id==='prev2'?-1:1;
 if(id.endsWith('2'))void groupStep(direction);else mainStep(direction);
}
// Installed before legacy genre listeners; each tap has exactly one owner.
window.addEventListener('click',intercept,true);
window.addEventListener('turkradyo-station-changed',()=>{selectionVersion++;busy(false);labels()});
document.addEventListener('visibilitychange',()=>{if(document.hidden){++requestId;busy(false)}else labels()});
window.addEventListener('turkradyo-theme-synced',labels);
window.trNavigation={mainStations,groupStep,mainStep};
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',labels,{once:true});else labels();
})();
