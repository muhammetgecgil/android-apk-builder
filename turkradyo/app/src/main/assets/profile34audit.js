(function(){
if(window.__P34_AUDIT_LOADED__)return;window.__P34_AUDIT_LOADED__=true;
const $=s=>document.querySelector(s),$$=s=>Array.from(document.querySelectorAll(s));
function cur(){try{return stations[index]}catch(e){return null}}
function j(s,d){try{return JSON.parse(s)}catch(e){return d}}
function bridge(n,d){try{return window.RadioNative&&typeof RadioNative[n]==='function'?RadioNative[n]():d}catch(e){return d}}
function tele(){return j(bridge('getTelemetry','{}'),{})}
function esc(s){return String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function score(s){if(!s)return 0;let n=35;if(+s.lastcheckok===1)n+=30;if(+s.bitrate>=192)n+=20;else if(+s.bitrate>=128)n+=15;else if(+s.bitrate>=64)n+=8;if(/aac/i.test(s.codec||''))n+=5;return Math.min(100,n)}
function sheet(title,html){let s=$('#auditSheet');if(!s){s=document.createElement('div');s.id='auditSheet';s.className='auditSheet';s.innerHTML='<div class="auditPanel"><div class="auditHead"><h3></h3><button>KAPAT</button></div><div class="auditBody"></div></div>';document.body.appendChild(s);s.querySelector('button').onclick=()=>s.classList.remove('show')}s.querySelector('h3').textContent=title;s.querySelector('.auditBody').innerHTML=html;s.classList.add('show')}
function metric(k,v){return '<div class="auditMetric"><small>'+esc(k)+'</small><b>'+esc(v)+'</b></div>'}
function showTechnical(){let s=cur(),t=tele();sheet('TEKNİK DETAYLAR','<div class="auditGrid">'+metric('İstasyon',s?.name||'—')+metric('Sağlık',score(s)+'/100')+metric('Codec',s?.codec||'—')+metric('Bitrate',s?.bitrate?s.bitrate+' kbps':'—')+metric('Açılış',t.startupMs?t.startupMs+' ms':'—')+metric('Buffer',String(t.bufferCount||0))+metric('Son hata',String(t.lastError||0))+'</div>')}
function wire(){let p=$('#p3Exact');if(!p||p.dataset.auditBound==='1')return;p.dataset.auditBound='1';let tech=$('#p3Details');if(tech)tech.onclick=showTechnical;$$('#p3Exact .toolGrid button').forEach(b=>{let t=b.textContent.toLocaleUpperCase('tr-TR');if(t.includes('SELF-HEAL'))b.onclick=()=>{if(typeof play==='function')play()};else if(t.includes('KOPMA')||t.includes('TEKNİK LOG'))b.onclick=showTechnical})}
const H={s:[],b:[],k:[]};function push(a,v){a.push(v);if(a.length>20)a.shift()}function pts(a,w=400,h=120){let mx=Math.max(...a,1);return a.map((v,i)=>((i/Math.max(1,a.length-1))*w).toFixed(1)+','+(h-10-(v/mx)*(h-20)).toFixed(1)).join(' ')}
let timer=0;
function update(){timer=0;if(document.hidden||!document.body.classList.contains('profile3'))return;let s=cur(),t=tele();if(!s)return;wire();let a=$('#p3Latency'),b=$('#p3Latency2');if(a)a.textContent=t.startupMs?t.startupMs+' ms':'ölçülüyor';if(b)b.textContent=String(t.bufferCount||0);push(H.s,+t.startupMs||0);push(H.b,+t.bufferCount||0);push(H.k,+s.bitrate||0);let svg=$('#p3Exact .graph svg');if(svg){let l=svg.querySelectorAll('polyline');if(l[0])l[0].setAttribute('points',pts(H.s));if(l[1])l[1].setAttribute('points',pts(H.b));if(l[2])l[2].setAttribute('points',pts(H.k));}schedule();}
function schedule(){if(timer||document.hidden||!document.body.classList.contains('profile3'))return;timer=setTimeout(update,1200)}
function changed(){if(timer){clearTimeout(timer);timer=0}if(document.body.classList.contains('profile3'))update()}
new MutationObserver(changed).observe(document.body,{attributes:true,attributeFilter:['class']});document.addEventListener('visibilitychange',changed);window.addEventListener('load',changed);setTimeout(changed,500);
})();