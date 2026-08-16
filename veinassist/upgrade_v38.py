from pathlib import Path
import runpy

root = Path(__file__).resolve().parent
runpy.run_path(str(root / 'upgrade_v37.py'), run_name='__main__')

analyzer_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

s = analyzer_p.read_text(encoding='utf-8')

def rep(old, new, label):
    global s
    if old not in s:
        raise RuntimeError(f'Patch point not found: {label}')
    s = s.replace(old, new, 1)

# v3.8: UI/layout/palette stay exactly as v3.7. Algorithm only.
# Real S24 Ultra test showed TEN detected but CIZGI=0: v3.7 rejected weak RGB
# vessel evidence too early. Keep skin/background safety but allow weak evidence
# to accumulate into a continuous path across several frames.

rep('float threshold = Math.max(3.05f, 9.35f - sensitivity * 0.0305f);',
    'float threshold = Math.max(2.05f, 8.10f - sensitivity * 0.0540f);',
    'adaptive RGB threshold')

# Permit continuity analysis a little closer to the learned limb interior.
# Final painting remains guarded by the separate dense-skin paint gate.
rep('innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.978f;',
    'innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.955f;',
    'inner skin continuity support')

# Visible-light vein contrast can change sign with exposure/white balance.
# Use the magnitude of local R-G residual. Hair/creases are still suppressed by
# black-pixel, thinness, edge and bilateral-profile tests.
rep('float haemoglobinCue = Math.max(0f, rgIndex[i] - rgMean10[i]);\n                haemoglobinCue = Math.min(7.0f, haemoglobinCue * 0.34f);',
    'float rgResidual = rgIndex[i] - rgMean10[i];\n                float haemoglobinCue = Math.min(7.0f, Math.abs(rgResidual) * 0.30f);',
    'bidirectional RGB chromatic cue')

rep('float ridge = body * 0.56f\n                        + illuminationNorm * 0.46f\n                        + hessian * 0.78f\n                        + haemoglobinCue * 0.56f',
    'float ridge = body * 0.64f\n                        + illuminationNorm * 0.60f\n                        + hessian * 0.86f\n                        + haemoglobinCue * 0.72f',
    'weak vessel response gain')

# Long directional agreement is more important than high single-pixel contrast.
rep('if (lineSkin < 8 || lineStrong < 6) continue;',
    'if (lineSkin < 7 || lineStrong < 4) continue;',
    'directional continuity support')
rep('if (bilateral < 1.20f || outerBilateral < 0.95f || sideAsymmetry > 13.5f) continue;',
    'if (bilateral < 0.78f || outerBilateral < 0.58f || sideAsymmetry > 15.5f) continue;',
    'soft ridge bilateral profile')

# Let faint but stable evidence build over multiple frames.
rep('if (s > threshold * 0.72f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);',
    'if (s > threshold * 0.52f) persistence[i] = (byte)Math.min(15, persistence[i] + 1);',
    'persistence entry')
rep('strong[i] = temporal[i] > threshold * 1.12f && persistence[i] >= 3;',
    'strong[i] = temporal[i] > threshold * 0.96f && persistence[i] >= 2;',
    'strong temporal seed')
rep('weak[i] = temporal[i] > threshold * 0.72f && persistence[i] >= 3;',
    'weak[i] = temporal[i] > threshold * 0.52f && persistence[i] >= 2;',
    'weak temporal path')

# Keep long vessel-like curves; reject dots/blobs, but don't require bright contrast.
rep('if (size < 18) continue;', 'if (size < 10) continue;', 'component minimum size')
rep('boolean longEnough = span >= 18;', 'boolean longEnough = span >= 14;', 'component span')
rep('boolean sparseCurve = span >= 28 && fill <= 0.54f;',
    'boolean sparseCurve = span >= 22 && fill <= 0.62f;',
    'sparse curve')
rep('boolean elongated = elongation >= 1.80f && fill <= 0.70f;',
    'boolean elongated = elongation >= 1.52f && fill <= 0.76f;',
    'elongation')
rep('boolean scoreOk = meanScore >= 5.25f;',
    'boolean scoreOk = meanScore >= Math.max(2.85f, thresholdForComponent() * 0.80f);',
    'adaptive component score')

# Anatomy remains only a weak orientation prior after image evidence exists.
rep('if (d0 <= 24f) livePrior = 1.07f;\n            else if (d0 >= 68f) livePrior = 0.90f;',
    'if (d0 <= 28f) livePrior = 1.12f;\n            else if (d0 >= 72f) livePrior = 0.88f;',
    'live anatomy prior')

analyzer_p.write_text(s, encoding='utf-8')

build = build_p.read_text(encoding='utf-8')
if "applicationId 'com.mg.veinassist.continuity37'" not in build:
    raise RuntimeError('v3.8 build patch point missing')
build = build.replace("applicationId 'com.mg.veinassist.continuity37'", "applicationId 'com.mg.veinassist.adaptive38'", 1)
build = build.replace('versionCode 37', 'versionCode 38', 1)
build = build.replace("versionName '3.7-skin-continuity'", "versionName '3.8-adaptive-rgb-path'", 1)
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = manifest.replace('MG VeinAssist v3.7 Continuity', 'MG VeinAssist v3.8 Adaptive Path', 1)
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.8 adaptive RGB vessel-path detector applied; UI/palette unchanged')
