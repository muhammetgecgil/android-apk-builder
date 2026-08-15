from pathlib import Path
import runpy

root = Path(__file__).resolve().parent

# Keep the complete v3.3 Vein First algorithm unchanged.
runpy.run_path(str(root / 'upgrade_v33.py'), run_name='__main__')

build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

build = build_p.read_text(encoding='utf-8')
old_id = "applicationId 'com.mg.veinassist.stable33'"
new_id = "applicationId 'com.mg.veinassist.installfix331'"
if old_id not in build:
    raise RuntimeError('v3.3 applicationId patch point not found')
build = build.replace(old_id, new_id)

if 'versionCode 33' not in build:
    raise RuntimeError('v3.3 versionCode patch point not found')
build = build.replace('versionCode 33', 'versionCode 331')

if "versionName '3.3-bilateral-ridge-skin-graph'" not in build:
    raise RuntimeError('v3.3 versionName patch point not found')
build = build.replace("versionName '3.3-bilateral-ridge-skin-graph'",
                      "versionName '3.3.1-installfix'" )
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
old_label = 'MG VeinAssist v3.3 Vein First'
new_label = 'MG VeinAssist v3.3 Vein First FIX'
if old_label not in manifest:
    raise RuntimeError('v3.3 label patch point not found')
manifest = manifest.replace(old_label, new_label)
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.3.1 install-fix applied: algorithm unchanged, fresh package id')
