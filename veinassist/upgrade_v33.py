from pathlib import Path
import runpy

root = Path(__file__).resolve().parent

# First apply the v3.2 skin + long-continuity baseline.
runpy.run_path(str(root / 'upgrade_v32.py'), run_name='__main__')

main_p = root / 'app/src/main/java/com/mg/veinassist/MainActivityV29.java'
an_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

main = main_p.read_text(encoding='utf-8')
an = an_p.read_text(encoding='utf-8')


def must_replace(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Patch point not found: {label}')
    return text.replace(old, new)

# v3.3: algorithm-only redesign. UI/layout/colours stay unchanged.
# Goal: skin-only + true dark-ridge symmetry + long continuity + temporal stability.

# 1) Tracking must not smear old vein candidates when the phone/arm moves.
an = must_replace(an,
    '    private final float[] temporal = new float[N];\n    private final byte[] persistence = new byte[N];',
    '    private final float[] temporal = new float[N];\n    private final float[] prevLum = new float[N];\n    private boolean havePrevLum = false;\n    private final byte[] persistence = new byte[N];',
    'temporal motion fields')

an = must_replace(an,
    '        sampleYuv(f);\n        float sharpness = computeSharpness();',
    '        sampleYuv(f);\n        float sceneDiff = updateSceneMotion();\n        if (sceneDiff > 18.0f) resetTemporalTracking();\n        float sharpness = computeSharpness();',
    'scene motion call')

helper_marker = '    private float computeSharpness() {'
if helper_marker not in an:
    raise RuntimeError('sharpness helper marker not found')
motion_helpers = r'''    private float updateSceneMotion() {
        if (!havePrevLum) {
            System.arraycopy(lum, 0, prevLum, 0, N);
            havePrevLum = true;
            return 0f;
        }
        double sum = 0;
        int n = 0;
        for (int y = 2; y < H - 2; y += 4) {
            for (int x = 2; x < W - 2; x += 4) {
                int i = y * W + x;
                sum += Math.abs(lum[i] - prevLum[i]);
                n++;
            }
        }
        System.arraycopy(lum, 0, prevLum, 0, N);
        return n == 0 ? 0f : (float)(sum / n);
    }

    private void resetTemporalTracking() {
        Arrays.fill(temporal, 0f);
        Arrays.fill(persistence, (byte)0);
    }

'''
an = an.replace(helper_marker, motion_helpers + helper_marker)

# 2) Use a denser learned skin interior. This removes arm boundary, clothing and background edges.
an = must_replace(an,
    'innerSkin[i] = borderSafe && skinDensity[i] >= 0.80f;',
    'innerSkin[i] = borderSafe && skinDensity[i] >= 0.92f;',
    'dense skin core')

# 3) Hair rule: a very thin response without medium/wide support is rejected before continuity.
an = must_replace(an,
    '                float hairPenalty = Math.max(0f, hairRatio - 1.08f) * 2.8f;\n\n                float gx = Math.abs(lum[i + 1] - lum[i - 1]);',
    '                float hairPenalty = Math.max(0f, hairRatio - 1.08f) * 2.8f;\n                if (thin > 6.5f && body < thin * 0.82f) continue;\n\n                float gx = Math.abs(lum[i + 1] - lum[i - 1]);',
    'hair scale rejection')

# 4) Critical fix: a vein is a DARK RIDGE with brighter skin on BOTH sides.
# A silhouette/object edge is bright on only one side, so it must not pass.
old_side = '''                    float sideSum = 0f;
                    int sideN = 0;
                    for (int j = 0; j < 2; j++) {
                        int dx = sideX[k][j];
                        int dy = sideY[k][j];
                        int s1 = (y + dy) * W + (x + dx);
                        int s2 = (y - dy) * W + (x - dx);
                        if (innerSkin[s1]) { sideSum += green[s1]; sideN++; }
                        if (innerSkin[s2]) { sideSum += green[s2]; sideN++; }
                    }
                    float across = sideN == 0 ? 0f : sideSum / sideN - green[i];
                    across = Math.max(0f, Math.min(16f, across));

                    float coherence = lineStrong / 8f;
                    float score = base * 0.48f + lineMean * 0.42f + across * 0.25f;
                    score *= 0.78f + coherence * 0.34f;'''
new_side = '''                    float leftSum = 0f, rightSum = 0f;
                    int leftN = 0, rightN = 0;
                    for (int j = 0; j < 2; j++) {
                        int dx = sideX[k][j];
                        int dy = sideY[k][j];
                        int s1 = (y + dy) * W + (x + dx);
                        int s2 = (y - dy) * W + (x - dx);
                        if (innerSkin[s1]) { leftSum += green[s1]; leftN++; }
                        if (innerSkin[s2]) { rightSum += green[s2]; rightN++; }
                    }
                    if (leftN < 2 || rightN < 2) continue;
                    float leftMean = leftSum / leftN;
                    float rightMean = rightSum / rightN;
                    float leftRise = leftMean - green[i];
                    float rightRise = rightMean - green[i];
                    float bilateral = Math.min(leftRise, rightRise);
                    float sideAsymmetry = Math.abs(leftMean - rightMean);
                    if (bilateral < 1.15f || sideAsymmetry > 17.0f) continue;
                    float across = Math.max(0f, Math.min(16f, bilateral));

                    float coherence = lineStrong / 8f;
                    float symmetryFactor = Math.max(0.72f, 1f - sideAsymmetry / 55f);
                    float score = base * 0.46f + lineMean * 0.43f + across * 0.31f;
                    score *= (0.78f + coherence * 0.34f) * symmetryFactor;'''
an = must_replace(an, old_side, new_side, 'bilateral ridge test')

# 5) Reject the short blue fragments seen in the supplied photo.
an = must_replace(an, 'boolean longEnough = span >= 18;',
                  'boolean longEnough = span >= 22;', 'component span v33')
an = must_replace(an, 'boolean sparseCurve = span >= 24 && fill <= 0.66f;',
                  'boolean sparseCurve = span >= 28 && fill <= 0.66f;', 'curve span v33')

# 6) Final paint gate stays strictly inside the learned skin core.
an = must_replace(an,
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.82f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.92f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'final strict skin paint')

# 7) Anatomy is a SOFT prior even in live mode, stronger only when ATLAS is selected.
# It never creates a line without image evidence. This respects the known large M/N/Y variation
# of superficial forearm/cubital veins.
start_marker = '    /** Anatomy is only a weak orientation prior because superficial venous patterns vary. */'
end_marker = '    private void keepBestSkinComponentInPlace'
if start_marker not in an or end_marker not in an:
    raise RuntimeError('anatomy method markers not found')
start = an.index(start_marker)
end = an.index(end_marker, start)
new_anatomy = r'''    /**
     * Anatomy is only a soft directional prior. Image evidence is always mandatory.
     * Forearm/upper-arm superficial veins are usually longitudinal; the cubital region
     * permits oblique/transverse communicating branches. Individual anatomy can vary.
     */
    private float anatomyFactor(int dir, float skinAxis, boolean armLike) {
        float angle = (float)(dir * Math.PI / 8.0);
        float deg = (float)Math.toDegrees(angleDiff(angle, skinAxis));
        String r = region == null ? "AUTO" : region;
        float strength = atlasPrior ? 1.0f : 0.42f;

        if ("ONKOL".equals(r) || "UST KOL".equals(r)) {
            if (deg <= 24f) return 1f + 0.34f * strength;
            if (deg <= 45f) return 1f + 0.12f * strength;
            if (deg >= 70f) return 1f - 0.38f * strength;
            return 1f - 0.08f * strength;
        }
        if ("DIRSEK".equals(r)) {
            if (deg <= 30f) return 1f + 0.15f * strength;
            if (deg <= 65f) return 1f + 0.10f * strength;
            return 1f - 0.10f * strength;
        }
        if ("EL".equals(r) || "AYAK".equals(r)) return 1.00f;

        if (armLike) {
            if (deg <= 28f) return 1f + 0.15f * strength;
            if (deg >= 72f) return 1f - 0.18f * strength;
        }
        return 1f;
    }

'''
an = an[:start] + new_anatomy + an[end:]

an_p.write_text(an, encoding='utf-8')
main_p.write_text(main, encoding='utf-8')

# Fresh package/version for real A/B testing; this is a genuine new build, not a renamed APK.
build = build_p.read_text(encoding='utf-8')
build = must_replace(build, "applicationId 'com.mg.veinassist.stable32'",
                     "applicationId 'com.mg.veinassist.stable33'", 'application id v33')
build = must_replace(build, 'versionCode 32', 'versionCode 33', 'version code v33')
build = must_replace(build, "versionName '3.2-skin-long-continuity'",
                     "versionName '3.3-bilateral-ridge-skin-graph'", 'version name v33')
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = must_replace(manifest, 'MG VeinAssist v3.2 Continuity',
                        'MG VeinAssist v3.3 Vein First', 'label v33')
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.3 vein-first bilateral-ridge patch applied')
