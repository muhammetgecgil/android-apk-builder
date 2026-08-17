from pathlib import Path

main=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
s=main.read_text(encoding='utf-8')
# Replace MainActivity startup with an immediate handoff to the professional CAD workspace.
old='@Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);buildUi();}'
new='@Override public void onCreate(Bundle b){super.onCreate(b);Intent i=new Intent(this,CadViewerActivity.class);if(getIntent()!=null&&getIntent().getData()!=null)i.setData(getIntent().getData());startActivity(i);finish();}'
if old not in s: raise SystemExit('MainActivity onCreate patch point not found')
s=s.replace(old,new,1)
main.write_text(s,encoding='utf-8')

cad=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/CadViewerActivity.java')
c=cad.read_text(encoding='utf-8')
# imports / request code
c=c.replace('public class CadViewerActivity extends Activity {','public class CadViewerActivity extends Activity {\n    private static final int OPEN_MODEL = 742;')
# Default to empty workspace, not fake STEP.
c=c.replace('if (modelName == null) modelName = "CAD Model";\n        if (modelType == null) modelType = "step";','if (modelName == null) modelName = "Model seçilmedi";\n        if (modelType == null) modelType = "none";')
# Bridge: same screen opens Android document picker.
c=c.replace('@JavascriptInterface public void closeViewer() { runOnUiThread(() -> finish()); }','@JavascriptInterface public void closeViewer() { runOnUiThread(() -> finish()); }\n            @JavascriptInterface public void openFileFromCad() { runOnUiThread(() -> openFilePicker()); }')
# model.bin must only be served if a model exists.
c=c.replace('if ("/model.bin".equals(p)) {\n                        InputStream in = getContentResolver().openInputStream(modelUri);\n                        return resp("application/octet-stream", in);\n                    }','if ("/model.bin".equals(p)) {\n                        if(modelUri==null) return new WebResourceResponse("application/octet-stream", "utf-8", new ByteArrayInputStream(new byte[0]));\n                        InputStream in = getContentResolver().openInputStream(modelUri);\n                        return resp("application/octet-stream", in);\n                    }')
# Add picker + reload methods before resp().
anchor='    private WebResourceResponse resp(String mime, InputStream in) {'
methods='''    private void openFilePicker() {\n        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);\n        i.addCategory(Intent.CATEGORY_OPENABLE);\n        i.setType("*/*");\n        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/step","model/iges","model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"});\n        startActivityForResult(i,OPEN_MODEL);\n    }\n    @Override protected void onActivityResult(int req,int res,Intent data){\n        super.onActivityResult(req,res,data);\n        if(req!=OPEN_MODEL||res!=RESULT_OK||data==null||data.getData()==null)return;\n        modelUri=data.getData();\n        try{getContentResolver().takePersistableUriPermission(modelUri,data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION));}catch(Throwable ignored){}\n        modelName=queryName(modelUri);\n        String l=modelName.toLowerCase(Locale.ROOT);\n        if(l.endsWith(".iges")||l.endsWith(".igs"))modelType="iges";\n        else if(l.endsWith(".brep")||l.endsWith(".brp"))modelType="brep";\n        else if(l.endsWith(".obj"))modelType="obj";\n        else if(l.endsWith(".stl"))modelType="stl";\n        else if(l.endsWith(".ply"))modelType="ply";\n        else modelType="step";\n        loadWorkspace();\n    }\n    private String queryName(Uri u){\n        try(android.database.Cursor cur=getContentResolver().query(u,null,null,null,null)){\n            if(cur!=null&&cur.moveToFirst()){int x=cur.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(x>=0)return cur.getString(x);}\n        }catch(Throwable ignored){}\n        return "model";\n    }\n    private void loadWorkspace(){\n        String url="https://mg3d.local/index.html?type="+Uri.encode(modelType)+"&name="+Uri.encode(modelName);\n        web.loadUrl(url);\n    }\n\n'''
if anchor not in c: raise SystemExit('CadViewerActivity anchor not found')
c=c.replace(anchor,methods+anchor,1)
# Replace direct URL construction with reusable loader.
oldurl='String url = "https://mg3d.local/index.html?type=" + Uri.encode(modelType) +\n                "&name=" + Uri.encode(modelName);\n        web.loadUrl(url);'
c=c.replace(oldurl,'loadWorkspace();')
cad.write_text(c,encoding='utf-8')

# Make empty CAD workspace not attempt parsing until a file has been selected.
html=Path('modelviewer/src/main/assets/cadviewer/index.html')
h=html.read_text(encoding='utf-8')
h=h.replace("async function main(){try{", "async function main(){try{if(fileType==='none'){document.getElementById('busy').style.display='none';document.getElementById('status').textContent='DOSYA AÇ ile STEP / IGES / BREP / OBJ / STL / PLY seç';document.getElementById('dims').innerHTML='<b>CAD ÇALIŞMA ALANI</b><br>Model yüklenmedi';return;}")
html.write_text(h,encoding='utf-8')
Path('modelviewer/src/main/assets/cadviewer/cad-v174.js').write_text("window.MG_CAD_V174={version:'1.7.4',legacyBlackScreen:false,directCadHome:true};\n",encoding='utf-8')
print('v1.7.4 direct CAD home patch applied')
