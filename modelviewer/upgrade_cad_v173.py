from pathlib import Path
p=Path('modelviewer/src/main/assets/cadviewer/index.html')
s=p.read_text(encoding='utf-8')
s=s.replace('<button onclick="fit()">SIĞDIR</button>', '<button id="openFileTop" onclick="openAndroidFile()">DOSYA AÇ</button><button onclick="fit()">SIĞDIR</button>')
s=s.replace('<script>', '<script>\nfunction openAndroidFile(){try{if(window.AndroidHost&&AndroidHost.openFileFromCad){AndroidHost.openFileFromCad();return}if(window.Android&&Android.openFileFromCad){Android.openFileFromCad();return}}catch(e){} alert("Dosya seçici açılamadı");}\n')
s=s.replace('OpenCascade WebAssembly yükleniyor…','CAD çalışma alanı hazır • DOSYA AÇ ile model seç')
p.write_text(s,encoding='utf-8')

j=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/MainActivity.java')
t=j.read_text(encoding='utf-8')
needle='@JavascriptInterface public void closeViewer()'
if needle in t and 'openFileFromCad' not in t:
    t=t.replace(needle,'@JavascriptInterface public void openFileFromCad(){runOnUiThread(()->openFile());}\n  '+needle)
t=t.replace('setContentView(root);updateModeButtons();','setContentView(root);updateModeButtons();')
j.write_text(t,encoding='utf-8')

Path('modelviewer/src/main/assets/cadviewer/cad-v173.js').write_text("window.MG_CAD_V173={version:'1.7.3',directWorkspace:true,filePicker:true};\n",encoding='utf-8')
