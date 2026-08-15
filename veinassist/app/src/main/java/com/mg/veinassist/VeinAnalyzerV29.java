package com.mg.veinassist;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import java.util.Arrays;

/**
 * MG VeinAssist v3.0 image analyzer.
 * Keeps the existing UI/colour contract but makes vein overlay more conservative,
 * continuous and skin-only.
 */
final class VeinAnalyzerV29 {
    static final int MODE_LIVE = 0;
    static final int MODE_MASK = 1;
    static final int MODE_THERMAL = 2;

    private static final int W = 320;
    private static final int H = 240;
    private static final int N = W * H;

    private int sensitivity = 21;
    private int mode = MODE_LIVE;
    private boolean atlasPrior = false;
    private String region = "AUTO";

    private final float[] lum = new float[N];
    private final float[] red = new float[N];
    private final float[] green = new float[N];
    private final float[] blue = new float[N];
    private final float[] cb = new float[N];
    private final float[] cr = new float[N];

    private final float[] skinFloat = new float[N];
    private final float[] skinDensity = new float[N];
    private final float[] skinMemory = new float[N];
    private final boolean[] skin = new boolean[N];
    private final boolean[] innerSkin = new boolean[N];

    private final float[] m2 = new float[N];
    private final float[] m5 = new float[N];
    private final float[] m10 = new float[N];
    private final float[] m16 = new float[N];
    private final float[] raw = new float[N];
    private final float[] continuous = new float[N];
    private final float[] temporal = new float[N];
    private final byte[] persistence = new byte[N];
    private final byte[] bestDir = new byte[N];

    private final boolean[] strong = new boolean[N];
    private final boolean[] weak = new boolean[N];
    private final boolean[] candidate = new boolean[N];
    private final boolean[] bridge = new boolean[N];
    private final boolean[] keep = new boolean[N];
    private final boolean[] expandedKeep = new boolean[N];

    private final int[] labels = new int[N];
    private final int[] queue = new int[N];
    private final int[] pixels = new int[N];
    private final int[] thermalPx = new int[N];
    private final double[] integral = new double[(W + 1) * (H + 1)];

    private final int[][] offX = new int[8][4];
    private final int[][] offY = new int[8][4];
    private final int[][] sideX = new int[8][2];
    private final int[][] sideY = new int[8][2];

    VeinAnalyzerV29() {
        int[] ds = {3, 5, 8, 11};
        int[] ps = {2, 4};
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 8.0;
            double p = a + Math.PI / 2.0;
            for (int j = 0; j < ds.length; j++) {
                offX[k][j] = (int)Math.round(Math.cos(a) * ds[j]);
                offY[k][j] = (int)Math.round(Math.sin(a) * ds[j]);
            }
            for (int j = 0; j < ps.length; j++) {
                sideX[k][j] = (int)Math.round(Math.cos(p) * ps[j]);
                sideY[k][j] = (int)Math.round(Math.sin(p) * ps[j]);
            }
        }
    }

    void setSensitivity(int s) { sensitivity = Math.max(1, Math.min(99, s)); }
    void setMode(int m) { mode = m; }
    void setAtlasPrior(boolean b) { atlasPrior = b; }
    void setRegion(String r) { region = (r == null ? "AUTO" : r); }

    static class Result {
        Bitmap overlay;
        Bitmap thermal;
        float sharpness;
        int skinPercent;
        int lineCount;
    }

    Result process(FrameDataV29 f, int sensorOrientation) {
        sampleYuv(f);
        float sharpness = computeSharpness();

        int skinCount = buildSkinMask();
        float skinAxis = principalAxis(innerSkin);
        boolean armLike = axisElongation(innerSkin) >= 1.45f;

        boxMeanInto(green, m2, 2);
        boxMeanInto(green, m5, 5);
        boxMeanInto(green, m10, 10);
        boxMeanInto(green, m16, 16);

        float threshold = Math.max(5.9f, 8.75f - sensitivity * 0.026f);
        Arrays.fill(raw, 0f);
        Arrays.fill(continuous, 0f);
        Arrays.fill(bestDir, (byte)0);

        buildRawVesselResponse();
        buildDirectionalContinuity(threshold, skinAxis, armLike);
        updateTemporalAndHysteresis(threshold);
        bridgeShortGaps();

        int components = filterContinuousComponents(candidate);
        thickenKeptOnePixel();

        for (int i = 0; i < N; i++) {
            if (expandedKeep[i] && innerSkin[i]) {
                float a = Math.min(0.86f, 0.43f + temporal[i] / 34f);
                pixels[i] = Color.argb((int)(255 * a), 0, 235, 225);
            } else {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        Bitmap sensorMask = Bitmap.createBitmap(pixels, W, H, Bitmap.Config.ARGB_8888);
        Bitmap overlay = rotate(sensorMask, sensorOrientation == 270 ? 270 : 90);
        sensorMask.recycle();

        Bitmap thermal = null;
        if (mode == MODE_THERMAL) {
            for (int i = 0; i < N; i++) thermalPx[i] = thermalColor((int)lum[i]);
            Bitmap sensorThermal = Bitmap.createBitmap(thermalPx, W, H, Bitmap.Config.ARGB_8888);
            thermal = rotate(sensorThermal, sensorOrientation == 270 ? 270 : 90);
            sensorThermal.recycle();
        }

        Result result = new Result();
        result.overlay = overlay;
        result.thermal = thermal;
        result.sharpness = sharpness;
        result.skinPercent = Math.round(100f * skinCount / N);
        result.lineCount = components;
        return result;
    }

    private void sampleYuv(FrameDataV29 f) {
        for (int y = 0; y < H; y++) {
            int sy = y * f.height / H;
            for (int x = 0; x < W; x++) {
                int sx = x * f.width / W;
                int yi = Math.min(f.y.length - 1, sy * f.yRowStride + sx);
                int ui = Math.min(f.u.length - 1,
                        (sy / 2) * f.uvRowStride + (sx / 2) * f.uvPixelStride);
                int vi = Math.min(f.v.length - 1,
                        (sy / 2) * f.uvRowStride + (sx / 2) * f.uvPixelStride);

                int Y = f.y[yi] & 0xff;
                int U = (f.u[ui] & 0xff) - 128;
                int V = (f.v[vi] & 0xff) - 128;

                int r = clamp255((int)(Y + 1.402f * V));
                int g = clamp255((int)(Y - 0.344136f * U - 0.714136f * V));
                int b = clamp255((int)(Y + 1.772f * U));

                int i = y * W + x;
                lum[i] = Y;
                red[i] = r;
                green[i] = g;
                blue[i] = b;
                cb[i] = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b;
                cr[i] = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b;
            }
        }
    }

    /** Learns the current skin colour from the original frame and keeps one limb component. */
    private int buildSkinMask() {
        float refCb = 0f, refCr = 0f, refY = 0f;
        int refN = 0;

        for (int y = H * 25 / 100; y < H * 75 / 100; y++) {
            for (int x = W * 25 / 100; x < W * 75 / 100; x++) {
                int i = y * W + x;
                if (genericSkinCandidate(i)) {
                    refCb += cb[i];
                    refCr += cr[i];
                    refY += lum[i];
                    refN++;
                }
            }
        }

        if (refN < 220) {
            refCb = refCr = refY = 0f;
            refN = 0;
            for (int i = 0; i < N; i++) {
                if (genericSkinCandidate(i)) {
                    refCb += cb[i];
                    refCr += cr[i];
                    refY += lum[i];
                    refN++;
                }
            }
        }

        if (refN > 0) {
            refCb /= refN;
            refCr /= refN;
            refY /= refN;
        } else {
            refCb = 110f;
            refCr = 150f;
            refY = 140f;
        }

        for (int i = 0; i < N; i++) {
            float dc = (cb[i] - refCb) / 20f;
            float dr = (cr[i] - refCr) / 24f;
            float dy = Math.abs(lum[i] - refY);
            float chroma = chroma(i);

            boolean generic = genericSkinCandidate(i);
            boolean adaptive = dc * dc + dr * dr <= 1.0f;
            boolean lightCompatible = dy < 105f || (lum[i] > 55f && lum[i] < 225f);
            boolean notWhiteOrGrayObject = !(lum[i] > 220f && chroma < 16f);
            boolean notBlack = lum[i] > 30f;

            skinFloat[i] = (generic && adaptive && lightCompatible &&
                    notWhiteOrGrayObject && notBlack) ? 1f : 0f;
        }

        boxMeanInto(skinFloat, skinDensity, 2);
        for (int i = 0; i < N; i++) skin[i] = skinDensity[i] >= 0.66f;

        keepBestSkinComponentInPlace(skin);

        for (int i = 0; i < N; i++) {
            float now = skin[i] ? 1f : 0f;
            skinMemory[i] = skinMemory[i] * 0.72f + now * 0.28f;
            skin[i] = skin[i] && skinMemory[i] >= 0.34f;
        }

        Arrays.fill(skinFloat, 0f);
        for (int i = 0; i < N; i++) if (skin[i]) skinFloat[i] = 1f;
        boxMeanInto(skinFloat, skinDensity, 5);

        int count = 0;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int i = y * W + x;
                boolean borderSafe = x >= 3 && x < W - 3 && y >= 3 && y < H - 3;
                innerSkin[i] = borderSafe && skin[i] && skinDensity[i] >= 0.955f;
                if (innerSkin[i]) count++;
            }
        }
        return count;
    }

    private boolean genericSkinCandidate(int i) {
        float c = chroma(i);
        boolean ycbcr = lum[i] > 28f && lum[i] < 246f &&
                cb[i] > 72f && cb[i] < 150f &&
                cr[i] > 124f && cr[i] < 194f &&
                cr[i] - cb[i] > 7f && c > 8f;
        boolean rgbWarm = lum[i] > 32f && lum[i] < 242f &&
                red[i] >= green[i] * 0.94f && red[i] >= blue[i] * 0.92f &&
                red[i] - blue[i] > 3f && cr[i] > 122f && c > 6f;
        return ycbcr || rgbWarm;
    }

    private float chroma(int i) {
        float dc = cb[i] - 128f;
        float dr = cr[i] - 128f;
        return (float)Math.sqrt(dc * dc + dr * dr);
    }

    private void buildRawVesselResponse() {
        for (int y = 2; y < H - 2; y++) {
            for (int x = 2; x < W - 2; x++) {
                int i = y * W + x;
                if (!innerSkin[i] || lum[i] <= 60f) continue;

                float thin = Math.max(0f, m2[i] - green[i]);
                float mid = Math.max(0f, m5[i] - green[i]);
                float wide = Math.max(0f, m10[i] - green[i]);
                float veryWide = Math.max(0f, m16[i] - green[i]);

                float body = Math.max(mid, Math.max(wide * 0.92f, veryWide * 0.68f));
                float hairRatio = thin / Math.max(1.0f, body);
                float hairPenalty = Math.max(0f, hairRatio - 1.08f) * 2.8f;

                float gx = Math.abs(lum[i + 1] - lum[i - 1]);
                float gy = Math.abs(lum[i + W] - lum[i - W]);
                float edgePenalty = Math.max(0f, Math.max(gx, gy) - 20f) * 0.12f;

                float chromaticCue = Math.max(0f,
                        (blue[i] - red[i]) * 0.025f + (green[i] - red[i]) * 0.012f);
                chromaticCue = Math.min(1.8f, chromaticCue);

                float ridge = body - thin * 0.17f - hairPenalty - edgePenalty + chromaticCue;
                raw[i] = Math.max(0f, ridge);
            }
        }
    }

    /** Requires both along-line support and across-line dark-ridge contrast. */
    private void buildDirectionalContinuity(float threshold, float skinAxis, boolean armLike) {
        for (int y = 13; y < H - 13; y++) {
            for (int x = 13; x < W - 13; x++) {
                int i = y * W + x;
                float base = raw[i];
                if (!innerSkin[i] || base < threshold * 0.30f) continue;

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
                        if (raw[i1] > threshold * 0.24f) lineStrong++;
                        if (raw[i2] > threshold * 0.24f) lineStrong++;
                    }

                    if (lineSkin < 7 || lineStrong < 5) continue;
                    float lineMean = lineSum / Math.max(1, lineSkin);

                    float sideSum = 0f;
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
                    float score = base * 0.56f + lineMean * 0.34f + across * 0.22f;
                    score *= 0.78f + coherence * 0.34f;
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

    private void updateTemporalAndHysteresis(float threshold) {
        Arrays.fill(strong, false);
        Arrays.fill(weak, false);
        Arrays.fill(candidate, false);

        for (int i = 0; i < N; i++) {
            float s = continuous[i];
            temporal[i] = temporal[i] * 0.80f + s * 0.20f;

            if (s > threshold * 0.68f) persistence[i] = (byte)Math.min(12, persistence[i] + 1);
            else persistence[i] = (byte)Math.max(0, persistence[i] - 1);

            if (!innerSkin[i]) continue;
            strong[i] = temporal[i] > threshold * 1.08f && persistence[i] >= 2;
            weak[i] = temporal[i] > threshold * 0.66f && persistence[i] >= 2;
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
                    if (dirDiff(pd, qd) <= 2) {
                        candidate[q] = true;
                        queue[tail++] = q;
                    }
                }
            }
        }
    }

    /** Fill only short 1-2 px gaps with vessel evidence on opposite sides. */
    private void bridgeShortGaps() {
        System.arraycopy(candidate, 0, bridge, 0, N);

        for (int y = 3; y < H - 3; y++) {
            for (int x = 3; x < W - 3; x++) {
                int i = y * W + x;
                if (candidate[i] || !innerSkin[i]) continue;

                for (int k = 0; k < 8; k++) {
                    int dx = sign(offX[k][0]);
                    int dy = sign(offY[k][0]);
                    if (dx == 0 && dy == 0) continue;

                    int a1 = (y + dy) * W + (x + dx);
                    int a2 = (y + 2 * dy) * W + (x + 2 * dx);
                    int b1 = (y - dy) * W + (x - dx);
                    int b2 = (y - 2 * dy) * W + (x - 2 * dx);

                    boolean left = candidate[a1] || candidate[a2];
                    boolean right = candidate[b1] || candidate[b2];
                    if (left && right) {
                        float neighbourScore = Math.max(
                                Math.max(temporal[a1], temporal[a2]),
                                Math.max(temporal[b1], temporal[b2]));
                        if (neighbourScore > 5.0f) {
                            bridge[i] = true;
                            temporal[i] = Math.max(temporal[i], neighbourScore * 0.72f);
                            bestDir[i] = (byte)k;
                            break;
                        }
                    }
                }
            }
        }

        System.arraycopy(bridge, 0, candidate, 0, N);
    }

    private int filterContinuousComponents(boolean[] src) {
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

                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        if (ox == 0 && oy == 0) continue;
                        int nx = x + ox, ny = y + oy;
                        if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                        int q = ny * W + nx;
                        if (src[q] && labels[q] == 0) {
                            labels[q] = id;
                            queue[tail++] = q;
                        }
                    }
                }
            }

            int size = tail;
            if (size < 12) continue;

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

            boolean longEnough = span >= 13;
            boolean sparseCurve = span >= 22 && fill <= 0.58f;
            boolean elongated = elongation >= 1.55f && fill <= 0.76f;
            boolean widthOk = width <= 16.0f;
            boolean scoreOk = meanScore >= 4.8f;

            if (longEnough && widthOk && scoreOk && (elongated || sparseCurve)) {
                keptComponents++;
                for (int k = 0; k < tail; k++) keep[queue[k]] = true;
            }
        }
        return keptComponents;
    }

    /** Adds one-pixel cross-vessel thickness, but only inside the learned skin. */
    private void thickenKeptOnePixel() {
        System.arraycopy(keep, 0, expandedKeep, 0, N);
        for (int y = 1; y < H - 1; y++) {
            for (int x = 1; x < W - 1; x++) {
                int i = y * W + x;
                if (!keep[i]) continue;
                int d = bestDir[i] & 0xff;
                int px = sign(sideX[d][0]);
                int py = sign(sideY[d][0]);
                int a = (y + py) * W + (x + px);
                int b = (y - py) * W + (x - px);
                if (innerSkin[a]) expandedKeep[a] = true;
                if (innerSkin[b]) expandedKeep[b] = true;
            }
        }
    }

    /** Anatomy is only a weak orientation prior because superficial venous patterns vary. */
    private float anatomyFactor(int dir, float skinAxis, boolean armLike) {
        if (!atlasPrior) return 1f;

        float angle = (float)(dir * Math.PI / 8.0);
        float deg = (float)Math.toDegrees(angleDiff(angle, skinAxis));
        String r = region == null ? "AUTO" : region;

        if ("ONKOL".equals(r) || "UST KOL".equals(r)) {
            if (deg <= 24f) return 1.24f;
            if (deg <= 45f) return 1.08f;
            if (deg >= 70f) return 0.70f;
            return 0.92f;
        }
        if ("DIRSEK".equals(r)) {
            if (deg <= 28f) return 1.12f;
            if (deg <= 62f) return 1.08f;
            return 0.88f;
        }
        if ("EL".equals(r) || "AYAK".equals(r)) return 1.00f;

        if (armLike) {
            if (deg <= 28f) return 1.13f;
            if (deg >= 72f) return 0.84f;
        }
        return 1f;
    }

    private void keepBestSkinComponentInPlace(boolean[] src) {
        Arrays.fill(labels, 0);
        int nextLabel = 0;
        int bestLabel = 0;
        double bestScore = 0;
        int cx = W / 2, cy = H / 2;

        for (int start = 0; start < N; start++) {
            if (!src[start] || labels[start] != 0) continue;
            nextLabel++;
            int head = 0, tail = 0;
            queue[tail++] = start;
            labels[start] = nextLabel;
            int size = 0;
            int minDist2 = Integer.MAX_VALUE;
            int touchesCenterBand = 0;

            while (head < tail) {
                int p = queue[head++];
                size++;
                int x = p % W, y = p / W;
                int dx = x - cx, dy = y - cy;
                minDist2 = Math.min(minDist2, dx * dx + dy * dy);
                if (Math.abs(dx) < W / 6 && Math.abs(dy) < H / 5) touchesCenterBand++;

                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        if (ox == 0 && oy == 0) continue;
                        int nx = x + ox, ny = y + oy;
                        if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                        int q = ny * W + nx;
                        if (src[q] && labels[q] == 0) {
                            labels[q] = nextLabel;
                            queue[tail++] = q;
                        }
                    }
                }
            }

            double centerBonus = 1.0 + 2.6 * Math.max(0,
                    1.0 - Math.sqrt(minDist2) / (Math.min(W, H) * 0.52));
            double centerContact = touchesCenterBand > 12 ? 1.35 : 0.90;
            double score = size * centerBonus * centerContact;
            if (score > bestScore) {
                bestScore = score;
                bestLabel = nextLabel;
            }
        }

        for (int i = 0; i < N; i++) src[i] = bestLabel != 0 && labels[i] == bestLabel;
    }

    private float principalAxis(boolean[] mask) {
        double sx = 0, sy = 0;
        int n = 0;
        for (int i = 0; i < N; i++) {
            if (mask[i]) { sx += i % W; sy += i / W; n++; }
        }
        if (n < 20) return 0f;

        double mx = sx / n, my = sy / n;
        double xx = 0, yy = 0, xy = 0;
        for (int i = 0; i < N; i++) {
            if (!mask[i]) continue;
            double dx = i % W - mx, dy = i / W - my;
            xx += dx * dx; yy += dy * dy; xy += dx * dy;
        }
        return (float)(0.5 * Math.atan2(2 * xy, xx - yy));
    }

    private float axisElongation(boolean[] mask) {
        double sx = 0, sy = 0;
        int n = 0;
        for (int i = 0; i < N; i++) {
            if (mask[i]) { sx += i % W; sy += i / W; n++; }
        }
        if (n < 20) return 1f;

        double mx = sx / n, my = sy / n;
        double xx = 0, yy = 0, xy = 0;
        for (int i = 0; i < N; i++) {
            if (!mask[i]) continue;
            double dx = i % W - mx, dy = i / W - my;
            xx += dx * dx; yy += dy * dy; xy += dx * dy;
        }
        xx /= n; yy /= n; xy /= n;
        double tr = xx + yy;
        double disc = Math.sqrt(Math.max(0, (xx - yy) * (xx - yy) + 4 * xy * xy));
        double l1 = Math.max(0.001, (tr + disc) * 0.5);
        double l2 = Math.max(0.001, (tr - disc) * 0.5);
        return (float)Math.sqrt(l1 / l2);
    }

    private float computeSharpness() {
        double sum = 0;
        int count = 0;
        for (int y = 2; y < H - 2; y += 2) {
            for (int x = 2; x < W - 2; x += 2) {
                int i = y * W + x;
                float lap = 4f * lum[i] - lum[i - 1] - lum[i + 1] - lum[i - W] - lum[i + W];
                sum += Math.abs(lap);
                count++;
            }
        }
        return count == 0 ? 0f : (float)(sum / count);
    }

    private void boxMeanInto(float[] src, float[] out, int r) {
        Arrays.fill(integral, 0d);
        int stride = W + 1;
        for (int y = 1; y <= H; y++) {
            double row = 0;
            for (int x = 1; x <= W; x++) {
                row += src[(y - 1) * W + (x - 1)];
                integral[y * stride + x] = integral[(y - 1) * stride + x] + row;
            }
        }
        for (int y = 0; y < H; y++) {
            int y0 = Math.max(0, y - r), y1 = Math.min(H - 1, y + r);
            for (int x = 0; x < W; x++) {
                int x0 = Math.max(0, x - r), x1 = Math.min(W - 1, x + r);
                int a = y0 * stride + x0;
                int b = y0 * stride + x1 + 1;
                int c = (y1 + 1) * stride + x0;
                int d = (y1 + 1) * stride + x1 + 1;
                double sum = integral[d] - integral[b] - integral[c] + integral[a];
                out[y * W + x] = (float)(sum / ((x1 - x0 + 1) * (y1 - y0 + 1)));
            }
        }
    }

    private static int dirDiff(int a, int b) {
        int d = Math.abs(a - b);
        return Math.min(d, 8 - d);
    }

    private static int sign(int x) { return x < 0 ? -1 : (x > 0 ? 1 : 0); }

    private static float angleDiff(float a, float b) {
        float d = Math.abs(a - b);
        while (d > Math.PI) d -= Math.PI;
        if (d > Math.PI / 2) d = (float)Math.PI - d;
        return Math.abs(d);
    }

    private static Bitmap rotate(Bitmap b, int degrees) {
        Matrix m = new Matrix();
        m.postRotate(degrees);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
    }

    private static int thermalColor(int y) {
        float t = Math.max(0f, Math.min(1f, y / 255f));
        int r, g, b;
        if (t < 0.25f) {
            float q = t / 0.25f;
            r = (int)(30 * q); g = 0; b = (int)(80 + 175 * q);
        } else if (t < 0.50f) {
            float q = (t - 0.25f) / 0.25f;
            r = (int)(30 + 225 * q); g = (int)(80 * q); b = (int)(255 - 155 * q);
        } else if (t < 0.75f) {
            float q = (t - 0.50f) / 0.25f;
            r = 255; g = (int)(80 + 175 * q); b = (int)(100 * (1 - q));
        } else {
            float q = (t - 0.75f) / 0.25f;
            r = 255; g = 255; b = (int)(255 * q);
        }
        return Color.rgb(clamp255(r), clamp255(g), clamp255(b));
    }

    private static int clamp255(int x) { return x < 0 ? 0 : (x > 255 ? 255 : x); }
}
