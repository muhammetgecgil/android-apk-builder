from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2560.js' not in h: h=h.replace('</body>','<script src="/cad-v2560.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
function hideLegacy(){
 const m=document.getElementById('mB');
 if(m){const row=m.closest('.row');if(row)row.style.display='none';let head=row&&row.previousElementSibling;if(head&&head.classList.contains('head')&&/ÖLÇÜM/.test(head.textContent))head.style.display='none';let small=row&&row.nextElementSibling;if(small&&small.classList.contains('small'))small.style.display='none'}
 const sb=document.getElementById('mgSmartMeasure2550');
 if(sb){let box=sb.closest('div');while(box&&box.parentElement&&box.parentElement.id!=='tools')box=box.parentElement;if(box)box.style.display='none'}
}
function stopAll(){
 const sb=document.getElementById('mgSmartMeasure2550');if(sb&&sb.classList.contains('on'))sb.click();
 if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();
 if(window.clearMeasure)window.clearMeasure();
}
function install(){
 const tools=document.getElementById('tools');if(!tools||document.getElementById('mgMeasureHub2560'))return;
 hideLegacy();
 const wrap=document.createElement('div');wrap.id='mgMeasureHub2560';wrap.innerHTML='<div class="sep"></div><div class="head">ÖLÇÜM</div><button id="mgMeasureMain2560" style="width:100%">ÖLÇÜM</button><div id="mgMeasureMenu2560" style="display:none;margin-top:6px"><div class="row"><button id="mgMeasureSmart2560">AKILLI</button><button id="mgMeasureSimple2560">2 NOKTA</button></div><div class="row"><button id="mgMeasureRef2560">REF DEĞİŞTİR</button><button id="mgMeasureClear2560">TEMİZLE</button></div><div class="small" id="mgMeasureHint2560">Varsayılan: AKILLI • Referans → 1. nokta → 2. nokta</div></div>';
 tools.appendChild(wrap);
 const main=document.getElementById('mgMeasureMain2560'),menu=document.getElementById('mgMeasureMenu2560'),smart=document.getElementById('mgMeasureSmart2560'),simple=document.getElementById('mgMeasureSimple2560'),ref=document.getElementById('mgMeasureRef2560'),clear=document.getElementById('mgMeasureClear2560'),hint=document.getElementById('mgMeasureHint2560');
 function setActive(b){[smart,simple].forEach(x=>x.classList.remove('on'));if(b)b.classList.add('on')}
 main.onclick=e=>{e.preventDefault();menu.style.display=menu.style.display==='none'?'block':'none';main.classList.toggle('on',menu.style.display!=='none')};
 smart.onclick=e=>{e.preventDefault();if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();const b=document.getElementById('mgSmartMeasure2550');if(b&&!b.classList.contains('on'))b.click();setActive(smart);hint.textContent='AKILLI: Referans seç → 1. nokta → 2. nokta. Sonuçta 3D, boyunca, dik ve Local X/Y/Z verilir.'};
 simple.onclick=e=>{e.preventDefault();const sb=document.getElementById('mgSmartMeasure2550');if(sb&&sb.classList.contains('on'))sb.click();if(!window.measureOn&&window.toggleMeasure)window.toggleMeasure();setActive(simple);hint.textContent='2 NOKTA: Modelde iki noktaya dokun. 3D mesafe ve ΔX/ΔY/ΔZ gösterilir.'};
 ref.onclick=e=>{e.preventDefault();const b=document.getElementById('mgSmartMeasure2550'),r=document.getElementById('mgSmartMeasureRef2550');if(b&&!b.classList.contains('on'))b.click();if(r)r.click();setActive(smart);hint.textContent='Yeni referans için kenar veya yüzey seç.'};
 clear.onclick=e=>{e.preventDefault();stopAll();setActive(null);hint.textContent='Temizlendi. AKILLI veya 2 NOKTA seç.'};
 smart.click();
}
function init(){install();window.MG_CAD_V2560={version:'2.5.6',unifiedMeasurementCenter:true,singleMeasurementEntry:true,legacyMeasurementButtonsHidden:true,smartDefault:true,preservesExistingMeasurementEngines:true,baseline:'2.5.2'}}
ready(init)
})();'''
(AS/'cad-v2560.js').write_text(js,encoding='utf-8')
print('v2.5.6 unified measurement center')
