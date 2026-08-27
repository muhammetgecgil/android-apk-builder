from pathlib import Path
import re

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

a = main.read_text(encoding='utf-8')

# Style any row that a ListView attaches. This is deliberately adapter-independent:
# it also covers future filtered/category/unit/search adapters.
helper = r'''    private void styleMenuTree(View v){
        if(v==null)return;
        v.setBackgroundColor(CARD);
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setTextColor(TEXT);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP,17);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setMinHeight(dp(56));
            t.setPadding(dp(18),dp(12),dp(18),dp(12));
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)styleMenuTree(g.getChildAt(i));
        }
    }

'''

if 'private void styleMenuTree(View v)' not in a:
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

listener = r'''list.setBackgroundColor(CARD);list.setDivider(new android.graphics.drawable.ColorDrawable(LINE));list.setDividerHeight(1);list.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener(){public void onChildViewAdded(View parent,View child){styleMenuTree(child);}public void onChildViewRemoved(View parent,View child){}});'''

# Inject the styling immediately after every reusable picker ListView is created.
patterns = [
    r'(ListView\s+list\s*=\s*new\s+ListView\(this\);)',
    r'(final\s+ListView\s+list\s*=\s*new\s+ListView\(this\);)'
]
changed = 0
for pattern in patterns:
    def repl(m):
        nonlocal_holder[0] += 1
        return m.group(1) + listener
    nonlocal_holder = [0]
    a = re.sub(pattern, repl, a)
    changed += nonlocal_holder[0]

# If the safe activity uses an existing pickerDialog with explicit transparent background,
# force it to the app card surface too.
a = a.replace('list.setBackgroundColor(Color.TRANSPARENT);', 'list.setBackgroundColor(CARD);')

# Readable system dialogs: dark surface + light text, consistent with the app.
a = a.replace('new AlertDialog.Builder(this)', 'new AlertDialog.Builder(this,AlertDialog.THEME_DEVICE_DEFAULT_DARK)')

# Remove the stale stability label inherited by the RC build.
a = a.replace('Akıllı Birim Dönüştürücü • Stability 1.0.2', 'Akıllı Birim Dönüştürücü • RC 1.1')
a = a.replace('Stability 1.0.2', 'RC 1.1')

# A ListView creation must have been found; otherwise do not silently ship the visual bug.
if changed == 0:
    print('--- MainActivity menu-related source lines ---')
    for line in a.splitlines():
        if any(k in line for k in ('ListView','ArrayAdapter','picker','Picker','AlertDialog')):
            print(line[:1200])
    raise SystemExit('No picker ListView creation found; menu contrast patch not applied')

main.write_text(a, encoding='utf-8')
print(f'Applied menu contrast fix to {changed} ListView picker(s): navy surfaces + white text + readable dark dialogs')
