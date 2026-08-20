from pathlib import Path
import re

j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
t=j.read_text(encoding='utf-8')

# Make DOSYA AÇ reliable on Samsung/Android SAF while keeping the picker CAD/archive focused.
pat=r'void openFile\(\)\{Intent i=new Intent\(Intent\.ACTION_OPEN_DOCUMENT\);.*?startActivityForResult\(i,OPEN_MODEL\);\}'
rep='''void openFile(){
 Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
 i.addCategory(Intent.CATEGORY_OPENABLE);
 i.setType("*/*");
 i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
 i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{
  "model/step","model/stp","application/step","application/x-step",
  "model/iges","model/igs","application/iges","application/x-iges",
  "model/stl","application/sla","model/obj","model/3mf",
  "application/vnd.ms-package.3dmanufacturing-3dmodel+xml",
  "model/gltf+json","model/gltf-binary","application/vnd.collada+xml",
  "application/zip","application/x-zip-compressed","application/gzip","application/x-gzip","application/x-tar"
 });
 i.putExtra(Intent.EXTRA_TITLE,"CAD MODEL SEÇ: STEP STP IGES IGS BREP OBJ STL PLY GLTF GLB 3MF DAE FBX DXF X3D OFF IFC ZIP");
 try{startActivityForResult(i,OPEN_MODEL);}catch(android.content.ActivityNotFoundException e){
  Intent f=new Intent(Intent.ACTION_GET_CONTENT);
  f.addCategory(Intent.CATEGORY_OPENABLE);
  f.setType("*/*");
  startActivityForResult(Intent.createChooser(f,"CAD MODEL SEÇ"),OPEN_MODEL);
 }
}'''
t2,n=re.subn(pat,rep,t,count=1,flags=re.S)
if n==0:
    raise SystemExit('openFile() patch point not found')
t=t2

# v2.0.4 already injects an extension guard. Replace that whole guard instead of
# inserting a second String fn variable (which would make Java compilation fail).
old_guard=r'Uri u=data\.getData\(\);if\(u==null\)return;String fn=displayName\(u\)\.toLowerCase\(Locale\.ROOT\);if\(!\(.*?\)\)\{Toast\.makeText\(this,"Desteklenmeyen dosya türü",Toast\.LENGTH_SHORT\)\.show\(\);return;\}'
new_guard='''Uri u=data.getData();if(u==null)return;String fn=displayName(u).toLowerCase(Locale.ROOT);final String MG_SUPPORTED_CAD_EXTENSIONS=".step .stp .iges .igs .brep .brp .obj .stl .ply .gltf .glb .3mf .dae .fbx .dxf .x3d .off .ifc .zip .gz .tgz .tar";boolean ok=false;for(String ex:MG_SUPPORTED_CAD_EXTENSIONS.split(" ")){if(fn.endsWith(ex)){ok=true;break;}}if(!ok){Toast.makeText(this,"Bu dosya CAD/model veya desteklenen arşiv değil",Toast.LENGTH_LONG).show();return;}'''
t2,n2=re.subn(old_guard,new_guard,t,count=1,flags=re.S)
if n2==0:
    # Base source may not contain the older v2.0.4 guard; insert exactly one clean guard.
    needle='Uri u=data.getData();if(u==null)return;'
    if needle not in t:
        raise SystemExit('onActivityResult URI patch point not found')
    t=t.replace(needle,new_guard,1)
else:
    t=t2

j.write_text(t,encoding='utf-8')

# Feature marker for CI verification.
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v210.js' not in h:
    h=h.replace('</body>','<script src="/cad-v210.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
(AS/'cad-v210.js').write_text("window.MG_CAD_V210={version:'2.0.10',fileOpenReliable:true,safFallback:true,cadExtensionGuard:true,singleExtensionGuard:true};\n",encoding='utf-8')
print('v2.0.10: DOSYA AÇ reliable SAF picker + fallback + single CAD extension guard')
