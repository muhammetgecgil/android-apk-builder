from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v180.js' not in s:
    s=s.replace('</body>','<script src="/cad-v180.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
let engMode=null,engPicks=[];
function B(txt){return Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()===txt)}
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function msg(h){const r=document.getElementById('proResult');if(r)r.innerHTML=h}
function U(v){try{return fmt(v)+' '+document.getElementById('unit').value}catch(e){return Number(v).toFixed(3)}}
function hit(ev){try{const r=canvas.getBoundingClientRect();mouse.x=((ev.clientX-r.left)/r.width)*2-1;mouse.y=-((ev.clientY-r.top)/r.height)*2+1;ray.setFromCamera(mouse,camera);return ray.intersectObjects(meshes().filter(x=>x.visible),false)[0]||null}catch(e){return null}}
function normal(h){let n=h.face?h.face.normal.clone():new THREE.Vector3(0,0,1);try{return n.applyMatrix3(new THREE.Matrix3().getNormalMatrix(h.object.matrixWorld)).normalize()}catch(e){return n}}
function setMode(m,text){engMode=m;engPicks=[];try{controls.enabled=!m}catch(e){};msg(text)}
function clearMode(){engMode=null;engPicks=[];try{controls.enabled=true}catch(e){}}

function onEngPick(ev){if(!engMode)return;const h=hit(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();const p=h.point.clone(),n=normal(h);
 if(engMode==='surface'){msg('<b>YÜZEY / NORMAL</b><br>X '+U(p.x)+' • Y '+U(p.y)+' • Z '+U(p.z)+'<br>Normal ['+n.x.toFixed(4)+', '+n.y.toFixed(4)+', '+n.z.toFixed(4)+']<br>Parça: '+(h.object.name||'Part'));clearMode();return}
 if(engMode==='angle'){engPicks.push({p,n});if(engPicks.length===1){msg('<b>AÇI</b><br>1. yüzey seçildi. İkinci yüzeyi seç.');return}const a=engPicks[0].n,b=engPicks[1].n;let c=Math.max(-1,Math.min(1,a.dot(b))),deg=Math.acos(Math.abs(c))*180/Math.PI;msg('<b>YÜZEY AÇISI '+deg.toFixed(3)+'°</b><br>Normal-1 ['+a.x.toFixed(3)+','+a.y.toFixed(3)+','+a.z.toFixed(3)+']<br>Normal-2 ['+b.x.toFixed(3)+','+b.y.toFixed(3)+','+b.z.toFixed(3)+']');clearMode();return}
}

function removeDuplicateExplode(){const x=document.getElementById('mgExplode');if(x){const prev=x.previousElementSibling;if(prev&&prev.classList.contains('sep'))prev.remove();x.remove()}}

function tagAutoHoleLabels(){setTimeout(()=>{let i=0;document.querySelectorAll('.mgDimLabel').forEach(el=>{const t=el.textContent.trim();if(t.startsWith('Ø ')){i++;el.textContent='H'+i+'  '+t;el.dataset.autoHole='1'}});const r=document.getElementById('proResult');if(r&&i)r.innerHTML='<b>OTOMATİK ÖLÇÜLENDİRME</b><br>Dış boyutlar ve <b>'+i+' delik/çap</b> model üzerinde otomatik gösteriliyor.<br>H1…H'+i+' delik etiketleri görünümle birlikte hareket eder.'},80)}
function installAutoHoleMeasure(){const b=document.getElementById('autoDimB');if(!b)return;const old=b.onclick;b.onclick=function(e){if(old)old.call(this,e);if(this.classList.contains('on'))tagAutoHoleLabels()};const u=document.getElementById('unit');if(u)u.addEventListener('change',()=>{if(b.classList.contains('on'))tagAutoHoleLabels()})}

function installIndustryPanel(){const tools=document.getElementById('tools');if(!tools||document.getElementById('industryModes'))return;
 const oldHead=Array.from(tools.querySelectorAll('.head')).find(x=>x.textContent.trim().toUpperCase()==='MÜHENDİSLİK MODLARI');if(oldHead)oldHead.textContent='MÜHENDİSLİK ARAÇLARI';
 const sep=document.createElement('div');sep.className='sep';tools.appendChild(sep);
 const box=document.createElement('div');box.id='industryModes';box.innerHTML='<div class="head">ENDÜSTRİ MODLARI</div><div class="row"><button id="designMode">TASARIM</button><button id="mfgMode">İMALAT</button></div><div class="row"><button id="drawingMode">TEKNİK RESİM</button><button id="angleMode">AÇI</button></div><div class="row"><button id="surfaceMode">YÜZEY / NORMAL</button><button id="holeMode">DELİK TABLOSU</button></div><div id="industryOut" class="small">Tasarımcı, imalatçı ve teknik resim inceleme araçları.</div>';
 tools.appendChild(box);
 const out=box.querySelector('#industryOut');
 box.querySelector('#angleMode').onclick=()=>setMode('angle','<b>AÇI ÖLÇÜMÜ</b><br>İki yüzey seç. Yüzey normalleri arasındaki açı hesaplanır.');
 box.querySelector('#surfaceMode').onclick=()=>setMode('surface','<b>YÜZEY / NORMAL</b><br>Bir yüzey seç. Nokta koordinatı ve yüzey normali gösterilir.');
 box.querySelector('#holeMode').onclick=()=>{const h=B('DELİKLER');if(h)h.click();else msg('Delik analizi aracı bulunamadı.')};
 box.querySelector('#designMode').onclick=()=>{if(typeof fit==='function')fit();if(typeof updateInfo==='function')updateInfo();out.innerHTML='<b>TASARIM İNCELEME</b><br>Dış ölçüler, yüzey alanı, hacim, kesit, açı ve yüzey normali araçları aktif kullanım için hazır.'};
 box.querySelector('#mfgMode').onclick=()=>{const h=B('DELİKLER');if(h)h.click();out.innerHTML='<b>İMALAT İNCELEME</b><br>Delikler, kalınlık, prob/XYZ, kesit ve kütle-CG kontrollerini kullan.'};
 box.querySelector('#drawingMode').onclick=()=>{try{if(typeof viewDir==='function')viewDir('front');if(typeof wire!=='undefined'&&!wire&&typeof toggleWire==='function')toggleWire();const ad=document.getElementById('autoDimB');if(ad&&!ad.classList.contains('on'))ad.click();out.innerHTML='<b>TEKNİK RESİM İNCELEME</b><br>Ön görünüş + kenar görünümü + otomatik dış/çap ölçülendirme açıldı.'}catch(e){out.textContent='Teknik resim modu başlatılamadı: '+e.message}};
 canvas.addEventListener('pointerup',onEngPick,true);
}

function boot(){removeDuplicateExplode();installAutoHoleMeasure();installIndustryPanel();window.MG_CAD_V180={version:'1.8.0',autoHoleDimensions:true,noDuplicateExplode:true,designMode:true,manufacturingMode:true,drawingMode:true,angle:true,surfaceNormal:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,220));else setTimeout(boot,220);
})();'''
(AS/'cad-v180.js').write_text(js,encoding='utf-8')
print('v1.8.0 industry CAD modes + auto hole dimensions applied')
