from pathlib import Path

root = Path(__file__).resolve().parent
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

# v3.2: ALGORITHM ONLY. UI/layout/colours are deliberately not changed.

# 1) Keep the requested wide sensitivity range.
main = must_replace(main,
    'if (sensitivity < 99) sensitivity++;',
    'if (sensitivity < 200) sensitivity++;',
    'sensitivity button')
an = must_replace(an,
    'void setSensitivity(int s) { sensitivity = Math.max(1, Math.min(99, s)); }',
    'void setSensitivity(int s) { sensitivity = Math.max(1, Math.min(200, s)); }',
    'analyzer sensitivity')

# 2) Sensitivity 100..200 must really affect detection, but continuity filtering remains strict.
an = must_replace(an,
    'float threshold = Math.max(5.9f, 8.75f - sensitivity * 0.026f);',
    'float threshold = Math.max(2.55f, 9.10f - sensitivity * 0.0325f);',
    'threshold')

# 3) Learn skin from the ORIGINAL frame and keep only the dense interior of the largest limb.
# The centre of a vein can be darker/bluer than skin, therefore use neighbourhood density instead
# of requiring the vein centre pixel itself to pass the skin-colour test.
an = must_replace(an,
    'innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.955f;',
    'innerSkin[i] = borderSafe && skinDensity[i] >= 0.80f;',
    'inner skin')

# 4) Directional evidence: allow a weak vein centre to enter ONLY if it has support along a line.
an = must_replace(an,
    'if (!innerSkin[i] || base < threshold * 0.30f) continue;',
    'if (!innerSkin[i] || base < threshold * 0.20f) continue;',
    'direction base')
an = must_replace(an,
    'if (raw[i1] > threshold * 0.24f) lineStrong++;',
    'if (raw[i1] > threshold * 0.17f) lineStrong++;',
    'line strong positive')
an = must_replace(an,
    'if (raw[i2] > threshold * 0.24f) lineStrong++;',
    'if (raw[i2] > threshold * 0.17f) lineStrong++;',
    'line strong negative')
an = must_replace(an,
    'if (lineSkin < 7 || lineStrong < 5) continue;',
    'if (lineSkin < 6 || lineStrong < 4) continue;',
    'line support')
an = must_replace(an,
    'float score = base * 0.56f + lineMean * 0.34f + across * 0.22f;',
    'float score = base * 0.48f + lineMean * 0.42f + across * 0.25f;',
    'continuity score')

# 5) Temporal persistence. Stable long veins accumulate; one-frame edge noise does not.
an = must_replace(an,
    'if (s > threshold * 0.68f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);',
    'if (s > threshold * 0.50f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);',
    'persistence')
an = must_replace(an,
    'strong[i] = temporal[i] > threshold * 1.08f && persistence[i] >= 2;',
    'strong[i] = temporal[i] > threshold * 0.94f && persistence[i] >= 2;',
    'strong hysteresis')
an = must_replace(an,
    'weak[i] = temporal[i] > threshold * 0.66f && persistence[i] >= 2;',
    'weak[i] = temporal[i] > threshold * 0.48f && persistence[i] >= 2;',
    'weak hysteresis')

# 6) Replace tiny 1-2 px bridge with a directional 2-7 px bridge. It can only cross the learned
# skin interior, must meet another candidate with a compatible direction and must have weak vessel
# evidence through the gap. This is the core 'follow the vein continuously' change.
an = must_replace(an, 'bridgeShortGaps();', 'bridgeDirectionalGaps(threshold);', 'bridge call')
start_marker = '    /** Fill only short 1-2 px gaps with vessel evidence on opposite sides. */'
end_marker = '    private int filterContinuousComponents'
if start_marker not in an or end_marker not in an:
    raise RuntimeError('Bridge method markers not found')
start = an.index(start_marker)
end = an.index(end_marker, start)
bridge_method = r'''    /**
     * Directional continuity bridge: joins short interruptions only when both ends agree on
     * vessel direction, the complete gap stays inside the learned skin region and the gap
     * contains weak vessel evidence. It does NOT simply dilate isolated dots together.
     */
    private void bridgeDirectionalGaps(float threshold) {
        System.arraycopy(candidate, 0, bridge, 0, N);

        for (int y = 8; y < H - 8; y++) {
            for (int x = 8; x < W - 8; x++) {
                int p = y * W + x;
                if (!candidate[p] || !innerSkin[p]) continue;
                int d0 = bestDir[p] & 0xff;

                for (int dk = -1; dk <= 1; dk++) {
                    int k = (d0 + dk + 8) % 8;
                    int dx = sign(offX[k][0]);
                    int dy = sign(offY[k][0]);
                    if (dx == 0 && dy == 0) continue;

                    for (int signDir : new int[]{-1, 1}) {
                        int ex = dx * signDir;
                        int ey = dy * signDir;
                        int hitDist = 0;
                        int hit = -1;
                        float evidence = 0f;
                        int evidenceN = 0;
                        boolean allSkin = true;

                        for (int dist = 2; dist <= 7; dist++) {
                            int nx = x + ex * dist;
                            int ny = y + ey * dist;
                            int q = ny * W + nx;
                            if (!innerSkin[q]) { allSkin = false; break; }
                            evidence += Math.max(raw[q], continuous[q] * 0.55f);
                            evidenceN++;
                            if (candidate[q] && dirDiff(k, bestDir[q] & 0xff) <= 2) {
                                hit = q;
                                hitDist = dist;
                                break;
                            }
                        }

                        if (!allSkin || hit < 0 || hitDist < 3) continue;
                        float meanEvidence = evidence / Math.max(1, evidenceN);
                        if (meanEvidence < threshold * 0.14f) continue;

                        float endScore = Math.min(temporal[p], temporal[hit]);
                        for (int dist = 1; dist < hitDist; dist++) {
                            int nx = x + ex * dist;
                            int ny = y + ey * dist;
                            int q = ny * W + nx;
                            if (!innerSkin[q]) break;
                            bridge[q] = true;
                            bestDir[q] = (byte)k;
                            temporal[q] = Math.max(temporal[q], endScore * 0.68f);
                        }
                    }
                }
            }
        }
        System.arraycopy(bridge, 0, candidate, 0, N);
    }

'''
an = an[:start] + bridge_method + an[end:]

# 7) Component-level continuity filter. The photo showed many short blue fragments; reject them.
# A retained structure must span a meaningful distance and be elongated/curve-like, not a compact blob.
an = must_replace(an, 'boolean longEnough = span >= 13;',
                  'boolean longEnough = span >= 18;', 'component span')
an = must_replace(an, 'boolean sparseCurve = span >= 22 && fill <= 0.58f;',
                  'boolean sparseCurve = span >= 24 && fill <= 0.66f;', 'sparse curve')
an = must_replace(an, 'boolean elongated = elongation >= 1.55f && fill <= 0.76f;',
                  'boolean elongated = elongation >= 1.45f && fill <= 0.78f;', 'elongation')
an = must_replace(an, 'boolean widthOk = width <= 16.0f;',
                  'boolean widthOk = width <= 16.0f;', 'width')
an = must_replace(an, 'boolean scoreOk = meanScore >= 4.8f;',
                  'boolean scoreOk = meanScore >= Math.max(2.35f, thresholdForComponent());', 'component score')

helper_marker = '    /** Adds one-pixel cross-vessel thickness, but only inside the learned skin. */'
if helper_marker not in an:
    raise RuntimeError('Component helper marker not found')
helper = '''    private float thresholdForComponent() {\n        float t = Math.max(2.55f, 9.10f - sensitivity * 0.0325f);\n        return t * 0.58f;\n    }\n\n'''
an = an.replace(helper_marker, helper + helper_marker)

# 8) Final safety gate: never paint cyan/blue on bright neutral objects or outside dense learned skin.
# This affects only the overlay; the original camera image itself is not recoloured.
an = must_replace(an,
    'if (expandedKeep[i] && innerSkin[i]) {',
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.82f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'final skin paint gate')

# 9) Anatomy remains a PRIOR only. It may favour anatomically plausible directions but can never
# create a vein without image evidence. Forearm/upper-arm superficial veins are usually longitudinal;
# elbow allows more oblique/transverse communicating patterns.
an = must_replace(an, 'if (deg <= 24f) return 1.24f;',
                  'if (deg <= 24f) return 1.34f;', 'anatomy longitudinal strong')
an = must_replace(an, 'if (deg <= 45f) return 1.08f;',
                  'if (deg <= 45f) return 1.12f;', 'anatomy longitudinal medium')
an = must_replace(an, 'if (deg >= 70f) return 0.70f;',
                  'if (deg >= 70f) return 0.62f;', 'anatomy transverse reject')

main_p.write_text(main, encoding='utf-8')
an_p.write_text(an, encoding='utf-8')

# Separate installable package for safe A/B testing; no renamed/copy-only APK.
build = build_p.read_text(encoding='utf-8')
build = must_replace(build, "applicationId 'com.mg.veinassist.stable30'",
                     "applicationId 'com.mg.veinassist.stable32'", 'application id')
build = must_replace(build, 'versionCode 30', 'versionCode 32', 'version code')
build = must_replace(build, "versionName '3.0-skin-continuity-anatomy'",
                     "versionName '3.2-skin-long-continuity'", 'version name')
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = must_replace(manifest, 'MG VeinAssist v3.0 Stable',
                        'MG VeinAssist v3.2 Continuity', 'app label')
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.2 algorithm-only skin/continuity patch applied')
