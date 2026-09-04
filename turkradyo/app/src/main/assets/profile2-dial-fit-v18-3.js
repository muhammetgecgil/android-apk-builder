(()=>{'use strict';
const $=s=>document.querySelector(s);
const SHORT_LAST=new Set(['FM','RADYO','TÜRK','TR','TV','AM']);
let ctx=null,pending=false;
function context(){if(ctx)return ctx;const c=document.createElement('canvas');ctx=c.getContext('2d');return ctx}
function normName(s){return String(s||'').replace(/\s+/g,' ').trim().toLocaleUpperCase('tr-TR')}
function width(text,size){const c=context();if(!c)return text.length*size*.58;c.font=`900 ${size}px "Roboto Condensed","Arial Narrow",Roboto,system-ui,sans-serif`;return c.measureText(text).width}
function candidateLines(name){const words=name.split(' ').filter(Boolean);if(words.length<=1)return [[name]];const out=[];
for(let i=1;i<words.length;i++){
  let a=words.slice(0,i).join(' '),b=words.slice(i).join(' ');
  if(SHORT_LAST.has(a)||SHORT_LAST.has(b))continue;
  out.push([a,b]);
}
if(!out.length){const mid=Math.ceil(words.length/2);out.push([words.slice(0,mid).join(' '),words.slice(mid).join(' ')])}
return out}
function chooseLines(name,maxW){const oneW=width(name,44);if(oneW<=maxW&&name.length<=15)return [name];const cands=candidateLines(name);let best=cands[0],score=1e9;for(const lines of cands){const w1=width(lines[0],40),w2=width(lines[1],40),mx=Math.max(w1,w2),bal=Math.abs(w1-w2),orphan=(SHORT_LAST.has(lines[0])||SHORT_LAST.has(lines[1]))?200:0;const s=mx+bal*.18+orphan;if(s<score){score=s;best=lines}}return best}
function bestSize(lines,maxW,maxH){let lo=15,hi=lines.length===1?48:43,best=15;while(lo<=hi){const m=(lo+hi)>>1;const maxLine=Math.max(...lines.map(x=>width(x,m)));const h=lines.length*m*.92+(lines.length-1)*2;if(maxLine<=maxW&&h<=maxH){best=m;lo=m+1}else hi=m-1}return best}
function fitDial(){pending=false;const logo=$('.logo'),now=$('#now');if(!logo||!now)return;const name=normName(now.textContent||'TÜRK RADYO');if(!name)return;const maxW=Math.max(120,logo.clientWidth*.80),maxH=Math.max(70,logo.clientHeight*.56);const lines=chooseLines(name,maxW);const size=bestSize(lines,maxW,maxH);const longest=Math.max(...lines.map(x=>x.length));const tracking=size>=40&&longest<=9?'.015em':size<=22?'-.035em':'-.015em';logo.dataset.fitName=name;logo.innerHTML=lines.map((x,i)=>`<span class="dialFitLine dialFitLine${i+1}">${x.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}</span>`).join('');logo.style.setProperty('--dial-font',size+'px');logo.style.setProperty('--dial-track',tracking);logo.style.setProperty('--dial-lines',String(lines.length))}
function schedule(){if(pending)return;pending=true;requestAnimationFrame(()=>requestAnimationFrame(fitDial))}
function mount(){schedule();const now=$('#now');if(now)new MutationObserver(schedule).observe(now,{childList:true,characterData:true,subtree:true});window.addEventListener('resize',schedule,{passive:true});document.addEventListener('click',e=>{if(e.target.closest('#prev,#next,#prev2,#next2,[data-p2play],.p2Station button'))setTimeout(schedule,80)},true);setTimeout(schedule,350);setTimeout(schedule,1200)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',mount,{once:true});else mount();
})();
