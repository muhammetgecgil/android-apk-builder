from pathlib import Path
import json
import re
import sys
import zipfile
import xml.etree.ElementTree as ET

root = Path('unit-master')
build = root / 'app/build.gradle'
manifest = root / 'app/src/main/AndroidManifest.xml'
main_activity = root / 'app/src/main/java/com/mg/unitmasterx/MainActivity.java'
selected_row = root / 'app/src/main/res/layout/selected_spinner_row.xml'
white_row = root / 'app/src/main/res/layout/white_menu_row.xml'
selected_bg = root / 'app/src/main/res/drawable/selected_field_bg.xml'
apk_debug = root / 'app/build/outputs/apk/debug/app-debug.apk'
release_dir = root / 'app/build/outputs/apk/release'
release_apks = sorted(release_dir.glob('*.apk')) if release_dir.exists() else []
apk_release = release_apks[0] if release_apks else None
aab_release = root / 'app/build/outputs/bundle/release/app-release.aab'

EXPECTED_VERSION_CODE = 112
EXPECTED_VERSION_NAME = '1.1.2'

errors = []

def require(cond, message):
    if not cond:
        errors.append(message)

b = build.read_text(encoding='utf-8')
m = manifest.read_text(encoding='utf-8')
a = main_activity.read_text(encoding='utf-8')

require(re.search(rf'versionCode\s+{EXPECTED_VERSION_CODE}\b', b) is not None,
        f'versionCode must be {EXPECTED_VERSION_CODE}')
require(re.search(rf'versionName\s+[\"\']{re.escape(EXPECTED_VERSION_NAME)}[\"\']', b) is not None,
        f'versionName must be {EXPECTED_VERSION_NAME}')
require(re.search(r'targetSdk\s+36\b', b) is not None, 'targetSdk must be 36')
require('minifyEnabled true' in b, 'release minification must be enabled')
require("applicationIdSuffix '.rc110'" in b, 'debug package must remain isolated with .rc110')
require('<uses-permission' not in m, 'Unit Master X should not request Android permissions')
require('android.webkit.WebView' not in a and 'WebView' not in a, 'WebView is not allowed in production converter shell')
require('android:exported="true"' in m, 'launcher activity must explicitly declare exported=true')
require('android:screenOrientation=' not in m, 'launcher activity must not force screen orientation')
require('.commit()' not in a, 'blocking SharedPreferences.commit() is not allowed in production UI code')

# Picker contrast regression gate:
# Closed fields must be dark/readable; opened menus must be white/readable.
require(selected_row.exists(), 'selected_spinner_row.xml must exist for closed Spinner state')
require(white_row.exists(), 'white_menu_row.xml must exist for opened menu rows')
require(selected_bg.exists(), 'selected_field_bg.xml must exist for filled closed selector fields')
if selected_row.exists():
    sr = selected_row.read_text(encoding='utf-8')
    require('@drawable/selected_field_bg' in sr, 'closed selector must use dark filled background drawable')
    require('#FFF7FAFF' in sr, 'closed selector text must be high-contrast light text')
if white_row.exists():
    wr = white_row.read_text(encoding='utf-8')
    require('#FFFFFFFF' in wr, 'opened menu rows must be opaque white')
    require('#FF14181C' in wr, 'opened menu rows must use dark text')
if selected_bg.exists():
    sb = selected_bg.read_text(encoding='utf-8')
    require('#FF0F2238' in sb, 'closed selector fill must be deep navy')

require('R.layout.selected_spinner_row' in a, 'closed Spinners must use selected_spinner_row')
require('R.layout.white_menu_row' in a, 'opened menus must use white_menu_row')
require('setDropDownViewResource(R.layout.white_menu_row)' in a,
        'Spinner dropdown rows must explicitly use white_menu_row')
require('setPopupBackgroundDrawable(new android.graphics.drawable.ColorDrawable(MENU_BG))' in a,
        'Spinner popup windows must have explicit white background')
require('private static final int MENU_BG=Color.WHITE;' in a,
        'menu popup surface must be explicitly white')
require('RC 1.1.2' in a, 'visible build label must identify RC 1.1.2')

artifacts = [(apk_debug, 'debug APK')]
if apk_release is None:
    errors.append(f'Missing release APK in {release_dir}')
else:
    artifacts.append((apk_release, 'release APK'))
artifacts.append((aab_release, 'release AAB'))

for path, kind in artifacts:
    require(path.exists(), f'Missing {kind}: {path}')
    if path.exists():
        require(path.stat().st_size > 20_000, f'{kind} is unexpectedly small')
        try:
            with zipfile.ZipFile(path) as z:
                bad = z.testzip()
                require(bad is None, f'{kind} ZIP CRC failure: {bad}')
                names = set(z.namelist())
                if path.suffix == '.apk':
                    require('AndroidManifest.xml' in names, f'{kind} missing AndroidManifest.xml')
                    require('classes.dex' in names, f'{kind} missing classes.dex')
                else:
                    require('base/manifest/AndroidManifest.xml' in names, 'AAB missing base manifest')
                    require(any(n.startswith('base/dex/classes') and n.endswith('.dex') for n in names), 'AAB missing dex payload')
        except zipfile.BadZipFile:
            errors.append(f'{kind} is not a valid ZIP container')

# Aggregate both debug and release unit-test results into a machine-readable quality report.
test_root = root / 'app/build/test-results'
summary = {'test_suites': 0, 'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
for xml in test_root.rglob('*.xml') if test_root.exists() else []:
    try:
        e = ET.parse(xml).getroot()
        summary['test_suites'] += 1
        for key in ('tests', 'failures', 'errors', 'skipped'):
            summary[key] += int(e.attrib.get(key, 0))
    except Exception as exc:
        errors.append(f'Cannot parse test result {xml}: {exc}')

require(summary['tests'] >= 20, f'Expected at least 20 automated debug+release tests, found {summary["tests"]}')
require(summary['failures'] == 0, f'Unit test failures={summary["failures"]}')
require(summary['errors'] == 0, f'Unit test errors={summary["errors"]}')
require(summary['skipped'] == 0, f'Unit test skipped={summary["skipped"]}')

report = {
    'product': 'Unit Master X',
    'version': EXPECTED_VERSION_NAME,
    'versionCode': EXPECTED_VERSION_CODE,
    'targetSdk': 36,
    'native_ui': True,
    'permissions_requested': 0,
    'release_minified': True,
    'picker_contrast_gate': {
        'closed_fields': 'deep navy with light text',
        'opened_menus': 'white with dark text',
        'status': True,
    },
    'unit_test_summary': summary,
    'artifact_checks': {
        'debug_apk': apk_debug.exists(),
        'release_apk': bool(apk_release and apk_release.exists()),
        'release_apk_name': apk_release.name if apk_release else None,
        'release_aab': aab_release.exists(),
    },
    'status': 'PASS' if not errors else 'FAIL',
    'errors': errors,
}
out = root / 'build/product-quality-report.json'
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))

if errors:
    print('\nPRODUCT QUALITY GATE FAILED:', file=sys.stderr)
    for err in errors:
        print(' - ' + err, file=sys.stderr)
    raise SystemExit(1)

print('PRODUCT QUALITY GATE PASS')
