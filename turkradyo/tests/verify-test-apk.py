"""Check the delivered APK, including its signature and independent identity."""
import os
from pathlib import Path
import re
import subprocess

project = Path(__file__).resolve().parents[1]
apk = project / "app/build/outputs/apk/debug/app-debug.apk"
sdk = Path(os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"])
build_tools = sdk / "build-tools/35.0.0"

subprocess.run([str(build_tools / "apksigner"), "verify", "--verbose", str(apk)], check=True)
badging = subprocess.check_output([str(build_tools / "aapt"), "dump", "badging", str(apk)], text=True)
package = re.search(r"^package: name='([^']+)'", badging, re.M)
assert package and package[1] == "com.muhammetgecgil.turkradyo.test.v300", badging
assert "application-label:'MGtürk Radyo 2.9.0'" in badging, badging
assert "versionName='2.9.0-test'" in badging, badging
assert "launchable-activity: name='com.muhammetgecgil.turkradyo.MainActivity'" in badging, badging
print("Verified signed side-by-side APK: " + package[1])

manifest = subprocess.check_output([str(build_tools / "aapt"), "dump", "xmltree", str(apk), "AndroidManifest.xml"], text=True)
assert re.search(r"android:screenOrientation[^\n]*\(type 0x10\)0x1\b", manifest), manifest
assert "android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY" in manifest, manifest
print("Verified portrait orientation in packaged Android manifest")
