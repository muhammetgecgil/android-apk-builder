from pathlib import Path

cad=Path('modelviewer/src/main/java/com/muhammetgecgil/modelviewer/CadViewerActivity.java')
c=cad.read_text(encoding='utf-8')

c=c.replace('private String modelType;','private String modelType;\n    private File extractedModelFile;\n    private File zipWorkDir;')

old='''if ("/model.bin".equals(p)) {\n                        if(modelUri==null) return new WebResourceResponse("application/octet-stream", "utf-8", new ByteArrayInputStream(new byte[0]));\n                        InputStream in = getContentResolver().openInputStream(modelUri);\n                        return resp("application/octet-stream", in);\n                    }'''
new='''if ("/model.bin".equals(p)) {\n                        InputStream in;\n                        if(extractedModelFile!=null && extractedModelFile.exists()) in=new FileInputStream(extractedModelFile);\n                        else if(modelUri!=null) in=getContentResolver().openInputStream(modelUri);\n                        else return new WebResourceResponse("application/octet-stream", "utf-8", new ByteArrayInputStream(new byte[0]));\n                        return resp("application/octet-stream", in);\n                    }'''
if old not in c: raise SystemExit('model.bin patch point not found')
c=c.replace(old,new,1)

needle='''        modelName=queryName(modelUri);\n        String l=modelName.toLowerCase(Locale.ROOT);'''
rep='''        modelName=queryName(modelUri);\n        String l=modelName.toLowerCase(Locale.ROOT);\n        if(l.endsWith(".zip")){ openZipArchive(modelUri); return; }\n        extractedModelFile=null;'''
if needle not in c: raise SystemExit('onActivityResult patch point not found')
c=c.replace(needle,rep,1)

anchor='''    private String queryName(Uri u){'''
methods=r'''    private boolean supportedModelName(String name){
        String l=name.toLowerCase(Locale.ROOT);
        String[] exts={".step",".stp",".iges",".igs",".brep",".brp",".obj",".stl",".ply",".gltf",".glb",".3mf",".dae",".fbx",".dxf",".x3d",".off",".ifc"};
        for(String e:exts) if(l.endsWith(e)) return true;
        return false;
    }
    private String typeFromName(String name){
        String l=name.toLowerCase(Locale.ROOT);
        int dot=l.lastIndexOf('.');
        String t=(dot>=0&&dot<l.length()-1)?l.substring(dot+1):"unknown";
        if(t.equals("stp")) return "step";
        if(t.equals("igs")) return "iges";
        if(t.equals("brp")) return "brep";
        return t;
    }
    private void openZipArchive(Uri uri){
        new Thread(() -> {
            ArrayList<File> files=new ArrayList<>();
            ArrayList<String> names=new ArrayList<>();
            try{
                if(zipWorkDir!=null) deleteTree(zipWorkDir);
                zipWorkDir=new File(getCacheDir(),"zipcad_"+System.currentTimeMillis());
                if(!zipWorkDir.mkdirs()) throw new IOException("ZIP geçici klasörü oluşturulamadı");
                long total=0; int idx=0;
                try(java.util.zip.ZipInputStream zin=new java.util.zip.ZipInputStream(new BufferedInputStream(getContentResolver().openInputStream(uri)))){
                    java.util.zip.ZipEntry e;
                    byte[] buf=new byte[65536];
                    while((e=zin.getNextEntry())!=null){
                        if(e.isDirectory()) continue;
                        String entryName=e.getName().replace('\\','/');
                        String base=entryName.substring(entryName.lastIndexOf('/')+1);
                        if(base.isEmpty()||!supportedModelName(base)) continue;
                        File out=new File(zipWorkDir,String.format(Locale.ROOT,"%03d_%s",idx++,base.replaceAll("[^A-Za-z0-9._ -]","_")));
                        long one=0;
                        try(FileOutputStream fos=new FileOutputStream(out)){
                            int n;
                            while((n=zin.read(buf))>0){
                                one+=n; total+=n;
                                if(one>536870912L||total>1073741824L) throw new IOException("ZIP içeriği güvenli boyut sınırını aşıyor");
                                fos.write(buf,0,n);
                            }
                        }
                        files.add(out); names.add(entryName);
                    }
                }
                runOnUiThread(() -> {
                    if(files.isEmpty()){
                        android.widget.Toast.makeText(this,"ZIP içinde desteklenen CAD/3B model bulunamadı",android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(files.size()==1){ activateZipModel(files.get(0),names.get(0)); return; }
                    String[] arr=names.toArray(new String[0]);
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("ZIP içindeki modeli seç")
                        .setItems(arr,(d,which)->activateZipModel(files.get(which),names.get(which)))
                        .setNegativeButton("İPTAL",null)
                        .show();
                });
            }catch(Throwable ex){
                runOnUiThread(() -> android.widget.Toast.makeText(this,"ZIP açılamadı: "+ex.getMessage(),android.widget.Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    private void activateZipModel(File f,String displayName){
        extractedModelFile=f;
        modelUri=null;
        modelName=displayName;
        modelType=typeFromName(displayName);
        loadWorkspace();
    }
    private void deleteTree(File f){
        if(f==null||!f.exists()) return;
        if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File x:a)deleteTree(x);}
        try{f.delete();}catch(Throwable ignored){}
    }
    @Override protected void onDestroy(){
        super.onDestroy();
        if(zipWorkDir!=null) deleteTree(zipWorkDir);
    }

'''
if anchor not in c: raise SystemExit('queryName anchor not found')
c=c.replace(anchor,methods+anchor,1)

cad.write_text(c,encoding='utf-8')

html=Path('modelviewer/src/main/assets/cadviewer/index.html')
h=html.read_text(encoding='utf-8')
h=h.replace('DOSYA AÇ • STEP/STP • IGES/IGS • BREP • OBJ • STL • PLY • GLTF/GLB • 3MF • DAE • FBX • DXF • X3D • OFF • IFC', 'DOSYA AÇ • ZIP • STEP/STP • IGES/IGS • BREP • OBJ • STL • PLY • GLTF/GLB • 3MF • DAE • FBX • DXF • X3D • OFF • IFC')
if '/cad-v183.js' not in h:
    h=h.replace('</body>','<script src="/cad-v183.js"></script></body>',1)
html.write_text(h,encoding='utf-8')
(AS:=Path('modelviewer/src/main/assets/cadviewer')).mkdir(parents=True,exist_ok=True)
(AS/'cad-v183.js').write_text("window.MG_CAD_V183={version:'1.8.3',zipOpen:true,zipMultiModelPicker:true,zipOffline:true};\n",encoding='utf-8')
print('v1.8.3 ZIP archive support applied')
