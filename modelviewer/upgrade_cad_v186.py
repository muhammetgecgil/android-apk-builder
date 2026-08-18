from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v186.js' not in s:
    s=s.replace('</body>','<script src="/cad-v186.js"></script></body>',1)
html.write_text(s,encoding='utf-8')

js=r'''(function(){
'use strict';
let holePickMode=false, diaFeatures=[], diaLabels=[];
function E(id){return document.getElementById(id)}
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function unitText(v){try{return fmt(v)+' '+E('unit').value}catch(e){return Number(v).toFixed(3)+' mm'}}
function industryMsg(h){const a=E('industryOut'),b=E('proResult');if(a)a.innerHTML=h;if(b)b.innerHTML=h}
function forceDims(){const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}

function installAnalysisButton(){
 const info=E('info');if(!info)return;
 const old=E('mgInfoToggle');if(old)old.remove();
 let b=E('mgAnalysisPanelB');if(!b){b=document.createElement('button');b.id='mgAnalysisPanelB';b.textContent='ANALİZ ◀';b.style.cssText='position:absolute;right:8px;top:7px;z-index:2;padding:5px 8px';info.appendChild(b)}
 info.style.paddingTop='38px';let open=true;
 const hideables=()=>Array.from(info.children).filter(x=>x!==b);
 b.onclick=()=>{open=!open;hideables().forEach(x=>x.style.display=open?'':'none');if(open){info.style.minWidth='245px';info.style.maxWidth='330px';info.style.padding='38px 9px 9px';info.style.background='rgba(3,10,20,.92)';info.style.border='1px solid #173c60';b.textContent='ANALİZ ◀'}else{info.style.minWidth='0';info.style.width='auto';info.style.padding='0';info.style.background='transparent';info.style.border='0';b.textContent='ANALİZ ▶'}};
}

function weld(g,tol){const p=g.attributes.position,ids=[],pts=[],map=new Map();for(let i=0;i<p.count;i++){const x=p.getX(i),y=p.getY(i),z=p.getZ(i),k=Math.round(x/tol)+','+Math.round(y/tol)+','+Math.round(z/tol);let id=map.get(k);if(id===undefined){id=pts.length;map.set(k,id);pts.push(new THREE.Vector3(x,y,z))}ids.push(id)}return{ids,pts}}
function fitCircle(comp,pts){let mn=new THREE.Vector3(Infinity,Infinity,Infinity),mx=new THREE.Vector3(-Infinity,-Infinity,-Infinity),c=new THREE.Vector3();comp.forEach(i=>{mn.min(pts[i]);mx.max(pts[i]);c.add(pts[i])});c.multiplyScalar(1/comp.length);const s=mx.clone().sub(mn);let drop=s.x<=s.y&&s.x<=s.z?0:(s.y<=s.z?1:2),rs=[];for(const i of comp){const p=pts[i],a=drop===0?p.y-c.y:p.x-c.x,b=drop===2?p.y-c.y:p.z-c.z;rs.push(Math.hypot(a,b))}const r=rs.reduce((a,b)=>a+b,0)/rs.length,sd=Math.sqrt(rs.reduce((a,b)=>a+(b-r)*(b-r),0)/rs.length);if(!isFinite(r)||r<=0||sd/r>.065)return null;return{center:c,r,axis:drop===0?'X':drop===1?'Y':'Z',q:1-sd/r}}
function detectCircularFeatures(){
 const out=[],scale=Math.max(baseDims.x,baseDims.y,baseDims.z,1),tol=scale*1e-5;
 meshes().forEach(mesh=>{const g=mesh.geometry,p=g&&g.attributes&&g.attributes.position;if(!p)return;const w=weld(g,tol),idx=g.index,edges=new Map(),tc=idx?idx.count/3:p.count/3;function add(a,b){if(a>b){const t=a;a=b;b=t}const k=a+','+b;edges.set(k,(edges.get(k)||0)+1)}for(let t=0;t<tc;t++){const a=w.ids[idx?idx.getX(t*3):t*3],b=w.ids[idx?idx.getX(t*3+1):t*3+1],c=w.ids[idx?idx.getX(t*3+2):t*3+2];add(a,b);add(b,c);add(c,a)}const adj=new Map();for(const [k,n] of edges)if(n===1){const [a,b]=k.split(',').map(Number);if(!adj.has(a))adj.set(a,[]);if(!adj.has(b))adj.set(b,[]);adj.get(a).push(b);adj.get(b).push(a)}const seen=new Set();for(const st of adj.keys()){if(seen.has(st))continue;const stack=[st],comp=[];while(stack.length){const v=stack.pop();if(seen.has(v))continue;seen.add(v);comp.push(v);(adj.get(v)||[]).forEach(n=>{if(!seen.has(n))stack.push(n)})}if(comp.length>=8){const f=fitCircle(comp,w.pts);if(f&&f.r>scale*.001){f.mesh=mesh;out.push(f)}}}});
 diaFeatures=out.filter((h,i,a)=>!a.slice(0,i).some(k=>k.mesh===h.mesh&&k.center.distanceTo(h.center)<Math.max(h.r,k.r)*.14&&Math.abs(k.r-h.r)<Math.max(h.r,k.r)*.08));return diaFeatures;
}
function radialScore(f,p){let radial,ax;if(f.axis==='X'){radial=Math.hypot(p.y-f.center.y,p.z-f.center.z);ax=Math.abs(p.x-f.center.x)}else if(f.axis==='Y'){radial=Math.hypot(p.x-f.center.x,p.z-f.center.z);ax=Math.abs(p.y-f.center.y)}else{radial=Math.hypot(p.x-f.center.x,p.y-f.center.y);ax=Math.abs(p.z-f.center.z)}const span=Math.max(baseDims.x,baseDims.y,baseDims.z,1);return Math.abs(radial-f.r)/Math.max(f.r,span*.002)+ax/span*.08}
function makeLabel(text,p){const c=document.createElement('canvas');c.width=420;c.height=90;const x=c.getContext('2d');x.fillStyle='rgba(3,10,20,.94)';x.strokeStyle='#ffd84d';x.lineWidth=4;x.beginPath();x.roundRect(4,4,412,82,16);x.fill();x.stroke();x.fillStyle='#ffe477';x.font='bold 34px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(text,210,45);const tex=new THREE.CanvasTexture(c),mat=new THREE.SpriteMaterial({map:tex,depthTest:false,transparent:true});const sp=new THREE.Sprite(mat);const sc=Math.max(baseDims.x,baseDims.y,baseDims.z,1)*.18;sp.scale.set(sc,sc*.214,1);sp.position.copy(p);sp.renderOrder=50;scene.add(sp);diaLabels.push(sp)}
function clearDiaLabels(){diaLabels.forEach(s=>{scene.remove(s);if(s.material&&s.material.map)s.material.map.dispose();if(s.material)s.material.dispose()});diaLabels=[]}
function canvasHit(ev){const r=canvas.getBoundingClientRect();mouse.x=((ev.clientX-r.left)/r.width)*2-1;mouse.y=-((ev.clientY-r.top)/r.height)*2+1;ray.setFromCamera(mouse,camera);return ray.intersectObjects(meshes().filter(x=>x.visible),false)[0]||null}
function pickDiameter(ev){if(!holePickMode)return;const h=canvasHit(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();if(!diaFeatures.length)detectCircularFeatures();const same=diaFeatures.filter(f=>f.mesh===h.object);const pool=same.length?same:diaFeatures;if(!pool.length){industryMsg('<b>DELİK / ÇAP</b><br>Bu bölgede dairesel geometri bulunamadı.');return}let best=pool[0],score=radialScore(best,h.point);pool.slice(1).forEach(f=>{const s=radialScore(f,h.point);if(s<score){score=s;best=f}});const txt='Ø '+unitText(best.r*2);clearDiaLabels();makeLabel(txt,h.point.clone());industryMsg('<b>DOKUNULAN ÇAP: '+txt+'</b><br>'+best.axis+' ekseni • dairesel yüzey/sınır geometrisinden hesaplandı.');}
function installHolePick(){const b=E('holesB');if(!b)return;b.textContent='DELİKLER / ÇAP';b.onclick=()=>{holePickMode=!holePickMode;b.classList.toggle('on',holePickMode);controls.enableRotate=!holePickMode;if(holePickMode){detectCircularFeatures();industryMsg('<b>DELİK / ÇAP MODU</b><br>Deliğin iç yüzeyine veya dış silindirik yüzeye dokun. Çap doğrudan model üzerinde gösterilir.')}else{clearDiaLabels();industryMsg('Delik/çap modu kapalı.')}};canvas.addEventListener('click',pickDiameter,true)}

function suggestedTol(v){v=Math.abs(v);return v<=6?.10:v<=30?.20:v<=120?.30:v<=400?.50:.80}
function showTolerance(){forceDims();let p=E('mgTolPanel');if(!p){p=document.createElement('div');p.id='mgTolPanel';p.className='panel';p.style.cssText='left:8px;top:66px;max-width:420px;z-index:15';document.body.appendChild(p)}const u=E('unit').value;const vals=[['X',baseDims.x],['Y',baseDims.y],['Z',baseDims.z]];let h='<div class="head">TOLERANS</div><div class="small"><b>ÖNERİLEN GENEL TOLERANSLAR</b><br>Modelde gerçek PMI/GD&T yoksa öneri olarak gösterilir; üretim resmi yerine geçmez.<br><br>';vals.forEach(([n,v])=>h+=n+' '+unitText(v)+' &nbsp; ±'+unitText(suggestedTol(v))+'<br>');if(!diaFeatures.length)detectCircularFeatures();diaFeatures.slice(0,12).forEach((f,i)=>{const d=f.r*2;h+='Ø'+unitText(d)+' &nbsp; ±'+unitText(suggestedTol(d))+'<br>'});h+='</div><div class="row"><button id="mgTolClose">KAPAT</button></div>';p.innerHTML=h;p.classList.remove('hide');E('mgTolClose').onclick=()=>p.classList.add('hide');industryMsg('<b>TOLERANS MODU</b><br>Nominal ölçüler için önerilen genel toleranslar açıldı. Gerçek PMI/GD&T mevcutsa o veri önceliklidir.')}
function installIndustryModes(){
 const q=E('modeQuality');if(q){q.textContent='TOLERANS';q.onclick=()=>showTolerance()}
 const d=E('modeDesign');if(d)d.onclick=()=>{try{viewDir('iso');fit();if(typeof setEdgeMode==='function')setEdgeMode(true,false)}catch(e){}industryMsg('<b>TASARIM MODU</b><br>Geometri, kesit, açı, yüzey/normal, radyüs/çap, kalınlık, alan ve hacim incelemesi için CAD görünümü hazırlandı.')};
 const m=E('modeMfg');if(m)m.onclick=()=>{forceDims();industryMsg('<b>İMALAT MODU</b><br>Dış ölçüler ve çaplar açıldı. Delik/çap, kalınlık, kesit, prob, kütle/CG ve üretilebilirlik kontrol araçlarını kullan.')};
 const c=E('modeCam');if(c)c.onclick=()=>{forceDims();holePickMode=true;const hb=E('holesB');if(hb)hb.classList.add('on');controls.enableRotate=false;detectCircularFeatures();industryMsg('<b>CAM / CNC HAZIRLIK</b><br>Delik eksenleri/çapları, cep ve işlenecek yüzeyleri incele. Bu mod G-code üretmez; bağlama ve operasyon hazırlığı içindir. Model üzerinde bir deliğe dokunarak çapı alabilirsin.')};
}
function boot(){installAnalysisButton();installHolePick();installIndustryModes();window.MG_CAD_V186={version:'1.8.6',analysisButtonOnPanel:true,directSurfaceDiameter:true,toleranceMode:true,designMode:true,manufacturingMode:true,camPreparation:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,520));else setTimeout(boot,520);
})();'''
(AS/'cad-v186.js').write_text(js,encoding='utf-8')
print('v1.8.6 direct diameter + tolerance + industry workflow + analysis button applied')
