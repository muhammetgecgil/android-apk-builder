from pathlib import Path
import re

main = Path('unit-master/app/src/main/java/com/mg/unitmasterx/MainActivity.java')
gradle = Path('unit-master/app/build.gradle')
layout_dir = Path('unit-master/app/src/main/res/layout')
drawable_dir = Path('unit-master/app/src/main/res/drawable')
if not main.exists():
    raise SystemExit('MainActivity.java not found')

layout_dir.mkdir(parents=True, exist_ok=True)
drawable_dir.mkdir(parents=True, exist_ok=True)

selected_bg = drawable_dir / 'selected_field_bg.xml'
selected_bg.write_text('''<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FF0F2238"/>
    <stroke android:width="1dp" android:color="#FF294A6A"/>
    <corners android:radius="12dp"/>
    <padding android:left="2dp" android:top="2dp" android:right="2dp" android:bottom="2dp"/>
</shape>
''', encoding='utf-8')

selected = layout_dir / 'selected_spinner_row.xml'
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
    android:background="@drawable/selected_field_bg"
    android:textColor="#FFF7FAFF"
    android:textSize="20sp"
    android:maxLines="2"
    android:ellipsize="end" />
''', encoding='utf-8')

a = main.read_text(encoding='utf-8')

# Category Spinner: dark filled selected surface.
a, category_count = re.subn(
    r'new ArrayAdapter<String>\(this,R\.layout\.white_menu_row,categoryNames\(\)\)',
    'new ArrayAdapter<String>(this,R.layout.selected_spinner_row,categoryNames())',
    a
)

# Source/target unit Spinner: dark filled selected surface.
a, unit_count = re.subn(
    r'new ArrayAdapter<String>\(this,R\.layout\.white_menu_row,names\)',
    'new ArrayAdapter<String>(this,R.layout.selected_spinner_row,names)',
    a
)

# Reinforce selected-state colors in custom getView overrides.
a = a.replace(
    'styleSpinnerText(t,22);return t;',
    'styleSpinnerText(t,22);t.setBackgroundResource(R.drawable.selected_field_bg);t.setTextColor(Color.rgb(247,250,255));t.setAlpha(1f);return t;'
)
a = a.replace(
    'styleSpinnerText(t,18);return t;',
    'styleSpinnerText(t,18);t.setBackgroundResource(R.drawable.selected_field_bg);t.setTextColor(Color.rgb(247,250,255));t.setAlpha(1f);return t;'
)
a = a.replace(
    'styleSpinnerText(t,20);return t;',
    'styleSpinnerText(t,20);t.setBackgroundResource(R.drawable.selected_field_bg);t.setTextColor(Color.rgb(247,250,255));t.setAlpha(1f);return t;'
)

# Opened dropdowns intentionally remain white and high contrast.
if 'setDropDownViewResource(R.layout.white_menu_row)' not in a:
    raise SystemExit('White dropdown row resource missing after menu contrast patch')
if 'setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(MENU_BG))' not in a:
    raise SystemExit('White Spinner popup background patch missing')

# Normalize visible build label to prevent repeated replacement artifacts.
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

print(f'SELECTED SPINNER SURFACE FIX PASS: category={category_count}, unit={unit_count}; selected=deep navy/light text, dropdown=white/dark text; version=1.1.2/112')
