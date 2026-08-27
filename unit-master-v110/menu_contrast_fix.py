from pathlib import Path
import re

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
gradle = Path('unit-master/app/build.gradle')
res_layout = Path('unit-master/app/src/main/res/layout')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

# Product-level menu fix: do not rely on Android/One UI list-row colors at all.
# Every ArrayAdapter menu row is inflated from our own white resource.
res_layout.mkdir(parents=True, exist_ok=True)
row = res_layout / 'white_menu_row.xml'
row.write_text('''<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/text1"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="60dp"
    android:gravity="center_vertical"
    android:paddingStart="18dp"
    android:paddingEnd="18dp"
    android:paddingTop="10dp"
    android:paddingBottom="10dp"
    android:background="#FFFFFFFF"
    android:textColor="#FF14181C"
    android:textSize="18sp"
    android:maxLines="2"
    android:ellipsize="end" />
''', encoding='utf-8')

a = main.read_text(encoding='utf-8')

helper = r'''    private static final int MENU_BG=Color.WHITE;
    private static final int MENU_TEXT=Color.rgb(20,24,28);
    private static final int MENU_DIVIDER=Color.rgb(226,232,240);
    private void styleMenuTree(View v){
        if(v==null)return;
        v.setBackgroundColor(MENU_BG);
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setTextColor(MENU_TEXT);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setMinHeight(dp(60));
            t.setPadding(dp(18),dp(10),dp(18),dp(10));
            t.setAlpha(1f);
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)styleMenuTree(g.getChildAt(i));
        }
    }
    private void refreshMenuContrast(final ListView list){
        if(list==null)return;
        list.setBackgroundColor(MENU_BG);
        list.setCacheColorHint(MENU_BG);
        styleMenuTree(list);
        list.post(new Runnable(){@Override public void run(){styleMenuTree(list);}});
        list.postDelayed(new Runnable(){@Override public void run(){styleMenuTree(list);}},80);
        list.postDelayed(new Runnable(){@Override public void run(){styleMenuTree(list);}},250);
        list.setOnScrollListener(new android.widget.AbsListView.OnScrollListener(){
            @Override public void onScrollStateChanged(android.widget.AbsListView view,int state){styleMenuTree(list);}
            @Override public void onScroll(android.widget.AbsListView view,int first,int visible,int total){styleMenuTree(list);}
        });
    }

'''

# Remove an older generated helper when this script is run repeatedly.
a = re.sub(r'    private static final int MENU_BG=.*?\n    \}\n\n', '', a, flags=re.S)
a = re.sub(r'    private void styleMenuTree\(View v\)\{.*?\n    \}\n\n', '', a, flags=re.S)

markers = [
    '    private void showAllResults(){',
    '    private void openSettings(){',
    '    private void copyResult(){'
]
inserted = False
for marker in markers:
    if marker in a:
        a = a.replace(marker, helper + marker, 1)
        inserted = True
        break
if not inserted:
    raise SystemExit('Could not locate helper insertion point in MainActivity')

# Force ArrayAdapter rows to use an app-owned white TextView instead of Samsung/system layouts.
layout_patterns = [
    'android.R.layout.simple_list_item_1',
    'android.R.layout.simple_spinner_item',
    'android.R.layout.simple_spinner_dropdown_item'
]
row_replacements = 0
for old in layout_patterns:
    n = a.count(old)
    if n:
        a = a.replace(old, 'R.layout.white_menu_row')
        row_replacements += n

# Remove any older list styling injection before adding the current one.
a = re.sub(
    r'list\.setBackgroundColor\([^;]+\);list\.setCacheColorHint\([^;]+\);list\.setDivider\(new android\.graphics\.drawable\.ColorDrawable\([^\)]+\)\);list\.setDividerHeight\([^;]+\);list\.setOnHierarchyChangeListener\(new ViewGroup\.OnHierarchyChangeListener\(\)\{public void onChildViewAdded\(View parent,View child\)\{styleMenuTree\(child\);\}public void onChildViewRemoved\(View parent,View child\)\{\}\}\);refreshMenuContrast\(list\);',
    '', a)

listener = r'''list.setBackgroundColor(MENU_BG);list.setCacheColorHint(MENU_BG);list.setDivider(new android.graphics.drawable.ColorDrawable(MENU_DIVIDER));list.setDividerHeight(dp(1));list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){public void onChildViewAdded(View parent,View child){styleMenuTree(child);}public void onChildViewRemoved(View parent,View child){}});refreshMenuContrast(list);'''

patterns = [
    r'(final\s+ListView\s+list\s*=\s*new\s+ListView\(this\);)',
    r'(?<!final\s)(ListView\s+list\s*=\s*new\s+ListView\(this\);)'
]
list_count = 0
for pattern in patterns:
    matches = len(re.findall(pattern, a))
    if matches:
        a = re.sub(pattern, lambda m: m.group(1) + listener, a)
        list_count += matches

# Eliminate dark/transparent menu surfaces inherited from the app theme.
a = a.replace('list.setBackgroundColor(Color.TRANSPARENT);', 'list.setBackgroundColor(MENU_BG);')
a = a.replace('list.setBackgroundColor(CARD);', 'list.setBackgroundColor(MENU_BG);')

# The dialog window itself must be light as well as its rows.
a = a.replace('new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK)', 'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)')
a = a.replace('new AlertDialog.Builder(this)', 'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)')

# Make the installed build unmistakably newer so Android cannot leave the old RC in place.
a = a.replace('Akıllı Birim Dönüştürücü • Stability 1.0.2', 'Akıllı Birim Dönüştürücü • RC 1.1.1')
a = a.replace('Akıllı Birim Dönüştürücü • RC 1.1', 'Akıllı Birim Dönüştürücü • RC 1.1.1')
a = a.replace('Stability 1.0.2', 'RC 1.1.1')

if list_count == 0:
    print('--- MainActivity menu-related source lines ---')
    for line in a.splitlines():
        if any(k in line for k in ('ListView','ArrayAdapter','setAdapter','AlertDialog')):
            print(line[:1600])
    raise SystemExit('No picker ListView creation found; white menu patch not applied')
if row_replacements == 0:
    print('--- Adapter diagnostics ---')
    for line in a.splitlines():
        if 'setAdapter' in line or 'ArrayAdapter' in line:
            print(line[:1600])
    raise SystemExit('No system ArrayAdapter row layouts found; refusing to publish an unverified menu-color patch')

main.write_text(a, encoding='utf-8')

if gradle.exists():
    g = gradle.read_text(encoding='utf-8')
    g, vc = re.subn(r'\bversionCode\s+\d+', 'versionCode 111', g)
    g, vn = re.subn(r'\bversionName\s+[\"\'][^\"\']+[\"\']', 'versionName "1.1.1"', g)
    gradle.write_text(g, encoding='utf-8')
    print(f'Version bump: versionCode replacements={vc}, versionName replacements={vn}')

print(f'WHITE MENU FIX VERIFIED IN SOURCE: {list_count} ListView picker(s), {row_replacements} adapter row layout replacement(s), app-owned white_menu_row.xml')
for line in a.splitlines():
    if 'setAdapter' in line and ('list.' in line or 'ArrayAdapter' in line):
        print('ADAPTER:', line.strip()[:1600])
