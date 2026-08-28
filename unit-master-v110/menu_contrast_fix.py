from pathlib import Path
import re

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
gradle = Path('unit-master/app/build.gradle')
res_layout = Path('unit-master/app/src/main/res/layout')
res_drawable = Path('unit-master/app/src/main/res/drawable')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

# Product-level picker styling:
# 1) Closed Spinner selections stay inside the dark Unit Master design.
# 2) Open dropdown rows are always white with dark text for maximum readability.
# Samsung/One UI therefore cannot turn either state into low-contrast white-on-white.
res_layout.mkdir(parents=True, exist_ok=True)
res_drawable.mkdir(parents=True, exist_ok=True)

selected_bg = res_drawable / 'selected_field_bg.xml'
selected_bg.write_text('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FF0F2238" />
    <stroke android:width="1dp" android:color="#FF2A4A69" />
    <corners android:radius="14dp" />
    <padding android:left="2dp" android:top="2dp" android:right="2dp" android:bottom="2dp" />
</shape>
''', encoding='utf-8')

selected_row = res_layout / 'selected_spinner_row.xml'
selected_row.write_text('''<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/text1"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:minHeight="62dp"
    android:gravity="center_vertical"
    android:paddingStart="18dp"
    android:paddingEnd="18dp"
    android:paddingTop="10dp"
    android:paddingBottom="10dp"
    android:background="@drawable/selected_field_bg"
    android:textColor="#FFF7FAFF"
    android:textSize="20sp"
    android:maxLines="2"
    android:ellipsize="end" />
''', encoding='utf-8')

white_row = res_layout / 'white_menu_row.xml'
white_row.write_text('''<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
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
    android:ellipsize="end"
    tools:ignore="Overdraw" />
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

# Remove older generated helpers when the script is run repeatedly.
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

# First make all popup/list rows app-owned white rows instead of system rows.
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

# Closed CATEGORY Spinner must use the dark selected-field resource, while its dropdown stays white.
category_old = 'new ArrayAdapter<String>(this,R.layout.white_menu_row,categoryNames())'
category_new = 'new ArrayAdapter<String>(this,R.layout.selected_spinner_row,categoryNames())'
category_selected_changes = a.count(category_old)
if category_selected_changes:
    a = a.replace(category_old, category_new)

# Closed SOURCE/TARGET Spinners are created through unitAdapter(). Change only that method's
# constructor layout to the dark selected-field resource. Search/dialog ListViews remain white.
unit_selected_changes = 0
u_pos = a.find('ArrayAdapter<String> unitAdapter(')
if u_pos >= 0:
    u_start = a.rfind('\n', 0, u_pos) + 1
    next_method = re.search(r'\n\s*(?:private|public|protected)\s+', a[u_pos + 1:])
    u_end = (u_pos + 1 + next_method.start()) if next_method else len(a)
    block = a[u_start:u_end]
    if 'R.layout.white_menu_row' in block:
        block = block.replace('R.layout.white_menu_row', 'R.layout.selected_spinner_row', 1)
        a = a[:u_start] + block + a[u_end:]
        unit_selected_changes = 1

# Dropdown side of each Spinner remains explicitly white.
# Existing setDropDownViewResource calls may have been converted above; enforce them again.
a = a.replace('setDropDownViewResource(R.layout.selected_spinner_row)', 'setDropDownViewResource(R.layout.white_menu_row)')

# Remove older ListView styling injection before adding the current one.
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

# Eliminate dark/transparent popup-list surfaces inherited from the app theme.
a = a.replace('list.setBackgroundColor(Color.TRANSPARENT);', 'list.setBackgroundColor(MENU_BG);')
a = a.replace('list.setBackgroundColor(CARD);', 'list.setBackgroundColor(MENU_BG);')

# Explicitly force all three converter Spinners' POPUP windows to opaque white.
spinner_bg = 'setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(MENU_BG))'
spinner_changes = 0
for needle, replacement in [
    ('categorySpinner.setAdapter(a);', 'categorySpinner.setAdapter(a);categorySpinner.' + spinner_bg + ';'),
    ('fromSpinner.setAdapter(a);toSpinner.setAdapter(b);', 'fromSpinner.setAdapter(a);toSpinner.setAdapter(b);fromSpinner.' + spinner_bg + ';toSpinner.' + spinner_bg + ';')
]:
    if needle in a:
        a = a.replace(needle, replacement)
        spinner_changes += 1

# Dialog windows containing menu lists are light as well.
a = a.replace('new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK)', 'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)')
a = a.replace('new AlertDialog.Builder(this)', 'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)')

# Normalize the visible build label instead of repeatedly appending .1 to an older RC string.
a = re.sub(r'Akıllı Birim Dönüştürücü\s*•\s*(?:Stability|RC)\s*[0-9.]+',
           'Akıllı Birim Dönüştürücü • RC 1.1.2', a)
a = re.sub(r'\bStability\s+1\.0\.2\b', 'RC 1.1.2', a)

if list_count == 0:
    raise SystemExit('No picker ListView creation found; menu patch not applied')
if row_replacements == 0:
    raise SystemExit('No system ArrayAdapter row layouts found; refusing to publish an unverified menu patch')
if category_selected_changes == 0:
    raise SystemExit('Category Spinner closed-state adapter was not moved to selected_spinner_row')
if unit_selected_changes == 0:
    raise SystemExit('Source/target unitAdapter closed-state layout was not moved to selected_spinner_row')
if spinner_changes < 2:
    raise SystemExit(f'Expected category and unit Spinner popup background patches; found {spinner_changes}')

main.write_text(a, encoding='utf-8')

if gradle.exists():
    g = gradle.read_text(encoding='utf-8')
    g, vc = re.subn(r'\bversionCode\s+\d+', 'versionCode 112', g)
    g, vn = re.subn(r'\bversionName\s+[\"\'][^\"\']+[\"\']', 'versionName "1.1.2"', g)
    gradle.write_text(g, encoding='utf-8')
    print(f'Version bump: versionCode replacements={vc}, versionName replacements={vn}')

print(f'PICKER CONTRAST FIX VERIFIED: {list_count} ListView(s), {row_replacements} white popup-row replacement(s), category dark selected={category_selected_changes}, unit dark selected={unit_selected_changes}, popup white={spinner_changes}')
for line in a.splitlines():
    if 'setAdapter' in line and ('list.' in line or 'ArrayAdapter' in line or 'Spinner' in line):
        print('ADAPTER:', line.strip()[:1800])
