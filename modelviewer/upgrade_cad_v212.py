from pathlib import Path
import re

AS=Path('modelviewer/src/main/assets/cadviewer')

# Stable recovery strategy:
# Keep the proven CAD/technical-drawing stack only through v2.0.4.
# Do not add periodic MutationObservers/timers here. Later v205-v211 runtime layers
# are intentionally not applied by the workflow for this build.

# 1) Preserve v2.0.4 technical drawing + ISO dimensioning, but repair its toolbar.
p=AS/'cad-v199.js'
s=p.read_text(encoding='utf-8')
# Replace the single-sheet sticky toolbar with only a working exit button.
pat=r"o\.innerHTML='<div style=\"position:sticky;top:0;z-index:2;display:flex;gap:8px;justify-content:flex-end;padding:4px;background:#fff\">.*?</div>'\+svg;"
rep="o.innerHTML='<div style=\"position:sticky;top:0;z-index:2;display:flex;gap:8px;justify-content:flex-end;padding:4px;background:#fff\"><button id=\"mg199Menu\" style=\"display:none\"></button><button id=\"mg199Exit\" style=\"padding:10px 16px\">TEKNİK RESİMDEN ÇIK</button></div>'+svg;"
s2,n=re.subn(pat,rep,s,count=1,flags=re.S)
if n:
    s=s2
# Remove any v2.0.4 PDF injections that may remain elsewhere in the generated sheet code.
s=re.sub(r'<button[^>]*>PDF</button>','',s)
# Never call a non-existent exitTechDrawing() from the technical sheet.
s=s.replace('onclick="exitTechDrawing()"','')
# Mark stable recovery.
s += "\nwindow.MG_CAD_V212={version:'2.0.12',baseline:'2.0.4',stableTechnicalDrawing:true,isoDimensioningPreserved:true,noLaterRuntimeObservers:true,techExitWorks:true};\n"
p.write_text(s,encoding='utf-8')

# 2) Robust DOSYA AÇ without any JS polling/runtime hooks.
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
if n==0:
    raise SystemExit('openFile patch point missing')
t=t2
j.write_text(t,encoding='utf-8')

# 3) Zero-cost build marker only; no timers/observers.
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v212.js' not in h:
    h=h.replace('</body>','<script src="/cad-v212.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
(AS/'cad-v212.js').write_text("window.MG_CAD_V212_RUNTIME={stable:true,baseline:'2.0.4',noPolling:true};\n",encoding='utf-8')
print('v2.0.12 stable: v2.0.4 technical drawing/dimensioning baseline + robust native file open')
