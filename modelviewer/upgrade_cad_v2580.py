from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2580.js' not in h: h=h.replace('</body>','<script src="/cad-v2580.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
function hidden(id){const e=document.getElementById(id);if(e)e.style.display='none'}
function smartOff(){const b=document.getElementById('mgSmartMeasure2550');if(b&&b.classList.contains('on'))b.click()}
function thicknessOff(){const b=document.getElementById('mgThickness2570');if(b&&b.classList.contains('on'))b.click()}
function setInfo(t){const e=document.getElementById('mgMeasureInfo2580');if(e)e.innerHTML=t}
function setOn(id){['mgDistance2580','mgThickness2580'].forEach(x=>document.getElementById(x)?.classList.toggle('on',x===id))}
function install(){
 const tools=document.getElementById('tools');if(!tools||document.getElementById('mgMeasureSimple2580'))return;
 hidden('mgSimpleMeasure2570'); hidden('mgMeasureHub2560');
 const w=document.createElement('div');w.id='mgMeasureSimple2580';
 w.innerHTML='<div class="sep"></div><div class="head">ÖLÇÜM</div><div class="row"><button id="mgDistance2580">MESAFE</button><button id="mgThickness2580">KALINLIK</button><button id="mgClear2580">SİL</button></div><div class="small" id="mgMeasureInfo2580">Bir ölçüm seç.</div>';
 tools.appendChild(w);
 const d=document.getElementById('mgDistance2580'),t=document.getElementById('mgThickness2580'),c=document.getElementById('mgClear2580');
 d.onclick=e=>{e.preventDefault();smartOff();thicknessOff();if(window.clearMeasure)window.clearMeasure();if(!window.measureOn&&window.toggleMeasure)window.toggleMeasure();setOn('mgDistance2580');setInfo('<b>1. noktaya dokun → 2. noktaya dokun</b>');};
 t.onclick=e=>{e.preventDefault();smartOff();if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();const old=document.getElementById('mgThickness2570');if(old&&!old.classList.contains('on'))old.click();setOn('mgThickness2580');setInfo('<b>Yüzeye bir kez dokun.</b> Karşı yüz otomatik ölçülür.');};
 c.onclick=e=>{e.preventDefault();smartOff();if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();thicknessOff();if(window.clearMeasure)window.clearMeasure();document.getElementById('mgClear2570')?.click();setOn('');setInfo('Temizlendi.');};
}
function init(){install();window.MG_CAD_V2580={version:'2.5.8',ultraSimpleMeasurementMenu:true,distanceTwoTap:true,thicknessOneTap:true,visibleChoices:['MESAFE','KALINLIK','SİL'],smartReferenceEnginePreservedHidden:true,baseline:'2.5.2'}}
ready(init)
})();'''
(AS/'cad-v2580.js').write_text(js,encoding='utf-8')
print('v2.5.8 ultra simple measurement menu')
