(function(){
const $$=s=>[...document.querySelectorAll(s)];
function txt(el){return (el?.textContent||'').toLocaleUpperCase('tr-TR')}
function S(){try{return stations||[]}catch(e){return[]}}
function esc(s){return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function isSlow(s){let t=[s?.name,s?.tags,s?.language,s?.state].join(' ').toLocaleLowerCase('tr-TR');return /slow|soft|love|romantik|romantic|akustik|acoustic|chill|ballad|balad|easy listening|relax|lounge|nostalji|nostalgia|classical|klasik/.test(t)}
function removeRadar(){
  $$('button,.mode,.tool,.p4Tool,.featureChip,.card,[data-mode]').forEach(el=>{let t=txt(el);if(t.includes('RADYO RADAR')||t.includes('SAĞLIK RADARI')||t==='RADAR'||t.includes('RADAR EKRANINI AÇ')){el.remove();}});
}
function openSlow(){
  let arr=S().slice(0,80).filter(isSlow);
  if(!arr.length) arr=S().slice(0,80).filter(s=>/pop|adult contemporary|easy/.test(String(s.tags||'').toLowerCase())).slice(0,20);
  let sh=document.getElementById('slowModeSheet');
  if(!sh){sh=document.createElement('div');sh.id='slowModeSheet';sh.style.cssText='position:fixed;inset:0;background:#000d;z-index:9999;padding:28px 12px;display:none';sh.innerHTML='<div style="max-width:720px;height:100%;margin:auto;background:#0d0b10;border:1px solid #4a2b3b;border-radius:24px;padding:14px;overflow:auto"><div style="display:flex;justify-content:space-between;align-items:center;position:sticky;top:0;background:#0d0b10;padding-bottom:10px"><div><b style="font-size:20px">SLOW MOD</b><div style="font-size:10px;color:#aaa">Slow · Soft · Love · Akustik · Chill</div></div><button id="slowClose" style="padding:9px 12px;border-radius:12px;border:1px solid #4a2b3b;background:#171119;color:white">Kapat</button></div><div id="slowRows"></div></div>';document.body.appendChild(sh);document.getElementById('slowClose').onclick=()=>sh.style.display='none';}
  document.getElementById('slowRows').innerHTML=arr.map((s,i)=>`<div style="display:grid;grid-template-columns:42px 1fr 44px;gap:10px;align-items:center;padding:10px;margin:8px 0;border:1px solid #33242c;border-radius:16px;background:#141017"><div>${String(i+1).padStart(2,'0')}</div><div><b>${esc(s.name||'Radyo')}</b><small style="display:block;color:#999">${esc((s.tags||'Slow').split(',').slice(0,3).join(' • '))}</small></div><button data-slow-i="${S().indexOf(s)}" style="height:40px;border-radius:12px;border:1px solid #702335;background:#110d13;color:white">▶</button></div>`).join('')||'<div style="padding:24px;color:#aaa">Slow karakterli istasyon bulunamadı.</div>';
  $$('[data-slow-i]').forEach(b=>b.onclick=()=>{if(typeof select==='function')select(+b.dataset.slowI,true);sh.style.display='none'});
  sh.style.display='block';
}
function replaceZapping(){
  $$('button,.mode,.tool,.p4Tool,.featureChip').forEach(el=>{let t=txt(el);if(t.includes('ZAPPING')||t.includes('ZAP')){let b=el.closest('button')||el;if(b.tagName==='BUTTON'){b.innerHTML=b.innerHTML.replace(/AKILLI\s*ZAPPING|ZAPPING|ZAP/gi,'SLOW MOD');b.onclick=e=>{e.preventDefault();e.stopImmediatePropagation();openSlow()};}else{el.textContent=el.textContent.replace(/AKILLI\s*ZAPPING|ZAPPING|ZAP/gi,'SLOW MOD')}}});
}
function boot(){removeRadar();replaceZapping();setInterval(()=>{removeRadar();replaceZapping()},1200)}
window.addEventListener('load',boot,{once:true});boot();
})();
