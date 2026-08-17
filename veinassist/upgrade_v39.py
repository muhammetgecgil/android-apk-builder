from pathlib import Path
import runpy

root = Path(__file__).resolve().parent
runpy.run_path(str(root / 'upgrade_v38.py'), run_name='__main__')

main_p = root / 'app/src/main/java/com/mg/veinassist/MainActivityV29.java'
an_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'
icon_p = root / 'app/src/main/res/drawable/ic_launcher.xml'

main = main_p.read_text(encoding='utf-8')
an = an_p.read_text(encoding='utf-8')


def must_replace(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Patch point not found: {label}')
    return text.replace(old, new, 1)

# UI: keep v3.8 palette/layout language; add only BILEK + explicit TAKIP control.
main = must_replace(main,
    '    private Button sensitivityButton, freezeButton, liveButton, atlasButton, maskButton, thermalButton, autoButton;',
    '    private Button sensitivityButton, freezeButton, liveButton, atlasButton, maskButton, thermalButton, autoButton, trackButton;',
    'track button field')
main = must_replace(main,
    '    private boolean atlasPrior = false;\n    private volatile boolean frozen = false;',
    '    private boolean atlasPrior = false;\n    private boolean trackingMode = false;\n    private volatile boolean frozen = false;',
    'tracking state')
main = must_replace(main,
    '    private final String[] regions = {"AUTO", "EL", "ONKOL", "DIRSEK", "UST KOL", "AYAK"};',
    '    private final String[] regions = {"AUTO", "EL", "BILEK", "ONKOL", "DIRSEK", "UST KOL", "AYAK"};',
    'wrist region')
main = must_replace(main,
    '        analyzer.setRegion(region);',
    '        analyzer.setRegion(region);\n        analyzer.setTrackingMode(trackingMode);',
    'initial tracking state')
main = must_replace(main,
    '        freezeButton = button("DONDUR");\n        thermalButton = button("TERMAL MOD");\n        controls.addView(row(freezeButton, thermalButton));',
    '        freezeButton = button("DONDUR");\n        trackButton = button("TAKIP KAPALI");\n        thermalButton = button("TERMAL MOD");\n        controls.addView(row(freezeButton, trackButton, thermalButton));',
    'tracking control row')
main = must_replace(main,
    '        freezeButton.setOnClickListener(v -> toggleFreeze());',
    '        freezeButton.setOnClickListener(v -> toggleFreeze());\n        trackButton.setOnClickListener(v -> toggleTracking());',
    'tracking click')
main = must_replace(main,
    '        autoButton.setOnClickListener(v -> {\n            regionIndex = 0;\n            region = "AUTO";\n            autoButton.setText("AUTO");\n            analyzer.setRegion(region);\n            updateHeader(null);\n        });',
    '        autoButton.setOnClickListener(v -> {\n            regionIndex = 0;\n            region = "AUTO";\n            autoButton.setText("AUTO");\n            analyzer.setRegion(region);\n            updateHeader(null);\n        });\n        autoButton.setOnLongClickListener(v -> {\n            regionIndex = 2;\n            region = "BILEK";\n            autoButton.setText("BILEK");\n            analyzer.setRegion(region);\n            updateHeader(null);\n            return true;\n        });',
    'wrist long press shortcut')
main = must_replace(main,
    '    private void updateSensitivity() {',
    '    private void toggleTracking() {\n        trackingMode = !trackingMode;\n        analyzer.setTrackingMode(trackingMode);\n        trackButton.setText(trackingMode ? "TAKIP ACIK" : "TAKIP KAPALI");\n        select(trackButton, trackingMode);\n        updateHeader(null);\n    }\n\n    private void updateSensitivity() {',
    'tracking toggle method')
main = must_replace(main,
    '        select(thermalButton, viewMode == VeinAnalyzerV29.MODE_THERMAL);',
    '        select(thermalButton, viewMode == VeinAnalyzerV29.MODE_THERMAL);\n        select(trackButton, trackingMode);',
    'tracking selected state')
main = must_replace(main,
    '        String foc = focusSeeking ? "AF" : (focusLocked ? "KILIT" : "AF?");',
    '        String foc = focusSeeking ? "AF" : (focusLocked ? "KILIT" : "AF?");\n        String trk = trackingMode ? "TAKIP" : "SERBEST";',
    'tracking header state')
main = must_replace(main,
    '            statusText.setText(String.format("BOLGE %s   %s   ZOOM %.1fx", region, foc, zoomRatio));',
    '            statusText.setText(String.format("BOLGE %s   %s   %s   ZOOM %.1fx", region, foc, trk, zoomRatio));',
    'tracking header compact')
main = must_replace(main,
    '                    "BOLGE %s   NET %.0f   CIZGI %d   TEN %d%%   %s   ZOOM %.1fx",\n                    region, r.sharpness, r.lineCount, r.skinPercent, foc, zoomRatio));',
    '                    "BOLGE %s   NET %.0f   CIZGI %d   TEN %d%%   %s   %s   ZOOM %.1fx",\n                    region, r.sharpness, r.lineCount, r.skinPercent, foc, trk, zoomRatio));',
    'tracking header detailed')
main_p.write_text(main, encoding='utf-8')

# Analyzer: wrist-aware, skin-only temporal vessel tracking.
an = must_replace(an,
    '    private String region = "AUTO";',
    '    private String region = "AUTO";\n    private boolean trackingMode = false;\n    private final float[] trackMemory = new float[N];\n    private final boolean[] trackSupport = new boolean[N];',
    'tracking analyzer buffers')
an = must_replace(an,
    '    void setRegion(String r) { region = (r == null ? "AUTO" : r); }',
    '    void setRegion(String r) { region = (r == null ? "AUTO" : r); }\n    void setTrackingMode(boolean enabled) { trackingMode = enabled; }',
    'tracking setter')
an = must_replace(an,
    '        int components = filterContinuousComponents(candidate);\n        thickenKeptOnePixel();',
    '        int components = filterContinuousComponents(candidate);\n        thickenKeptOnePixel();\n        applyTrackedContinuity(threshold);',
    'tracking continuity call')
an = must_replace(an,
    '        if ("ONKOL".equals(r) || "UST KOL".equals(r)) {',
    '        if ("ONKOL".equals(r) || "BILEK".equals(r) || "UST KOL".equals(r)) {',
    'wrist anatomy prior')

tracking_method = r'''
    /**
     * Frame-to-frame vessel tracking.
     * Remembers only accepted image paths and requires current RGB evidence + dense skin.
     */
    private void applyTrackedContinuity(float threshold) {
        Arrays.fill(trackSupport, false);
        for (int y = 4; y < H - 4; y++) {
            for (int x = 4; x < W - 4; x++) {
                int i = y * W + x;
                if (trackMemory[i] < 0.44f) continue;
                for (int oy = -4; oy <= 4; oy++) {
                    for (int ox = -4; ox <= 4; ox++) {
                        if (ox * ox + oy * oy > 18) continue;
                        int q = (y + oy) * W + (x + ox);
                        if (innerSkin[q] && skinDensity[q] >= 0.955f) trackSupport[q] = true;
                    }
                }
            }
        }

        if (trackingMode) {
            for (int i = 0; i < N; i++) {
                if (expandedKeep[i] || !trackSupport[i] || !innerSkin[i]) continue;
                if (skinDensity[i] < 0.965f || lum[i] <= 58f) continue;
                boolean currentEvidence = raw[i] > threshold * 0.10f
                        && temporal[i] > threshold * 0.30f
                        && ridgeSoftness[i] >= 0.30f;
                if (currentEvidence) expandedKeep[i] = true;
            }

            int maxGap = "BILEK".equals(region) ? 10 : 7;
            boolean[] add = new boolean[N];
            for (int y = 12; y < H - 12; y++) {
                for (int x = 12; x < W - 12; x++) {
                    int i = y * W + x;
                    if (!expandedKeep[i] || !innerSkin[i]) continue;
                    int d = bestDir[i] & 0xff;
                    int dx = sign(offX[d][0]);
                    int dy = sign(offY[d][0]);
                    if (dx == 0 && dy == 0) continue;

                    for (int signDir : new int[]{-1, 1}) {
                        int hit = -1;
                        int supported = 0;
                        float evidence = 0f;
                        boolean skinOk = true;
                        for (int dist = 2; dist <= maxGap; dist++) {
                            int nx = x + dx * dist * signDir;
                            int ny = y + dy * dist * signDir;
                            int q = ny * W + nx;
                            if (!innerSkin[q] || skinDensity[q] < 0.965f) {
                                skinOk = false;
                                break;
                            }
                            float e = Math.max(raw[q], temporal[q] * 0.72f);
                            evidence += e;
                            if (e > threshold * 0.10f) supported++;
                            if (expandedKeep[q] || trackMemory[q] > 0.58f) {
                                hit = dist;
                                break;
                            }
                        }
                        if (!skinOk || hit < 3) continue;
                        int needed = Math.max(1, (hit - 1) / 3);
                        float meanEvidence = evidence / Math.max(1, hit - 1);
                        if (supported < needed || meanEvidence < threshold * 0.075f) continue;

                        for (int dist = 1; dist < hit; dist++) {
                            int nx = x + dx * dist * signDir;
                            int ny = y + dy * dist * signDir;
                            int q = ny * W + nx;
                            if (innerSkin[q] && skinDensity[q] >= 0.965f && lum[q] > 58f) {
                                add[q] = true;
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < N; i++) if (add[i]) expandedKeep[i] = true;
        }

        for (int i = 0; i < N; i++) {
            float accepted = expandedKeep[i] ? 1f : 0f;
            float decay = trackingMode ? 0.88f : 0.72f;
            trackMemory[i] = Math.min(1f, trackMemory[i] * decay + accepted * 0.46f);
            if (!innerSkin[i]) trackMemory[i] *= 0.35f;
        }
    }

'''
anchor = '    private void keepBestSkinComponentInPlace(boolean[] src) {'
if anchor not in an:
    raise RuntimeError('Patch point not found: tracking method anchor')
an = an.replace(anchor, tracking_method + anchor, 1)
an_p.write_text(an, encoding='utf-8')

# Version/package + launcher icon.
build = build_p.read_text(encoding='utf-8')
build = must_replace(build, "applicationId 'com.mg.veinassist.adaptive38'", "applicationId 'com.mg.veinassist.wristtrack39'", 'v39 application id')
build = must_replace(build, 'versionCode 38', 'versionCode 39', 'v39 version code')
build = must_replace(build, "versionName '3.8-adaptive-rgb-path'", "versionName '3.9-wrist-track'", 'v39 version name')
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = must_replace(manifest, 'MG VeinAssist v3.8 Adaptive Path', 'MG VeinAssist v3.9 Wrist Track', 'v39 label')
if 'android:icon=' not in manifest:
    manifest = must_replace(manifest,
        '        android:allowBackup="false"\n        android:label=',
        '        android:allowBackup="false"\n        android:icon="@drawable/ic_launcher"\n        android:label=',
        'launcher icon manifest')
manifest_p.write_text(manifest, encoding='utf-8')

icon_p.parent.mkdir(parents=True, exist_ok=True)
icon_p.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#06131D" android:pathData="M0,0h108v108h-108z"/>
    <path android:fillColor="#0B2632" android:pathData="M54,7A47,47 0,1 0,54 101A47,47 0,1 0,54 7"/>
    <path android:fillColor="@android:color/transparent"
        android:strokeColor="#19DCDC" android:strokeWidth="8"
        android:strokeLineCap="round" android:strokeLineJoin="round"
        android:pathData="M54,91 C54,74 53,62 54,51 C54,39 48,31 38,23 M54,51 C61,43 68,34 76,22 M54,63 C42,58 31,51 24,41"/>
    <path android:fillColor="#FFFFFF" android:pathData="M50,47h8v8h-8z"/>
    <path android:fillColor="#19DCDC" android:pathData="M34,19A5,5 0,1 0,34 29A5,5 0,1 0,34 19 M76,17A5,5 0,1 0,76 27A5,5 0,1 0,76 17 M21,36A5,5 0,1 0,21 46A5,5 0,1 0,21 36"/>
</vector>
''', encoding='utf-8')

print('MG VeinAssist v3.9 wrist region + skin-only temporal vessel tracking + launcher icon applied')
