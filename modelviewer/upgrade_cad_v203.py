from pathlib import Path

AS=Path('modelviewer/src/main/assets/cadviewer')
p=AS/'cad-v199.js'
s=p.read_text(encoding='utf-8')

# Improve hidden-line classification using ray occlusion from the current orthographic view direction.
needle="function edgeData(view){const out={vis:[],hid:[]};const dir=view.dir.clone().normalize();"
insert="""function edgeOccluded(A,B,dir){try{const bb=bbox();if(!bb)return false;const sz=new THREE.Vector3();bb.getSize(sz);const span=Math.max(sz.x,sz.y,sz.z,1),eps=span*1e-4,mid=A.clone().add(B).multiplyScalar(.5);const origin=mid.clone().add(dir.clone().normalize().multiplyScalar(span*2.5));const rr=new THREE.Raycaster(origin,dir.clone().normalize().negate(),0,span*5);const hits=rr.intersectObjects(meshes(),false);const target=span*2.5;return hits.length>0&&hits[0].distance<target-eps}catch(e){return false}}
function edgeData(view){const out={vis:[],hid:[]};const dir=view.dir.clone().normalize();"""
if needle not in s: raise SystemExit('edgeData patch point missing')
s=s.replace(needle,insert,1)
old="(sil||e.f.some(f=>f.front)?out.vis:out.hid).push([e.A,e.B])"
new="(edgeOccluded(e.A,e.B,dir)?out.hid:out.vis).push([e.A,e.B])"
if old not in s: raise SystemExit('edge classification patch point missing')
s=s.replace(old,new,1)
old="axisTriadPerView:true,isoStyleDimensionLines:true,extensionLines:true,arrowheads:true,viewSpecificDimensionPlacement:true};"
new="axisTriadPerView:true,isoStyleDimensionLines:true,extensionLines:true,arrowheads:true,viewSpecificDimensionPlacement:true,trueHiddenLineOcclusion:true};"
if old not in s: raise SystemExit('feature marker patch point missing')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')

# Main 3D workspace: move analysis card above the bottom command bar and make its content vertically scrollable.
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
h=h.replace('#info{left:8px;bottom:8px;min-width:245px;max-width:330px}', '#info{left:8px;bottom:118px;min-width:245px;max-width:360px;max-height:36vh;overflow-y:auto;overflow-x:hidden;touch-action:pan-y;overscroll-behavior:contain;padding-right:12px}',1)
if '/cad-v203.js' not in h:
    h=h.replace('</body>','<script src="/cad-v203.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
let triad=null,legend=null,lastSize=0;
function spriteLabel(text,color,scale){const c=document.createElement('canvas');c.width=128;c.height=64;const x=c.getContext('2d');x.clearRect(0,0,128,64);x.font='bold 42px Arial';x.textAlign='center';x.textBaseline='middle';x.lineWidth=8;x.strokeStyle='white';x.strokeText(text,64,32);x.fillStyle=color;x.fillText(text,64,32);const tx=new THREE.CanvasTexture(c);const m=new THREE.SpriteMaterial({map:tx,transparent:true,depthTest:false,depthWrite:false});const s=new THREE.Sprite(m);s.scale.set(scale*1.25,scale*.62,1);s.renderOrder=999;return s}
function rebuild(){try{if(!bbox||bbox.isEmpty())return;const sz=new THREE.Vector3(),ctr=new THREE.Vector3();bbox.getSize(sz);bbox.getCenter(ctr);const L=Math.max(sz.x,sz.y,sz.z,1)*.16;if(Math.abs(L-lastSize)<1e-6&&triad)return;lastSize=L;if(triad)scene.remove(triad);triad=new THREE.Group();triad.userData.mgAxisLabels=true;const o=bbox.min.clone();const mk=(to,col)=>{const g=new THREE.BufferGeometry().setFromPoints([new THREE.Vector3(0,0,0),to]);const m=new THREE.LineBasicMaterial({color:col,depthTest:false});const ln=new THREE.Line(g,m);ln.renderOrder=998;triad.add(ln)};mk(new THREE.Vector3(L,0,0),0xd62828);mk(new THREE.Vector3(0,L,0),0x2a9d46);mk(new THREE.Vector3(0,0,L),0x1565c0);const sx=spriteLabel('X','#d62828',L*.22),sy=spriteLabel('Y','#2a9d46',L*.22),szp=spriteLabel('Z','#1565c0',L*.22);sx.position.set(L*1.12,0,0);sy.position.set(0,L*1.12,0);szp.position.set(0,0,L*1.12);triad.add(sx,sy,szp);triad.position.copy(o);scene.add(triad)}catch(e){}}
function addLegend(){if(document.getElementById('mgAxisLegend203'))return;legend=document.createElement('div');legend.id='mgAxisLegend203';legend.style.cssText='position:absolute;left:14px;top:72px;z-index:9;background:rgba(3,10,20,.82);border:1px solid #245582;border-radius:9px;padding:6px 9px;font:bold 13px Arial;pointer-events:none';legend.innerHTML='<span style="color:#ff4b4b">X</span> <span style="color:#9db4c8">kırmızı</span> &nbsp; <span style="color:#56d364">Y</span> <span style="color:#9db4c8">yeşil</span> &nbsp; <span style="color:#4aa3ff">Z</span> <span style="color:#9db4c8">mavi</span>';document.body.appendChild(legend)}
addLegend();setInterval(rebuild,500);
window.MG_CAD_V203={version:'2.0.3',xyzLabelsIn3D:true,axisColorLegend:true,analysisPanelRaised:true,analysisVerticalScroll:true,trueHiddenLineOcclusion:true};
})();'''
(AS/'cad-v203.js').write_text(js,encoding='utf-8')
print('v2.0.3: XYZ labels, true hidden-line occlusion, raised scrollable analysis panel')
