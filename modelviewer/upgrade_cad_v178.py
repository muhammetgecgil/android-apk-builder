from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
s=html.read_text(encoding='utf-8')
if '/cad-v178.js' not in s:
    s=s.replace('</body>','<script src="/cad-v178.js"></script></body>',1)
html.write_text(s,encoding='utf-8')
js=r'''(function(){
'use strict';
let mode=null,picks=[];
function meshes(){try{return group.children.filter(x=>x&&x.isMesh)}catch(e){return[]}}
function B(txt){return Array.from(document.querySelectorAll('button')).find(b=>b.textContent.trim().toUpperCase()===txt)}
function resultBox(){return document.getElementById('proResult')||document.querySelector('.small')}
function setMsg(h){const r=resultBox();if(r)r.innerHTML=h}
function mark(p,c=0xffd34d){try{const d=Math.max(baseDims.x,baseDims.y,baseDims.z,1);const g=new THREE.SphereGeometry(d*.006,12,8),m=new THREE.MeshBasicMaterial({color:c}),q=new THREE.Mesh(g,m);q.position.copy(p);scene.add(q);markers.push(q);return q}catch(e){}}
function hit(ev){try{const r=canvas.getBoundingClientRect();mouse.x=((ev.clientX-r.left)/r.width)*2-1;mouse.y=-((ev.clientY-r.top)/r.height)*2+1;ray.setFromCamera(mouse,camera);return ray.intersectObjects(meshes().filter(x=>x.visible),false)[0]||null}catch(e){return null}}
function U(v){try{return fmt(v)+' '+document.getElementById('unit').value}catch(e){return Number(v).toFixed(3)}}
function normal(h){let n=h.face?h.face.normal.clone():new THREE.Vector3(0,0,1);try{return n.applyMatrix3(new THREE.Matrix3().getNormalMatrix(h.object.matrixWorld)).normalize()}catch(e){return n}}
function clearMeasures(){picks=[];try{markers.splice(0).forEach(x=>scene.remove(x))}catch(e){};setMsg('Ölçüm işaretleri temizlendi.');mode=null;try{controls.enabled=true}catch(e){}}
function choose(m,msg){mode=m;picks=[];setMsg(msg);try{controls.enabled=false}catch(e){}}
function onPick(ev){if(!mode)return;const h=hit(ev);if(!h)return;ev.preventDefault();ev.stopImmediatePropagation();const p=h.point.clone(),n=normal(h);mark(p,mode==='thickness'?0x73ff9d:0xffd34d);
 if(mode==='probe'){setMsg('<b>PROB</b><br>X '+U(p.x)+' • Y '+U(p.y)+' • Z '+U(p.z)+'<br>Normal ['+n.x.toFixed(3)+', '+n.y.toFixed(3)+', '+n.z.toFixed(3)+']');return}
 if(mode==='thickness'){const D=Math.max(baseDims.x,baseDims.y,baseDims.z,1),eps=D*1e-5;const rr=new THREE.Raycaster(p.clone().add(n.clone().multiplyScalar(-eps*4)),n.clone().negate(),eps,D*3);const hs=rr.intersectObject(h.object,false).filter(z=>z.distance>eps*3);if(hs.length)setMsg('<b>KALINLIK ≈ '+U(hs[0].distance)+'</b><br>'+h.object.name);else setMsg('Karşı yüzey bulunamadı. Açık geometri olabilir.');return}
 picks.push(p);if(picks.length===1){setMsg('<b>1. NOKTA</b><br>X '+U(p.x)+' • Y '+U(p.y)+' • Z '+U(p.z)+'<br>İkinci noktayı seç.');return}
 const a=picks[picks.length-2],b=picks[picks.length-1],d=a.distanceTo(b),dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;setMsg('<b>MESAFE '+U(d)+'</b><br>ΔX '+U(dx)+' • ΔY '+U(dy)+' • ΔZ '+U(dz));picks=[];
}
function wireTools(){
 const two=B('2 NOKTA');if(two)two.onclick=()=>choose('two','İki yüzey noktası seç. Mesafe ve ΔX/ΔY/ΔZ hesaplanır.');
 const clr=B('TEMİZLE');if(clr)clr.onclick=clearMeasures;
 const smart=B('AKILLI ÖLÇ');if(smart)smart.onclick=()=>choose('smart','Akıllı ölç: iki nokta seç. Koordinat ve mesafe hesaplanır.');
 const thick=B('KALINLIK');if(thick)thick.onclick=()=>choose('thickness','Kalınlık: bir yüzeye dokun. Karşı yüzeye ray atılır.');
 const probe=B('PROB');if(probe)probe.onclick=()=>choose('probe','Prob: model üzerinde istediğin noktaya dokun. X/Y/Z gösterilir.');
 const holes=B('DELİKLER');if(holes){const old=holes.onclick;holes.onclick=()=>{setMsg('Dairesel sınırlar analiz ediliyor…');try{if(typeof detectHoles==='function'){detectHoles();setTimeout(()=>setMsg('<b>DELİKLER</b><br>'+(window.holeCandidates?window.holeCandidates.length:'Analiz tamamlandı')+' dairesel açıklık adayı.'),30)}else if(old)old();else setMsg('Delik motoru hazır değil.')}catch(e){if(old)old();else setMsg('Delik analizi hatası: '+e.message)}}}
 canvas.addEventListener('pointerup',onPick,true);
}
function installExplode(){
 const tools=document.getElementById('tools');if(!tools||document.getElementById('mgExplode'))return;
 const sep=document.createElement('div');sep.className='sep';tools.appendChild(sep);
 const box=document.createElement('div');box.id='mgExplode';box.innerHTML='<div class="head">PATLATILMIŞ GÖRÜNÜM</div><input id="explodeRange" type="range" min="0" max="100" value="0" style="width:100%"><div id="explodeTxt" class="small">0%</div>';tools.appendChild(box);
 const r=box.querySelector('#explodeRange'),t=box.querySelector('#explodeTxt');
 function apply(v){const ms=meshes();if(!ms.length)return;let c=new THREE.Vector3();ms.forEach(m=>{if(!m.userData.basePos)m.userData.basePos=m.position.clone();if(!m.userData.explodeCenter){m.geometry.computeBoundingBox();const cc=new THREE.Vector3();m.geometry.boundingBox.getCenter(cc);m.localToWorld(cc);m.userData.explodeCenter=cc.clone()}c.add(m.userData.explodeCenter)});c.multiplyScalar(1/ms.length);const D=Math.max(baseDims.x,baseDims.y,baseDims.z,1),k=(v/100)*D*.45;ms.forEach((m,i)=>{let dir=m.userData.explodeCenter.clone().sub(c);if(dir.length()<1e-6)dir=new THREE.Vector3((i%3)-1,((i+1)%3)-1,((i+2)%3)-1);dir.normalize();m.position.copy(m.userData.basePos).add(dir.multiplyScalar(k))});t.textContent=v+'%';}
 r.oninput=()=>apply(+r.value);r.onchange=()=>apply(+r.value);
}
function boot(){wireTools();installExplode();window.MG_CAD_V178={version:'1.7.8',twoPoint:true,clear:true,smart:true,thickness:true,holes:true,probe:true,exploded:true}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,150));else setTimeout(boot,150);
})();'''
(AS/'cad-v178.js').write_text(js,encoding='utf-8')
print('v1.7.8 engineering tools + exploded view patch applied')
