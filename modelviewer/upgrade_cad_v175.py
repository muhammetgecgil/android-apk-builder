from pathlib import Path
import re

cad=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/CadViewerActivity.java')
c=cad.read_text(encoding='utf-8')
# Android document picker: keep */* so vendor-specific CAD MIME types are visible.
c=c.replace('i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/step","model/iges","model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"});','// */* remains intentionally: many CAD vendors report generic MIME types.')
# Preserve the exact selected extension for broad importer routing. If an older mapping exists, replace it.
pat=r'''\s*if\(l\.endsWith\("\.iges"\)\|\|l\.endsWith\("\.igs"\)\)modelType="iges";\s*else if\(l\.endsWith\("\.brep"\)\|\|l\.endsWith\("\.brp"\)\)modelType="brep";\s*else if\(l\.endsWith\("\.obj"\)\)modelType="obj";\s*else if\(l\.endsWith\("\.stl"\)\)modelType="stl";\s*else if\(l\.endsWith\("\.ply"\)\)modelType="ply";\s*else modelType="step";'''
rep='''\n        int dot=l.lastIndexOf('.');\n        modelType=(dot>=0&&dot<l.length()-1)?l.substring(dot+1):"unknown";\n        if(modelType.equals("stp"))modelType="step";\n        else if(modelType.equals("igs"))modelType="iges";\n        else if(modelType.equals("brp"))modelType="brep";'''
c,n=re.subn(pat,rep,c,count=1)
# v1.7.4 may already have extension extraction. In that case leave it intact.
if n==0 and 'int dot=l.lastIndexOf' not in c:
    marker='String l=modelName.toLowerCase(Locale.ROOT);'
    if marker in c:
        c=c.replace(marker,marker+rep,1)
cad.write_text(c,encoding='utf-8')

html=Path('modelviewer/src/main/assets/cadviewer/index.html')
h=html.read_text(encoding='utf-8')
if '/assimpjs.js' not in h:
    h=h.replace('<script src="/occt-import-js.js"></script>','<script src="/occt-import-js.js"></script><script src="/assimpjs.js"></script>',1)

helper=r'''
function addAssimpMesh(am,mi){
  if(!am||!am.vertices||am.vertices.length<9)return;
  const pos=Array.from(am.vertices),faces=am.faces||[],idx=[];
  faces.forEach(f=>{const a=Array.isArray(f)?f:(f.indices||f);if(!a||a.length<3)return;for(let i=1;i<a.length-1;i++)idx.push(a[0],a[i],a[i+1])});
  const g=new THREE.BufferGeometry();g.setAttribute('position',new THREE.Float32BufferAttribute(pos,3));if(idx.length)g.setIndex(idx);
  if(am.normals&&am.normals.length===pos.length)g.setAttribute('normal',new THREE.Float32BufferAttribute(Array.from(am.normals),3));else g.computeVertexNormals();
  const mat=new THREE.MeshStandardMaterial({color:0x69aee8,metalness:.12,roughness:.58,side:THREE.DoubleSide,clippingPlanes:[]});
  const mesh=new THREE.Mesh(g,mat);mesh.name=am.name||('Part '+(mi+1));g.computeBoundingBox();g.boundingBox.getCenter(mesh.userData.center=new THREE.Vector3());mesh.userData.basePos=mesh.position.clone();
  const met=meshMetrics(g);mesh.userData.area=met.area;mesh.userData.volume=met.vol;totalArea+=met.area;totalVolume+=met.vol;triCount+=idx.length?Math.floor(idx.length/3):Math.floor(pos.length/9);group.add(mesh);partCount++;
}
async function loadViaAssimp(data){
  if(typeof assimpjs!=='function')throw Error('Genel format motoru yüklenemedi');
  document.getElementById('status').textContent='Assimp çoklu-format motoru hazırlanıyor…';
  const ajs=await assimpjs({locateFile:p=>'/'+p});
  const fl=new ajs.FileList();fl.AddFile(fileName,new Uint8Array(data));
  const result=ajs.ConvertFileList(fl,'assjson');
  if(!result.IsSuccess()||result.FileCount()===0)throw Error('Bu dosya formatı içe aktarılamadı');
  const rf=result.GetFile(0),txt=new TextDecoder().decode(rf.GetContent()),j=JSON.parse(txt);
  (j.meshes||[]).forEach(addAssimpMesh);
  if(group.children.length===0)throw Error('Görüntülenecek geometri bulunamadı');
}
'''
if 'async function loadViaAssimp' not in h:
    start=h.find('async function main(){')
    if start<0: raise SystemExit('main function not found')
    h=h[:start]+helper+'\n'+h[start:]

# Replace the entire main routine by position instead of fragile exact-string matching.
start=h.find('async function main(){')
end=h.find('main();',start)
if start<0 or end<0: raise SystemExit('main routine boundaries not found')
end+=len('main();')
main=r'''async function main(){try{
  if(fileType==='none'){
    document.getElementById('busy').style.display='none';
    document.getElementById('status').textContent='DOSYA AÇ • STEP/STP • IGES/IGS • BREP • OBJ • STL • PLY • GLTF/GLB • 3MF • DAE • FBX • DXF • X3D • OFF • IFC';
    document.getElementById('dims').innerHTML='<b>CAD ÇALIŞMA ALANI</b><br>Model yüklenmedi';
    return;
  }
  const ab=await fetch('/model.bin',{cache:'no-store'}).then(r=>{if(!r.ok)throw Error('Dosya okunamadı');return r.arrayBuffer()});
  if(fileType==='obj'){
    document.getElementById('status').textContent='OBJ geometrisi işleniyor…';
    parseObjUnified(ab);
  }else if(fileType==='stl'){
    document.getElementById('status').textContent='STL geometrisi işleniyor…';
    parseStlUnified(ab);
  }else if(fileType==='ply'){
    document.getElementById('status').textContent='PLY geometrisi işleniyor…';
    parsePlyUnified(ab);
  }else if(fileType==='step'||fileType==='stp'||fileType==='iges'||fileType==='igs'||fileType==='brep'||fileType==='brp'){
    document.getElementById('status').textContent='OpenCascade WebAssembly yükleniyor…';
    const occt=await occtimportjs({locateFile:p=>'/'+p});
    document.getElementById('status').textContent='CAD topolojisi işleniyor…';
    const data=new Uint8Array(ab),params={linearUnit:'millimeter',linearDeflectionType:'bounding_box_ratio',linearDeflection:.0007,angularDeflection:.35};
    let result;
    if(fileType==='iges'||fileType==='igs')result=occt.ReadIgesFile(data,params);
    else if(fileType==='brep'||fileType==='brp')result=occt.ReadBrepFile(data,null);
    else result=occt.ReadStepFile(data,params);
    if(!result||!result.success)throw Error('CAD içe aktarma başarısız');
    (result.meshes||[]).forEach(addMesh);
  }else{
    await loadViaAssimp(ab);
  }
  finishUnified();
}catch(e){
  document.getElementById('busy').style.display='none';
  document.getElementById('status').textContent='Hata: '+(e&&e.message?e.message:e);
  document.getElementById('dims').textContent='Geometri açılamadı';
  console.error(e);
}}
main();'''
h=h[:start]+main+h[end:]
h=h.replace('DOSYA AÇ ile STEP / IGES / BREP / OBJ / STL / PLY seç','DOSYA AÇ • STEP/STP • IGES/IGS • BREP • OBJ • STL • PLY • GLTF/GLB • 3MF • DAE • FBX • DXF • X3D • OFF • IFC')
if '/cad-v175.js' not in h:
    h=h.replace('</body>','<script src="/cad-v175.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

# Runtime pen FAB; avoids patching older generator scripts.
AS=Path('modelviewer/src/main/assets/cadviewer')
(AS/'cad-v175.js').write_text(r'''(function(){
function installPen(){
  if(document.getElementById('mgPenFab'))return;
  const mark=document.getElementById('markB');
  if(!mark)return;
  const b=document.createElement('button');
  b.id='mgPenFab'; b.textContent='✎'; b.setAttribute('aria-label','Kalem işaretleme');
  b.style.cssText='position:absolute;left:58px;bottom:270px;z-index:18;width:54px;height:54px;border-radius:50%;font-size:28px;padding:0;background:rgba(30,39,53,.94);box-shadow:0 3px 14px rgba(0,0,0,.45)';
  b.onclick=function(){mark.click();b.classList.toggle('on',mark.classList.contains('on'));};
  document.body.appendChild(b);
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',installPen);else installPen();
window.MG_CAD_V175={version:'1.7.5',assimp:true,broadFormats:true,penFab:true};
})();
''',encoding='utf-8')
print('v1.7.5 broad format + pen fix applied')
