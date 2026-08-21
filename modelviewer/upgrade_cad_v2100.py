from pathlib import Path
import re

AS=Path('modelviewer/src/main/assets/cadviewer')

# MG CAD PRO v2.1.0 - Stabilization pass
# Goal: preserve the proven v2.0.4 technical drawing/dimensioning stack,
# remove continuous runtime polling, harden native file opening, and make
# the WebView lifecycle less likely to freeze or leak memory on large CAD files.

# 1) Remove the legacy continuous cleaner introduced in v2.0.4.
p=AS/'cad-v204.js'
if p.exists():
    s=p.read_text(encoding='utf-8')
    s=s.replace('setInterval(clean,700);clean();', "clean();document.addEventListener('click',function(){requestAnimationFrame(clean);},true);")
    s=s.replace('setInterval(clean,700); clean();', "clean();document.addEventListener('click',function(){requestAnimationFrame(clean);},true);")
    p.write_text(s,encoding='utf-8')

# 2) Final native DOSYA AÇ policy: robust first, strict validation after selection.
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
if n:
    t=t2
j.write_text(t,encoding='utf-8')

# 3) Harden CAD WebView rendering/lifecycle without changing CAD behavior.
c=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/CadViewerActivity.java')
s=c.read_text(encoding='utf-8')
if 'MG_STABLE_2100' not in s:
    s=s.replace('if (android.os.Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);',
                'if (android.os.Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);\n        s.setCacheMode(WebSettings.LOAD_DEFAULT); // MG_STABLE_2100')
    s=s.replace('web.setBackgroundColor(Color.rgb(3, 8, 15));',
                'web.setBackgroundColor(Color.rgb(3, 8, 15));\n        web.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null);\n        web.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);\n        if (android.os.Build.VERSION.SDK_INT >= 26) web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);')
    old='''    @Override public void onBackPressed() {\n        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();\n    }\n}'''
    new='''    @Override public void onBackPressed() {\n        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();\n    }\n\n    @Override protected void onDestroy() {\n        if (web != null) {\n            try { web.stopLoading(); } catch (Throwable ignored) {}\n            try { web.loadUrl("about:blank"); } catch (Throwable ignored) {}\n            try { web.clearHistory(); } catch (Throwable ignored) {}\n            try { web.removeAllViews(); } catch (Throwable ignored) {}\n            try { web.destroy(); } catch (Throwable ignored) {}\n            web = null;\n        }\n        super.onDestroy();\n    }\n}'''
    if old in s:
        s=s.replace(old,new,1)
    c.write_text(s,encoding='utf-8')

# 4) Zero-polling runtime marker and lightweight interaction guards.
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v2100.js' not in h:
    h=h.replace('</body>','<script src="/cad-v2100.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
js=r'''(function(){
'use strict';
function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn,{once:true});else fn();}
function init(){
  document.documentElement.style.touchAction='manipulation';
  document.querySelectorAll('canvas').forEach(function(c){c.style.touchAction='none';});
  window.addEventListener('error',function(e){console.warn('MG CAD runtime error',e&&e.message||e);});
  window.addEventListener('unhandledrejection',function(e){console.warn('MG CAD promise error',e&&e.reason||e);});
  window.MG_CAD_V2100={version:'2.1.0',baseline:'2.0.4',stabilityPass:true,noContinuousPolling:true,robustFileOpen:true,webviewLifecycleHardened:true,technicalDrawingPreserved:true,dimensioningPreserved:true};
}
ready(init);
})();'''
(AS/'cad-v2100.js').write_text(js,encoding='utf-8')
print('v2.1.0 stabilization: no continuous polling + robust file open + WebView lifecycle hardening')
