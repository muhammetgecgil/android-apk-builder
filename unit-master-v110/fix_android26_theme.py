from pathlib import Path

p = Path('unit-master/app/src/main/res/values/themes.xml')
if not p.exists():
    raise SystemExit('themes.xml not found')

s = p.read_text(encoding='utf-8')
needle = '        <item name="android:windowLightNavigationBar">false</item>\n'
if needle not in s:
    needle = '<item name="android:windowLightNavigationBar">false</item>'
    if needle not in s:
        raise SystemExit('Expected windowLightNavigationBar item not found; inspect theme before changing')
    s = s.replace(needle, '')
else:
    s = s.replace(needle, '')

p.write_text(s, encoding='utf-8')
print('Removed API-27-only navigation-bar style from API-26 base theme')
