from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2430.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2430.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').trim().toLocaleUpperCase('tr-TR')}
function byText(prefix){return [...document.querySelectorAll('button')].find(b=>tr(b.textContent).startsWith(prefix))||null}
function styleLike(src,dst){if(!src||!dst)return;const cs=getComputedStyle(src);dst.style.background=cs.background;dst.style.color=cs.color;dst.style.border=cs.border;dst.style.borderRadius=cs.borderRadius;dst.style.padding=cs.padding;dst.style.font=cs.font;dst.style.fontWeight=cs.fontWeight;dst.style.minHeight=cs.minHeight;dst.style.boxShadow=cs.boxShadow;}
function placeInfo(info){if(!info)return;info.style.position='fixed';info.style.left='16px';info.style.bottom='calc(104px + env(safe-area-inset-bottom, 0px))';info.style.top='auto';info.style.width='min(42vw,650px)';info.style.maxWidth='calc(100vw - 48px)';info.style.maxHeight='40vh';info.style.overflowY='auto';info.style.overflowX='hidden';info.style.zIndex='44';info.style.boxSizing='border-box';info.style.margin='0';}
function installAnalysis(){
 const info=document.getElementById('info'); if(!info)return false; placeInfo(info);
 const cad=byText('CAD'); if(!cad)return false;
 [...document.querySelectorAll('button')].filter(b=>tr(b.textContent).startsWith('ANALİZ')&&b.id!=='mgAnalysis2430').forEach(b=>b.style.display='none');
 let a=document.getElementById('mgAnalysis2430');
 if(!a){a=document.createElement('button');a.id='mgAnalysis2430';a.type='button';a.textContent='ANALİZ';a.setAttribute('aria-expanded','false');styleLike(cad,a);a.style.whiteSpace='nowrap';a.style.flex='0 0 auto';a.onclick=function(e){e.preventDefault();e.stopPropagation();const opening=getComputedStyle(info).display==='none';placeInfo(info);info.style.display=opening?'block':'none';a.textContent=opening?'ANALİZ ◀':'ANALİZ';a.setAttribute('aria-expanded',opening?'true':'false');};}
 if(a.parentElement!==cad.parentElement || a.previousElementSibling!==cad) cad.insertAdjacentElement('afterend',a);
 if(!info.dataset.mg2430Init){info.dataset.mg2430Init='1';info.style.display='none';a.textContent='ANALİZ';a.setAttribute('aria-expanded','false');}
 return true;
}
let pivotGuide=null;
function clearPivotGuide(){if(!pivotGuide)return;try{scene.remove(pivotGuide);pivotGuide.traverse(o=>{if(o.geometry)o.geometry.dispose();if(o.material){if(Array.isArray(o.material))o.material.forEach(m=>m.dispose&&m.dispose());else o.material.dispose&&o.material.dispose();}})}catch(e){}pivotGuide=null;}
function pivotScale(){try{return Math.max(baseDims.x||0,baseDims.y||0,baseDims.z||0,1)*0.055}catch(e){return 1}}
function addPivotGuide(p){clearPivotGuide();try{const s=pivotScale();pivotGuide=new THREE.Group();pivotGuide.position.copy(p);pivotGuide.userData.mgPivotGuide=true;const mk=(a,b,c)=>{const g=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(-a,-b,-c),new THREE.Vector3(a,b,c)]);const m=new THREE.LineBasicMaterial({color:0xffb000,depthTest:false,transparent:true,opacity:.95});const l=new THREE.Line(g,m);l.renderOrder=1000;pivotGuide.add(l)};mk(s,0,0);mk(0,s,0);mk(0,0,s);const ringMat=new THREE.MeshBasicMaterial({color:0xff8c32,side:THREE.DoubleSide,transparent:true,opacity:.7,depthTest:false});const rg=new THREE.RingGeometry(s*.62,s*.72,48);const r1=new THREE.Mesh(rg,ringMat.clone());r1.renderOrder=999;pivotGuide.add(r1);const r2=new THREE.Mesh(rg,ringMat.clone());r2.rotation.x=Math.PI/2;r2.renderOrder=999;pivotGuide.add(r2);const r3=new THREE.Mesh(rg,ringMat.clone());r3.rotation.y=Math.PI/2;r3.renderOrder=999;pivotGuide.add(r3);scene.add(pivotGuide)}catch(e){console.warn('pivot guide',e)}}
function toast(msg){let t=document.getElementById('mgPivotToast2430');if(!t){t=document.createElement('div');t.id='mgPivotToast2430';t.style.cssText='position:fixed;left:50%;top:74px;transform:translateX(-50%);z-index:120;background:rgba(4,18,31,.96);border:1px solid #39b8ff;border-radius:10px;padding:10px 16px;color:#eaf7ff;font-weight:800;box-shadow:0 6px 22px #0008;pointer-events:none;transition:.2s';document.body.appendChild(t)}t.textContent=msg;t.style.opacity='1';clearTimeout(t._tm);t._tm=setTimeout(()=>t.style.opacity='0',1800)}
function setPivotButton(state){const b=document.getElementById('pivotB');if(!b)return;if(state==='pick'){b.textContent='NOKTA SEÇ…';b.classList.add('on');b.style.background='#174a70';b.style.borderColor='#65c9ff'}else if(state==='set'){b.textContent='✓ PİVOT SEÇİLDİ';b.classList.add('on');b.style.background='#126338';b.style.borderColor='#55e88b'}else{b.textContent='PİVOT SEÇ';b.classList.remove('on');b.style.background='';b.style.borderColor=''}}
function enhancePivot(){
 const pb=document.getElementById('pivotB'); if(!pb || typeof window.setPivot!=='function' || pb.dataset.mg2430)return false; pb.dataset.mg2430='1';
 const row=pb.parentElement;
 if(row && !document.getElementById('mgPivotPart2430')){const part=document.createElement('button');part.id='mgPivotPart2430';part.type='button';part.textContent='PARÇA MERKEZİ';part.onclick=function(e){e.preventDefault();e.stopPropagation();try{if(!selected){toast('Önce bir parça seç');return}const b=new THREE.Box3().setFromObject(selected),c=new THREE.Vector3();b.getCenter(c);window.setPivot(c)}catch(err){toast('Parça merkezi alınamadı')}};row.insertBefore(part,row.lastElementChild);}
 const oldToggle=window.togglePivotPick, oldSet=window.setPivot, oldReset=window.resetPivot;
 window.togglePivotPick=function(){oldToggle();if(typeof pivotPick!=='undefined'&&pivotPick){setPivotButton('pick');const i=document.getElementById('pivotInfo');if(i)i.textContent='Model üzerinde dönme merkezi olacak yüzey noktasına dokun';toast('Pivot noktası seç: model yüzeyine dokun')}else if(typeof pivotPoint!=='undefined'&&pivotPoint){setPivotButton('set')}else setPivotButton('idle')};
 window.setPivot=function(p){oldSet(p);addPivotGuide(p);setPivotButton('set');const i=document.getElementById('pivotInfo');if(i){const f=(v)=>{try{return typeof fmt==='function'?fmt(v):Number(v).toFixed(3)}catch(e){return Number(v).toFixed(3)}};i.innerHTML='<b>✓ Pivot seçildi</b><br>X '+f(p.x)+' • Y '+f(p.y)+' • Z '+f(p.z)+'<br><span style="color:#8bd8ff">Dönüş merkezi bu noktadır.</span>'}toast('✓ Pivot seçildi — model bu nokta etrafında dönecek')};
 window.resetPivot=function(){oldReset();clearPivotGuide();setPivotButton('idle');toast('Pivot model merkezine alındı')};
 setPivotButton('idle');
 return true;
}
function init(){
 const info=document.getElementById('info');if(info){placeInfo(info);if(!info.dataset.mg2430Init){info.dataset.mg2430Init='1';info.style.display='none'}}
 installAnalysis();enhancePivot();
 const mo=new MutationObserver(()=>{installAnalysis();enhancePivot()});mo.observe(document.body,{childList:true,subtree:true});
 window.addEventListener('resize',()=>{placeInfo(document.getElementById('info'));installAnalysis()},{passive:true});
 window.MG_CAD_V2430={version:'2.4.3',analysisButtonPersistent:true,analysisBesideCad:true,analysisClosedOnStart:true,analysisSafeBottomLeft:true,pivotSelectedFeedback:true,pivotCrosshairGuide:true,pivotPartCenter:true,pivotResetFeedback:true};
}
ready(init);
})();'''
(AS/'cad-v2430.js').write_text(js,encoding='utf-8')
print('v2.4.3: persistent ANALIZ beside CAD + enhanced selected-point pivot feedback and guide')
