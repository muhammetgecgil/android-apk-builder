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

# v3.8 algorithm-only update. UI/layout/palette remain unchanged.
# S24 Ultra field test: learned skin succeeds (TEN ~83%) while the old path gate
# returns CIZGI=0. The fix is weak-evidence accumulation + continuity, not atlas hallucination.

# At sensitivity 73 this lowers the working floor substantially while retaining a hard floor.
rep('float threshold = Math.max(3.05f, 9.35f - sensitivity * 0.0305f);',
    'float threshold = Math.max(1.85f, 7.70f - sensitivity * 0.0520f);',
    'adaptive RGB threshold')

# v3.5's actual detector interior is 0.965. Relax only analysis support; final cyan paint
# still has its separate 0.985 dense-skin gate, so tables/chairs/background stay unpainted.
rep('innerSkin[i] = borderSafe && skinDensity[i] >= 0.965f;',
    'innerSkin[i] = borderSafe && skinDensity[i] >= 0.945f;',
    'inner skin continuity support')

# RGB vein colour can change sign with exposure/white balance. Use local chromatic residual
# magnitude as one weak cue; it never paints by itself.
rep('float haemoglobinCue = Math.max(0f, rgIndex[i] - rgMean10[i]);\n                haemoglobinCue = Math.min(7.0f, haemoglobinCue * 0.34f);',
    'float rgResidual = rgIndex[i] - rgMean10[i];\n                float haemoglobinCue = Math.min(7.0f, Math.abs(rgResidual) * 0.30f);',
    'bidirectional RGB chromatic cue')

rep('float ridge = body * 0.56f\n                        + illuminationNorm * 0.46f\n                        + hessian * 0.78f\n                        + haemoglobinCue * 0.56f',
    'float ridge = body * 0.66f\n                        + illuminationNorm * 0.62f\n                        + hessian * 0.90f\n                        + haemoglobinCue * 0.74f',
    'weak vessel response gain')

# Let weak centre pixels enter directional testing, but still demand support on both sides.
rep('if (!innerSkin[i] || base < threshold * 0.22f) continue;',
    'if (!innerSkin[i] || base < threshold * 0.12f) continue;',
    'directional base floor')
rep('if (ridgeSoftness[i] < 0.50f && base < threshold * 1.30f) continue;',
    'if (ridgeSoftness[i] < 0.38f && base < threshold * 1.05f) continue;',
    'soft ridge entry')
rep('if (raw[i1] > threshold * 0.19f) lineStrong++;\n                        if (raw[i2] > threshold * 0.19f) lineStrong++;',
    'if (raw[i1] > threshold * 0.13f) lineStrong++;\n                        if (raw[i2] > threshold * 0.13f) lineStrong++;',
    'along line weak evidence')
rep('if (bilateral < 1.20f || outerBilateral < 0.95f || sideAsymmetry > 13.5f) continue;',
    'if (bilateral < 0.62f || outerBilateral < 0.42f || sideAsymmetry > 16.0f) continue;',
    'soft bilateral profile')

# Temporal hysteresis: stable faint evidence accumulates instead of disappearing every frame.
rep('if (s > threshold * 0.58f) persistence[i] = (byte)Math.min(15, persistence[i] + 1);',
    'if (s > threshold * 0.40f) persistence[i] = (byte)Math.min(15, persistence[i] + 1);',
    'persistence entry')
rep('strong[i] = temporal[i] > threshold * 1.00f && persistence[i] >= 3;',
    'strong[i] = temporal[i] > threshold * 0.80f && persistence[i] >= 2;',
    'strong temporal seed')
rep('weak[i] = temporal[i] > threshold * 0.60f && persistence[i] >= 2;',
    'weak[i] = temporal[i] > threshold * 0.40f && persistence[i] >= 2;',
    'weak temporal path')

# Component/path gate. Old v3.5 demanded span 34 + geodesic 42 + 50% temporal stability.
# That erased the user's faint wrist paths. Keep a continuity requirement, but make it adaptive.
rep('if (size < 20) continue;', 'if (size < 9) continue;', 'component minimum size')
rep('boolean longEnough = span >= 34 && geodesic >= 42;',
    'boolean longEnough = span >= 16 && geodesic >= 21;',
    'component long enough')
rep('boolean longCurve = span >= 44 && geodesic >= 55 && fill <= 0.66f;',
    'boolean longCurve = span >= 24 && geodesic >= 31 && fill <= 0.76f;',
    'component long curve')
rep('boolean elongated = elongation >= 1.62f && fill <= 0.74f;',
    'boolean elongated = elongation >= 1.30f && fill <= 0.84f;',
    'component elongation')
rep('boolean branchLike = geodesic >= 58 && span >= 42 && fill <= 0.52f;',
    'boolean branchLike = geodesic >= 34 && span >= 24 && fill <= 0.67f;',
    'component branch')
rep('boolean widthOk = width <= 13.8f;',
    'boolean widthOk = width <= 16.5f;',
    'component width')
rep('boolean scoreOk = meanScore >= Math.max(2.80f, thresholdForComponent() * 1.03f);',
    'boolean scoreOk = meanScore >= Math.max(1.90f, thresholdForComponent() * 0.66f);',
    'component score')
rep('boolean temporalOk = stableFraction >= 0.50f || strongStable >= 10;',
    'boolean temporalOk = stableFraction >= 0.24f || strongStable >= 4;',
    'component temporal stability')
rep('boolean directionOk = directionLinkFraction >= 0.60f;',
    'boolean directionOk = directionLinkFraction >= 0.36f;',
    'component direction links')
rep('boolean pathOk = pathness >= 3.15f || branchLike;',
    'boolean pathOk = pathness >= 1.75f || branchLike;',
    'component pathness')

# Anatomy is only a direction prior after image evidence exists. It cannot manufacture a vessel.
if 'if (d0 <= 24f) livePrior = 1.07f;\n            else if (d0 >= 68f) livePrior = 0.90f;' in s:
    s = s.replace('if (d0 <= 24f) livePrior = 1.07f;\n            else if (d0 >= 68f) livePrior = 0.90f;',
                  'if (d0 <= 28f) livePrior = 1.12f;\n            else if (d0 >= 72f) livePrior = 0.88f;', 1)

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
