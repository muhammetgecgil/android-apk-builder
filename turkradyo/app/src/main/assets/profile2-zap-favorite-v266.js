(()=>{'use strict';
const SEL='.modes > .mode[data-mode="favorites"]';
const ICON='<svg viewBox="0 0 24 24" aria-hidden="true" style="width:34px;height:34px;display:block;margin:auto;fill:none;stroke:currentColor;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round"><path d="M13.4 2.5 6.7 12.8h4.8l-.9 8.7 7.2-11.1h-4.7z"/></svg>';
function isP2(){try{return localStorage.getItem('p2Active')==='1'}catch(e){return document.body?.classList.contains('profile2-active')}}
function target(){return document.querySelector(SEL)}
function patch(){const b=target();if(!b)return;if(isP2()){if(b.dataset.p266Zap==='1')return;b.dataset.p266Zap='1';b.innerHTML='<b>'+ICON+'</b>AKILLI ZAPPING';b.setAttribute('aria-label','Akıllı Zapping');b.title='Akıllı Zapping'}else if(b.dataset.p266Zap==='1'){delete b.dataset.p266Zap;b.innerHTML='<b>♥</b>FAVORİLER';b.removeAttribute('aria-label');b.removeAttribute('title')}}
function fireZap(){const z=document.getElementById('p2Zap');if(z){z.click();return true}return false}
function boot(){[0,160,520,1100].forEach(ms=>setTimeout(patch,ms));document.addEventListener('click',e=>{const b=e.target.closest(SEL);if(!b||b.dataset.p266Zap!=='1'||!isP2())return;e.preventDefault();e.stopImmediatePropagation();if(!fireZap())setTimeout(fireZap,180)},true);document.addEventListener('click',e=>{if(e.target.closest('[data-prof]')){setTimeout(patch,50);setTimeout(patch,300);setTimeout(patch,900)}},true);window.addEventListener('pageshow',()=>setTimeout(patch,0));document.addEventListener('visibilitychange',()=>{if(!document.hidden)setTimeout(patch,0)})}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();