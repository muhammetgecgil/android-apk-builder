from pathlib import Path

p=Path('modelviewer/src/main/assets/cadviewer/index.html')
s=p.read_text(encoding='utf-8')

# Add selected-point pivot controls to the main CAD tools panel.
anchor='<div id="tools" class="panel"><div class="head">GÖRÜNÜM</div><div class="row"><button id="wireB" onclick="toggleWire()">TEL</button><button id="xrayB" onclick="toggleXray()">X-RAY</button><button onclick="toggleGrid()">GRID</button></div><div class="row"><button onclick="fitSelected()">SEÇİLENE SIĞDIR</button><button onclick="resetAll()">SIFIRLA</button></div><div class="sep"></div>'
replacement='<div id="tools" class="panel"><div class="head">GÖRÜNÜM</div><div class="row"><button id="wireB" onclick="toggleWire()">TEL</button><button id="xrayB" onclick="toggleXray()">X-RAY</button><button onclick="toggleGrid()">GRID</button></div><div class="row"><button onclick="fitSelected()">SEÇİLENE SIĞDIR</button><button onclick="resetAll()">SIFIRLA</button></div><div class="sep"></div><div class="head">DÖNME MERKEZİ</div><div class="row"><button id="pivotB" onclick="togglePivotPick()">PİVOT SEÇ</button><button onclick="resetPivot()">MERKEZE AL</button></div><div id="pivotInfo" class="small">Dönme merkezi: model merkezi</div><div class="sep"></div>'
if 'id="pivotB"' not in s:
    if anchor not in s: raise SystemExit('pivot UI anchor not found')
    s=s.replace(anchor,replacement,1)

old='let bbox=null,baseDims={x:0,y:0,z:0},partCount=0,triCount=0,totalArea=0,totalVolume=0,wire=false,xray=false,selected=null,sectionOn=false,measureOn=false,picks=[],markers=[],measureLine=null;'
new='let bbox=null,baseDims={x:0,y:0,z:0},partCount=0,triCount=0,totalArea=0,totalVolume=0,wire=false,xray=false,selected=null,sectionOn=false,measureOn=false,pivotPick=false,pivotPoint=null,pivotMarker=null,picks=[],markers=[],measureLine=null;'
if 'pivotPoint=null' not in s:
    if old not in s: raise SystemExit('pivot state anchor not found')
    s=s.replace(old,new,1)

hook='function hideSelected(){if(selected){selected.visible=false;selectMesh(null)}}'
functions='''function togglePivotPick(){pivotPick=!pivotPick;const b=document.getElementById('pivotB');if(b)b.classList.toggle('on',pivotPick);if(pivotPick&&measureOn){measureOn=false;const mb=document.getElementById('mB');if(mb)mb.classList.remove('on')}controls.enableRotate=!pivotPick;const i=document.getElementById('pivotInfo');if(i&&pivotPick)i.textContent='Model üzerinde dönme merkezini seç';}\nfunction setPivot(p){pivotPoint=p.clone();controls.target.copy(pivotPoint);camera.lookAt(pivotPoint);controls.update();if(pivotMarker){scene.remove(pivotMarker);pivotMarker.geometry.dispose();pivotMarker.material.dispose()}const r=Math.max(baseDims.x,baseDims.y,baseDims.z)*.012||.5;pivotMarker=new THREE.Mesh(new THREE.SphereGeometry(r,18,12),new THREE.MeshBasicMaterial({color:0xff5a36,depthTest:false}));pivotMarker.position.copy(pivotPoint);pivotMarker.renderOrder=999;scene.add(pivotMarker);const i=document.getElementById('pivotInfo');if(i)i.innerHTML='Pivot: X '+fmt(pivotPoint.x)+' • Y '+fmt(pivotPoint.y)+' • Z '+fmt(pivotPoint.z);}\nfunction resetPivot(){pivotPick=false;const b=document.getElementById('pivotB');if(b)b.classList.remove('on');if(pivotMarker){scene.remove(pivotMarker);pivotMarker.geometry.dispose();pivotMarker.material.dispose();pivotMarker=null}pivotPoint=null;controls.enableRotate=!measureOn;if(bbox){const c=new THREE.Vector3();bbox.getCenter(c);controls.target.copy(c);camera.lookAt(c);controls.update()}const i=document.getElementById('pivotInfo');if(i)i.textContent='Dönme merkezi: model merkezi';}\n'''
if 'function togglePivotPick()' not in s:
    if hook not in s: raise SystemExit('pivot function anchor not found')
    s=s.replace(hook,functions+hook,1)

old_click="canvas.addEventListener('click',ev=>{if(ev.clientY<58)return;const hit=pointer(ev);if(!hit.length)return;if(measureOn){addPick(hit[0].point);return}selectMesh(hit[0].object)});"
new_click="canvas.addEventListener('click',ev=>{if(ev.clientY<58)return;const hit=pointer(ev);if(!hit.length)return;if(pivotPick){setPivot(hit[0].point);pivotPick=false;const b=document.getElementById('pivotB');if(b)b.classList.remove('on');controls.enableRotate=true;return}if(measureOn){addPick(hit[0].point);return}selectMesh(hit[0].object)});"
if old_click in s:
    s=s.replace(old_click,new_click,1)
elif 'if(pivotPick){setPivot(hit[0].point)' not in s:
    raise SystemExit('pivot click anchor not found')

old_measure="function toggleMeasure(){measureOn=!measureOn;document.getElementById('mB').classList.toggle('on',measureOn);controls.enableRotate=!measureOn;if(!measureOn&&picks.length<2)clearMeasure()}"
new_measure="function toggleMeasure(){measureOn=!measureOn;document.getElementById('mB').classList.toggle('on',measureOn);if(measureOn&&pivotPick){pivotPick=false;const pb=document.getElementById('pivotB');if(pb)pb.classList.remove('on')}controls.enableRotate=!measureOn;if(!measureOn&&picks.length<2)clearMeasure()}"
if old_measure in s:
    s=s.replace(old_measure,new_measure,1)

old_reset="function resetAll(){showAll();selectMesh(null);document.getElementById('explode').value=0;applyExplode();if(sectionOn)toggleSection();if(xray)toggleXray();if(wire)toggleWire();clearMeasure();fit()}"
new_reset="function resetAll(){showAll();selectMesh(null);document.getElementById('explode').value=0;applyExplode();if(sectionOn)toggleSection();if(xray)toggleXray();if(wire)toggleWire();clearMeasure();resetPivot();fit()}"
if old_reset in s:
    s=s.replace(old_reset,new_reset,1)

marker="<script>window.MG_CAD_V2410={version:'2.4.1',pivotPointRotation:true,surfacePointPick:true,orbitTargetPivot:true,pivotMarker:true,pivotReset:true};</script>"
if 'MG_CAD_V2410' not in s:
    s=s.replace('</body></html>',marker+'</body></html>',1)

p.write_text(s,encoding='utf-8')
print('v2.4.1 selected-point pivot rotation applied')
