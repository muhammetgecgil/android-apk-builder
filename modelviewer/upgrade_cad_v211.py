from pathlib import Path
import re

j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
t=j.read_text(encoding='utf-8')

pat=r'void openFile\(\)\{.*?\}\s*@Override protected void onActivityResult'
rep='''void openFile(){
 try{
  Intent i=new Intent(Intent.ACTION_GET_CONTENT);
  i.addCategory(Intent.CATEGORY_OPENABLE);
  i.setType("*/*");
  i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  startActivityForResult(Intent.createChooser(i,"CAD / MODEL DOSYASI SEÇ"),OPEN_MODEL);
 }catch(Throwable first){
  try{
   Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
   i.addCategory(Intent.CATEGORY_OPENABLE);
   i.setType("*/*");
   i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
   startActivityForResult(i,OPEN_MODEL);
  }catch(Throwable second){
   Toast.makeText(this,"Dosya seçici açılamadı",Toast.LENGTH_LONG).show();
  }
 }
}
@Override protected void onActivityResult'''

t2,n=re.subn(pat,rep,t,count=1,flags=re.S)
if n==0: raise SystemExit('openFile patch point missing')
t=t2

# Keep post-selection extension validation only; chooser itself stays maximally compatible.
needle='Uri u=data.getData();if(u==null)return;'
if needle in t and 'MG_V211_EXT' not in t:
 guard='''Uri u=data.getData();if(u==null)return;String fn=displayName(u).toLowerCase(Locale.ROOT);final String MG_V211_EXT=".step .stp .iges .igs .brep .brp .obj .stl .ply .gltf .glb .3mf .dae .fbx .dxf .x3d .off .ifc .zip .gz .tgz .tar";boolean mgok=false;for(String ex:MG_V211_EXT.split(" ")){if(fn.endsWith(ex)){mgok=true;break;}}if(!mgok){Toast.makeText(this,"Desteklenen CAD/model/arşiv dosyası seçin",Toast.LENGTH_LONG).show();return;}'''
 t=t.replace(needle,guard,1)

j.write_text(t,encoding='utf-8')

AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v211.js' not in h:
 h=h.replace('</body>','<script src="/cad-v211.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
(AS/'cad-v211.js').write_text("window.MG_CAD_V211={version:'2.0.11',fileOpenGetContentPrimary:true,openDocumentFallback:true,extensionValidation:true};\n",encoding='utf-8')
print('v2.0.11 robust DOSYA AÇ applied')
