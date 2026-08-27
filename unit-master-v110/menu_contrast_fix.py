from pathlib import Path
import re

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

a = main.read_text(encoding='utf-8')

# Explicit popup palette. Do not depend on Samsung/system theme colors.
# Surface #10243A, primary text #F5F9FF, cyan accent #27D3FF.
helper = r'''    private static final int MENU_BG=Color.rgb(16,36,58);
    private static final int MENU_TEXT=Color.rgb(245,249,255);
    private static final int MENU_DIVIDER=Color.rgb(40,76,108);
    private void styleMenuTree(View v){
        if(v==null)return;
        v.setBackgroundColor(MENU_BG);
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setTextColor(MENU_TEXT);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setMinHeight(dp(60));
            t.setPadding(dp(18),dp(12),dp(18),dp(12));
            t.setAlpha(1f);
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)styleMenuTree(g.getChildAt(i));
        }
    }
    private void refreshMenuContrast(final ListView list){
        if(list==null)return;
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

# Idempotently remove older helper versions from the generated activity.
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

# Remove previous menu styling injection if present.
a = re.sub(
    r'list\.setBackgroundColor\([^;]+\);list\.setCacheColorHint\([^;]+\);list\.setDivider\(new android\.graphics\.drawable\.ColorDrawable\([^\)]+\)\);list\.setDividerHeight\([^;]+\);list\.setOnHierarchyChangeListener\(new ViewGroup\.OnHierarchyChangeListener\(\)\{public void onChildViewAdded\(View parent,View child\)\{styleMenuTree\(child\);\}public void onChildViewRemoved\(View parent,View child\)\{\}\}\);refreshMenuContrast\(list\);',
    '',
    a,
)
a = re.sub(
    r'list\.setBackgroundColor\(CARD\);list\.setDivider\(new android\.graphics\.drawable\.ColorDrawable\(LINE\)\);list\.setDividerHeight\(1\);list\.setOnHierarchyChangeListener\(new ViewGroup\.OnHierarchyChangeListener\(\)\{public void onChildViewAdded\(View parent,View child\)\{styleMenuTree\(child\);\}public void onChildViewRemoved\(View parent,View child\)\{\}\}\);',
    '',
    a,
)

listener = r'''list.setBackgroundColor(MENU_BG);list.setCacheColorHint(MENU_BG);list.setDivider(new android.graphics.drawable.ColorDrawable(MENU_DIVIDER));list.setDividerHeight(dp(1));list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){public void onChildViewAdded(View parent,View child){styleMenuTree(child);}public void onChildViewRemoved(View parent,View child){}});refreshMenuContrast(list);'''

patterns = [
    r'(final\s+ListView\s+list\s*=\s*new\s+ListView\(this\);)',
    r'(?<!final\s)(ListView\s+list\s*=\s*new\s+ListView\(this\);)'
]
changed = 0
for pattern in patterns:
    matches = len(re.findall(pattern, a))
    if matches:
        a = re.sub(pattern, lambda m: m.group(1) + listener, a)
        changed += matches

# Explicitly replace transparent/list surface variants from older code.
a = a.replace('list.setBackgroundColor(Color.TRANSPARENT);', 'list.setBackgroundColor(MENU_BG);')
a = a.replace('list.setBackgroundColor(CARD);', 'list.setBackgroundColor(MENU_BG);')

# Readable system dialogs, independent of One UI light/dark choice.
a = a.replace('new AlertDialog.Builder(this)', 'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK)')

# Remove the stale stability label inherited by the RC build.
a = a.replace('Akıllı Birim Dönüştürücü • Stability 1.0.2', 'Akıllı Birim Dönüştürücü • RC 1.1')
a = a.replace('Stability 1.0.2', 'RC 1.1')

if changed == 0:
    print('--- MainActivity menu-related source lines ---')
    for line in a.splitlines():
        if any(k in line for k in ('ListView','ArrayAdapter','picker','Picker','AlertDialog')):
            print(line[:1200])
    raise SystemExit('No picker ListView creation found; menu contrast patch not applied')

main.write_text(a, encoding='utf-8')
print(f'Applied deterministic menu contrast to {changed} ListView picker(s): #10243A surface, #F5F9FF text, 18sp/60dp rows')
