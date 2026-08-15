from pathlib import Path
import runpy

root = Path(__file__).resolve().parent
runpy.run_path(str(root / 'upgrade_v33.py'), run_name='__main__')

an_p = root / 'app/src/main/java/com/mg/veinassist/VeinAnalyzerV29.java'
build_p = root / 'app/build.gradle'
manifest_p = root / 'app/src/main/AndroidManifest.xml'
an = an_p.read_text(encoding='utf-8')

def must_replace(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Patch point not found: {label}')
    return text.replace(old, new)

def replace_method(text, start_marker, end_marker, replacement, label):
    if start_marker not in text or end_marker not in text:
        raise RuntimeError(f'Method markers not found: {label}')
    start = text.index(start_marker)
    end = text.index(end_marker, start)
    return text[:start] + replacement + text[end:]

# v3.4: algorithm only. UI/layout/colours stay unchanged.
# Skin-first + local illumination correction + multi-scale Hessian-like ridges +
# bilateral ridge profile + directional continuity + temporal/geodesic filtering.

an = must_replace(an,
    '    private final float[] m16 = new float[N];\n    private final float[] raw = new float[N];',
    '    private final float[] m16 = new float[N];\n'
    '    private final float[] rgIndex = new float[N];\n'
    '    private final float[] rgMean5 = new float[N];\n'
    '    private final float[] rgMean10 = new float[N];\n'
    '    private final float[] ridgeSoftness = new float[N];\n'
    '    private final float[] raw = new float[N];',
    'v34 spectral buffers')

an = must_replace(an,
    '    private final int[] labels = new int[N];\n    private final int[] queue = new int[N];',
    '    private final int[] labels = new int[N];\n'
    '    private final int[] queue = new int[N];\n'
    '    private final int[] geoQueue = new int[N];\n'
    '    private final int[] geoSeen = new int[N];\n'
    '    private final short[] geoDist = new short[N];\n'
    '    private int geoEpoch = 1;\n',
    'v34 geodesic buffers')

an = must_replace(an,
    '    private final int[][] sideX = new int[8][2];\n    private final int[][] sideY = new int[8][2];',
    '    private final int[][] sideX = new int[8][3];\n    private final int[][] sideY = new int[8][3];',
    'v34 side profile arrays')
an = must_replace(an, '        int[] ps = {2, 4};', '        int[] ps = {2, 4, 6};',
                  'v34 side profile distances')

an = must_replace(an,
    '        boxMeanInto(green, m2, 2);\n'
    '        boxMeanInto(green, m5, 5);\n'
    '        boxMeanInto(green, m10, 10);\n'
    '        boxMeanInto(green, m16, 16);',
    '        for (int i = 0; i < N; i++) rgIndex[i] = red[i] - green[i];\n'
    '        boxMeanInto(rgIndex, rgMean5, 5);\n'
    '        boxMeanInto(rgIndex, rgMean10, 10);\n'
    '        boxMeanInto(green, m2, 2);\n'
    '        boxMeanInto(green, m5, 5);\n'
    '        boxMeanInto(green, m10, 10);\n'
    '        boxMeanInto(green, m16, 16);',
    'v34 spectral normalization call')

an = must_replace(an,
    'float threshold = Math.max(2.55f, 9.10f - sensitivity * 0.0325f);',
    'float threshold = Math.max(3.05f, 9.35f - sensitivity * 0.0305f);',
    'v34 threshold')

an = must_replace(an,
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.92f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'if (expandedKeep[i] && innerSkin[i] && skinDensity[i] >= 0.965f && lum[i] > 35f && !(lum[i] > 225f && chroma(i) < 14f)) {',
    'v34 final dense skin paint')

raw_start = '    private void buildRawVesselResponse() {'
raw_end = '    /** Requires both along-line support and across-line dark-ridge contrast. */'
raw_method = r'''    private void buildRawVesselResponse() {
        Arrays.fill(ridgeSoftness, 0f);

        for (int y = 4; y < H - 4; y++) {
            for (int x = 4; x < W - 4; x++) {
                int i = y * W + x;
                if (!innerSkin[i] || lum[i] <= 48f) continue;

                float thin = Math.max(0f, m2[i] - green[i]);
                float mid = Math.max(0f, m5[i] - green[i]);
                float wide = Math.max(0f, m10[i] - green[i]);
                float veryWide = Math.max(0f, m16[i] - green[i]);

                float body = Math.max(mid * 0.96f,
                        Math.max(wide, veryWide * 0.72f));

                float illuminationNorm =
                        Math.max(0f, (m16[i] - green[i]) / Math.max(28f, m16[i]) * 30f);

                float haemoglobinCue = Math.max(0f, rgIndex[i] - rgMean10[i]);
                haemoglobinCue = Math.min(7.0f, haemoglobinCue * 0.34f);

                float hSmall = hessianDarkRidge(m2, x, y, 1);
                float hMedium = hessianDarkRidge(m5, x, y, 2);
                float hessian = Math.max(hSmall * 1.10f, hMedium * 1.55f);
                hessian = Math.min(8.5f, hessian);

                float softness = (mid * 0.55f + wide * 0.70f + veryWide * 0.25f) /
                        Math.max(1.8f, thin + 1.6f);
                ridgeSoftness[i] = softness;

                if (thin > 6.0f && softness < 0.72f) continue;

                float hairRatio = thin / Math.max(1.0f, body);
                float hairPenalty = Math.max(0f, hairRatio - 1.12f) * 2.9f;

                float gx = Math.abs(lum[i + 1] - lum[i - 1]);
                float gy = Math.abs(lum[i + W] - lum[i - W]);
                float edge = Math.max(gx, gy);
                float edgePenalty = Math.max(0f, edge - 19f) * 0.13f;

                float creasePenalty = 0f;
                if (thin > body * 0.88f && haemoglobinCue < 0.9f) {
                    creasePenalty = Math.min(4.5f, (thin - body * 0.88f) * 0.55f);
                }

                float ridge = body * 0.56f
                        + illuminationNorm * 0.46f
                        + hessian * 0.78f
                        + haemoglobinCue * 0.56f
                        - thin * 0.08f
                        - hairPenalty
                        - edgePenalty
                        - creasePenalty;

                if (body < 1.0f && hessian < 0.65f) ridge *= 0.35f;
                raw[i] = Math.max(0f, ridge);
            }
        }
    }

    private float hessianDarkRidge(float[] src, int x, int y, int step) {
        int i = y * W + x;
        int xp = y * W + (x + step);
        int xm = y * W + (x - step);
        int yp = (y + step) * W + x;
        int ym = (y - step) * W + x;

        float c = src[i];
        float dxx = src[xp] + src[xm] - 2f * c;
        float dyy = src[yp] + src[ym] - 2f * c;
        float dxy = (src[(y + step) * W + (x + step)]
                - src[(y + step) * W + (x - step)]
                - src[(y - step) * W + (x + step)]
                + src[(y - step) * W + (x - step)]) * 0.25f;

        float tr = dxx + dyy;
        float disc = (float)Math.sqrt(Math.max(0f,
                (dxx - dyy) * (dxx - dyy) + 4f * dxy * dxy));
        float lA = (tr - disc) * 0.5f;
        float lB = (tr + disc) * 0.5f;

        float small = lA, large = lB;
        if (Math.abs(small) > Math.abs(large)) {
            float t = small; small = large; large = t;
        }
        if (large <= 0f) return 0f;

        float ratio = Math.abs(small) / (Math.abs(large) + 0.001f);
        float tubular = Math.max(0f, 1f - ratio * 1.45f);
        return Math.max(0f, large * tubular);
    }

'''
an = replace_method(an, raw_start, raw_end, raw_method, 'v34 raw vessel method')

dir_start = '    /** Requires both along-line support and across-line dark-ridge contrast. */'
dir_end = '    private void updateTemporalAndHysteresis(float threshold) {'
dir_method = r'''    /** Requires long along-line support and a soft dark ridge with skin on both sides. */
    private void buildDirectionalContinuity(float threshold, float skinAxis, boolean armLike) {
        for (int y = 14; y < H - 14; y++) {
            for (int x = 14; x < W - 14; x++) {
                int i = y * W + x;
                float base = raw[i];
                if (!innerSkin[i] || base < threshold * 0.22f) continue;
                if (ridgeSoftness[i] < 0.50f && base < threshold * 1.30f) continue;

                float bestScore = 0f;
                int bestK = 0;

                for (int k = 0; k < 8; k++) {
                    float lineSum = 0f;
                    int lineStrong = 0;
                    int lineSkin = 0;

                    for (int j = 0; j < 4; j++) {
                        int dx = offX[k][j];
                        int dy = offY[k][j];
                        int i1 = (y + dy) * W + (x + dx);
                        int i2 = (y - dy) * W + (x - dx);
                        if (innerSkin[i1]) { lineSkin++; lineSum += raw[i1]; }
                        if (innerSkin[i2]) { lineSkin++; lineSum += raw[i2]; }
                        if (raw[i1] > threshold * 0.19f) lineStrong++;
                        if (raw[i2] > threshold * 0.19f) lineStrong++;
                    }

                    if (lineSkin < 7 || lineStrong < 4) continue;
                    float lineMean = lineSum / Math.max(1, lineSkin);

                    float leftSum = 0f, rightSum = 0f;
                    int leftN = 0, rightN = 0;
                    float outerLeft = 0f, outerRight = 0f;

                    for (int j = 0; j < 3; j++) {
                        int dx = sideX[k][j];
                        int dy = sideY[k][j];
                        int s1 = (y + dy) * W + (x + dx);
                        int s2 = (y - dy) * W + (x - dx);
                        if (innerSkin[s1]) {
                            leftSum += green[s1]; leftN++;
                            if (j >= 1) outerLeft += green[s1] - green[i];
                        }
                        if (innerSkin[s2]) {
                            rightSum += green[s2]; rightN++;
                            if (j >= 1) outerRight += green[s2] - green[i];
                        }
                    }

                    if (leftN < 3 || rightN < 3) continue;

                    float leftMean = leftSum / leftN;
                    float rightMean = rightSum / rightN;
                    float leftRise = leftMean - green[i];
                    float rightRise = rightMean - green[i];
                    float bilateral = Math.min(leftRise, rightRise);
                    float sideAsymmetry = Math.abs(leftMean - rightMean);
                    float outerBilateral = Math.min(outerLeft, outerRight) * 0.5f;

                    if (bilateral < 1.20f || outerBilateral < 0.95f || sideAsymmetry > 13.5f) continue;

                    float across = Math.max(0f, Math.min(15f,
                            bilateral * 0.65f + outerBilateral * 0.35f));

                    float coherence = lineStrong / 8f;
                    float symmetryFactor = Math.max(0.68f, 1f - sideAsymmetry / 42f);
                    float softnessFactor = Math.max(0.72f,
                            Math.min(1.16f, 0.72f + ridgeSoftness[i] * 0.23f));

                    float score = base * 0.44f + lineMean * 0.43f + across * 0.34f;
                    score *= (0.77f + coherence * 0.36f) * symmetryFactor * softnessFactor;
                    score *= anatomyFactor(k, skinAxis, armLike);

                    if (score > bestScore) {
                        bestScore = score;
                        bestK = k;
                    }
                }

                continuous[i] = bestScore;
                bestDir[i] = (byte)bestK;
            }
        }
    }

'''
an = replace_method(an, dir_start, dir_end, dir_method, 'v34 directional method')

temp_start = '    private void updateTemporalAndHysteresis(float threshold) {'
temp_end = '    /**\n     * Directional continuity bridge:'
temp_method = r'''    private void updateTemporalAndHysteresis(float threshold) {
        Arrays.fill(strong, false);
        Arrays.fill(weak, false);
        Arrays.fill(candidate, false);

        for (int i = 0; i < N; i++) {
            float s = continuous[i];
            temporal[i] = temporal[i] * 0.76f + s * 0.24f;

            if (s > threshold * 0.54f) persistence[i] = (byte)Math.min(15, persistence[i] + 1);
            else persistence[i] = (byte)Math.max(0, persistence[i] - 1);

            if (!innerSkin[i]) continue;
            strong[i] = temporal[i] > threshold * 1.00f && persistence[i] >= 3;
            weak[i] = temporal[i] > threshold * 0.55f && persistence[i] >= 2;
        }

        int head = 0, tail = 0;
        for (int i = 0; i < N; i++) {
            if (strong[i]) {
                candidate[i] = true;
                queue[tail++] = i;
            }
        }

        while (head < tail) {
            int p = queue[head++];
            int x = p % W, y = p / W;
            int pd = bestDir[p] & 0xff;
            for (int oy = -1; oy <= 1; oy++) {
                for (int ox = -1; ox <= 1; ox++) {
                    if (ox == 0 && oy == 0) continue;
                    int nx = x + ox, ny = y + oy;
                    if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                    int q = ny * W + nx;
                    if (!weak[q] || candidate[q] || !innerSkin[q]) continue;
                    int qd = bestDir[q] & 0xff;
                    int dd = dirDiff(pd, qd);
                    boolean directionOk = dd <= 1 ||
                            (dd <= 2 && temporal[q] > threshold * 1.18f && persistence[q] >= 4);
                    if (directionOk) {
                        candidate[q] = true;
                        queue[tail++] = q;
                    }
                }
            }
        }
    }

'''
an = replace_method(an, temp_start, temp_end, temp_method, 'v34 temporal method')

bridge_start = '    /**\n     * Directional continuity bridge:'
bridge_end = '    private int filterContinuousComponents'
bridge_method = r'''    /**
     * Directional gap bridge. It follows the current ridge direction and can cross a small
     * low-contrast interruption only if another compatible vessel endpoint is found.
     */
    private void bridgeDirectionalGaps(float threshold) {
        System.arraycopy(candidate, 0, bridge, 0, N);

        for (int y = 11; y < H - 11; y++) {
            for (int x = 11; x < W - 11; x++) {
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
                        int supported = 0;
                        boolean allSkin = true;

                        for (int dist = 2; dist <= 10; dist++) {
                            int nx = x + ex * dist;
                            int ny = y + ey * dist;
                            int q = ny * W + nx;
                            if (!innerSkin[q] || skinDensity[q] < 0.94f) {
                                allSkin = false;
                                break;
                            }
                            float e = Math.max(raw[q], continuous[q] * 0.56f);
                            evidence += e;
                            evidenceN++;
                            if (e > threshold * 0.10f) supported++;
                            if (candidate[q] && dirDiff(k, bestDir[q] & 0xff) <= 1) {
                                hit = q;
                                hitDist = dist;
                                break;
                            }
                        }

                        if (!allSkin || hit < 0 || hitDist < 3) continue;
                        float meanEvidence = evidence / Math.max(1, evidenceN);
                        int requiredSupported = Math.max(1, (hitDist - 1) / 3);
                        if (meanEvidence < threshold * 0.20f || supported < requiredSupported) continue;

                        float endScore = Math.min(temporal[p], temporal[hit]);
                        for (int dist = 1; dist < hitDist; dist++) {
                            int nx = x + ex * dist;
                            int ny = y + ey * dist;
                            int q = ny * W + nx;
                            if (!innerSkin[q]) break;
                            bridge[q] = true;
                            bestDir[q] = (byte)k;
                            temporal[q] = Math.max(temporal[q], endScore * 0.64f);
                            persistence[q] = (byte)Math.max(persistence[q], 2);
                        }
                    }
                }
            }
        }
        System.arraycopy(bridge, 0, candidate, 0, N);
    }

'''
an = replace_method(an, bridge_start, bridge_end, bridge_method, 'v34 bridge method')

comp_start = '    private int filterContinuousComponents(boolean[] src) {'
comp_end = '    private float thresholdForComponent() {'
comp_method = r'''    private int filterContinuousComponents(boolean[] src) {
        Arrays.fill(labels, 0);
        Arrays.fill(keep, false);
        int id = 0;
        int keptComponents = 0;

        for (int start = 0; start < N; start++) {
            if (!src[start] || labels[start] != 0) continue;
            id++;
            int head = 0, tail = 0;
            queue[tail++] = start;
            labels[start] = id;

            int minX = W, maxX = 0, minY = H, maxY = 0;
            double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
            double scoreSum = 0;
            int stable = 0;
            int strongStable = 0;
            int alignedLinks = 0;
            int allLinks = 0;

            while (head < tail) {
                int p = queue[head++];
                int x = p % W, y = p / W;
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                sx += x; sy += y;
                sxx += x * (double)x;
                syy += y * (double)y;
                sxy += x * (double)y;
                scoreSum += temporal[p];
                if (persistence[p] >= 3) stable++;
                if (persistence[p] >= 4 && temporal[p] >= thresholdForComponent() * 1.25f) strongStable++;

                int pd = bestDir[p] & 0xff;
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        if (ox == 0 && oy == 0) continue;
                        int nx = x + ox, ny = y + oy;
                        if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                        int q = ny * W + nx;
                        if (!src[q]) continue;
                        allLinks++;
                        if (dirDiff(pd, bestDir[q] & 0xff) <= 1) alignedLinks++;
                        if (labels[q] == 0) {
                            labels[q] = id;
                            queue[tail++] = q;
                        }
                    }
                }
            }

            int size = tail;
            if (size < 20) continue;

            double mx = sx / size, my = sy / size;
            double cxx = sxx / size - mx * mx;
            double cyy = syy / size - my * my;
            double cxy = sxy / size - mx * my;
            double tr = cxx + cyy;
            double disc = Math.sqrt(Math.max(0,
                    (cxx - cyy) * (cxx - cyy) + 4 * cxy * cxy));
            double l1 = Math.max(0.01, (tr + disc) * 0.5);
            double l2 = Math.max(0.01, (tr - disc) * 0.5);

            float elongation = (float)Math.sqrt(l1 / l2);
            float width = (float)(4.0 * Math.sqrt(l2));
            int bw = maxX - minX + 1;
            int bh = maxY - minY + 1;
            int span = Math.max(bw, bh);
            float fill = size / (float)Math.max(1, bw * bh);
            float meanScore = (float)(scoreSum / size);
            float stableFraction = stable / (float)size;
            float directionLinkFraction = alignedLinks / (float)Math.max(1, allLinks);
            int geodesic = componentGeodesicDiameter(id, start);

            boolean longEnough = span >= 28 && geodesic >= 34;
            boolean longCurve = span >= 38 && geodesic >= 46 && fill <= 0.68f;
            boolean elongated = elongation >= 1.55f && fill <= 0.76f;
            boolean widthOk = width <= 14.5f;
            boolean scoreOk = meanScore >= Math.max(2.65f, thresholdForComponent());
            boolean temporalOk = stableFraction >= 0.43f || strongStable >= 8;
            boolean directionOk = directionLinkFraction >= 0.54f;

            if (longEnough && widthOk && scoreOk && temporalOk && directionOk
                    && (elongated || longCurve)) {
                keptComponents++;
                for (int k = 0; k < tail; k++) keep[queue[k]] = true;
            }
        }
        return keptComponents;
    }

    private int componentGeodesicDiameter(int id, int start) {
        int far = geodesicFarthest(id, start, false);
        return geodesicFarthest(id, far, true);
    }

    private int geodesicFarthest(int id, int start, boolean returnDistance) {
        geoEpoch++;
        if (geoEpoch == Integer.MAX_VALUE) {
            Arrays.fill(geoSeen, 0);
            geoEpoch = 1;
        }

        int head = 0, tail = 0;
        geoQueue[tail++] = start;
        geoSeen[start] = geoEpoch;
        geoDist[start] = 0;
        int far = start;
        int maxD = 0;

        while (head < tail) {
            int p = geoQueue[head++];
            int x = p % W, y = p / W;
            int pd = geoDist[p] & 0xffff;
            if (pd > maxD) {
                maxD = pd;
                far = p;
            }

            for (int oy = -1; oy <= 1; oy++) {
                for (int ox = -1; ox <= 1; ox++) {
                    if (ox == 0 && oy == 0) continue;
                    int nx = x + ox, ny = y + oy;
                    if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                    int q = ny * W + nx;
                    if (labels[q] != id || geoSeen[q] == geoEpoch) continue;
                    geoSeen[q] = geoEpoch;
                    geoDist[q] = (short)Math.min(32000, pd + 1);
                    geoQueue[tail++] = q;
                }
            }
        }
        return returnDistance ? maxD : far;
    }

'''
an = replace_method(an, comp_start, comp_end, comp_method, 'v34 component graph method')

an = must_replace(an,
    'float t = Math.max(2.55f, 9.10f - sensitivity * 0.0325f);',
    'float t = Math.max(3.05f, 9.35f - sensitivity * 0.0305f);',
    'v34 component threshold helper')

an_p.write_text(an, encoding='utf-8')

build = build_p.read_text(encoding='utf-8')
build = must_replace(build, "applicationId 'com.mg.veinassist.stable33'",
                     "applicationId 'com.mg.veinassist.continuity34'", 'v34 application id')
build = must_replace(build, 'versionCode 33', 'versionCode 34', 'v34 version code')
build = must_replace(build, "versionName '3.3-bilateral-ridge-skin-graph'",
                     "versionName '3.4-continuity-skin-hessian'", 'v34 version name')
build_p.write_text(build, encoding='utf-8')

manifest = manifest_p.read_text(encoding='utf-8')
manifest = must_replace(manifest, 'MG VeinAssist v3.3 Vein First',
                        'MG VeinAssist v3.4 Continuity', 'v34 label')
manifest_p.write_text(manifest, encoding='utf-8')

print('MG VeinAssist v3.4 continuity/skin/Hessian patch applied')
