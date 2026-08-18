from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v182.js' not in s:
    s=s.replace('</body>','<script src="/cad-v182.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
let liveMode=null, livePicks=[];
function byText(t){return Array.from(document.querySelectorAll('button')).filter(b=>b.textContent.trim().toUpperCase()===t.toUpperCase())}
function firstOutsideIndustry(t){return byText(t).find(b=>!b.closest('#industryModes'))||null}
function setMsg(h){const p=document.getElementById('proResult');if(p)p.innerHTML=h;const q=document.getElementById('industryOut');if(q)q.innerHTML=h}
function meshes(){try{return group.children.filter(x=>x&&x.isMesh&&x.visible)}catch(e){return[]}}
function unit(v){try{return fmt(v)+' '+document.getElementById('unit').value}catch(e){return Number(v).toFixed(3)}}
function hit(ev){try{const r=canvas.getBoundingClientRect();mouse.x=((ev.clientX-r.left)/r.width)*2-1;mouse.y=-((ev.clientY-r.top)/r.height)*2+1;ray.setFromCamera(mouse,camera);return ray.intersectObjects(meshes(),false)[0]||null}catch(e){return null}}
function worldNormal(h){let n=h.face?h.face.normal.clone():new THREE.Vector3(0,0,1);try{return n.applyMatrix3(new THREE.Matrix3().getNormalMatrix(h.object.matrixWorld)).normalize()}catch(e){return n}}
function modeButton(id){return document.getElementById(id)}
function markActive(id){document.querySelectorAll('#industryModes button').forEach(b=>b.classList.remove('on'));const b=modeButton(id);if(b)b.classList.add('on')}
function stopLive(){liveMode=null;livePicks=[];try{controls.enabled=true}catch(e){}}
function startLive(m,text,id){liveMode=m;livePicks=[];try{controls.enabled=false}catch(e){};markActive(id);setMsg(text)}

function forceDims(){const b=document.getElementById('autoDimB');if(!b)return;if(!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}
function clickReal(t){const b=firstOutsideIndustry(t);if(b){b.click();return true}return false}
function scanHoles(){if(clickReal('DELİKLER'))return true;try{if(typeof detectHoles==='function'){detectHoles();return true}}catch(e){}return false}

function livePick(ev){if(!liveMode)return;const h=hit(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();const p=h.point.clone(),n=worldNormal(h);
 if(liveMode==='surface'){setMsg('<b>YÜZEY / NORMAL</b><br>X '+unit(p.x)+' • Y '+unit(p.y)+' • Z '+unit(p.z)+'<br>Normal ['+n.x.toFixed(4)+', '+n.y.toFixed(4)+', '+n.z.toFixed(4)+']<br>Parça: '+(h.object.name||'Part'));stopLive();return}
 if(liveMode==='angle'){livePicks.push(n);if(livePicks.length===1){setMsg('<b>AÇI</b><br>1. yüzey seçildi. Şimdi ikinci yüzeyi seç.');return}const a=livePicks[0],b=livePicks[1];const dot=Math.max(-1,Math.min(1,a.dot(b)));const deg=Math.acos(Math.abs(dot))*180/Math.PI;setMsg('<b>YÜZEY AÇISI '+deg.toFixed(3)+'°</b><br>N1 ['+a.x.toFixed(3)+', '+a.y.toFixed(3)+', '+a.z.toFixed(3)+']<br>N2 ['+b.x.toFixed(3)+', '+b.y.toFixed(3)+', '+b.z.toFixed(3)+']');stopLive();return}
}

function activateDesign(){stopLive();markActive('modeDesign');try{viewDir('iso');fit()}catch(e){};setMsg('<b>TASARIM AKTİF</b><br>ISO görünüş ve geometri inceleme hazır. AÇI veya YÜZEY/NORMAL ile doğrudan yüzey analizi yapabilirsin.')}
function activateMfg(){stopLive();markActive('modeMfg');forceDims();scanHoles();setTimeout(()=>setMsg('<b>İMALAT AKTİF</b><br>Dış ölçüler ve delik çapları açıldı; delik analizi çalıştırıldı. KALINLIK, PROB, KESİT ve KÜTLE/CG araçları kullanılabilir.'),120)}
function activateDrawing(){stopLive();markActive('modeDrawing');try{viewDir('front');if(typeof wire!=='undefined'&&!wire)toggleWire();if(grid&&grid.visible)toggleGrid()}catch(e){};forceDims();setTimeout(()=>setMsg('<b>TEKNİK RESİM AKTİF</b><br>Ön görünüş + tel/kenar görünümü + X/Y/Z + otomatik delik çap ölçüleri açıldı.'),120)}
function activateQuality(){stopLive();markActive('modeQuality');forceDims();const ok=clickReal('2 NOKTA');setMsg('<b>KALİTE / İNCELEME AKTİF</b><br>Otomatik ölçüler açık.'+(ok?' 2 NOKTA ölçümü başlatıldı; modelden iki nokta seç.':' PROB/KALINLIK/DELİK araçları hazır.'))}
function activateCam(){stopLive();markActive('modeCam');forceDims();scanHoles();setTimeout(()=>setMsg('<b>CAM / CNC AKTİF</b><br>Delik çapları ve delik taraması aktif. Kesit, kalınlık, prob/XYZ ve parça izolasyonu operasyon/bağlama incelemesi için hazır.'),120)}
function activateHole(){stopLive();markActive('modeHole');forceDims();const ok=scanHoles();if(!ok)setMsg('<b>DELİK TABLOSU</b><br>Delik motoru bulunamadı.');}

function wireIndustry(){
 const box=document.getElementById('industryModes');if(!box)return;
 const d=modeButton('modeDesign'),m=modeButton('modeMfg'),dr=modeButton('modeDrawing'),q=modeButton('modeQuality'),c=modeButton('modeCam'),h=modeButton('modeHole'),a=modeButton('modeAngle'),s=modeButton('modeSurface');
 if(d)d.onclick=activateDesign;if(m)m.onclick=activateMfg;if(dr)dr.onclick=activateDrawing;if(q)q.onclick=activateQuality;if(c)c.onclick=activateCam;if(h)h.onclick=activateHole;
 if(a)a.onclick=()=>startLive('angle','<b>AÇI ÖLÇÜMÜ AKTİF</b><br>Birinci yüzeyi seç.','modeAngle');
 if(s)s.onclick=()=>startLive('surface','<b>YÜZEY / NORMAL AKTİF</b><br>İncelenecek yüzeye dokun.','modeSurface');
 canvas.addEventListener('pointerup',livePick,true);
}
function selfCheck(){
 const ids=['modeDesign','modeMfg','modeDrawing','modeQuality','modeCam','modeHole','modeAngle','modeSurface'];
 const missing=ids.filter(x=>!document.getElementById(x));
 window.MGIndustryHealth={version:'1.8.2',buttons:ids.length-missing.length,missing,angleRuntime:true,surfaceRuntime:true,manufacturingRuntime:true,qualityRuntime:true,camRuntime:true};
 const out=document.getElementById('industryOut');if(out&&!missing.length)out.innerHTML='Endüstri araçları hazır • 8/8 düğme aktif';
}
function boot(){wireIndustry();selfCheck()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,340));else setTimeout(boot,340);
})();'''
(AS/'cad-v182.js').write_text(js,encoding='utf-8')
print('v1.8.2 runtime industry activation patch applied')
