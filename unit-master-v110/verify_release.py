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
apk_debug = root / 'app/build/outputs/apk/debug/app-debug.apk'
apk_release = root / 'app/build/outputs/apk/release/app-release.apk'
aab_release = root / 'app/build/outputs/bundle/release/app-release.aab'

errors = []

def require(cond, message):
    if not cond:
        errors.append(message)

b = build.read_text(encoding='utf-8')
m = manifest.read_text(encoding='utf-8')
a = main_activity.read_text(encoding='utf-8')

require(re.search(r'versionCode\s+110\b', b) is not None, 'versionCode must be 110')
require("versionName '1.1.0'" in b, 'versionName must be 1.1.0')
require(re.search(r'targetSdk\s+36\b', b) is not None, 'targetSdk must be 36')
require('minifyEnabled true' in b, 'release minification must be enabled')
require("applicationIdSuffix '.rc110'" in b, 'debug package must be isolated with .rc110')
require('<uses-permission' not in m, 'Unit Master X should not request Android permissions')
require('android.webkit.WebView' not in a and 'WebView' not in a, 'WebView is not allowed in production converter shell')
require('android:exported="true"' in m, 'launcher activity must explicitly declare exported=true')

for path, kind in [(apk_debug, 'debug APK'), (apk_release, 'release APK'), (aab_release, 'release AAB')]:
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

# Aggregate unit-test results into a machine-readable quality report.
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

require(summary['tests'] >= 10, f'Expected at least 10 automated tests, found {summary["tests"]}')
require(summary['failures'] == 0, f'Unit test failures={summary["failures"]}')
require(summary['errors'] == 0, f'Unit test errors={summary["errors"]}')

report = {
    'product': 'Unit Master X',
    'version': '1.1.0',
    'versionCode': 110,
    'targetSdk': 36,
    'native_ui': True,
    'permissions_requested': 0,
    'release_minified': True,
    'unit_test_summary': summary,
    'artifact_checks': {
        'debug_apk': apk_debug.exists(),
        'release_apk': apk_release.exists(),
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
