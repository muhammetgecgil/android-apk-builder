(()=>{'use strict';
const API9=['https://de1.api.radio-browser.info','https://fi1.api.radio-browser.info','https://nl1.api.radio-browser.info'];
const BAD9=/kurd|kürt|kurdish|kurdi|zaza|zazaki|sorani|kurman/i;
const q9=s=>document.querySelector(s);let lastErr=0,lastBuffer=0,lastSwitch=0,currentName='',pool=[],prefetchBusy=false;
function norm9(s){return String(s||'').toLocaleLowerCase('tr').replace(/fm|radyo|radio|türkiye|turkiye/g,'').replace(/[^a-z0-9çğıöşü]/gi,'')}
function key9(u){return 'v9q_'+btoa(unescape(encodeURIComponent(String(u||'')))).replace(/[^a-z0-9]/gi,'').slice(0,80)}
function quarantined9(u){const t=Number(localStorage.getItem(key9(u))||0);return t>Date.now()}
function quarantine9(u,ms=10*60*1000){try{localStorage.setItem(key9(u),String(Date.now()+ms))}catch(e){}}
async function api9(path){for(const a of API9){try{const r=await fetch(a+path,{cache:'no-store'});if(r.ok)return await r.json()}catch(e){}}return []}
function score9(s){let z=0;z+=(s.lastcheckok?45:0);z+=Math.min(25,(s.bitrate||0)/12);z+=Math.min(15,(s.votes||0)/10);z+=Math.min(10,(s.clickcount||0)/1500);if(/^https:/i.test(s._url||''))z+=5;if(quarantined9(s._url))z-=1000;return z}
async function refreshPool9(name){if(!name||prefetchBusy)return;prefetchBusy=true;try{const d=await api9('/json/stations/byname/'+encodeURIComponent(name)+'?hidebroken=true&order=clickcount&reverse=true&limit=120');const n=norm9(name),seen=new Set();pool=d.filter(x=>{const txt=[x.name,x.tags,x.language,x.languagecodes].join(' '),u=x.url_resolved||x.url;if(!u||BAD9.test(txt)||norm9(x.name)!==n||seen.has(u))return false;seen.add(u);x._url=u;return true}).sort((a,b)=>score9(b)-score9(a));}finally{prefetchBusy=false}}
function healthEl9(){let el=q9('#v9Health');if(el)return el;const freq=q9('#freq');if(!freq)return null;el=document.createElement('div');el.id='v9Health';el.className='v9Health';el.innerHTML='<i></i><span>BAĞLANTI SAĞLIĞI • HAZIR</span>';freq.after(el);return el}
function setHealth9(score,text,state=''){const el=healthEl9();if(!el)return;el.className='v9Health'+(state?' '+state:'');el.querySelector('span').textContent='BAĞLANTI '+Math.max(0,Math.min(100,Math.round(score)))+'/100 • '+text}
function cur9(){try{return stations&&index>=0?stations[index]:null}catch(e){return null}}
function playAlt9(reason){const s=cur9();if(!s||Date.now()-lastSwitch<12000)return false;const alt=pool.find(x=>x._url&&x._url!==s._url&&!quarantined9(x._url));if(!alt)return false;quarantine9(s._url);s._url=alt._url;s.bitrate=alt.bitrate||s.bitrate;s.codec=alt.codec||s.codec;lastSwitch=Date.now();try{play();setHealth9(96,'YEDEK KAYNAĞA GEÇİLDİ');try{toast('Bağlantı güçlendirildi • yedek kaynak')}catch(e){}return true}catch(e){return false}}
async function heal9(reason){const s=cur9();if(!s)return;await refreshPool9(s.name);if(playAlt9(reason))return;setHealth9(45,'KAYNAK ARANIYOR','warn');const d=await api9('/json/stations/byname/'+encodeURIComponent(s.name)+'?hidebroken=true&limit=200');const cand=d.filter(x=>{const u=x.url_resolved||x.url;return u&&!BAD9.test([x.name,x.tags,x.language].join(' '))&&!quarantined9(u)}).map(x=>({...x,_url:x.url_resolved||x.url})).sort((a,b)=>score9(b)-score9(a))[0];if(cand&&cand._url&&cand._url!==s._url){quarantine9(s._url);s._url=cand._url;s.bitrate=cand.bitrate||s.bitrate;lastSwitch=Date.now();try{play();setHealth9(92,'YENİ KAYNAK BULUNDU');try{toast('Self-healing yeni yayın kaynağı buldu')}catch(e){}}catch(e){}}else setHealth9(38,'ALTERNATİF BEKLENİYOR','warn')}
function monitor9(){setInterval(async()=>{const s=cur9();if(!s)return;if(s.name!==currentName){currentName=s.name;pool=[];refreshPool9(s.name);lastErr=0;lastBuffer=0;setHealth9(98,'STABİL')}
let t={};try{t=JSON.parse(window.RadioNative?.getTelemetry?.()||'{}')}catch(e){}
let score=100;score-=Math.min(35,(t.bufferCount||0)*7);score-=t.buffering?18:0;score-=t.lastError?32:0;score-=Math.min(15,(t.reconnectAttempts||0)*5);if((t.startupMs||0)>4000)score-=8;
if(t.lastError&&t.lastError!==lastErr){lastErr=t.lastError;setHealth9(score,'HATA • YEDEK DENENİYOR','bad');await heal9('error');return}
if((t.bufferCount||0)>lastBuffer){lastBuffer=t.bufferCount||0;if(lastBuffer>=3){setHealth9(score,'SIK BUFFER • KAYNAK DEĞİŞİMİ','warn');await heal9('buffer');return}}
setHealth9(score,score>=90?'STABİL':score>=70?'İYİ':score>=50?'ZAYIF':'ONARILIYOR',score<50?'bad':score<75?'warn':'')},2500)}
function moveProfile9(){const pill=document.querySelector('.nature-profile-pill');const hero=document.querySelector('.hero');if(!pill||!hero)return;if(pill.parentElement!==hero)hero.prepend(pill)}
function mount9(){moveProfile9();healthEl9();monitor9();const mo=new MutationObserver(moveProfile9);mo.observe(document.body,{childList:true,subtree:true})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount9);else mount9();
})();