/* Durable preferences, loaded before the first catalog request. No playback on restore. */
(()=>{'use strict';
const url=s=>s?._url||s?.url_resolved||s?.url||'';
const key=s=>s?.stationuuid||s?.name||url(s);
function get(k,fallback){try{const v=JSON.parse(localStorage.getItem(k));return v==null?fallback:v}catch{return fallback}}
function set(k,v){try{localStorage.setItem(k,JSON.stringify(v));return true}catch{window.toast?.('Tercih kaydedilemedi; cihaz depolamasını kontrol et');return false}}
function valid(s){return s&&typeof s==='object'&&typeof s.name==='string'&&s.name.trim()&&/^https?:\/\//i.test(url(s))}
function snapshot(s){const out={};for(const k of ['stationuuid','name','tags','state','countrycode','language','languagecodes','bitrate','lastcheckok','clickcount','votes'])if(['string','number'].includes(typeof s[k]))out[k]=s[k];return{...out,_url:url(s),url:url(s)}}
function stream(s){if(!s)return s;const o=get('v6Overrides',{})?.[s.stationuuid||s.name];if(o&&/^https?:\/\//i.test(o.url)){s._url=o.url;if(Number.isFinite(o.bitrate))s.bitrate=o.bitrate}return s}
function remember(s){if(valid(s))set('trLastStation293',snapshot(s))}
function restore(list,catalog=[]){
 const saved=get('trLastStation293',null),cache=get('v201CatalogCache',{});
 const all=[...catalog,...(Array.isArray(cache?.data)?cache.data:[])];
 const map=new Map(all.filter(valid).map(s=>[key(s),s]));
 if(valid(saved))map.set(key(saved),map.get(key(saved))||saved);
 const extra=[...(Array.isArray(get('favs',[]))?get('favs',[]):[]),...(Array.isArray(get('recent',[]))?get('recent',[]):[])];
 if(valid(saved))extra.push(key(saved));
 const seen=new Set(list.map(key));
 for(const id of extra){const s=map.get(id);if(s&&!seen.has(id)){list.push({...s,_url:url(s)});seen.add(id)}}
 list.forEach(stream);
 return valid(saved)?list.findIndex(s=>key(s)===key(saved)):-1;
}
window.trMemory={get,set,stream,remember,restore};
})();
