from pathlib import Path
import runpy

root = Path(__file__).resolve().parent
runpy.run_path(str(root / 'upgrade_v34.py'), run_name='__main__')

main_p = root / 'app/src/main/java/com/mg/veinassist/MainActivityV29.java'
an_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

main = main_p.read_text(encoding='utf-8')
an = an_p.read_text(encoding='utf-8')


def must_replace(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Patch point not found: {label}')
    return text.replace(old, new, 1)

# v3.5 — NO UI/COLOUR REDESIGN.
# 1) Hold +/- to accelerate sensitivity while preserving single-tap stepping.
old_buttons = '''        minus.setOnClickListener(v -> {
            if (sensitivity > 1) sensitivity--;
            updateSensitivity();
        });
        plus.setOnClickListener(v -> {
            if (sensitivity < 200) sensitivity++;
            updateSensitivity();
        });'''
new_buttons = '''        bindSensitivityHold(minus, -1);
        bindSensitivityHold(plus, +1);'''
main = must_replace(main, old_buttons, new_buttons, 'hold sensitivity buttons')

old_update = '''    private void updateSensitivity() {
        sensitivityButton.setText("HASSASIYET " + sensitivity);
        analyzer.setSensitivity(sensitivity);
    }
'''
new_update = '''    private void updateSensitivity() {
        sensitivityButton.setText("HASSASIYET " + sensitivity);
        analyzer.setSensitivity(sensitivity);
    }

    private void changeSensitivity(int delta) {
        int next = Math.max(1, Math.min(200, sensitivity + delta));
        if (next != sensitivity) {
            sensitivity = next;
            updateSensitivity();
        }
    }

    /**
     * One tap = one step. Holding starts after 320 ms and accelerates progressively.
     * This runs only on the UI looper and never touches the camera/processing threads.
     */
    private void bindSensitivityHold(Button button, int delta) {
        final Handler repeatHandler = new Handler(getMainLooper());
        final boolean[] active = {false};
        final long[] downAt = {0L};
        final Runnable[] repeater = new Runnable[1];

        repeater[0] = () -> {
            if (!active[0]) return;
            changeSensitivity(delta);
            long held = SystemClock.uptimeMillis() - downAt[0];
            long delay = held >= 1800L ? 35L : (held >= 900L ? 60L : 105L);
            repeatHandler.postDelayed(repeater[0], delay);
        };

        button.setOnClickListener(v -> { /* handled by touch for exact 1-step taps */ });
        button.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    active[0] = true;
                    downAt[0] = SystemClock.uptimeMillis();
                    v.setPressed(true);
                    changeSensitivity(delta);
                    repeatHandler.postDelayed(repeater[0], 320L);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    active[0] = false;
                    repeatHandler.removeCallbacks(repeater[0]);
                    v.setPressed(false);
                    if (e.getActionMasked() == MotionEvent.ACTION_UP) v.performClick();
                    return true;
                default:
                    return true;
            }
        });
    }
'''
main = must_replace(main, old_update, new_update, 'sensitivity hold helper')
main_p.write_text(main, encoding='utf-8')

# 2) Skin-only paint: tighten the learned-skin interior and never paint near its edge/background.
an = must_replace(an,
    'innerSkin[i] = borderSafe && skinDensity[i] >= 0.92f;',
    'innerSkin[i] = borderSafe && skinDensity[i] >= 0.965f;',
    'v35 dense learned skin interior')
an = must_replace(an,
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.965f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.985f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'v35 final skin-only paint')

# Keep the old user rule: very dark/black hair pixels do not receive the cyan vein overlay.
an = must_replace(an,
    'if (!innerSkin[i] || lum[i] <= 48f) continue;',
    'if (!innerSkin[i] || lum[i] <= 58f) continue;',
    'v35 black hair paint rejection')

# 3) Directional gap bridging: allow a slightly longer interruption, but only deep inside skin
# and only when there is compatible vessel evidence between two aligned endpoints.
an = must_replace(an,
    'for (int dist = 2; dist <= 10; dist++) {',
    'for (int dist = 2; dist <= 12; dist++) {',
    'v35 bridge distance')
an = must_replace(an,
    'if (!innerSkin[q] || skinDensity[q] < 0.94f) {',
    'if (!innerSkin[q] || skinDensity[q] < 0.975f) {',
    'v35 bridge skin safety')
an = must_replace(an,
    'int requiredSupported = Math.max(1, (hitDist - 1) / 3);\n                        if (meanEvidence < threshold * 0.20f || supported < requiredSupported) continue;',
    'int requiredSupported = Math.max(2, (hitDist - 1) / 3);\n                        if (meanEvidence < threshold * 0.23f || supported < requiredSupported) continue;',
    'v35 bridge evidence')

# 4) Temporal continuity: short one-frame blue fragments should not survive.
an = must_replace(an,
    'if (s > threshold * 0.54f) persistence[i] = (byte)Math.min(15, persistence[i] + 1);',
    'if (s > threshold * 0.58f) persistence[i] = (byte)Math.min(15, persistence[i] + 1);',
    'v35 persistence entry')
an = must_replace(an,
    'weak[i] = temporal[i] > threshold * 0.55f && persistence[i] >= 2;',
    'weak[i] = temporal[i] > threshold * 0.60f && persistence[i] >= 2;',
    'v35 weak temporal gate')

# 5) Component/path filter: keep long vessel-like paths, reject dots, blobs and short edge fragments.
old_component = '''            int geodesic = componentGeodesicDiameter(id, start);

            boolean longEnough = span >= 28 && geodesic >= 34;
            boolean longCurve = span >= 38 && geodesic >= 46 && fill <= 0.68f;
            boolean elongated = elongation >= 1.55f && fill <= 0.76f;
            boolean widthOk = width <= 14.5f;
            boolean scoreOk = meanScore >= Math.max(2.65f, thresholdForComponent());
            boolean temporalOk = stableFraction >= 0.43f || strongStable >= 8;
            boolean directionOk = directionLinkFraction >= 0.54f;

            if (longEnough && widthOk && scoreOk && temporalOk && directionOk
                    && (elongated || longCurve)) {'''
new_component = '''            int geodesic = componentGeodesicDiameter(id, start);
            float pathness = geodesic / Math.max(2.0f, width);

            boolean longEnough = span >= 34 && geodesic >= 42;
            boolean longCurve = span >= 44 && geodesic >= 55 && fill <= 0.66f;
            boolean elongated = elongation >= 1.62f && fill <= 0.74f;
            boolean branchLike = geodesic >= 58 && span >= 42 && fill <= 0.52f;
            boolean widthOk = width <= 13.8f;
            boolean scoreOk = meanScore >= Math.max(2.80f, thresholdForComponent() * 1.03f);
            boolean temporalOk = stableFraction >= 0.50f || strongStable >= 10;
            boolean directionOk = directionLinkFraction >= 0.60f;
            boolean pathOk = pathness >= 3.15f || branchLike;

            if (longEnough && widthOk && scoreOk && temporalOk && directionOk && pathOk
                    && (elongated || longCurve || branchLike)) {'''
an = must_replace(an, old_component, new_component, 'v35 path continuity component filter')

# 6) Atlas remains a soft PRIOR only. Slightly stronger orientation support in selected arm regions;
# it still cannot create any pixel without image/skin/temporal evidence.
an = must_replace(an,
    'float strength = atlasPrior ? 1.0f : 0.42f;',
    'float strength = atlasPrior ? 1.08f : 0.46f;',
    'v35 anatomy soft prior')

an_p.write_text(an, encoding='utf-8')

# Fresh installable package/version for real A/B testing.
build = build_p.read_text(encoding='utf-8')
build = must_replace(build,
    "applicationId 'com.mg.veinassist.continuity34'",
    "applicationId 'com.mg.veinassist.continuity35'",
    'v35 application id')
build = must_replace(build, 'versionCode 34', 'versionCode 35', 'v35 version code')
build = must_replace(build,
    "versionName '3.4-continuity-skin-hessian'",
    "versionName '3.5-skin-path-continuity-hold'",
    'v35 version name')
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = must_replace(manifest,
    'MG VeinAssist v3.4 Continuity',
    'MG VeinAssist v3.5 Skin Path',
    'v35 label')
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.5 skin/path continuity + hold sensitivity patch applied')
