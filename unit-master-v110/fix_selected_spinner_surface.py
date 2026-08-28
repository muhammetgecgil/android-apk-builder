from pathlib import Path
import re

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
gradle = Path('unit-master/app/build.gradle')
layout_dir = Path('unit-master/app/src/main/res/layout')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

layout_dir.mkdir(parents=True, exist_ok=True)
selected = layout_dir / 'spinner_selected_row.xml'
selected.write_text('''<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@android:id/text1"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:minHeight="56dp"
    android:gravity="center_vertical"
    android:paddingStart="16dp"
    android:paddingEnd="16dp"
    android:paddingTop="8dp"
    android:paddingBottom="8dp"
    android:background="#FF0D2138"
    android:textColor="#FFF5F9FF"
    android:textSize="20sp"
    android:maxLines="2"
    android:ellipsize="end" />
''', encoding='utf-8')

a = main.read_text(encoding='utf-8')

# Category Spinner: selected surface uses the dark app-owned row.
a, category_count = re.subn(
    r'new ArrayAdapter<String>\(this,R\.layout\.white_menu_row,categoryNames\(\)\)',
    'new ArrayAdapter<String>(this,R.layout.spinner_selected_row,categoryNames())',
    a
)

# Source/target unit Spinner adapters use the same dark selected row.
# Search/list dialogs use different data expressions, so they intentionally stay white.
a, unit_count = re.subn(
    r'new ArrayAdapter<String>\(this,R\.layout\.white_menu_row,names\)',
    'new ArrayAdapter<String>(this,R.layout.spinner_selected_row,names)',
    a
)

# Explicitly reinforce selected-view contrast in Spinner getView implementations.
# Popup rows remain white because setDropDownViewResource(R.layout.white_menu_row) is preserved.
a = a.replace(
    'styleSpinnerText(t,22);return t;',
    'styleSpinnerText(t,22);t.setBackgroundColor(Color.rgb(13,33,56));t.setTextColor(Color.rgb(245,249,255));t.setAlpha(1f);return t;'
)
a = a.replace(
    'styleSpinnerText(t,18);return t;',
    'styleSpinnerText(t,18);t.setBackgroundColor(Color.rgb(13,33,56));t.setTextColor(Color.rgb(245,249,255));t.setAlpha(1f);return t;'
)
a = a.replace(
    'styleSpinnerText(t,20);return t;',
    'styleSpinnerText(t,20);t.setBackgroundColor(Color.rgb(13,33,56));t.setTextColor(Color.rgb(245,249,255));t.setAlpha(1f);return t;'
)

# Ensure dropdown rows stay white and readable.
if 'setDropDownViewResource(R.layout.white_menu_row)' not in a:
    raise SystemExit('White dropdown row resource missing after menu contrast patch')
if 'setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(MENU_BG))' not in a:
    raise SystemExit('White Spinner popup background patch missing')

# Normalize subtitle so repeated patching cannot create RC 1.1.1.1-style strings.
a = re.sub(
    r'Akıllı Birim Dönüştürücü • (?:Stability\s+[0-9.]+|RC\s+[0-9.]+)',
    'Akıllı Birim Dönüştürücü • RC 1.1.2',
    a
)
a = re.sub(r'RC\s+1\.1(?:\.1)+', 'RC 1.1.2', a)

if category_count < 1:
    raise SystemExit('Category selected Spinner row was not patched')
if unit_count < 1:
    raise SystemExit('Unit selected Spinner row was not patched')

main.write_text(a, encoding='utf-8')

if gradle.exists():
    g = gradle.read_text(encoding='utf-8')
    g = re.sub(r'\bversionCode\s+\d+', 'versionCode 112', g)
    g = re.sub(r'\bversionName\s+[\"\'][^\"\']+[\"\']', 'versionName "1.1.2"', g)
    gradle.write_text(g, encoding='utf-8')

print(f'SELECTED SPINNER SURFACE FIX PASS: category={category_count}, unit={unit_count}; selected=dark, dropdown=white; version=1.1.2/112')
