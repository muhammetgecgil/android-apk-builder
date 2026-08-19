from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v194.js' not in s:
    s=s.replace('</body>','<script src="/cad-v194.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
const E=id=>document.getElementById(id);
let tdSprites=[];
function msg(h){const a=E('industryOut'),b=E('proResult');if(a)a.innerHTML=h;if(b)b.innerHTML=h}
function U(v){try{return fmt(v)+' '+E('unit').value}catch(e){return Number(v).toFixed(3)+' mm'}}
function clearTD(){tdSprites.forEach(s=>{try{scene.remove(s);if(s.material&&s.material.map)s.material.map.dispose();if(s.material)s.material.dispose()}catch(e){}});tdSprites=[]}
function badge(text,p,small){const c=document.createElement('canvas');c.width=520;c.height=96;const x=c.getContext('2d');x.fillStyle='rgba(3,10,20,.95)';x.strokeStyle='#ffd84d';x.lineWidth=4;x.beginPath();x.roundRect(4,4,512,88,14);x.fill();x.stroke();x.fillStyle='#ffe477';x.font='bold '+(small?28:34)+'px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(text,260,48);const t=new THREE.CanvasTexture(c),m=new THREE.SpriteMaterial({map:t,depthTest:false,transparent:true}),s=new THREE.Sprite(m),sc=Math.max(baseDims.x,baseDims.y,baseDims.z,1)*(small?.12:.16);s.scale.set(sc,sc*.185,1);s.position.copy(p);s.renderOrder=65;scene.add(s);tdSprites.push(s)}
function features(){try{if(typeof detectCircularFeatures==='function')return detectCircularFeatures()||[]}catch(e){}return[]}
function addRadiusCallouts(){const f=features();if(!f.length)return 0;const scale=Math.max(baseDims.x,baseDims.y,baseDims.z,1),seen=[];let n=0;f.forEach(q=>{const r=q.r;if(!isFinite(r)||r<scale*.0015)return;const k=Math.round(r*1000)+'-'+q.axis;if(seen.includes(k))return;seen.push(k);badge('R '+U(r),q.center.clone(),true);n++});return n}
function addHoleAndShaft(){try{if(typeof autoDiameters==='function')autoDiameters()}catch(e){}}
function addOverall(){const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}
function drawingView(v){try{if(typeof viewDir==='function')viewDir(v);if(typeof fit==='function')fit();if(typeof wire!=='undefined'&&!wire&&typeof toggleWire==='function')toggleWire();}catch(e){}setTimeout(()=>{addOverall();addHoleAndShaft();clearTD();const nr=addRadiusCallouts();msg('<b>TEKNİK RESİM +</b><br>Görünüş: '+String(v).toUpperCase()+' • dış ölçüler • delik/mil çapları • '+nr+' radyüs çağrısı gösteriliyor.');},120)}
function installDrawingPanel(){const old=[...document.querySelectorAll('button')].find(x=>/^TEKNİK\s*RESİM$/i.test((x.textContent||'').trim()));if(!old||old.dataset.mg194)return false;old.dataset.mg194='1';old.textContent='TEKNİK RESİM +';old.onclick=()=>{let p=E('mgDrawing194');if(p){p.remove();clearTD();return}p=document.createElement('div');p.id='mgDrawing194';p.className='panel';p.style.cssText='position:fixed;left:10px;top:68px;z-index:38;max-width:360px;padding:10px;background:rgba(3,10,20,.96);border:1px solid #28506e';p.innerHTML='<div class="head"><b>TEKNİK RESİM ÖLÇÜLENDİRME</b></div><div class="row"><button id="tdFront">ÖN</button><button id="tdTop">ÜST</button><button id="tdRight">SAĞ</button><button id="tdIso">İZO</button></div><div class="row"><button id="tdAll">TÜM ÖLÇÜLER</button><button id="tdRadius">RADYÜS R</button></div><div class="small">Otomatik: X/Y/Z dış ölçüler, delik ve mil çapları, dairesel/radyüs özellikleri. STEP/IGES/BREP geometride daha güvenilir; mesh modellerde yaklaşık olabilir.</div><div class="row"><button id="tdClose">KAPAT</button></div>';document.body.appendChild(p);E('tdFront').onclick=()=>drawingView('front');E('tdTop').onclick=()=>drawingView('top');E('tdRight').onclick=()=>drawingView('right');E('tdIso').onclick=()=>drawingView('iso');E('tdAll').onclick=()=>{addOverall();addHoleAndShaft();clearTD();addRadiusCallouts()};E('tdRadius').onclick=()=>{clearTD();const n=addRadiusCallouts();msg('<b>RADYÜS</b><br>'+n+' adet R ölçüsü gösterildi.')};E('tdClose').onclick=()=>{clearTD();p.remove()};drawingView('front')};return true}
function boot(){installDrawingPanel();let n=0;const t=setInterval(()=>{n++;if(installDrawingPanel()||n>20)clearInterval(t)},300);window.MG_CAD_V194={version:'1.9.4',completeDrawingDimensions:true,radiusCallouts:true,orthographicViews:true,holeShaftDimensions:true};}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,1300));else setTimeout(boot,1300);
})();'''
(AS/'cad-v194.js').write_text(js,encoding='utf-8')
print('v1.9.4 complete drawing dimensions + radius callouts')
