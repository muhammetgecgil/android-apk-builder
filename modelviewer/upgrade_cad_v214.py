from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v214.js' not in h:
    h=h.replace('</body>','<script src="/cad-v214.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function tr(s){return String(s||'').toLocaleUpperCase('tr-TR')}
function findLegend(){return [...document.querySelectorAll('div,span,p')].find(e=>{let t=tr(e.textContent);return t.includes('X KIRMIZI')&&t.includes('Y YEŞİL')&&t.includes('Z MAVİ')})||null}
function findOriginalPen(){
 const cand=[...document.querySelectorAll('button,div')].filter(e=>e.id!=='mgPenStatus');
 let best=null;
 cand.forEach(e=>{const r=e.getBoundingClientRect();if(r.width<45||r.width>120||r.height<45||r.height>120||r.left>280||r.top>190)return;const cs=getComputedStyle(e);const txt=tr(e.textContent);const hasIcon=!!e.querySelector('svg,img')||txt.includes('✎')||txt.includes('PEN');if(!hasIcon)return;if(!best||r.left<best.getBoundingClientRect().left)best=e;});
 return best;
}
function labelSprite(letter,color){try{const c=document.createElement('canvas');c.width=128;c.height=128;const x=c.getContext('2d');x.clearRect(0,0,128,128);x.font='900 78px Arial';x.textAlign='center';x.textBaseline='middle';x.lineWidth=10;x.strokeStyle='#04101d';x.strokeText(letter,64,66);x.fillStyle=color;x.fillText(letter,64,66);const tx=new THREE.CanvasTexture(c),m=new THREE.SpriteMaterial({map:tx,transparent:true,depthTest:false});const s=new THREE.Sprite(m);s.scale.set(.55,.55,.55);s.userData.mgAxisLabel=true;return s}catch(e){return null}}
function addAxisLetters(){try{if(typeof scene==='undefined')return;scene.children.filter(o=>o.userData&&o.userData.mgAxisLabel).forEach(o=>scene.remove(o));let span=2.8;try{if(typeof baseDims!=='undefined')span=Math.max(baseDims.x,baseDims.y,baseDims.z,1)*.18}catch(_){};[['X','#ff3b48',[span,0,0]],['Y','#55ef6d',[0,span,0]],['Z','#46a6ff',[0,0,span]]].forEach(a=>{const s=labelSprite(a[0],a[1]);if(s){s.position.set(a[2][0],a[2][1],a[2][2]);s.renderOrder=999;scene.add(s)}})}catch(e){}}
function styleLegend(){const l=findLegend();if(!l)return;l.style.left='8px';l.style.top='108px';l.style.position='fixed';l.style.zIndex='42';l.style.margin='0';l.style.fontWeight='900';l.style.fontSize='18px';l.style.pointerEvents='none';}
function penState(on){const p=findOriginalPen();if(!p)return;p.style.transition='.15s';p.style.background=on?'#117a39':'';p.style.borderColor=on?'#48ef79':'';p.style.boxShadow=on?'0 0 20px #48ef79':' ';p.dataset.mgReady=on?'1':'0'}
function removeExtraPen(){const p=document.getElementById('mgPenStatus');if(p)p.remove()}
function val(id,fallback='-'){const e=document.getElementById(id);return e?String(e.textContent||'').trim():fallback}
function reportHTML(svg){let name=(document.getElementById('name')||{}).textContent||'CAD Model';let dims=val('dims');let stat=val('status');let unit=(document.getElementById('unit')||{}).value||'mm';let mat='';const sels=[...document.querySelectorAll('select')];for(const s of sels){if(/Alüminyum|Çelik|Titanyum|Plastik|Genel/i.test(s.value||s.options[s.selectedIndex]?.text||'')){mat=s.options[s.selectedIndex]?.text||s.value;break}}
return `<div style="display:grid;grid-template-columns:320px 1fr;gap:14px;padding:14px;background:#07111d;color:#eaf6ff;min-height:100%"><aside style="background:#0b1c2c;border:1px solid #245b82;border-radius:14px;padding:16px"><h2 style="color:#5bd1ff;margin:0 0 12px">RAPOR / TEKNİK RESİM</h2><h3>PARÇA BİLGİLERİ</h3><div style="line-height:1.7"><b>Dosya:</b> ${name}<br><b>Birim:</b> ${unit}<br><b>Malzeme:</b> ${mat||'-'}<br><br>${dims.replace(/<br>/gi,'<br>')}<br><br>${stat}</div></aside><main style="background:white;color:#111;border-radius:12px;padding:12px;overflow:auto"><div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px"><h2 style="margin:0">MÜHENDİSLİK RAPORU</h2><button id="mgReportClose" style="padding:10px 18px">RAPORU KAPAT</button></div>${svg||'<div>Teknik resim hazırlanamadı.</div>'}<div style="margin-top:14px;border-top:1px solid #aaa;padding-top:10px"><b>Rapor içeriği:</b> dış ölçüler, parça bilgileri, malzeme, yüzey/hacim, seçili ölçü/çap/kalınlık sonuçları ve teknik görünüşler.</div></main></div>`}
function openReport(){
 let tech=[...document.querySelectorAll('button')].find(b=>tr(b.textContent).trim()==='TEKNİK RESİM');
 let before=document.getElementById('mgSheet199');
 if(tech&&!before)try{tech.click()}catch(e){}
 setTimeout(()=>{let sheet=document.getElementById('mgSheet199');let svg='';if(sheet){let s=sheet.querySelector('svg');if(s)svg=s.outerHTML;sheet.remove()}
 let o=document.getElementById('mgReport214');if(o)o.remove();o=document.createElement('div');o.id='mgReport214';o.style.cssText='position:fixed;left:0;right:0;top:60px;bottom:0;z-index:90;background:#07111d;overflow:auto';o.innerHTML=reportHTML(svg);document.body.appendChild(o);document.getElementById('mgReportClose').onclick=()=>o.remove();},120)
}
function hookReport(){[...document.querySelectorAll('button')].forEach(b=>{if(tr(b.textContent).trim()==='RAPOR'&&!b.dataset.mg214){b.dataset.mg214='1';b.addEventListener('click',e=>{e.preventDefault();e.stopPropagation();openReport()},true)}})}
function init(){removeExtraPen();styleLegend();addAxisLetters();hookReport();document.addEventListener('click',e=>{const b=e.target&&e.target.closest?e.target.closest('button,[role=button]'):null;if(!b)return;const t=tr(b.textContent).trim();if(t==='ÇİZ'||t.includes('S-PEN'))penState(true);if(t==='SİL'||t.includes('TEMİZLE'))penState(false)},true);window.MG_CAD_V214={version:'2.0.14',extraPenRemoved:true,originalPenGreenReady:true,legendLeftAligned:true,axisLettersColored:true,reportTechnicalDrawing:true,noPolling:true}}
ready(init)
})();'''
(AS/'cad-v214.js').write_text(js,encoding='utf-8')
print('v2.0.14: single S-Pen, colored XYZ, engineering report with technical drawing')
