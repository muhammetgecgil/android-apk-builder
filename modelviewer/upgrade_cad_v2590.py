from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2590.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2590.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(f){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',f,{once:true}):f()}
const ORANGE=0xff9f43;
let dimGeom=[];
function span(){return Math.max(baseDims.x||1,baseDims.y||1,baseDims.z||1,1)}
function unit(v){const f=typeof unitFactor==='function'?unitFactor():1,u=document.getElementById('unit')?.value||'mm';return (v*f).toFixed(Math.abs(v*f)>=100?2:3)+' '+u}
function dispose(o){if(!o)return;scene.remove(o);if(o.geometry)o.geometry.dispose();if(o.material){const a=Array.isArray(o.material)?o.material:[o.material];a.forEach(m=>{if(m.map)m.map.dispose();m.dispose&&m.dispose()})}}
function clearDim(){dimGeom.forEach(dispose);dimGeom=[]}
function add(o){o.renderOrder=1650;scene.add(o);dimGeom.push(o);return o}
function line(a,b,c=ORANGE){return add(new THREE.Line(new THREE.BufferGeometry().setFromPoints([a,b]),new THREE.LineBasicMaterial({color:c,depthTest:false})))}
function hitAtXY(x,y){const r=canvas.getBoundingClientRect(),m=new THREE.Vector2(((x-r.left)/r.width)*2-1,-((y-r.top)/r.height)*2+1),rr=new THREE.Raycaster();rr.setFromCamera(m,camera);return rr.intersectObjects(group.children.filter(o=>o.visible),true)[0]||null}
function hitAt(ev){return hitAtXY(ev.clientX,ev.clientY)}
function centerHit(){const r=canvas.getBoundingClientRect();let h=hitAtXY(r.left+r.width/2,r.top+r.height/2);if(h)return h;const vis=group.children.filter(o=>o.visible);if(!vis.length)return null;const b=new THREE.Box3();vis.forEach(o=>b.expandByObject(o));const target=new THREE.Vector3();b.getCenter(target);const d=target.clone().sub(camera.position).normalize(),rr=new THREE.Raycaster(camera.position.clone(),d,0,1e9);return rr.intersectObjects(vis,true)[0]||null}
function opposite(hit){if(!hit||!hit.face)return null;const n=hit.face.normal.clone().transformDirection(hit.object.matrixWorld).normalize(),s=span(),eps=s*1e-5;function shoot(dir){const rr=new THREE.Raycaster(hit.point.clone().addScaledVector(dir,eps*8),dir,eps*4,s*4);const hs=rr.intersectObjects(group.children.filter(o=>o.visible),true).filter(x=>x.distance>eps*4);return hs.length?hs[0]:null}const a=shoot(n.clone().negate()),b=shoot(n.clone());if(a&&b)return a.distance<=b.distance?a:b;return a||b}
function arrowTip(tip,dir,size){const side=new THREE.Vector3();const view=new THREE.Vector3();camera.getWorldDirection(view);side.crossVectors(dir,view);if(side.lengthSq()<1e-8)side.crossVectors(dir,new THREE.Vector3(0,1,0));if(side.lengthSq()<1e-8)side.crossVectors(dir,new THREE.Vector3(1,0,0));side.normalize();const back=tip.clone().addScaledVector(dir,-size),w=size*.48;line(tip,back.clone().addScaledVector(side,w));line(tip,back.clone().addScaledVector(side,-w))}
function label(p,text){const cv=document.createElement('canvas');cv.width=1024;cv.height=256;const x=cv.getContext('2d');x.clearRect(0,0,1024,256);x.fillStyle='rgba(3,10,20,.94)';x.fillRect(18,28,988,200);x.strokeStyle='#ff9f43';x.lineWidth=12;x.strokeRect(18,28,988,200);x.fillStyle='#fff';x.font='bold 68px Arial';x.textAlign='center';x.textBaseline='middle';x.fillText(text,512,128);const tex=new THREE.CanvasTexture(cv),m=new THREE.SpriteMaterial({map:tex,depthTest:false,transparent:true}),s=new THREE.Sprite(m);s.position.copy(p);const z=span()*.19;s.scale.set(z*3.0,z*.72,1);add(s)}
function drawDimension(a,b,n){clearDim();const s=span(),measureDir=b.clone().sub(a).normalize();const view=new THREE.Vector3();camera.getWorldDirection(view);let offsetDir=measureDir.clone().cross(view);if(offsetDir.lengthSq()<1e-8)offsetDir=measureDir.clone().cross(n);if(offsetDir.lengthSq()<1e-8)offsetDir=measureDir.clone().cross(new THREE.Vector3(0,1,0));if(offsetDir.lengthSq()<1e-8)offsetDir=measureDir.clone().cross(new THREE.Vector3(1,0,0));offsetDir.normalize();const off=s*.075,ext=s*.018;const da=a.clone().addScaledVector(offsetDir,off),db=b.clone().addScaledVector(offsetDir,off);line(a,da.clone().addScaledVector(offsetDir,ext));line(b,db.clone().addScaledVector(offsetDir,ext));line(da,db);const as=Math.min(s*.028,Math.max(a.distanceTo(b)*.18,s*.012));arrowTip(da,measureDir,as);arrowTip(db,measureDir.clone().negate(),as);const mid=da.clone().add(db).multiplyScalar(.5).addScaledVector(offsetDir,s*.018);label(mid,'KALINLIK  '+unit(a.distanceTo(b)))}
function measure(h){if(!h||!h.face)return;const o=opposite(h);if(!o)return;const n=h.face.normal.clone().transformDirection(h.object.matrixWorld).normalize();drawDimension(h.point.clone(),o.point.clone(),n)}
function thickActive(){return !!document.getElementById('mgThick2560')?.classList.contains('mgm-active')}
function install(){const tb=document.getElementById('mgThick2560'),cb=document.getElementById('mgClear2560');if(tb)tb.addEventListener('click',()=>{setTimeout(()=>{if(thickActive())measure(centerHit());else clearDim()},30)});if(cb)cb.addEventListener('click',clearDim);canvas.addEventListener('click',ev=>{if(!thickActive()||ev.clientY<58)return;const h=hitAt(ev);if(h)setTimeout(()=>measure(h),0)},true);window.MG_CAD_V2590={version:'2.5.9',baseline:'2.5.5',caliperEngineeringDimension:true,edgeToEdgeDimension:true,extensionLines:true,inwardArrowheads:true,onModelThicknessLabel:true}}
ready(install)
})();'''
(AS/'cad-v2590.js').write_text(js,encoding='utf-8')
print('v2.5.9 caliper thickness with engineering edge-to-edge dimensioning')
