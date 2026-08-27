from pathlib import Path

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

a = main.read_text(encoding='utf-8')

old_adapter = 'ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());'
count = a.count(old_adapter)
if count != 3:
    raise SystemExit(f'Expected 3 picker adapters, found {count}')
a = a.replace(old_adapter, 'ArrayAdapter<String> adapter=menuAdapter();')

old_list = 'list.setBackgroundColor(Color.TRANSPARENT);'
if old_list not in a:
    raise SystemExit('Picker ListView background pattern not found')
a = a.replace(old_list, 'list.setBackgroundColor(CARD);')

marker = '    private interface QueryListener{void changed(String q);}'
if marker not in a:
    raise SystemExit('QueryListener insertion marker not found')
menu_method = r'''    private ArrayAdapter<String> menuAdapter(){
        return new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new ArrayList<String>()){
            private TextView styleMenuRow(View view){
                TextView t=(TextView)view;
                t.setTextColor(TEXT);
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setMinHeight(dp(56));
                t.setPadding(dp(18),dp(12),dp(18),dp(12));
                t.setBackgroundColor(CARD);
                return t;
            }
            @Override public View getView(int position,View convertView,ViewGroup parent){
                return styleMenuRow(super.getView(position,convertView,parent));
            }
            @Override public View getDropDownView(int position,View convertView,ViewGroup parent){
                return styleMenuRow(super.getDropDownView(position,convertView,parent));
            }
        };
    }

'''
a = a.replace(marker, menu_method + marker)

# Force the two system dialogs to a readable dark theme with light text.
a = a.replace('new AlertDialog.Builder(this).setTitle("Ayarlar")',
              'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Ayarlar")')
a = a.replace('new AlertDialog.Builder(this).setTitle("Unit Master X")',
              'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Unit Master X")')

# The RC inherited the old stability subtitle. Keep the user-visible build label accurate.
a = a.replace('Akıllı Birim Dönüştürücü • Stability 1.0.2', 'Akıllı Birim Dönüştürücü • RC 1.1')

main.write_text(a, encoding='utf-8')
print('Applied menu contrast fix: navy picker surfaces, white text, dark readable system dialogs')
