from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
AS.mkdir(parents=True, exist_ok=True)
js=r'''(function(){
'use strict';
let dimOn=false, dimGroup=new THREE.Group(), dimLabels=[], diaDims=[];
scene.add(dimGroup);
function D(tag,cls,txt){const e=document.createElement(tag);if(cls)e.className=cls;if(txt!==undefined)e.textContent=txt;return e}
function dimUnit(v){const u=document.getElementById('unit').value;return fmt(v)+' '+u}
function clearDims(){while(dimGroup.children.length){const x=dimGroup.children.pop();if(x.geometry)x.geometry.dispose();if(x.material)x.material.dispose()}dimLabels.forEach(x=>x.el.remove());dimLabels=[];diaDims=[]}
function mat(){return new THREE.LineBasicMaterial({color:0xffd34d,depthTest:false,transparent:true,opacity:.96})}
function line(a,b){const g=new THREE.BufferGeometry().setFromPoints([a,b]);const l=new THREE.Line(g,mat());l.renderOrder=1000;dimGroup.add(l);return l}
function tick(p,axis,size){let a=p.clone(),b=p.clone();if(axis==='x'){a.y-=size;b.y+=size}else if(axis==='y'){a.x-=size;b.x+=size}else{a.x-=size;b.x+=size}line(a,b)}
function label(text,p,kind='linear'){const el=D('div','mgDimLabel',text);document.body.appendChild(el);dimLabels.push({el,p:p.clone(),kind});return el}
function projectLabels(){if(!dimOn)return;const w=innerWidth,h=innerHeight;dimLabels.forEach(o=>{const p=o.p.clone().project(camera);const x=(p.x*.5+.5)*w,y=(-p.y*.5+.5)*h;o.el.style.display=(p.z<-1||p.z>1)?'none':'block';o.el.style.transform='translate(-50%,-50%) translate('+x+'px,'+y+'px)'})}
function drawLinearDims(){if(!bbox)return;const min=bbox.min,max=bbox.max,sz=new THREE.Vector3();bbox.getSize(sz);const span=Math.max(sz.x,sz.y,sz.z,1),off=span*.09,t=span*.012;
// X dimension below/front
let a=new THREE.Vector3(min.x,min.y-off,max.z),b=new THREE.Vector3(max.x,min.y-off,max.z);line(a,b);line(new THREE.Vector3(min.x,min.y,max.z),a);line(new THREE.Vector3(max.x,min.y,max.z),b);tick(a,'y',t);tick(b,'y',t);label('X  '+dimUnit(sz.x),a.clone().lerp(b,.5));
// Y dimension left/front
let c=new THREE.Vector3(min.x-off,min.y,max.z),d=new THREE.Vector3(min.x-off,max.y,max.z);line(c,d);line(new THREE.Vector3(min.x,min.y,max.z),c);line(new THREE.Vector3(min.x,max.y,max.z),d);tick(c,'x',t);tick(d,'x',t);label('Y  '+dimUnit(sz.y),c.clone().lerp(d,.5));
// Z dimension right/top-ish
let e=new THREE.Vector3(max.x+off,max.y,min.z),f=new THREE.Vector3(max.x+off,max.y,max.z);line(e,f);line(new THREE.Vector3(max.x,max.y,min.z),e);line(new THREE.Vector3(max.x,max.y,max.z),f);tick(e,'x',t);tick(f,'x',t);label('Z  '+dimUnit(sz.z),e.clone().lerp(f,.5));
}
function weld(g,tol){const p=g.attributes.position,ids=[],pts=[],map=new Map();for(let i=0;i<p.count;i++){const x=p.getX(i),y=p.getY(i),z=p.getZ(i),k=Math.round(x/tol)+','+Math.round(y/tol)+','+Math.round(z/tol);let id=map.get(k);if(id===undefined){id=pts.length;map.set(k,id);pts.push(new THREE.Vector3(x,y,z))}ids.push(id)}return{ids,pts}}
function circle(comp,pts){let min=new THREE.Vector3(Infinity,Infinity,Infinity),max=new THREE.Vector3(-Infinity,-Infinity,-Infinity),c=new THREE.Vector3();comp.forEach(i=>{min.min(pts[i]);max.max(pts[i]);c.add(pts[i])});c.multiplyScalar(1/comp.length);const s=max.clone().sub(min);let drop=s.x<=s.y&&s.x<=s.z?0:(s.y<=s.z?1:2),rs=[];for(const i of comp){const p=pts[i],u=drop===0?p.y-c.y:p.x-c.x,v=drop===2?p.y-c.y:p.z-c.z;rs.push(Math.hypot(u,v))}const r=rs.reduce((a,b)=>a+b,0)/rs.length,sd=Math.sqrt(rs.reduce((a,b)=>a+(b-r)*(b-r),0)/rs.length);if(!isFinite(r)||r<=0||sd/r>.05)return null;return{center:c,r,axis:drop===0?'X':drop===1?'Y':'Z',q:1-sd/r}}
function detectCircularDims(){diaDims=[];if(!bbox)return;const scale=Math.max(baseDims.x,baseDims.y,baseDims.z,1),tol=scale*1e-5,minR=scale*.0015;group.children.forEach(mesh=>{const g=mesh.geometry,p=g.attributes.position;if(!p)return;const w=weld(g,tol),idx=g.index,edges=new Map(),tc=idx?idx.count/3:p.count/3;function ae(a,b){if(a>b){let z=a;a=b;b=z}const k=a+','+b;edges.set(k,(edges.get(k)||0)+1)}for(let k=0;k<tc;k++){const a=w.ids[idx?idx.getX(k*3):k*3],b=w.ids[idx?idx.getX(k*3+1):k*3+1],c=w.ids[idx?idx.getX(k*3+2):k*3+2];ae(a,b);ae(b,c);ae(c,a)}const adj=new Map();for(const [k,n] of edges)if(n===1){const [a,b]=k.split(',').map(Number);if(!adj.has(a))adj.set(a,[]);if(!adj.has(b))adj.set(b,[]);adj.get(a).push(b);adj.get(b).push(a)}const seen=new Set();for(const st of adj.keys()){if(seen.has(st))continue;const q=[st],comp=[];while(q.length){const v=q.pop();if(seen.has(v))continue;seen.add(v);comp.push(v);(adj.get(v)||[]).forEach(n=>{if(!seen.has(n))q.push(n)})}if(comp.length>=12){const f=circle(comp,w.pts);if(f&&f.r>=minR){f.mesh=mesh;diaDims.push(f)}}}});diaDims=diaDims.filter((h,i,a)=>!a.slice(0,i).some(k=>k.center.distanceTo(h.center)<Math.max(h.r,k.r)*.12&&Math.abs(k.r-h.r)<Math.max(h.r,k.r)*.06)).slice(0,24)}
function drawDiameterDims(){if(!bbox)return;const span=Math.max(baseDims.x,baseDims.y,baseDims.z,1);diaDims.forEach((h,i)=>{const c=h.center.clone();let radial=h.axis==='X'?new THREE.Vector3(0,1,0):new THREE.Vector3(1,0,0);const edge=c.clone().add(radial.clone().multiplyScalar(h.r));const out=edge.clone().add(radial.clone().multiplyScalar(span*(.045+.008*(i%3))));line(c,edge);line(edge,out);label('Ø '+dimUnit(h.r*2),out,'dia')})}
function rebuildDims(){clearDims();if(!dimOn||!bbox)return;drawLinearDims();detectCircularDims();drawDiameterDims();const st=document.getElementById('proResult');if(st)st.innerHTML='<b>OTOMATİK ÖLÇÜLENDİRME</b><br>X/Y/Z dış boyutları ve '+diaDims.length+' çap adayı model üzerinde gösteriliyor.<br><span style="color:#9db4c8">Çaplar üçgenleştirilmiş dairesel sınır geometrisinden algılanır.</span>'}
function toggleDims(){dimOn=!dimOn;const b=document.getElementById('autoDimB');if(b){b.classList.toggle('on',dimOn);b.textContent=dimOn?'ÖLÇÜLERİ KAPAT':'ÖLÇÜLENDİR'}if(dimOn)rebuildDims();else clearDims()}

const style=document.createElement('style');style.textContent='.mgDimLabel{position:absolute;z-index:11;pointer-events:none;background:rgba(4,10,18,.92);color:#ffe16a;border:1px solid #d9b52e;border-radius:6px;padding:3px 6px;font:bold 12px Arial;white-space:nowrap;box-shadow:0 2px 8px rgba(0,0,0,.45)}#cadQuick{position:absolute;left:50%;transform:translateX(-50%);bottom:10px;z-index:10;display:flex;gap:6px;background:rgba(3,10,20,.93);border:1px solid #173c60;border-radius:12px;padding:6px;max-width:94%;overflow-x:auto}#cadQuick button,#cadQuick select{white-space:nowrap}';document.head.appendChild(style);
const quick=D('div');quick.id='cadQuick';quick.innerHTML='<button id="autoDimB">ÖLÇÜLENDİR</button><button id="quickSecB">KESİT</button><select id="quickAxis"><option>X</option><option>Y</option><option>Z</option></select><button id="secMinus">−</button><button id="secPlus">+</button>';document.body.appendChild(quick);
document.getElementById('autoDimB').onclick=toggleDims;
document.getElementById('quickSecB').onclick=()=>{document.getElementById('secAxis').value=document.getElementById('quickAxis').value;if(!sectionOn)toggleSection();else applySection();document.getElementById('quickSecB').classList.toggle('on',sectionOn)};
document.getElementById('quickAxis').onchange=e=>{document.getElementById('secAxis').value=e.target.value;if(sectionOn)applySection()};
function secStep(d){const s=document.getElementById('section');s.value=Math.max(0,Math.min(100,(+s.value)+d));if(!sectionOn)toggleSection();applySection();document.getElementById('quickSecB').classList.add('on')}
document.getElementById('secMinus').onclick=()=>secStep(-5);document.getElementById('secPlus').onclick=()=>secStep(5);
document.getElementById('unit').addEventListener('change',()=>{if(dimOn)setTimeout(rebuildDims,0)});
const oldReset=window.resetAll;window.resetAll=function(){if(typeof oldReset==='function')oldReset();if(dimOn)rebuildDims()};
(function loop(){requestAnimationFrame(loop);projectLabels()})();
window.MGAutoDimension={toggle:toggleDims,rebuild:rebuildDims};
})();'''
(AS/'cad-v170.js').write_text(js,encoding='utf-8')

p=AS/'index.html'
s=p.read_text(encoding='utf-8')
if '/cad-v170.js' not in s:
    s=s.replace('</body>','<script src="/cad-v170.js"></script></body>')
p.write_text(s,encoding='utf-8')
print('CAD v1.7 on-model auto dimension patch applied')
