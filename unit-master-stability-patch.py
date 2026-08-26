from pathlib import Path

root=Path('unit-master')
main=root/'app/src/main/java/com/mg/unitmasterx/MainActivity.java'
build=root/'app/build.gradle'
manifest=root/'app/src/main/AndroidManifest.xml'
test=root/'app/src/test/java/com/mg/unitmasterx/ConversionTest.java'

s=main.read_text(encoding='utf-8')

s=s.replace('''    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("unit_master_x",MODE_PRIVATE);
        precision=prefs.getInt("precision",8);
        loadState(); loadHistory(); loadFavorites();
        configureWindow();
        buildShell();
        showConverter();
    }
''','''    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("unit_master_x",MODE_PRIVATE);
        try { precision=Math.max(4,Math.min(12,prefs.getInt("precision",8))); } catch(Exception ignored){ precision=8; }
        try { loadState(); } catch(Exception ignored){ recoverDefaultSelection(); }
        try { loadHistory(); } catch(Exception ignored){ history.clear(); }
        try { loadFavorites(); } catch(Exception ignored){ favorites.clear(); }
        configureWindow();
        buildShell();
        showConverter();
    }
''')

s=s.replace('''    private void refreshUI(){
        if(categoryName!=null)categoryName.setText(category.name);
        if(fromIndex<0||fromIndex>=category.units.size())fromIndex=0; if(toIndex<0||toIndex>=category.units.size())toIndex=Math.min(1,category.units.size()-1);
        ConverterData.UnitDef a=category.units.get(fromIndex), b=category.units.get(toIndex);
        if(fromSymbol!=null){fromSymbol.setText(a.symbol);fromName.setText(a.name);} if(toSymbol!=null){toSymbol.setText(b.symbol);toName.setText(b.name);}
        updateBadges(a.symbol,b.symbol); updateFavoriteLabel(); convert(false);
    }
''','''    private void refreshUI(){
        if(!validateSelection()) recoverDefaultSelection();
        if(categoryName!=null)categoryName.setText(category.name);
        ConverterData.UnitDef a=category.units.get(fromIndex), b=category.units.get(toIndex);
        if(fromSymbol!=null&&fromName!=null){fromSymbol.setText(a.symbol);fromName.setText(a.name);} if(toSymbol!=null&&toName!=null){toSymbol.setText(b.symbol);toName.setText(b.name);}
        updateBadges(a.symbol,b.symbol); updateFavoriteLabel(); convert(false);
    }
''')

s=s.replace('''    private void convert(boolean commit){
        if(input==null||resultText==null)return; Double x=parse(input.getText().toString()); if(x==null){resultText.setText("—");equation.setText("Geçerli bir sayı yazın");return;}
        try{ ConverterData.UnitDef a=category.units.get(fromIndex), b=category.units.get(toIndex); double y=ConverterData.convert(category,fromIndex,toIndex,x); resultText.setText(format(y)+" "+b.symbol); equation.setText(format(x)+" "+a.symbol+" = "+format(y)+" "+b.symbol+"  •  "+category.name); if(commit)scheduleHistory(); }
        catch(Exception e){resultText.setText("—");equation.setText("Dönüşüm hesaplanamadı");}
    }
''','''    private void convert(boolean commit){
        if(input==null||resultText==null||equation==null)return;
        Double x=parse(input.getText().toString()); if(x==null){resultText.setText("—");equation.setText("Geçerli bir sayı yazın");return;}
        try{
            if(!validateSelection()) recoverDefaultSelection();
            ConverterData.UnitDef a=category.units.get(fromIndex), b=category.units.get(toIndex);
            double y=ConverterData.convert(category,fromIndex,toIndex,x);
            resultText.setText(format(y)+" "+b.symbol);
            equation.setText(format(x)+" "+a.symbol+" = "+format(y)+" "+b.symbol+"  •  "+category.name);
            if(commit)scheduleHistory();
        } catch(Exception e){ resultText.setText("—"); equation.setText("Dönüşüm hesaplanamadı"); }
    }
''')

s=s.replace('currentPage=1; setNavActive(1); Double x=', 'currentPage=1; setNavActive(1); if(!validateSelection())recoverDefaultSelection(); Double x=')

s=s.replace('''    private void scheduleHistory(){if(historyRunnable!=null)handler.removeCallbacks(historyRunnable);historyRunnable=this::saveHistoryNow;handler.postDelayed(historyRunnable,650);}
    private void saveHistoryNow(){Double x=input==null?null:parse(input.getText().toString());if(x==null)return;double y=ConverterData.convert(category,fromIndex,toIndex,x);HistoryItem h=new HistoryItem();h.category=category.name;h.from=category.units.get(fromIndex).symbol;h.to=category.units.get(toIndex).symbol;h.input=format(x);h.output=format(y);h.time=System.currentTimeMillis();if(!history.isEmpty()){HistoryItem p=history.get(0);if(p.category.equals(h.category)&&p.from.equals(h.from)&&p.to.equals(h.to)&&p.input.equals(h.input))return;}history.add(0,h);while(history.size()>60)history.remove(history.size()-1);saveHistory();}

    private void loadState(){String c=prefs.getString("category","Uzunluk");category=ConverterData.category(c);fromIndex=prefs.getInt("from",Math.min(5,category.units.size()-1));toIndex=prefs.getInt("to",Math.min(6,category.units.size()-1));if(fromIndex>=category.units.size())fromIndex=0;if(toIndex>=category.units.size())toIndex=Math.min(1,category.units.size()-1);}
''','''    private void scheduleHistory(){
        if(isFinishing()||(Build.VERSION.SDK_INT>=17&&isDestroyed()))return;
        if(historyRunnable!=null)handler.removeCallbacks(historyRunnable);
        final ConverterData.Category snapCategory=category; final int snapFrom=fromIndex, snapTo=toIndex;
        final String snapInput=input==null?null:input.getText().toString();
        historyRunnable=()->saveHistorySnapshot(snapCategory,snapFrom,snapTo,snapInput);
        handler.postDelayed(historyRunnable,650);
    }
    private void saveHistoryNow(){ if(!validateSelection())return; saveHistorySnapshot(category,fromIndex,toIndex,input==null?null:input.getText().toString()); }
    private void saveHistorySnapshot(ConverterData.Category c,int f,int t,String raw){
        try{
            if(c==null||c.units==null||c.units.isEmpty()||f<0||t<0||f>=c.units.size()||t>=c.units.size())return;
            Double x=parse(raw); if(x==null)return; double y=ConverterData.convert(c,f,t,x);
            HistoryItem h=new HistoryItem();h.category=c.name;h.from=c.units.get(f).symbol;h.to=c.units.get(t).symbol;h.input=format(x);h.output=format(y);h.time=System.currentTimeMillis();
            if(!history.isEmpty()){HistoryItem p=history.get(0);if(Objects.equals(p.category,h.category)&&Objects.equals(p.from,h.from)&&Objects.equals(p.to,h.to)&&Objects.equals(p.input,h.input))return;}
            history.add(0,h);while(history.size()>60)history.remove(history.size()-1);saveHistory();
        }catch(Exception ignored){}
    }
    private boolean validateSelection(){ return category!=null&&category.units!=null&&!category.units.isEmpty()&&fromIndex>=0&&toIndex>=0&&fromIndex<category.units.size()&&toIndex<category.units.size(); }
    private void recoverDefaultSelection(){ category=ConverterData.category("Uzunluk");fromIndex=Math.min(5,category.units.size()-1);toIndex=Math.min(6,category.units.size()-1);persistSelection(); }
    private void loadState(){String c=prefs.getString("category","Uzunluk");category=ConverterData.category(c);fromIndex=prefs.getInt("from",Math.min(5,category.units.size()-1));toIndex=prefs.getInt("to",Math.min(6,category.units.size()-1));if(!validateSelection())recoverDefaultSelection();}
''')

s=s.replace('Sürüm 1.0.0 • Target Android 16 / API 36','Sürüm 1.0.1 Stability • Android 16 / API 36')

s=s.replace('''    @Override public void onBackPressed(){ if(currentPage!=0){showConverter();} else super.onBackPressed(); }
}''','''    @Override protected void onStop(){ if(historyRunnable!=null)handler.removeCallbacks(historyRunnable); super.onStop(); }
    @Override protected void onDestroy(){ if(historyRunnable!=null)handler.removeCallbacks(historyRunnable);handler.removeCallbacksAndMessages(null);input=null;resultText=null;equation=null;categoryName=null;fromSymbol=null;fromName=null;toSymbol=null;toName=null;favoriteLabel=null;super.onDestroy(); }
    @Override public void onTrimMemory(int level){ if(level>=TRIM_MEMORY_UI_HIDDEN&&historyRunnable!=null)handler.removeCallbacks(historyRunnable);super.onTrimMemory(level); }
    @Override public void onBackPressed(){ if(currentPage!=0){showConverter();} else super.onBackPressed(); }
}''')
main.write_text(s,encoding='utf-8')

b=build.read_text(encoding='utf-8').replace('versionCode 100','versionCode 101').replace("versionName '1.0.0'","versionName '1.0.1'")
b=b.replace("release {\n            minifyEnabled true", "release {\n            manifestPlaceholders = [appLabel: 'Unit Master X']\n            minifyEnabled true")
b=b.replace("debug {\n            applicationIdSuffix '.debug'\n            versionNameSuffix '-debug'", "debug {\n            applicationIdSuffix '.stable'\n            versionNameSuffix '-stable'\n            manifestPlaceholders = [appLabel: 'Unit Master X Stable']")
build.write_text(b,encoding='utf-8')

m=manifest.read_text(encoding='utf-8').replace('android:label="@string/app_name"','android:label="${appLabel}"')
manifest.write_text(m,encoding='utf-8')

t=test.read_text(encoding='utf-8')
if 'everyCategoryHasUnitsAndIdentityIsStable' not in t:
    t=t.replace('\n}', '''\n    @Test public void everyCategoryHasUnitsAndIdentityIsStable(){\n        for(ConverterData.Category c:ConverterData.CATEGORIES){\n            assertNotNull(c); assertFalse(c.units.isEmpty());\n            for(int i=0;i<c.units.size();i++){ double x=1.23456789; double y=ConverterData.convert(c,i,i,x); assertEquals(c.name+" / "+c.units.get(i).symbol,x,y,1e-9); }\n        }\n    }\n}''')
    test.write_text(t,encoding='utf-8')

print('Unit Master X stability patch applied')
