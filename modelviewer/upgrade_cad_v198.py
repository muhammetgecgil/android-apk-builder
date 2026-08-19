from pathlib import Path

# Compatibility patch retained for v1.9.9. It must never abort a later build
# just because an older source variant has already been upgraded.
AS=Path('modelviewer/src/main/assets/cadviewer')
p=AS/'cad-v197.js'
if p.exists():
    s=p.read_text(encoding='utf-8')
    old="p.querySelectorAll('[data-v]').forEach(b=>b.onclick=()=>setView(b.dataset.v));"
    new="p.querySelectorAll('[data-v]').forEach(b=>b.onclick=()=>{setView(b.dataset.v);hidePanel()});"
    if old in s: s=s.replace(old,new,1)
    old="E('d197Dim').onclick=()=>{try{const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}catch(e){}};"
    new="E('d197Dim').onclick=()=>{try{const b=E('autoDimB');if(b&&!b.classList.contains('on'))b.click();else if(window.MGAutoDimension&&MGAutoDimension.rebuild)MGAutoDimension.rebuild()}catch(e){}hidePanel()};"
    if old in s: s=s.replace(old,new,1)
    old="function hook(){const b=[...document.querySelectorAll('button')].find(x=>/^TEKNİK\\s*RESİM$/i.test((x.textContent||'').trim()));if(!b||b.dataset.mg197)return false;b.dataset.mg197='1';b.onclick=(ev)=>{try{ev&&ev.stopPropagation()}catch(e){}active?exit():enter()};return true}"
    new="function hook(){const bs=[...document.querySelectorAll('button')].filter(x=>/^TEKNİK\\s*RESİM$/i.test((x.textContent||'').trim()));let hooked=0;bs.forEach(b=>{if(b.dataset.mg198)return;b.dataset.mg197='1';b.dataset.mg198='1';b.onclick=(ev)=>{try{ev&&ev.preventDefault();ev&&ev.stopPropagation()}catch(e){}if(active)exit();else enter()};hooked++});return hooked>0||bs.length>0}"
    if old in s: s=s.replace(old,new,1)
    old="window.MG_CAD_V197={version:'1.9.7',classicTechnicalDrawing:true,weldedFeatureEdges:true,noTriangleMeshLines:true,visibleSolidHiddenDashed:true,menuCloseOnly:true,exitViaTechnicalDrawingButton:true};"
    new="window.MG_CAD_V197={version:'1.9.8',classicTechnicalDrawing:true,weldedFeatureEdges:true,noTriangleMeshLines:true,visibleSolidHiddenDashed:true,menuCloseOnly:true,exitViaTechnicalDrawingButton:true,viewSelectionClosesMenu:true,menuShownOnEveryEnter:true,technicalDrawingToggle:true};"
    if old in s: s=s.replace(old,new,1)
    p.write_text(s,encoding='utf-8')

j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
if j.exists():
    t=j.read_text(encoding='utf-8')
    candidates=[
      'void openFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/step","model/iges","model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"});startActivityForResult(i,OPEN_MODEL);}',
      'void openFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"});startActivityForResult(i,OPEN_MODEL);}'
    ]
    repl='void openFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/octet-stream","model/step","model/iges","model/obj","model/stl","application/sla","application/x-ply","application/zip","application/x-zip-compressed"});i.putExtra(Intent.EXTRA_TITLE,"CAD: STEP STP IGES IGS BREP BRP OBJ STL PLY ZIP");startActivityForResult(i,OPEN_MODEL);}'
    for old in candidates:
        if old in t:
            t=t.replace(old,repl,1); break
    old='String n=displayName(u),l=n.toLowerCase(Locale.ROOT);if(isCadExt(l)){runOnUiThread(()->openCadViewer(u,n,l));return;}Mesh m;if(l.endsWith(".obj"))'
    new='String n=displayName(u),l=n.toLowerCase(Locale.ROOT);if(!(isCadExt(l)||l.endsWith(".obj")||l.endsWith(".stl")||l.endsWith(".ply")||l.endsWith(".zip")))throw new Exception("Desteklenmeyen dosya. Açılabilir: STEP STP IGES IGS BREP BRP OBJ STL PLY ZIP");if(l.endsWith(".zip"))throw new Exception("ZIP seçildi. Arşiv içindeki CAD dosyasını çıkartıp STEP/STP/IGES/IGS/BREP/BRP/OBJ/STL/PLY olarak aç.");if(isCadExt(l)){runOnUiThread(()->openCadViewer(u,n,l));return;}Mesh m;if(l.endsWith(".obj"))'
    if old in t: t=t.replace(old,new,1)
    j.write_text(t,encoding='utf-8')
print('v1.9.8 compatibility patch applied safely')
