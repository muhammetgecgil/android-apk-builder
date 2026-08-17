from pathlib import Path

cad=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/CadViewerActivity.java')
c=cad.read_text(encoding='utf-8')
# Android document picker: allow all MIME types, then route by extension.
c=c.replace('i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/step","model/iges","model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"});','// */* remains intentionally: many CAD vendors report generic MIME types.');
# Preserve exact extension for broad importer routing.
old='''        if(l.endsWith(".iges")||l.endsWith(".igs"))modelType="iges";\n        else if(l.endsWith(".brep")||l.endsWith(".brp"))modelType="brep";\n        else if(l.endsWith(".obj"))modelType="obj";\n        else if(l.endsWith(".stl"))modelType="stl";\n        else if(l.endsWith(".ply"))modelType="ply";\n        else modelType="step";'''
new='''        int dot=l.lastIndexOf('.');\n        modelType=(dot>=0&&dot<l.length()-1)?l.substring(dot+1):"unknown";\n        if(modelType.equals("stp"))modelType="step";\n        else if(modelType.equals("igs"))modelType="iges";\n        else if(modelType.equals("brp"))modelType="brep";'''
if old in c:
    c=c.replace(old,new,1)
cad.write_text(c,encoding='utf-8')

html=Path('modelviewer/src/main/assets/cadviewer/index.html')
h=html.read_text(encoding='utf-8')
if '/assimpjs.js' not in h:
    h=h.replace('<script src="/occt-import-js.js"></script>','<script src="/occt-import-js.js"></script><script src="/assimpjs.js"></script>')

anchor='async function main(){try{'
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
    h=h.replace(anchor,helper+'\n'+anchor,1)

# v1.7.2 already handles OBJ/STL/PLY and sends all other formats to OpenCascade.
# Replace only that final CAD branch with STEP/IGES/BREP + Assimp fallback.
old_branch="""else{document.getElementById('status').textContent='OpenCascade WebAssembly yükleniyor…';const occt=await occtimportjs({locateFile:p=>'/'+p});document.getElementById('status').textContent='CAD topolojisi işleniyor…';const data=new Uint8Array(ab),params={linearUnit:'millimeter',linearDeflectionType:'bounding_box_ratio',linearDeflection:.0007,angularDeflection:.35};let result;if(fileType==='iges'||fileType==='igs')result=occt.ReadIgesFile(data,params);else if(fileType==='brep'||fileType==='brp')result=occt.ReadBrepFile(data,null);else result=occt.ReadStepFile(data,params);if(!result||!result.success)throw Error('CAD içe aktarma başarısız');(result.meshes||[]).forEach(addMesh)}finishUnified()"""
new_branch="""else if(fileType==='step'||fileType==='stp'||fileType==='iges'||fileType==='igs'||fileType==='brep'||fileType==='brp'){document.getElementById('status').textContent='OpenCascade WebAssembly yükleniyor…';const occt=await occtimportjs({locateFile:p=>'/'+p});document.getElementById('status').textContent='CAD topolojisi işleniyor…';const data=new Uint8Array(ab),params={linearUnit:'millimeter',linearDeflectionType:'bounding_box_ratio',linearDeflection:.0007,angularDeflection:.35};let result;if(fileType==='iges'||fileType==='igs')result=occt.ReadIgesFile(data,params);else if(fileType==='brep'||fileType==='brp')result=occt.ReadBrepFile(data,null);else result=occt.ReadStepFile(data,params);if(!result||!result.success)throw Error('CAD içe aktarma başarısız');(result.meshes||[]).forEach(addMesh)}else{await loadViaAssimp(ab)}finishUnified()"""
if old_branch not in h:
    raise SystemExit('v175 CAD branch patch point not found')
h=h.replace(old_branch,new_branch,1)
h=h.replace('DOSYA AÇ ile STEP / IGES / BREP / OBJ / STL / PLY seç','DOSYA AÇ • STEP/STP • IGES/IGS • BREP • OBJ • STL • PLY • GLTF/GLB • 3MF • DAE • FBX • DXF • X3D • OFF • IFC')
html.write_text(h,encoding='utf-8')

# Fix markup/pen: add visible floating pencil that toggles existing markup mode.
gen=Path('modelviewer/upgrade_cad_v160.py')
g=gen.read_text(encoding='utf-8')
needle="document.getElementById('markClear').onclick=()=>markupCtx.clearRect(0,0,innerWidth,innerHeight);"
insert="""document.getElementById('markClear').onclick=()=>markupCtx.clearRect(0,0,innerWidth,innerHeight);\nconst penFab=el('button',{id:'mgPenFab','aria-label':'Kalem işaretleme'},'✎');penFab.style.cssText='position:absolute;left:58px;bottom:270px;z-index:16;width:54px;height:54px;border-radius:50%;font-size:28px;padding:0;background:rgba(30,39,53,.92);box-shadow:0 3px 14px rgba(0,0,0,.45)';document.body.appendChild(penFab);penFab.onclick=()=>{document.getElementById('markB').click();penFab.classList.toggle('on',markupOn);};"""
if needle in g and 'mgPenFab' not in g:
    g=g.replace(needle,insert,1)
gen.write_text(g,encoding='utf-8')

Path('modelviewer/src/main/assets/cadviewer/cad-v175.js').write_text("window.MG_CAD_V175={version:'1.7.5',assimp:true,broadFormats:true,penFab:true};\n",encoding='utf-8')
print('v1.7.5 broad format + pen fix applied')
