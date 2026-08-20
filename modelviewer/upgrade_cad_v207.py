from pathlib import Path
AS=Path('modelviewer/src/main/assets/cadviewer')
html=AS/'index.html'
h=html.read_text(encoding='utf-8')
if '/cad-v207.js' not in h:
    h=h.replace('</body>','<script src="/cad-v207.js"></script></body>',1)
html.write_text(h,encoding='utf-8')

js=r'''(function(){
'use strict';
function txt(el){return ((el&&el.textContent)||'').trim().toUpperCase()}
function clean(){
  try{
    document.querySelectorAll('button').forEach(function(b){
      var t=txt(b);
      if(t==='MENÜ'||t==='MENU'||t==='PDF') b.remove();
    });
  }catch(e){}
}
new MutationObserver(clean).observe(document.documentElement,{subtree:true,childList:true});
setInterval(clean,500);clean();
window.MG_CAD_V207={version:'2.0.7',technicalMenuRemoved:true,technicalPdfRemoved:true,technicalExitPreserved:true};
})();'''
(AS/'cad-v207.js').write_text(js,encoding='utf-8')
print('v2.0.7: technical drawing MENU and PDF buttons removed')
