(()=>{'use strict';
function esc(s){return String(s||'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function stationName(){const n=document.getElementById('now');return n?(n.textContent||'TÜRK RADYO').trim():'TÜRK RADYO'}
function syncLogo(){const logo=document.querySelector('.logo');if(!logo)return;const name=stationName();logo.innerHTML=`<span class="station-emblem-name">${esc(name)}</span><span class="station-emblem-sub">CANLI RADYO</span>`;syncHeart()}
function isFav(){try{return index>=0&&stations&&stations[index]&&favs.includes(key(stations[index]))}catch(e){return false}}
function syncHeart(){const b=document.getElementById('heroFavorite');if(!b)return;const on=isFav();b.classList.toggle('on',on);b.textContent=on?'♥':'♡';b.setAttribute('aria-label',on?'Favorilerden çıkar':'Favorilere ekle');b.title=on?'Favorilerden çıkar':'Favorilere ekle'}
function addHeart(){if(document.getElementById('heroFavorite'))return;const hero=document.querySelector('.hero');if(!hero)return;const b=document.createElement('button');b.id='heroFavorite';b.className='hero-favorite';b.type='button';b.textContent='♡';b.setAttribute('aria-label','Favorilere ekle');b.onclick=()=>{try{toggleFav(index);syncHeart()}catch(e){}};hero.appendChild(b);syncHeart()}
function mount(){addHeart();syncLogo();const n=document.getElementById('now');if(n)new MutationObserver(syncLogo).observe(n,{childList:true,characterData:true,subtree:true});document.addEventListener('click',e=>{if(e.target.closest('.fav'))setTimeout(syncHeart,0)},true);setInterval(syncHeart,1200)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount);else mount();
})();