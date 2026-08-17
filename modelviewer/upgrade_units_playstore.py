from pathlib import Path

p = Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
s = p.read_text(encoding='utf-8')

repls = []
repls.append((
"static final int OPEN_MODEL=501; ModelView view; TextView status,info,measureInfo; Button surfaceBtn,wireBtn,pointBtn,autoBtn;",
"static final int OPEN_MODEL=501; ModelView view; TextView status,info,measureInfo; Button surfaceBtn,wireBtn,pointBtn,autoBtn,unitBtn; String unit=\"mm\"; double unitFactor=1.0;"
))
repls.append((
"top.addView(autoBtn);top.addView(btn(\"MESAFE\",v->setMeasure(1)));",
"top.addView(autoBtn);unitBtn=btn(\"BİRİM: mm\",v->cycleUnit());top.addView(unitBtn);top.addView(btn(\"MESAFE\",v->setMeasure(1)));"
))
repls.append((
"void setMeasure(int m){if(view.mesh==null){Toast.makeText(this,\"Önce model aç\",Toast.LENGTH_SHORT).show();return;}view.measureMode=m;view.pickPoints.clear();measureInfo.setText(m==1?\"MESAFE: iki nokta seç\":m==2?\"ÇAP/R: dairesel kenardan üç nokta seç\":\"AÇI: köşe ortada olacak şekilde üç nokta seç\");}\n void autoDimension(){if(view.mesh==null)return;Mesh m=view.mesh;measureInfo.setText(String.format(Locale.US,\"OTOMATİK DIŞ ÖLÇÜLER  X=%.3f  Y=%.3f  Z=%.3f  (model birimi)\",m.sizeX,m.sizeY,m.sizeZ));view.showAutoDims=true;view.requestRender();}",
"void setMeasure(int m){if(view.mesh==null){Toast.makeText(this,\"Önce model aç\",Toast.LENGTH_SHORT).show();return;}view.measureMode=m;view.pickPoints.clear();measureInfo.setText(m==1?\"MESAFE: iki nokta seç\":m==2?\"ÇAP/R: dairesel kenardan üç nokta seç\":\"AÇI: köşe ortada olacak şekilde üç nokta seç\");}\n void cycleUnit(){String[] u={\"mm\",\"cm\",\"m\",\"in\"};double[] f={1.0,0.1,0.001,1.0/25.4};int i=0;for(int k=0;k<u.length;k++)if(u[k].equals(unit)){i=k;break;}i=(i+1)%u.length;unit=u[i];unitFactor=f[i];unitBtn.setText(\"BİRİM: \"+unit);if(view.mesh!=null){updateModelInfo();autoDimension();}}\n String len(double raw){double v=raw*unitFactor;int d=Math.abs(v)>=100?1:Math.abs(v)>=10?2:3;return String.format(Locale.US,\"%.\"+d+\"f %s\",v,unit);}\n void updateModelInfo(){if(view.mesh==null)return;Mesh m=view.mesh;info.setText(\"Köşe:\"+m.vertexCount+\"  Üçgen:\"+m.triangleCount+\"   X=\"+len(m.sizeX)+\"  Y=\"+len(m.sizeY)+\"  Z=\"+len(m.sizeZ));}\n void autoDimension(){if(view.mesh==null)return;Mesh m=view.mesh;measureInfo.setText(\"OTOMATİK DIŞ ÖLÇÜLER   X=\"+len(m.sizeX)+\"   Y=\"+len(m.sizeY)+\"   Z=\"+len(m.sizeZ));view.showAutoDims=true;view.requestRender();}"
))
repls.append((
"info.setText(String.format(Locale.US,\"Köşe:%d  Üçgen:%d   X=%.3f Y=%.3f Z=%.3f\",m.vertexCount,m.triangleCount,m.sizeX,m.sizeY,m.sizeZ));measureInfo.setText(\"ÖLÇÜM: bir araç seç\");",
"updateModelInfo();measureInfo.setText(\"OTOMATİK ÖLÇÜ hazırlanıyor…\");autoDimension();"
))
repls.append((
"runOnUiThread(()->measureInfo.setText(String.format(Locale.US,\"MESAFE = %.3f model birimi\",d)));",
"runOnUiThread(()->measureInfo.setText(\"MESAFE = \"+len(d)));"
))
repls.append((
"runOnUiThread(()->measureInfo.setText(String.format(Locale.US,\"YARIÇAP R=%.3f   ÇAP Ø=%.3f\",r,2*r)));",
"runOnUiThread(()->measureInfo.setText(\"YARIÇAP R=\"+len(r)+\"   ÇAP Ø=\"+len(2*r)));"
))

for old, new in repls:
    if old not in s:
        raise SystemExit('Expected source fragment not found: ' + old[:120])
    s = s.replace(old, new, 1)

p.write_text(s, encoding='utf-8')
print('Unit-aware measurement patch applied')
