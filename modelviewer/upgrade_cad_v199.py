from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v199.js' not in s:
    s=s.replace('</body>','<script src="/cad-v199.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
const E=id=>document.getElementById(id);let active=false;
function meshes(){try{return group.children.filter(o=>o&&o.isMesh&&!o.userData?.mgTechAux)}catch(e){return[]}}
function bbox(){try{return new THREE.Box3().setFromObject(group)}catch(e){return null}}
function worldNormal(mesh,A,B,C){const n=new THREE.Vector3().crossVectors(B.clone().sub(A),C.clone().sub(A)).normalize();const nm=new THREE.Matrix3().getNormalMatrix(mesh.matrixWorld);return n.applyMatrix3(nm).normalize()}
function edgeData(view){const out={vis:[],hid:[]};const dir=view.dir.clone().normalize();const bb=bbox(),sz=new THREE.Vector3();if(bb)bb.getSize(sz);const eps=Math.max(sz.x,sz.y,sz.z,1)*1e-5,th=Math.cos(32*Math.PI/180);
 meshes().forEach(mesh=>{try{const g=mesh.geometry,p=g&&g.attributes&&g.attributes.position;if(!p)return;mesh.updateMatrixWorld(true);const idx=g.index,tri=idx?Math.floor(idx.count/3):Math.floor(p.count/3),vi=(t,k)=>idx?idx.getX(t*3+k):t*3+k,vc=new Map(),wp=i=>{let v=vc.get(i);if(!v){v=new THREE.Vector3().fromBufferAttribute(p,i).applyMatrix4(mesh.matrixWorld);vc.set(i,v)}return v};const em=new Map(),q=v=>Math.round(v.x/eps)+','+Math.round(v.y/eps)+','+Math.round(v.z/eps);
 for(let t=0;t<tri;t++){const ia=vi(t,0),ib=vi(t,1),ic=vi(t,2),A=wp(ia),B=wp(ib),C=wp(ic),n=worldNormal(mesh,A.clone().applyMatrix4(new THREE.Matrix4().copy(mesh.matrixWorld).invert()),B.clone().applyMatrix4(new THREE.Matrix4().copy(mesh.matrixWorld).invert()),C.clone().applyMatrix4(new THREE.Matrix4().copy(mesh.matrixWorld).invert()));const front=n.dot(dir)<0;[[ia,ib],[ib,ic],[ic,ia]].forEach(([a,b])=>{const P=wp(a),Q=wp(b),ka=q(P),kb=q(Q),k=ka<kb?ka+'|'+kb:kb+'|'+ka;let e=em.get(k);if(!e){e={A:P.clone(),B:Q.clone(),f:[]};em.set(k,e)}e.f.push({n:n.clone(),front})})}
 em.forEach(e=>{let keep=false,sil=false;if(e.f.length===1){keep=true;sil=true}else{const a=e.f[0],b=e.f[1];sil=a.front!==b.front;keep=sil||a.n.dot(b.n)<th}if(!keep)return;(sil||e.f.some(f=>f.front)?out.vis:out.hid).push([e.A,e.B])})}catch(err){}});return out}
function views(){return [
 {k:'front',name:'ÖN GÖRÜNÜŞ',dir:new THREE.Vector3(0,0,1),u:v=>v.x,v:v=>v.y,dim:'X × Y'},
 {k:'top',name:'ÜST GÖRÜNÜŞ',dir:new THREE.Vector3(0,1,0),u:v=>v.x,v:v=>-v.z,dim:'X × Z'},
 {k:'right',name:'SAĞ YAN GÖRÜNÜŞ',dir:new THREE.Vector3(1,0,0),u:v=>-v.z,v:v=>v.y,dim:'Z × Y'},
 {k:'left',name:'SOL YAN GÖRÜNÜŞ',dir:new THREE.Vector3(-1,0,0),u:v=>v.z,v:v=>v.y,dim:'Z × Y'},
 {k:'back',name:'ARKA GÖRÜNÜŞ',dir:new THREE.Vector3(0,0,-1),u:v=>-v.x,v:v=>v.y,dim:'X × Y'},
 {k:'bottom',name:'ALT GÖRÜNÜŞ',dir:new THREE.Vector3(0,-1,0),u:v=>v.x,v:v=>v.z,dim:'X × Z'},
 {k:'iso',name:'İZOMETRİK GÖRÜNÜŞ',dir:new THREE.Vector3(1,1,1),u:v=>(v.x-v.z)*.7071,v:v=>v.y*.8165-(v.x+v.z)*.4082,dim:''}
 ]}
function fmt(x){return (Math.round(x*1000)/1000).toFixed(3)}
function line(x1,y1,x2,y2,cls){return '<line x1="'+x1.toFixed(1)+'" y1="'+y1.toFixed(1)+'" x2="'+x2.toFixed(1)+'" y2="'+y2.toFixed(1)+'" class="'+cls+'"/>'}
function drawView(v,x,y,w,h){const r=edgeData(v),pts=[];r.vis.concat(r.hid).forEach(e=>{pts.push([v.u(e[0]),v.v(e[0])],[v.u(e[1]),v.v(e[1])])});if(!pts.length)return '';let mnx=1e99,mxx=-1e99,mny=1e99,mxy=-1e99;pts.forEach(p=>{mnx=Math.min(mnx,p[0]);mxx=Math.max(mxx,p[0]);mny=Math.min(mny,p[1]);mxy=Math.max(mxy,p[1])});let dx=Math.max(mxx-mnx,1),dy=Math.max(mxy-mny,1),sc=Math.min((w-32)/dx,(h-44)/dy),ox=x+(w-dx*sc)/2-mnx*sc,oy=y+(h-dy*sc)/2+mxy*sc;const cv=p=>[ox+v.u(p)*sc,oy-v.v(p)*sc];let s='<g><text x="'+(x+w/2)+'" y="'+(y+15)+'" class="ttl">'+v.name+'</text>';r.hid.forEach(e=>{let a=cv(e[0]),b=cv(e[1]);s+=line(a[0],a[1],b[0],b[1],'hid')});r.vis.forEach(e=>{let a=cv(e[0]),b=cv(e[1]);s+=line(a[0],a[1],b[0],b[1],'vis')});const bb=bbox(),sz=new THREE.Vector3();if(bb)bb.getSize(sz);if(v.k==='front'||v.k==='back')s+='<text x="'+(x+w/2)+'" y="'+(y+h-5)+'" class="dim">'+fmt(sz.x)+' × '+fmt(sz.y)+' mm</text>';else if(v.k==='top'||v.k==='bottom')s+='<text x="'+(x+w/2)+'" y="'+(y+h-5)+'" class="dim">'+fmt(sz.x)+' × '+fmt(sz.z)+' mm</text>';else if(v.k==='right'||v.k==='left')s+='<text x="'+(x+w/2)+'" y="'+(y+h-5)+'" class="dim">'+fmt(sz.z)+' × '+fmt(sz.y)+' mm</text>';s+='</g>';return s}
function sheet(){let o=E('mgSheet199');if(o)o.remove();o=document.createElement('div');o.id='mgSheet199';o.style.cssText='position:fixed;left:0;right:0;top:62px;bottom:0;z-index:58;background:#fff;overflow:auto;padding:6px';const V=views(),W=1400,H=900,cells=[[0,0],[1,0],[2,0],[0,1],[1,1],[2,1],[1,2]],cw=450,ch=270;let svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 '+W+' '+H+'" style="width:100%;height:auto;background:white"><style>.vis{stroke:#111;stroke-width:1.7;fill:none}.hid{stroke:#666;stroke-width:1.15;stroke-dasharray:8 5;fill:none}.ttl{font:700 18px sans-serif;text-anchor:middle;fill:#111}.dim{font:14px sans-serif;text-anchor:middle;fill:#333}.note{font:14px sans-serif;fill:#222}.border{stroke:#222;stroke-width:1;fill:none}</style><rect x="5" y="5" width="1390" height="890" class="border"/>';
 V.forEach((v,i)=>{let c=cells[i];svg+=drawView(v,20+c[0]*460,35+c[1]*275,cw,ch)});const bb=bbox(),sz=new THREE.Vector3();if(bb)bb.getSize(sz);svg+='<text x="30" y="850" class="note">MG CAD PRO • TÜM GÖRÜNÜŞLER • Birim: mm • Dış ölçü: '+fmt(sz.x)+' × '+fmt(sz.y)+' × '+fmt(sz.z)+' mm</text><text x="30" y="875" class="note">Düz çizgi: görünür kenar • Kesikli çizgi: görünmeyen/arka kenar • Mesh üçgenleri gösterilmez.</text></svg>';
 o.innerHTML='<div style="position:sticky;top:0;z-index:2;display:flex;gap:8px;justify-content:flex-end;padding:4px;background:#fff"><button id="mg199Menu" style="padding:10px 16px">MENÜ</button><button id="mg199Exit" style="padding:10px 16px">TEKNİK RESİMDEN ÇIK</button></div>'+svg;document.body.appendChild(o);E('mg199Exit').onclick=()=>exit();E('mg199Menu').onclick=()=>{const p=E('mgDrawing197');if(p)p.style.display=p.style.display==='none'?'block':'none'};active=true}
function enter(){try{const p=E('mgDrawing197');if(p)p.style.display='none'}catch(e){}sheet()}
function exit(){active=false;const o=E('mgSheet199');if(o)o.remove();try{if(window.MG_CAD_V197&&typeof window.MG_CAD_V197.exit==='function')window.MG_CAD_V197.exit()}catch(e){}}
function hook(){const bs=[...document.querySelectorAll('button')].filter(x=>/^TEKNİK\s*RESİM$/i.test((x.textContent||'').trim()));if(!bs.length)return false;bs.forEach(b=>{b.dataset.mg199='1';b.onclick=(e)=>{try{e.preventDefault();e.stopPropagation()}catch(_){}active?exit():enter()}});return true}
const mo=new MutationObserver(()=>hook());mo.observe(document.documentElement,{subtree:true,childList:true});let n=0,t=setInterval(()=>{n++;if(hook()||n>30)clearInterval(t)},300);window.MG_CAD_V199={version:'1.9.9',singleSheetAllViews:true,frontTopRightLeftBackBottomIso:true,hiddenEdgesDashed:true,noMeshTriangles:true};
})();'''
(AS/'cad-v199.js').write_text(js,encoding='utf-8')
print('v1.9.9 single-sheet all-view technical drawing')
