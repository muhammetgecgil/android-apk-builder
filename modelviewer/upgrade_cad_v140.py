from pathlib import Path
p=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
s=p.read_text()
if 'openCadViewer' not in s:
    old='String n=displayName(u),l=n.toLowerCase(Locale.ROOT);Mesh m;if(l.endsWith(".obj"))'
    new='String n=displayName(u),l=n.toLowerCase(Locale.ROOT);if(isCadExt(l)){runOnUiThread(()->openCadViewer(u,n,l));return;}Mesh m;if(l.endsWith(".obj"))'
    if old not in s: raise SystemExit('loadModel patch point not found')
    s=s.replace(old,new,1)
    anchor=' byte[] readAllBytes(Uri u)throws Exception'
    methods=' boolean isCadExt(String l){return l.endsWith(".step")||l.endsWith(".stp")||l.endsWith(".iges")||l.endsWith(".igs")||l.endsWith(".brep")||l.endsWith(".brp");}\n void openCadViewer(Uri u,String n,String l){Intent i=new Intent(this,CadViewerActivity.class);i.setData(u);i.putExtra("name",n);String t=(l.endsWith(".iges")||l.endsWith(".igs"))?"iges":((l.endsWith(".brep")||l.endsWith(".brp"))?"brep":"step");i.putExtra("type",t);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);status.setText(n+" • CAD motoru açılıyor");startActivity(i);}\n'
    if anchor not in s: raise SystemExit('method anchor not found')
    s=s.replace(anchor,methods+anchor,1)
s=s.replace('OBJ / STL / PLY aç • döndür • zoom • ölçüm aracı seç','STEP / IGES / BREP / OBJ / STL / PLY • CAD + ölçüm')
s=s.replace('new String[]{"model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"}','new String[]{"model/step","model/iges","model/obj","model/stl","application/octet-stream","text/plain","application/x-ply"}')
p.write_text(s)
print('CAD v1.4 patch applied')
