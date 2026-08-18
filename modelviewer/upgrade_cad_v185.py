from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v185.js' not in s:
    s=s.replace('</body>','<script src="/cad-v185.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
function B(id){return document.getElementById(id)}
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function U(v){try{return fmt(v)+' '+B('unit').value}catch(e){return Number(v).toFixed(3)}}

function syncSectionButtons(){
 const on=typeof sectionOn!=='undefined'&&sectionOn;
 const a=B('quickSecB'),b=B('secB');
 if(a){a.textContent=on?'KESİT KAPAT':'KESİT';a.classList.toggle('on',on)}
 if(b){b.textContent=on?'KESİT KAPAT':'KESİT AÇ';b.classList.toggle('on',on)}
}
function installSectionToggle(){
 const a=B('quickSecB');if(a)a.onclick=()=>{if(typeof toggleSection==='function')toggleSection();syncSectionButtons()};
 const b=B('secB');if(b){const old=b.onclick;b.onclick=()=>{if(typeof toggleSection==='function')toggleSection();else if(old)old();syncSectionButtons()}}
 syncSectionButtons();
}

function installInfoToggle(){
 const info=B('info');if(!info||B('mgInfoToggle'))return;
 const t=document.createElement('button');t.id='mgInfoToggle';t.textContent='ANALİZ ◀';
 t.style.cssText='position:absolute;left:320px;bottom:18px;z-index:12;min-width:92px';document.body.appendChild(t);
 let open=true;
 function apply(){info.classList.toggle('hide',!open);t.textContent=open?'ANALİZ ◀':'ANALİZ ▶';t.style.left=open?'320px':'10px';}
 t.onclick=()=>{open=!open;apply()};apply();
}

function ensureDims(){const b=B('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}
function showDrawingBar(){
 let bar=B('mgDrawingBar');if(!bar){bar=document.createElement('div');bar.id='mgDrawingBar';bar.className='panel';bar.style.cssText='left:50%;transform:translateX(-50%);top:70px;z-index:12;display:flex;gap:6px;padding:6px';bar.innerHTML='<button data-v="front">ÖN</button><button data-v="top">ÜST</button><button data-v="right">SAĞ</button><button data-v="iso">İZOMETRİK</button><button id="mgDrawingClose">KAPAT</button>';document.body.appendChild(bar);bar.querySelectorAll('[data-v]').forEach(x=>x.onclick=()=>{if(typeof viewDir==='function')viewDir(x.dataset.v);ensureDims()});B('mgDrawingClose').onclick=()=>bar.classList.add('hide')}
 bar.classList.remove('hide');
}
function activateDrawing(){
 try{if(typeof viewDir==='function')viewDir('front');if(typeof wire!=='undefined'&&wire&&typeof toggleWire==='function')toggleWire();if(window.MG_CAD_V184&&typeof setEdgeMode==='function')setEdgeMode(true,false);if(grid&&grid.visible&&typeof toggleGrid==='function')toggleGrid()}catch(e){}
 ensureDims();showDrawingBar();const out=B('industryOut');if(out)out.innerHTML='<b>TEKNİK RESİM</b><br>ÖN / ÜST / SAĞ / İZOMETRİK görünüş seç. Ölçüler görünüş üzerinde kalır.';
}
function installDrawingMode(){const b=B('modeDrawing');if(b)b.onclick=activateDrawing}

function weld(g,tol){const p=g.attributes.position,ids=[],pts=[],map=new Map();for(let i=0;i<p.count;i++){const x=p.getX(i),y=p.getY(i),z=p.getZ(i),k=Math.round(x/tol)+','+Math.round(y/tol)+','+Math.round(z/tol);let id=map.get(k);if(id===undefined){id=pts.length;map.set(k,id);pts.push(new THREE.Vector3(x,y,z))}ids.push(id)}return{ids,pts}}
function circle(comp,pts){let mn=new THREE.Vector3(Infinity,Infinity,Infinity),mx=new THREE.Vector3(-Infinity,-Infinity,-Infinity),c=new THREE.Vector3();comp.forEach(i=>{mn.min(pts[i]);mx.max(pts[i]);c.add(pts[i])});c.multiplyScalar(1/comp.length);const s=mx.clone().sub(mn);let drop=s.x<=s.y&&s.x<=s.z?0:(s.y<=s.z?1:2),rs=[];for(const i of comp){const p=pts[i],a=drop===0?p.y-c.y:p.x-c.x,b=drop===2?p.y-c.y:p.z-c.z;rs.push(Math.hypot(a,b))}const r=rs.reduce((a,b)=>a+b,0)/rs.length,sd=Math.sqrt(rs.reduce((a,b)=>a+(b-r)*(b-r),0)/rs.length);if(!isFinite(r)||r<=0||sd/r>.05)return null;return{center:c,r,axis:drop===0?'X':drop===1?'Y':'Z',q:1-sd/r}}
function circularFeatures(){
 const out=[],scale=Math.max(baseDims.x,baseDims.y,baseDims.z,1),tol=scale*1e-5;
 meshes().forEach(mesh=>{const g=mesh.geometry,p=g&&g.attributes&&g.attributes.position;if(!p)return;const w=weld(g,tol),idx=g.index,edges=new Map(),tc=idx?idx.count/3:p.count/3;function ae(a,b){if(a>b){const z=a;a=b;b=z}const k=a+','+b;edges.set(k,(edges.get(k)||0)+1)}for(let k=0;k<tc;k++){const a=w.ids[idx?idx.getX(k*3):k*3],b=w.ids[idx?idx.getX(k*3+1):k*3+1],c=w.ids[idx?idx.getX(k*3+2):k*3+2];ae(a,b);ae(b,c);ae(c,a)}const adj=new Map();for(const [k,n] of edges)if(n===1){const [a,b]=k.split(',').map(Number);(adj.get(a)||adj.set(a,[]).get(a)).push(b);(adj.get(b)||adj.set(b,[]).get(b)).push(a)}const seen=new Set();for(const st of adj.keys()){if(seen.has(st))continue;const q=[st],comp=[];while(q.length){const v=q.pop();if(seen.has(v))continue;seen.add(v);comp.push(v);(adj.get(v)||[]).forEach(n=>{if(!seen.has(n))q.push(n)})}if(comp.length>=10){const f=circle(comp,w.pts);if(f&&f.r>scale*.0015){f.mesh=mesh;out.push(f)}}}});
 return out.filter((h,i,a)=>!a.slice(0,i).some(k=>k.center.distanceTo(h.center)<Math.max(h.r,k.r)*.12&&Math.abs(k.r-h.r)<Math.max(h.r,k.r)*.06));
}
function showDiameters(){
 const fs=circularFeatures(),span=Math.max(baseDims.x,baseDims.y,baseDims.z,1),outerCut=span*.20;let panel=B('holePanel');if(!panel){panel=document.createElement('div');panel.id='holePanel';panel.className='panel';panel.style.cssText='left:8px;top:66px;max-width:420px;max-height:68%;overflow:auto;z-index:14';document.body.appendChild(panel)}
 let inn=[],out=[];fs.forEach(f=>(f.r>=outerCut?out:inn).push(f));let h='<div class="head">DELİKLER + DIŞ ÇAPLAR</div><div class="small"><b>İÇ ÇAPLAR</b><br>';if(!inn.length)h+='Aday yok.<br>';inn.forEach((x,i)=>h+='H'+(i+1)+' &nbsp; Ø'+U(2*x.r)+' • '+x.axis+'<br>');h+='<br><b>DIŞ ÇAPLAR</b><br>';if(!out.length)h+='Aday yok.<br>';out.forEach((x,i)=>h+='D'+(i+1)+' &nbsp; Ø'+U(2*x.r)+' • '+x.axis+'<br>');h+='</div><div class="row"><button id="mgHoleClose">KAPAT</button></div>';panel.innerHTML=h;panel.classList.remove('hide');B('mgHoleClose').onclick=()=>panel.classList.add('hide');const pr=B('proResult');if(pr)pr.innerHTML='<b>'+inn.length+' iç çap • '+out.length+' dış çap</b><br>Dairesel sınır geometrisinden hesaplandı.';
}
function installDiameterButton(){const b=B('holesB');if(b)b.onclick=()=>{const pr=B('proResult');if(pr)pr.textContent='İç/dış dairesel çaplar analiz ediliyor…';setTimeout(showDiameters,15)};const m=B('modeHole');if(m)m.onclick=()=>{showDiameters();ensureDims()}}

function boot(){installSectionToggle();installInfoToggle();installDrawingMode();installDiameterButton();window.MG_CAD_V185={version:'1.8.5',sectionToggle:true,analysisCollapse:true,drawingViews:true,outerDiameters:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,420));else setTimeout(boot,420);
})();'''
(AS/'cad-v185.js').write_text(js,encoding='utf-8')
print('v1.8.5 section toggle + analysis collapse + drawing views + inner/outer diameter analysis applied')
