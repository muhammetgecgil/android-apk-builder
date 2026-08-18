from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v181.js' not in s:
    s=s.replace('</body>','<script src="/cad-v181.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
function B(txt){return Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()===txt)}
function allB(txt){return Array.from(document.querySelectorAll('button')).filter(b=>b.textContent.trim().toUpperCase()===txt)}
function setActive(btn){document.querySelectorAll('#industryModes button').forEach(b=>b.classList.remove('on'));if(btn)btn.classList.add('on')}
function msg(h){const r=document.getElementById('proResult');if(r)r.innerHTML=h;const o=document.getElementById('industryOut');if(o)o.innerHTML=h}

function forceAutoDimensions(){
 const b=document.getElementById('autoDimB');
 if(!b)return;
 if(!b.classList.contains('on')) b.click();
 else if(window.MGAutoDimension&&MGAutoDimension.rebuild) MGAutoDimension.rebuild();
 setTimeout(()=>{
   let i=0;const seen=[];
   document.querySelectorAll('.mgDimLabel').forEach(el=>{
     const t=el.textContent.trim();
     if(t.includes('Ø')){
       i++;
       const pure=t.replace(/^H\d+\s+/,'');
       el.textContent='H'+i+'  '+pure;
       el.dataset.autoHole='1';
       const m=pure.match(/Ø\s*([0-9.,]+)/);if(m)seen.push(m[1]);
     }
   });
   const counts={};seen.forEach(x=>counts[x]=(counts[x]||0)+1);
   const summary=Object.keys(counts).map(k=>counts[k]+' × Ø'+k).join(' • ');
   msg('<b>OTOMATİK ÖLÇÜLENDİRME</b><br>X/Y/Z dış ölçüler + <b>'+i+' delik/çap</b> model üzerinde gösteriliyor.'+(summary?'<br>'+summary:''));
 },120);
}

function clickTool(name){const b=B(name);if(b){b.click();return true}return false}
function stopMeasureModes(){try{controls.enabled=true}catch(e){} }
function prepareDesign(){
 stopMeasureModes();if(typeof viewDir==='function')viewDir('iso');if(typeof fit==='function')fit();
 msg('<b>TASARIM MODU</b><br>Geometri inceleme, kesit, yüzey/normal, açı, dış ölçü ve hacim analizi için hazır. AÇI veya YÜZEY / NORMAL aracını seç.');
}
function prepareManufacturing(){
 stopMeasureModes();forceAutoDimensions();
 msg('<b>İMALAT MODU</b><br>Delik çapları otomatik ölçülendirildi. KALINLIK, DELİKLER, PROB, KESİT ve KÜTLE / CG üretilebilirlik incelemesi için hazır.');
}
function prepareDrawing(){
 stopMeasureModes();try{if(typeof viewDir==='function')viewDir('front');if(typeof wire!=='undefined'&&!wire&&typeof toggleWire==='function')toggleWire();if(grid&&grid.visible&&typeof toggleGrid==='function')toggleGrid()}catch(e){}
 forceAutoDimensions();
 msg('<b>TEKNİK RESİM MODU</b><br>Ön görünüş + kenar görünümü + X/Y/Z ve delik çapları açıldı. Teknik resim ölçü kontrolü için hazır.');
}
function prepareQuality(){
 stopMeasureModes();forceAutoDimensions();
 msg('<b>KALİTE / İNCELEME MODU</b><br>2 NOKTA, PROB, KALINLIK, DELİKLER ve kesit araçları ölçüm/inspeksiyon için hazır. İstediğin aracı seç.');
}
function prepareCam(){
 stopMeasureModes();forceAutoDimensions();
 msg('<b>CAM / CNC MODU</b><br>Delik çapları, kesit, kalınlık, prob/XYZ ve parça izolasyonu bağlama/operasyon incelemesi için hazır.');
}

function rebuildIndustry(){
 const box=document.getElementById('industryModes');if(!box)return;
 box.innerHTML='<div class="head">ENDÜSTRİ MODLARI</div>'+
 '<div class="row"><button id="modeDesign">TASARIM</button><button id="modeMfg">İMALAT</button></div>'+
 '<div class="row"><button id="modeDrawing">TEKNİK RESİM</button><button id="modeQuality">KALİTE</button></div>'+
 '<div class="row"><button id="modeCam">CAM / CNC</button><button id="modeHole">DELİK TABLOSU</button></div>'+
 '<div class="sep"></div><div class="head">HIZLI MÜHENDİSLİK</div>'+
 '<div class="row"><button id="modeAngle">AÇI</button><button id="modeSurface">YÜZEY / NORMAL</button></div>'+
 '<div id="industryOut" class="small">Kullanım amacına göre mod seç. Araçlar aynı CAD geometrisi üzerinde çalışır.</div>';
 const d=box.querySelector('#modeDesign'),m=box.querySelector('#modeMfg'),r=box.querySelector('#modeDrawing'),q=box.querySelector('#modeQuality'),c=box.querySelector('#modeCam');
 d.onclick=()=>{setActive(d);prepareDesign()};m.onclick=()=>{setActive(m);prepareManufacturing()};r.onclick=()=>{setActive(r);prepareDrawing()};q.onclick=()=>{setActive(q);prepareQuality()};c.onclick=()=>{setActive(c);prepareCam()};
 box.querySelector('#modeHole').onclick=()=>{clickTool('DELİKLER');setTimeout(forceAutoDimensions,80)};
 box.querySelector('#modeAngle').onclick=()=>clickTool('AÇI');
 box.querySelector('#modeSurface').onclick=()=>clickTool('YÜZEY / NORMAL');
}
function strengthenAutoDim(){
 const b=document.getElementById('autoDimB');if(!b)return;const old=b.onclick;
 b.onclick=function(e){if(old)old.call(this,e);if(this.classList.contains('on'))setTimeout(forceAutoDimensions,30)};
}
function boot(){rebuildIndustry();strengthenAutoDim();window.MG_CAD_V181={version:'1.8.1',autoHoleOnDimension:true,design:true,manufacturing:true,drawing:true,quality:true,cam:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,260));else setTimeout(boot,260);
})();'''
(AS/'cad-v181.js').write_text(js,encoding='utf-8')
print('v1.8.1 industry workflow modes + strong auto-hole dimensions applied')
