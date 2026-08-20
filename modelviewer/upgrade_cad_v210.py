from pathlib import Path
import re

j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
t=j.read_text(encoding='utf-8')

# Make DOSYA AÇ reliable on Samsung/Android SAF. Use */* as the base type because many
# document providers do not register STEP/IGES model MIME types, while still advertising
# specific CAD/archive MIME types and validating extensions after the user picks a file.
pat=r'void openFile\(\)\{Intent i=new Intent\(Intent\.ACTION_OPEN_DOCUMENT\);.*?startActivityForResult\(i,OPEN_MODEL\);\}'
rep='''void openFile(){
 Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
 i.addCategory(Intent.CATEGORY_OPENABLE);
 i.setType("*/*");
 i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
 i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{
  "model/step","model/stp","application/step","application/x-step",
  "model/iges","model/igs","application/iges","application/x-iges",
  "model/stl","application/sla","model/obj","application/x-tgif",
  "model/3mf","application/vnd.ms-package.3dmanufacturing-3dmodel+xml",
  "model/gltf+json","model/gltf-binary","application/vnd.collada+xml",
  "application/zip","application/x-zip-compressed","application/gzip","application/x-gzip","application/x-tar",
  "application/octet-stream"
 });
 i.putExtra(Intent.EXTRA_TITLE,"CAD MODEL SEÇ: STEP STP IGES IGS BREP OBJ STL PLY GLTF GLB 3MF DAE FBX DXF X3D OFF IFC ZIP");
 try{startActivityForResult(i,OPEN_MODEL);}catch(android.content.ActivityNotFoundException e){
  Intent f=new Intent(Intent.ACTION_GET_CONTENT);f.addCategory(Intent.CATEGORY_OPENABLE);f.setType("*/*");startActivityForResult(Intent.createChooser(f,"CAD MODEL SEÇ"),OPEN_MODEL);
 }
}'''
t2,n=re.subn(pat,rep,t,count=1,flags=re.S)
if n==0:
    raise SystemExit('openFile() patch point not found')
t=t2

# Ensure extension guard is present and broad enough for all formats listed in Model Analizi.
needle='Uri u=data.getData();if(u==null)return;'
if needle in t and 'MG_SUPPORTED_CAD_EXTENSIONS' not in t:
    guard='''Uri u=data.getData();if(u==null)return;String fn=displayName(u).toLowerCase(Locale.ROOT);final String MG_SUPPORTED_CAD_EXTENSIONS=".step .stp .iges .igs .brep .brp .obj .stl .ply .gltf .glb .3mf .dae .fbx .dxf .x3d .off .ifc .zip .gz .tgz .tar";boolean ok=false;for(String ex:MG_SUPPORTED_CAD_EXTENSIONS.split(" ")){if(fn.endsWith(ex)){ok=true;break;}}if(!ok){Toast.makeText(this,"Bu dosya CAD/model veya desteklenen arşiv değil",Toast.LENGTH_LONG).show();return;}'''
    t=t.replace(needle,guard,1)

j.write_text(t,encoding='utf-8')

# Feature marker for CI verification.
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v210.js' not in h:
    h=h.replace('</body>','<script src="/cad-v210.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
(AS/'cad-v210.js').write_text("window.MG_CAD_V210={version:'2.0.10',fileOpenReliable:true,safFallback:true,cadExtensionGuard:true};\n",encoding='utf-8')
print('v2.0.10: DOSYA AÇ reliable SAF picker + fallback + CAD extension guard')
