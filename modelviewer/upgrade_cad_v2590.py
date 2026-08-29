from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2590.js' not in h: h=h.replace('</body>','<script src="/cad-v2590.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
let guided=false, stage='OFF', fx=[];
function hide(id){const e=document.getElementById(id);if(e)e.style.display='none'}
function smartBtn(){return document.getElementById('mgSmartMeasure2550')}
function smartInfo(){return document.getElementById('mgSmartMeasureInfo2550')}
function thicknessBtn(){return document.getElementById('mgThickness2570')}
function smartOff(){const b=smartBtn();if(b&&b.classList.contains('on'))b.click()}
function thickOff(){const b=thicknessBtn();if(b&&b.classList.contains('on'))b.click()}
function unit(v){const f=typeof unitFactor==='function'?unitFactor():1,u=document.getElementById('unit')?.value||'mm';return (v*f).toFixed(Math.abs(v*f)>=100?2:3)+' '+u}
function prompt(t,ok){const e=document.getElementById('mgGuide2590');if(!e)return;e.innerHTML=t;e.style.borderColor=ok?'#5bd68a':'#5fa8ff';e.style.boxShadow=ok?'0 0 0 2px rgba(91,214,138,.18)':'0 0 0 2px rgba(95,168,255,.12)'}
function flash(t){prompt('✓ '+t,true);if(navigator.vibrate)navigator.vibrate([18,22,18])}
function clearFx(){fx.forEach(o=>{scene.remove(o);o.geometry&&o.geometry.dispose();o.material&&o.material.dispose()});fx=[]}
function hitAt(ev){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((ev.clientX-r.left)/r.width)*2-1,-((ev.clientY-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function feedback(hit,col,ring){if(!hit)return;const span=Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1,1),s=span*.02;let o;if(ring&&hit.face){const g=new THREE.RingGeometry(s*.55,s,32),m=new THREE.MeshBasicMaterial({color:col,side:THREE.DoubleSide,depthTest:false,transparent:true,opacity:.95});o=new THREE.Mesh(g,m);const n=hit.face.normal.clone().transformDirection(hit.object.matrixWorld).normalize();o.quaternion.setFromUnitVectors(new THREE.Vector3(0,0,1),n);o.position.copy(hit.point).addScaledVector(n,span*1e-4)}else{o=new THREE.Mesh(new THREE.SphereGeometry(s*.55,16,12),new THREE.MeshBasicMaterial({color:col,depthTest:false}));o.position.copy(hit.point)}o.renderOrder=1001;scene.add(o);fx.push(o);setTimeout(()=>{if(o.parent){scene.remove(o);o.geometry&&o.geometry.dispose();o.material&&o.material.dispose()}},1500)}
function startGuide(){thickOff();if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();clearFx();const b=smartBtn();if(b&&!b.classList.contains('on'))b.click();guided=true;stage='REF';document.getElementById('mgGuided2590')?.classList.add('on');prompt('<b>1/3 • ŞİMDİ YÖNÜ / EKSENİ SEÇ</b><br><small>Ölçmek istediğin doğrultudaki kenara veya yüzeye dokun.</small>')}
function stopGuide(){smartOff();guided=false;stage='OFF';document.getElementById('mgGuided2590')?.classList.remove('on')}
function syncFromSmart(){if(!guided)return;const s=(smartInfo()?.textContent||'').trim();if(/2\/3|Referans:/.test(s)){if(stage==='REF')flash('YÖN / EKSEN SEÇİLDİ');stage='P1';setTimeout(()=>prompt('<b>2/3 • ŞİMDİ İLK YÜZEYİ / NOKTAYI SEÇ</b><br><small>Ölçümün başlayacağı yere dokun.</small>'),420)}else if(/3\/3|İkinci noktayı/.test(s)){if(stage==='P1')flash('İLK NOKTA SEÇİLDİ');stage='P2';setTimeout(()=>prompt('<b>3/3 • ŞİMDİ KARŞI YÜZEYİ / NOKTAYI SEÇ</b><br><small>Ölçümün biteceği yere dokun.</small>'),420)}else if(/SONUÇ/.test(s)){stage='P1';prompt('✓ <b>ÖLÇÜM TAMAMLANDI</b><br><small>'+s.replace(/SONUÇ/,'').trim()+'</small>',true);setTimeout(()=>{if(guided)prompt('<b>YENİ ÖLÇÜM • İLK YÜZEYİ / NOKTAYI SEÇ</b><br><small>Aynı eksen/yön kullanılacak.</small>')},1500)}}
function install(){const tools=document.getElementById('tools');if(!tools||document.getElementById('mgGuideWrap2590'))return;
 hide('mgMeasureSimple2580');hide('mgSimpleMeasure2570');hide('mgMeasureHub2560');
 const w=document.createElement('div');w.id='mgGuideWrap2590';w.innerHTML='<div class="sep"></div><div class="head">ÖLÇÜM</div><div class="row"><button id="mgGuided2590">AKILLI ÖLÇ</button><button id="mgThick2590">KALINLIK</button><button id="mgClear2590">SİL</button></div><div id="mgGuide2590" class="small" style="margin-top:7px;padding:9px;border:1px solid #5fa8ff;border-radius:9px;line-height:1.35">Bir ölçüm seç.</div>';
 tools.appendChild(w);
 const g=document.getElementById('mgGuided2590'),t=document.getElementById('mgThick2590'),c=document.getElementById('mgClear2590');
 g.onclick=e=>{e.preventDefault();startGuide();t.classList.remove('on')};
 t.onclick=e=>{e.preventDefault();stopGuide();if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();const b=thicknessBtn();if(b&&!b.classList.contains('on'))b.click();t.classList.add('on');prompt('<b>KALINLIK • YÜZEYE BİR KEZ DOKUN</b><br><small>Karşı yüz otomatik bulunacak.</small>')};
 c.onclick=e=>{e.preventDefault();stopGuide();thickOff();if(window.measureOn&&window.toggleMeasure)window.toggleMeasure();if(window.clearMeasure)window.clearMeasure();document.getElementById('mgClear2570')?.click();t.classList.remove('on');clearFx();prompt('Temizlendi.')};
 const si=smartInfo();if(si)new MutationObserver(syncFromSmart).observe(si,{subtree:true,childList:true,characterData:true});
 window.addEventListener('click',ev=>{if(!guided||ev.target!==canvas)return;const h=hitAt(ev);if(!h)return;if(stage==='REF')feedback(h,0x55dd88,true);else if(stage==='P1')feedback(h,0x66ccff,false);else if(stage==='P2')feedback(h,0xffd45c,false)},true);
}
function init(){install();window.MG_CAD_V2590={version:'2.5.9',guidedSmartMeasurement:true,stepByStepPrompts:true,selectionConfirmation:true,visualSelectionFeedback:true,hapticConfirmation:true,baseline:'2.5.2'}}
ready(init)
})();'''
(AS/'cad-v2590.js').write_text(js,encoding='utf-8')
print('v2.5.9 guided smart measurement feedback')
