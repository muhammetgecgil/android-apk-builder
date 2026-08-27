from pathlib import Path
import re
import shutil

root = Path('unit-master/app/src/main')
activity = root / 'java/com/mg/unitmasterx/MainActivity.java'
manifest = root / 'AndroidManifest.xml'
strings = root / 'res/values/strings.xml'
colors = root / 'res/values/colors.xml'

for p in (activity, manifest, strings, colors):
    if not p.exists():
        raise SystemExit(f'Required file missing: {p}')

# 1) Never block the UI thread while clearing preferences.
a = activity.read_text(encoding='utf-8')
a = a.replace('.edit().clear().commit();recreate();', '.edit().clear().apply();recreate();')

# 2) Move user-facing setText content to Android string resources.
replacements = {
    'resultText.setText(format(y)+" "+b.symbol);':
        'resultText.setText(getString(R.string.conversion_result,format(y),b.symbol));',
    'equation.setText(format(x)+" "+a.symbol+" = "+format(y)+" "+b.symbol+"  •  "+category.name);':
        'equation.setText(getString(R.string.conversion_equation,format(x),a.symbol,format(y),b.symbol,category.name));',
    'equation.setText("Geçerli bir sayı yazın");':
        'equation.setText(getString(R.string.invalid_number));',
    'equation.setText("Dönüşüm hesaplanamadı");':
        'equation.setText(getString(R.string.conversion_failed));',
    't.setText("Unit Master X güvenli mod");':
        't.setText(getString(R.string.safe_mode_fallback));',
}
for old, new in replacements.items():
    if old not in a:
        raise SystemExit(f'Expected MainActivity pattern not found: {old}')
    a = a.replace(old, new)

activity.write_text(a, encoding='utf-8')

# 3) Do not request a fixed/explicit orientation at all; let Android and large screens decide.
m = manifest.read_text(encoding='utf-8')
m = re.sub(r'\s+android:screenOrientation="unspecified"', '', m)
manifest.write_text(m, encoding='utf-8')

# 4) Add localized resource-backed messages and remove the unused app_name entry.
s = strings.read_text(encoding='utf-8')
s = re.sub(r'\s*<string name="app_name">[^<]*</string>\s*', '\n', s)
extra = '''
    <string name="conversion_result">%1$s %2$s</string>
    <string name="conversion_equation">%1$s %2$s = %3$s %4$s • %5$s</string>
    <string name="invalid_number">Geçerli bir sayı yazın</string>
    <string name="conversion_failed">Dönüşüm hesaplanamadı</string>
    <string name="safe_mode_fallback">Unit Master X güvenli mod</string>
'''
if 'name="conversion_result"' not in s:
    s = s.replace('</resources>', extra + '</resources>')
strings.write_text(s, encoding='utf-8')

# 5) Remove an unused color resource reported by release lint.
c = colors.read_text(encoding='utf-8')
c = re.sub(r'\s*<color name="umx_card">[^<]*</color>\s*', '\n', c)
colors.write_text(c, encoding='utf-8')

# 6) minSdk is 26, so the v26 adaptive-icon folder is redundant. Move it to the base anydpi folder.
res = root / 'res'
src_icons = res / 'mipmap-anydpi-v26'
dst_icons = res / 'mipmap-anydpi'
if src_icons.exists():
    dst_icons.mkdir(parents=True, exist_ok=True)
    for src in src_icons.iterdir():
        if not src.is_file():
            continue
        dst = dst_icons / src.name
        if dst.exists():
            dst.unlink()
        shutil.move(str(src), str(dst))
    try:
        src_icons.rmdir()
    except OSError:
        pass

# 7) Android 13 themed icons: add a monochrome layer based on the foreground vector.
for name in ('ic_launcher.xml', 'ic_launcher_round.xml'):
    p = dst_icons / name
    if not p.exists():
        raise SystemExit(f'Adaptive icon missing: {p}')
    x = p.read_text(encoding='utf-8')
    if '<monochrome ' not in x:
        fg = re.search(r'<foreground\s+android:drawable="([^"]+)"\s*/>', x)
        if not fg:
            raise SystemExit(f'Cannot infer foreground drawable for {p}')
        mono = f'    <monochrome android:drawable="{fg.group(1)}"/>\n'
        x = x.replace('</adaptive-icon>', mono + '</adaptive-icon>')
    p.write_text(x, encoding='utf-8')

# 8) Apply consistent high-contrast styling to every picker/dialog menu.
exec(Path('unit-master-v110/menu_contrast_fix.py').read_text(encoding='utf-8'))

print('Applied Unit Master X production polish: nonblocking prefs, resource strings, flexible orientation, clean resources, monochrome icons, menu contrast')
