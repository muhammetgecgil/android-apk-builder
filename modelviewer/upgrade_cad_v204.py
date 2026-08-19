from pathlib import Path
import re

AS=Path('modelviewer/src/main/assets/cadviewer')

# 1) Technical drawing toolbar + main CAD UI cleanup.
p=AS/'cad-v199.js'
s=p.read_text(encoding='utf-8')
# Add PDF button next to technical drawing exit and remove dead MENU control where present.
s=s.replace("<button id=\"techMenuB\"", "<button id=\"techMenuB\" style=\"display:none\"", 1)
s=s.replace("TEKNİK RESİMDEN ÇIK</button>", "PDF</button><button onclick=\"exitTechDrawing()\">TEKNİK RESİMDEN ÇIK</button>", 1) if "TEKNİK RESİMDEN ÇIK</button>" in s and ">PDF</button>" not in s else s
# If exact markup differs, inject PDF button before any visible technical-drawing exit button.
s=re.sub(r'(<button[^>]*>TEKNİK RESİMDEN ÇIK</button>)', r'<button onclick="mgTechPdf()">PDF</button>\1', s, count=1) if 'mgTechPdf()' not in s else s
# PDF/print function: Android host when available, browser print fallback.
if 'function mgTechPdf()' not in s:
    s += "\nfunction mgTechPdf(){try{if(window.AndroidHost&&AndroidHost.printTechnicalDrawing){AndroidHost.printTechnicalDrawing();return;}window.print();}catch(e){try{window.print()}catch(_){}}}\n"
# Feature marker.
s += "\nwindow.MG_CAD_V204={version:'2.0.4',technicalPdf:true,deadMenuRemoved:true,sectionButtonRemoved:true,analysisDockedBottom:true,cadPickerStrict:true};\n"
p.write_text(s,encoding='utf-8')

# 2) Runtime UI patch: remove KESİT command, close remaining bottom buttons together, dock analysis panel low.
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
h=h.replace('#info{left:8px;bottom:118px;', '#info{left:8px;bottom:8px;', 1)
if '/cad-v204.js' not in h:
    h=h.replace('</body>','<script src="/cad-v204.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function clean(){
  try{
    document.querySelectorAll('button').forEach(function(b){
      var t=(b.textContent||'').trim().toUpperCase();
      if(t==='KESİT'||t==='KESİT AÇ'||t==='KESİT KAPAT') b.style.display='none';
      if(t==='MENÜ' && document.body.classList.contains('mg-tech-sheet')) b.style.display='none';
    });
    var info=document.getElementById('info');if(info){info.style.bottom='8px';info.style.maxHeight='36vh';info.style.overflowY='auto';info.style.touchAction='pan-y';}
  }catch(e){}
}
setInterval(clean,700);clean();
window.mgTechPdf=window.mgTechPdf||function(){try{if(window.AndroidHost&&AndroidHost.printTechnicalDrawing){AndroidHost.printTechnicalDrawing();}else{window.print();}}catch(e){window.print();}};
})();'''
(AS/'cad-v204.js').write_text(js,encoding='utf-8')

# 3) Native file chooser: narrow advertised MIME types and reject unsupported extensions after selection.
j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
t=j.read_text(encoding='utf-8')
# Replace broad picker variants when present. Avoid application/octet-stream so APK/JPG do not flood the picker.
pat=r'void openFile\(\)\{Intent i=new Intent\(Intent\.ACTION_OPEN_DOCUMENT\);.*?startActivityForResult\(i,OPEN_MODEL\);\}'
rep='void openFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("model/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/step","model/stp","model/iges","model/igs","model/stl","model/obj","model/3mf","model/gltf+json","model/gltf-binary","application/zip","application/x-zip-compressed","application/gzip","application/x-gzip","application/x-tar","application/vnd.collada+xml","model/vnd.collada+xml"});i.putExtra(Intent.EXTRA_TITLE,"CAD MODEL: STEP STP IGES IGS BREP OBJ STL PLY GLTF GLB 3MF DAE FBX DXF X3D OFF IFC ZIP");startActivityForResult(i,OPEN_MODEL);}'
t2,n=re.subn(pat,rep,t,count=1,flags=re.S)
if n: t=t2
# Add extension guard to onActivityResult before processing URI.
needle='Uri u=data.getData();if(u==null)return;'
if needle in t and 'Desteklenmeyen dosya türü' not in t:
    guard='Uri u=data.getData();if(u==null)return;String fn=displayName(u).toLowerCase(Locale.ROOT);if(!(fn.endsWith(".step")||fn.endsWith(".stp")||fn.endsWith(".iges")||fn.endsWith(".igs")||fn.endsWith(".brep")||fn.endsWith(".brp")||fn.endsWith(".obj")||fn.endsWith(".stl")||fn.endsWith(".ply")||fn.endsWith(".gltf")||fn.endsWith(".glb")||fn.endsWith(".3mf")||fn.endsWith(".dae")||fn.endsWith(".fbx")||fn.endsWith(".dxf")||fn.endsWith(".x3d")||fn.endsWith(".off")||fn.endsWith(".ifc")||fn.endsWith(".zip")||fn.endsWith(".gz")||fn.endsWith(".tgz")||fn.endsWith(".tar"))){Toast.makeText(this,"Desteklenmeyen dosya türü",Toast.LENGTH_SHORT).show();return;}'
    t=t.replace(needle,guard,1)
j.write_text(t,encoding='utf-8')
print('v2.0.4: PDF technical drawing, menu removed, strict CAD picker, section removed, analysis docked bottom')
