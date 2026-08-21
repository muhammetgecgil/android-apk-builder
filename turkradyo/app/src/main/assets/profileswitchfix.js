(function(){
'use strict';
var applying=false,last=0;
function clamp(p){p=parseInt(p,10)||1;return Math.max(1,Math.min(4,p))}
function chosen(){return clamp(localStorage.getItem('radioProfile')||2)}
function decorate(p){
  var badge=document.getElementById('profileBadge'),title=document.getElementById('profileTitle'),sub=document.getElementById('profileSub');
  var titles={1:'BASİT / ERİŞİLEBİLİR',2:'GÜNLÜK PROFİL',3:'UZMAN / TEKNİK PROFİL',4:'TAM / GELİŞMİŞ PROFİL'};
  var subs={1:'Büyük kontroller • radyo adı • geri/ileri • Shazam',2:'Alarm • uyku • favoriler • benzer radyolar • kalite',3:'Canlı radar • İstasyon DNA • codec/bitrate • kalite • teknik log',4:'Tam kontrol • gelişmiş telemetri • otomasyon • mühendislik araçları'};
  if(badge)badge.textContent=p;if(title)title.textContent=titles[p];if(sub)sub.textContent=subs[p];
  document.querySelectorAll('.profileDots i').forEach(function(x,i){x.classList.toggle('on',i===p-1)});
  document.querySelectorAll('[data-profile]').forEach(function(b){var n=+b.dataset.profile;b.classList.toggle('current',n===p);var z=b.querySelector('.profileLock');if(z)z.textContent=n===p?'AKTİF':'SEÇ'});
}
function forceProfile(p,save){
  p=clamp(p); applying=true; last=Date.now();
  if(save!==false)localStorage.setItem('radioProfile',String(p));
  var b=document.body;if(!b){applying=false;return}
  b.classList.remove('profile1','profile2','profile3','profile4','profile2Large');b.classList.add('profile'+p);
  decorate(p);
  setTimeout(function(){
    var want='profile'+p;
    if(!document.body.classList.contains(want)){
      document.body.classList.remove('profile1','profile2','profile3','profile4','profile2Large');
      document.body.classList.add(want);
    }
    decorate(p);
    if(p===3&&typeof window.renderProfile3==='function')try{window.renderProfile3()}catch(e){}
    if(p===4&&typeof window.renderProfile4==='function')try{window.renderProfile4()}catch(e){}
    applying=false;
  },80);
}
function patchGlobal(){
  window.applyRadioProfile=function(p){forceProfile(p,true)};
  var sw=document.getElementById('profileSwitch');if(sw&&!sw.dataset.fixed){sw.dataset.fixed='1';sw.addEventListener('click',function(){setTimeout(bindOptions,20)},true)}
  bindOptions();
}
function bindOptions(){document.querySelectorAll('[data-profile]').forEach(function(b){if(b.dataset.fixedProfile)return;b.dataset.fixedProfile='1';b.addEventListener('click',function(ev){ev.preventDefault();ev.stopImmediatePropagation();var p=clamp(this.dataset.profile);forceProfile(p,true);var s=document.getElementById('profileSheet2');if(s)s.classList.remove('show')},true)})}
function guard(){
  if(!document.body)return;
  new MutationObserver(function(m){if(applying)return;var p=chosen(),want='profile'+p;if(!document.body.classList.contains(want)){
      var hasOther=['profile1','profile2','profile3','profile4'].some(function(c){return document.body.classList.contains(c)});
      if(hasOther||Date.now()-last>120)forceProfile(p,false);
    }}).observe(document.body,{attributes:true,attributeFilter:['class']});
}
function boot(){patchGlobal();forceProfile(chosen(),false);guard();setInterval(function(){patchGlobal();var p=chosen();if(!document.body.classList.contains('profile'+p))forceProfile(p,false)},700)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();