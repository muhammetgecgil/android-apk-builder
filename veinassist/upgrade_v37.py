from pathlib import Path
import runpy

root = Path(__file__).resolve().parent
runpy.run_path(str(root / 'upgrade_v36.py'), run_name='__main__')

analyzer_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'

s = analyzer_p.read_text(encoding='utf-8')

# v3.7: keep the v3.6 UI/palette unchanged. Only make the detector more conservative.
s = s.replace('boolean adaptive = dc * dc + dr * dr <= 1.0f;',
              'boolean adaptive = dc * dc + dr * dr <= 0.72f;', 1)
s = s.replace('boolean notWhiteOrGrayObject = !(lum[i] > 220f && chroma < 16f);',
              'boolean notWhiteOrGrayObject = !(lum[i] > 205f && chroma < 20f);', 1)
s = s.replace('boolean borderSafe = x >= 3 && x < W - 3 && y >= 3 && y < H - 3;',
              'boolean borderSafe = x >= 5 && x < W - 5 && y >= 5 && y < H - 5;', 1)
s = s.replace('innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.955f;',
              'innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.978f;', 1)

# More continuity: fewer isolated blue fragments, more long vessel-like paths.
s = s.replace('if (lineSkin < 7 || lineStrong < 5) continue;',
              'if (lineSkin < 8 || lineStrong < 6) continue;', 1)
s = s.replace('if (s > threshold * 0.68f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);',
              'if (s > threshold * 0.72f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);', 1)
s = s.replace('strong[i] = temporal[i] > threshold * 1.08f && persistence[i] >= 2;',
              'strong[i] = temporal[i] > threshold * 1.12f && persistence[i] >= 3;', 1)
s = s.replace('weak[i] = temporal[i] > threshold * 0.66f && persistence[i] >= 2;',
              'weak[i] = temporal[i] > threshold * 0.72f && persistence[i] >= 3;', 1)
s = s.replace('if (size < 12) continue;', 'if (size < 18) continue;', 1)
s = s.replace('boolean longEnough = span >= 13;', 'boolean longEnough = span >= 18;', 1)
s = s.replace('boolean sparseCurve = span >= 22 && fill <= 0.58f;',
              'boolean sparseCurve = span >= 28 && fill <= 0.54f;', 1)
s = s.replace('boolean elongated = elongation >= 1.55f && fill <= 0.76f;',
              'boolean elongated = elongation >= 1.80f && fill <= 0.70f;', 1)
s = s.replace('boolean scoreOk = meanScore >= 4.8f;',
              'boolean scoreOk = meanScore >= 5.25f;', 1)

# Anatomy never creates a line; it only biases direction after image evidence exists.
# Apply a very weak arm-axis prior even in LIVE, stronger only when ATLAS is selected.
s = s.replace('if (!atlasPrior) return 1f;\n\n        float angle =',
              'float livePrior = 1f;\n        if (!atlasPrior && armLike) {\n            float a0 = (float)(dir * Math.PI / 8.0);\n            float d0 = (float)Math.toDegrees(angleDiff(a0, skinAxis));\n            if (d0 <= 24f) livePrior = 1.07f;\n            else if (d0 >= 68f) livePrior = 0.90f;\n            return livePrior;\n        }\n        if (!atlasPrior) return 1f;\n\n        float angle =', 1)

analyzer_p.write_text(s, encoding='utf-8')

build = build_p.read_text(encoding='utf-8')
build = build.replace("applicationId 'com.mg.veinassist.fullscreen36'", "applicationId 'com.mg.veinassist.continuity37'", 1)
build = build.replace('versionCode 36', 'versionCode 37', 1)
build = build.replace("versionName '3.6-fullscreen-overlay'", "versionName '3.7-skin-continuity'", 1)
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = manifest.replace('MG VeinAssist v3.6 Fullscreen', 'MG VeinAssist v3.7 Continuity', 1)
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.7 strict skin + continuity analyzer patch applied; UI unchanged')
