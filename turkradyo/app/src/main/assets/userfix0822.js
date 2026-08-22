(function(){
if(window.__USERFIX0822__)return;window.__USERFIX0822__=1;
const $=s=>document.querySelector(s),$$=s=>Array.from(document.querySelectorAll(s));
const BAD=/kurd|kürt|kurdî|kurdi|kurdish|kurman|sorani|zaza|zazaki|rojav|govend|gundi|azadi/i;
function tracks(){try{return JSON.parse(window.RadioNative?RadioNative.getRecentTracks():'[]')}catch(e){return[]}}
function tele(){try{return JSON.parse(window.RadioNative?RadioNative.getTelemetry():'{}')}catch(e){return{}}}
function norm(s){return String(s||'').toLocaleLowerCase('tr-TR').replace(/[^a-z0-9çğıöşü]+/g,' ').trim()}
function bad(s){return BAD.test([s&&s.name,s&&s.tags,s&&s.language,s&&s.languagecodes].join(' '))}
function score(s){let n=35;if(+s.lastcheckok===1)n+=30;if(+s.bitrate>=192)n+=20;else if(+s.bitrate>=128)n+=15;else if(+s.bitrate>=64)n+=8;if(/aac/i.test(s.codec||''))n+=5;return Math.min(100,n)}
function purgeMain(){if(!window.stations)return;for(let i=stations.length-1;i>=0;i--)if(bad(stations[i])&&i!==window.index)stations.splice(i,1);}
function forceSmooth(){if(localStorage.getItem('p4pref_p4Smooth')!=='1')localStorage.setItem('p4pref_p4Smooth','1');if(localStorage.getItem('smooth')!=='1')localStorage.setItem('smooth','1');try{native('radioapp://smooth?on=1')}catch(e){}}
function fixButtons(){let labels=['SON 50 ŞARKI','SON 50 ŞARKIYI AÇ'];$$('button,.dailyMore,.p4Tool').forEach(b=>{let t=(b.textContent||'').trim().toLocaleUpperCase('tr-TR');if(labels.some(x=>t.includes(x))&&!b.dataset.real50){b.dataset.real50='1';b.onclick=()=>{if(typeof window.openRealTracks50==='function')window.openRealTracks50();else if(typeof window.openMode==='function')openMode('history')}}});}
function patchP3Tracks(){let root=$('#p3Exact');if(!root)return;let card=$$('#p3Exact .card').find(x=>/BENZER ŞARKI AKIŞI|SON 50 GERÇEK ŞARKI/.test((x.textContent||'').toLocaleUpperCase('tr-TR')));if(!card)return;let a=tracks();card.innerHTML='<div class="head">♫ SON ÇALAN 50 ŞARKI <button id="uf50">TÜMÜNÜ GÖR</button></div><div class="finalP3Tracks">'+(a.length?a.slice(0,8).map(x=>'<p><time>'+new Date(x.time||Date.now()).toLocaleTimeString('tr-TR',{hour:'2-digit',minute:'2-digit'})+'</time><b>'+String(x.title||'').replace(/[<>]/g,'')+'</b></p>').join(''):'<p><time>—</time><b>Yayın metadata bilgisi bekleniyor</b></p>')+'</div>';let b=$('#uf50');if(b)b.onclick=()=>window.openRealTracks50&&window.openRealTracks50()}
function radarTruth(){let box=$('#p3RadarList')||$('#p4RadarList');if(!box||!window.stations)return;let a=stations.filter(s=>!bad(s)).slice().sort((a,b)=>score(b)-score(a)||(+b.bitrate||0)-(+a.bitrate||0)).slice(0,8);box.innerHTML=a.map(s=>'<div class="p4Row"><div><strong>'+s.name+'</strong><small>'+[(s.codec||'codec ?'),(s.bitrate?s.bitrate+' kbps':'bitrate ?'),(+s.lastcheckok===1?'dizin kontrolü OK':'kontrol gerekli')].join(' • ')+'</small></div><span class="p4Val">'+score(s)+'/100</span></div>').join('')}
function fasterGraph(){try{if(typeof window.refreshProfile4Telemetry==='function')refreshProfile4Telemetry()}catch(e){}try{if(typeof window.renderProfile3Deep==='function')renderProfile3Deep()}catch(e){}}
function profileEntry(){let p=$('.profileBar');if(!p)return;let b=p.querySelector('button');if(b)b.textContent='PROFİL 1–4';}
function run(){purgeMain();forceSmooth();fixButtons();patchP3Tracks();radarTruth();profileEntry()}
window.openRealTracks50=function(){let a=tracks();if(typeof window.openMode==='function'){try{openMode('history');return}catch(e){}}let txt=a.slice(0,50).map(x=>x.title).join('\n');alert(txt||'Henüz gerçek şarkı metadata kaydı yok.')};
setTimeout(run,900);setInterval(()=>{if(!document.hidden){fixButtons();patchP3Tracks();radarTruth();fasterGraph()}},350);
})();