from pathlib import Path

# Route every supported geometry format to the same professional viewer.
p=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
s=p.read_text(encoding='utf-8')
s=s.replace('if(isCadExt(l)){runOnUiThread(()->openCadViewer(u,n,l));return;}', 'if(isUnifiedExt(l)){runOnUiThread(()->openCadViewer(u,n,l));return;}')
old='boolean isCadExt(String l){return l.endsWith(".step")||l.endsWith(".stp")||l.endsWith(".iges")||l.endsWith(".igs")||l.endsWith(".brep")||l.endsWith(".brp");}'
new='boolean isCadExt(String l){return l.endsWith(".step")||l.endsWith(".stp")||l.endsWith(".iges")||l.endsWith(".igs")||l.endsWith(".brep")||l.endsWith(".brp");}\n boolean isUnifiedExt(String l){return isCadExt(l)||l.endsWith(".obj")||l.endsWith(".stl")||l.endsWith(".ply");}'
s=s.replace(old,new)
old2='String t=(l.endsWith(".iges")||l.endsWith(".igs"))?"iges":((l.endsWith(".brep")||l.endsWith(".brp"))?"brep":"step");'
new2='String t=(l.endsWith(".iges")||l.endsWith(".igs"))?"iges":((l.endsWith(".brep")||l.endsWith(".brp"))?"brep":(l.endsWith(".obj")?"obj":(l.endsWith(".stl")?"stl":(l.endsWith(".ply")?"ply":"step"))));'
s=s.replace(old2,new2)
p.write_text(s,encoding='utf-8')

# Replace CAD-only loader with a unified CAD/mesh loader.
p=Path('modelviewer/src/main/assets/cadviewer/index.html')
s=p.read_text(encoding='utf-8')
old="""async function main(){try{document.getElementById('status').textContent='OpenCascade WebAssembly yükleniyor…';const occt=await occtimportjs({locateFile:p=>'/'+p});document.getElementById('status').textContent='CAD topolojisi işleniyor…';const ab=await fetch('/model.bin',{cache:'no-store'}).then(r=>{if(!r.ok)throw Error('Dosya okunamadı');return r.arrayBuffer()});const data=new Uint8Array(ab),params={linearUnit:'millimeter',linearDeflectionType:'bounding_box_ratio',linearDeflection:.0007,angularDeflection:.35};let result;if(fileType==='iges'||fileType==='igs')result=occt.ReadIgesFile(data,params);else if(fileType==='brep'||fileType==='brp')result=occt.ReadBrepFile(data,null);else result=occt.ReadStepFile(data,params);if(!result||!result.success)throw Error('CAD içe aktarma başarısız');(result.meshes||[]).forEach(addMesh);if(group.children.length===0)throw Error('Görüntülenecek yüzey bulunamadı');bbox=new THREE.Box3().setFromObject(group);const sz=new THREE.Vector3();bbox.getSize(sz);baseDims={x:sz.x,y:sz.y,z:sz.z};const max=Math.max(sz.x,sz.y,sz.z,1);grid.scale.setScalar(max/10);axes.scale.setScalar(max/10);buildParts();fit();updateInfo();document.getElementById('status').textContent=fileType.toUpperCase()+' • '+partCount+' parça • CAD Pro • cihaz içinde işlendi';document.getElementById('busy').style.display='none'}catch(e){document.getElementById('busy').style.display='none';document.getElementById('status').textContent='Hata: '+(e&&e.message?e.message:e);document.getElementById('dims').textContent='CAD dosyası açılamadı';console.error(e)}}main();"""
new=r"""
function addRawPositions(pos,name){addMesh({name:name||'Part 1',attributes:{position:{array:pos}}})}
function parseObjUnified(ab){const txt=new TextDecoder('utf-8').decode(ab),vs=[],out=[];for(const raw of txt.split(/\r?\n/)){const l=raw.trim();if(l.startsWith('v ')){const a=l.split(/\s+/);if(a.length>=4)vs.push([+a[1],+a[2],+a[3]])}else if(l.startsWith('f ')){const a=l.slice(2).trim().split(/\s+/),ids=a.map(x=>{let n=parseInt(x.split('/')[0],10);return n<0?vs.length+n:n-1});for(let i=1;i<ids.length-1;i++)for(const id of [ids[0],ids[i],ids[i+1]]){const v=vs[id];if(v)out.push(v[0],v[1],v[2])}}}if(out.length<9)throw Error('OBJ yüzeyi bulunamadı');addRawPositions(out,'OBJ Geometry')}
function parseStlUnified(ab){const u=new Uint8Array(ab),dv=new DataView(ab),out=[];if(u.length>=84){const n=dv.getUint32(80,true);if(84+n*50===u.length){let o=84;for(let i=0;i<n;i++){o+=12;for(let k=0;k<9;k++,o+=4)out.push(dv.getFloat32(o,true));o+=2}addRawPositions(out,'STL Geometry');return}}const txt=new TextDecoder('utf-8').decode(ab);for(const m of txt.matchAll(/vertex\s+([-+0-9.eE]+)\s+([-+0-9.eE]+)\s+([-+0-9.eE]+)/g))out.push(+m[1],+m[2],+m[3]);if(out.length<9)throw Error('STL yüzeyi bulunamadı');addRawPositions(out,'STL Geometry')}
function parsePlyUnified(ab){const txt=new TextDecoder('utf-8').decode(ab),lines=txt.split(/\r?\n/);let vc=0,fc=0,end=-1,ascii=false;for(let i=0;i<lines.length;i++){const l=lines[i].trim();if(l.startsWith('format ascii'))ascii=true;else if(l.startsWith('element vertex'))vc=parseInt(l.split(/\s+/)[2]);else if(l.startsWith('element face'))fc=parseInt(l.split(/\s+/)[2]);else if(l==='end_header'){end=i;break}}if(!ascii||end<0)throw Error('Bu sürümde ASCII PLY destekleniyor');const vs=[];for(let i=0;i<vc;i++){const a=lines[end+1+i].trim().split(/\s+/);vs.push([+a[0],+a[1],+a[2]])}const out=[];for(let i=0;i<fc;i++){const a=lines[end+1+vc+i].trim().split(/\s+/).map(Number),n=a[0];if(n<3)continue;for(let k=2;k<n;k++)for(const id of [a[1],a[k],a[k+1]]){const v=vs[id];if(v)out.push(v[0],v[1],v[2])}}if(out.length<9)throw Error('PLY yüzeyi bulunamadı');addRawPositions(out,'PLY Geometry')}
function finishUnified(){if(group.children.length===0)throw Error('Görüntülenecek geometri bulunamadı');bbox=new THREE.Box3().setFromObject(group);const sz=new THREE.Vector3();bbox.getSize(sz);baseDims={x:sz.x,y:sz.y,z:sz.z};const max=Math.max(sz.x,sz.y,sz.z,1);grid.scale.setScalar(max/10);axes.scale.setScalar(max/10);buildParts();fit();updateInfo();document.getElementById('status').textContent=fileType.toUpperCase()+' • '+partCount+' parça • Unified CAD Pro • cihaz içinde işlendi';document.getElementById('busy').style.display='none'}
async function main(){try{const ab=await fetch('/model.bin',{cache:'no-store'}).then(r=>{if(!r.ok)throw Error('Dosya okunamadı');return r.arrayBuffer()});if(fileType==='obj'){document.getElementById('status').textContent='OBJ geometrisi işleniyor…';parseObjUnified(ab)}else if(fileType==='stl'){document.getElementById('status').textContent='STL geometrisi işleniyor…';parseStlUnified(ab)}else if(fileType==='ply'){document.getElementById('status').textContent='PLY geometrisi işleniyor…';parsePlyUnified(ab)}else{document.getElementById('status').textContent='OpenCascade WebAssembly yükleniyor…';const occt=await occtimportjs({locateFile:p=>'/'+p});document.getElementById('status').textContent='CAD topolojisi işleniyor…';const data=new Uint8Array(ab),params={linearUnit:'millimeter',linearDeflectionType:'bounding_box_ratio',linearDeflection:.0007,angularDeflection:.35};let result;if(fileType==='iges'||fileType==='igs')result=occt.ReadIgesFile(data,params);else if(fileType==='brep'||fileType==='brp')result=occt.ReadBrepFile(data,null);else result=occt.ReadStepFile(data,params);if(!result||!result.success)throw Error('CAD içe aktarma başarısız');(result.meshes||[]).forEach(addMesh)}finishUnified()}catch(e){document.getElementById('busy').style.display='none';document.getElementById('status').textContent='Hata: '+(e&&e.message?e.message:e);document.getElementById('dims').textContent='Geometri açılamadı';console.error(e)}}main();"""
if old not in s:
    raise SystemExit('Unified loader patch point not found')
s=s.replace(old,new,1)
p.write_text(s,encoding='utf-8')

# UI: reserve a dedicated header strip for CAD collapse button, never on top of X-RAY.
AS=Path('modelviewer/src/main/assets/cadviewer')
js=r'''(function(){
'use strict';
const st=document.createElement('style');
st.textContent=`
#tools{padding-top:52px!important;}
#mgPanelTab{top:74px!important;right:calc(var(--mg-safe-r) + 8px)!important;min-width:84px!important;height:38px!important;padding:6px 10px!important;z-index:16!important;}
#tools>.head:first-child{margin-top:0!important;}
@media(max-width:900px){#tools{padding-top:50px!important}#mgPanelTab{height:36px!important;min-width:78px!important;font-size:12px!important}}
`;
document.head.appendChild(st);
const tab=document.getElementById('mgPanelTab');if(tab)tab.title='CAD araç panelini aç/kapat';
window.MGUnifiedGeometryViewer={version:'1.7.2',formats:['STEP','STP','IGES','IGS','BREP','BRP','OBJ','STL','PLY']};
})();'''
(AS/'cad-v172.js').write_text(js,encoding='utf-8')
p=AS/'index.html';s=p.read_text(encoding='utf-8')
if '/cad-v172.js' not in s:s=s.replace('</body>','<script src="/cad-v172.js"></script></body>')
p.write_text(s,encoding='utf-8')
print('CAD v1.7.2 unified viewer + non-overlap tab patch applied')
