(()=>{'use strict';
const ICON='<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M13 2 6 13h5l-1 9 8-12h-5z"/></svg>';
let patched=null;
function isP2(){try{return localStorage.getItem('p2Active')==='1'}catch(e){return document.body?.classList.contains('profile2-active')}}
function target(){return patched?.isConnected?patched:document.querySelector('.modes .mode[data-p2-zap-shortcut="1"]')||document.querySelector('.modes .mode[data-mode="favorites"]')}
function enable(){const el=target();if(!el)return false;if(el.dataset.p2ZapShortcut==='1'){patched=el;return true}el.dataset.p2OriginalHtml=el.innerHTML;el.dataset.p2OriginalMode=el.getAttribute('data-mode')||'favorites';el.removeAttribute('data-mode');el.dataset.p2ZapShortcut='1';el.setAttribute('aria-label','Akıllı Zapping');el.innerHTML='<b class="p266ZapIcon">'+ICON+'</b><span class="p266ZapText">AKILLI ZAPPING</span>';patched=el;return true}
function disable(){const el=document.querySelector('.modes .mode[data-p2-zap-shortcut="1"]');if(!el)return;const html=el.dataset.p2OriginalHtml||'<b>♥</b>FAVORİLER';const mode=el.dataset.p2OriginalMode||'favorites';el.innerHTML=html;el.setAttribute('data-mode',mode);el.removeAttribute('data-p2-zap-shortcut');el.removeAttribute('data-p2-original-html');el.removeAttribute('data-p2-original-mode');el.removeAttribute('aria-label');patched=null}
function sync(){if(isP2())enable();else disable()}
function fireZap(){const z=document.getElementById('p2Zap');if(z){z.click();return}setTimeout(()=>{const q=document.getElementById('p2Zap');if(q)q.click()},120)}
function boot(){[0,160,520,1200].forEach(ms=>setTimeout(sync,ms));document.addEventListener('click',e=>{const btn=e.target.closest('.mode[data-p2-zap-shortcut="1"]');if(btn&&isP2()){e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();fireZap();return}if(e.target.closest('[data-prof]')){setTimeout(sync,40);setTimeout(sync,260);setTimeout(sync,800)}},true);window.addEventListener('pageshow',()=>setTimeout(sync,0));document.addEventListener('visibilitychange',()=>{if(!document.hidden)setTimeout(sync,0)})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
