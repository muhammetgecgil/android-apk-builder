package com.mg.veinassist;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import java.util.Arrays;

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
    private final float[] green = new float[N];
    private final float[] cb = new float[N];
    private final float[] cr = new float[N];
    private final float[] skinFloat = new float[N];
    private final float[] skinDensity = new float[N];
    private final float[] m2 = new float[N];
    private final float[] m5 = new float[N];
    private final float[] m10 = new float[N];
    private final float[] raw = new float[N];
    private final float[] continuous = new float[N];
    private final float[] temporal = new float[N];
    private final byte[] persistence = new byte[N];
    private final byte[] bestDir = new byte[N];

    private final boolean[] skin = new boolean[N];
    private final boolean[] innerSkin = new boolean[N];
    private final boolean[] candidate = new boolean[N];
    private final boolean[] keep = new boolean[N];

    private final int[] labels = new int[N];
    private final int[] queue = new int[N];
    private final int[] pixels = new int[N];
    private final int[] thermalPx = new int[N];
    private final double[] integral = new double[(W + 1) * (H + 1)];

    private final int[][] offX = new int[8][3];
    private final int[][] offY = new int[8][3];

    VeinAnalyzerV29() {
        int[] ds = {3, 5, 7};
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 8.0;
            for (int j = 0; j < ds.length; j++) {
                offX[k][j] = (int)Math.round(Math.cos(a) * ds[j]);
                offY[k][j] = (int)Math.round(Math.sin(a) * ds[j]);
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
        boolean armLike = axisElongation(innerSkin) >= 1.55f;

        boxMeanInto(green, m2, 2);
        boxMeanInto(green, m5, 5);
        boxMeanInto(green, m10, 10);

        float threshold = 8.9f - sensitivity * 0.024f;
        Arrays.fill(raw, 0f);
        Arrays.fill(continuous, 0f);

        for (int y = 2; y < H - 2; y++) {
            for (int x = 2; x < W - 2; x++) {
                int i = y * W + x;
                if (!innerSkin[i] || lum[i] <= 58f) continue;

                float thin = Math.max(0f, m2[i] - green[i]);
                float mid = Math.max(0f, m5[i] - green[i]);
                float wide = Math.max(0f, m10[i] - green[i]);

                float body = Math.max(mid, wide * 0.88f);
                float hairPenalty = Math.max(0f, thin - body * 0.88f) * 1.45f;

                float gx = Math.abs(lum[i + 1] - lum[i - 1]);
                float gy = Math.abs(lum[i + W] - lum[i - W]);
                float edgePenalty = Math.max(0f, Math.max(gx, gy) - 18f) * 0.10f;

                float ridge = body - thin * 0.18f - hairPenalty - edgePenalty;
                raw[i] = Math.max(0f, ridge);
            }
        }

        for (int y = 9; y < H - 9; y++) {
            for (int x = 9; x < W - 9; x++) {
                int i = y * W + x;
                float base = raw[i];
                if (base < threshold * 0.38f) continue;

                float bestSupport = 0f;
                int bestCount = 0;
                int bestK = 0;

                for (int k = 0; k < 8; k++) {
                    float sum = 0f;
                    int count = 0;
                    for (int j = 0; j < 3; j++) {
                        int dx = offX[k][j];
                        int dy = offY[k][j];
                        int i1 = (y + dy) * W + (x + dx);
                        int i2 = (y - dy) * W + (x - dx);
                        float a = raw[i1], b = raw[i2];
                        sum += a + b;
                        if (a > threshold * 0.26f) count++;
                        if (b > threshold * 0.26f) count++;
                    }
                    float support = sum / 6f;
                    if (count > bestCount || (count == bestCount && support > bestSupport)) {
                        bestSupport = support;
                        bestCount = count;
                        bestK = k;
                    }
                }

                if (bestCount < 4) continue;

                float factor = 0.72f +
                        Math.min(0.62f, bestSupport / Math.max(2f, base) * 0.38f);
                float score = base * factor;
                score *= anatomyFactor(bestK, skinAxis, armLike);

                continuous[i] = score;
                bestDir[i] = (byte)bestK;
            }
        }

        for (int i = 0; i < N; i++) {
            float s = continuous[i];
            temporal[i] = temporal[i] * 0.76f + s * 0.24f;
            if (s > threshold * 0.70f)
                persistence[i] = (byte)Math.min(8, persistence[i] + 1);
            else
                persistence[i] = (byte)Math.max(0, persistence[i] - 1);

            candidate[i] = innerSkin[i] && temporal[i] > threshold && persistence[i] >= 3;
        }

        int components = filterContinuousComponents(candidate);

        for (int i = 0; i < N; i++) {
            if (keep[i]) {
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
                green[i] = g;
                cb[i] = 128f - 0.168736f * r - 0.331264f * g + 0.5f * b;
                cr[i] = 128f + 0.5f * r - 0.418688f * g - 0.081312f * b;
            }
        }
    }

    private int buildSkinMask() {
        float refCb = 0f, refCr = 0f;
        int refN = 0;

        for (int y = H * 30 / 100; y < H * 70 / 100; y++) {
            for (int x = W * 30 / 100; x < W * 70 / 100; x++) {
                int i = y * W + x;
                float chroma = (float)Math.sqrt(
                        (cb[i] - 128f) * (cb[i] - 128f) +
                        (cr[i] - 128f) * (cr[i] - 128f));
                boolean warm =
                        lum[i] > 32f && lum[i] < 248f &&
                        cb[i] > 72f && cb[i] < 150f &&
                        cr[i] > 126f && cr[i] < 198f &&
                        cr[i] - cb[i] > 8f && chroma > 9f;
                if (warm) {
                    refCb += cb[i];
                    refCr += cr[i];
                    refN++;
                }
            }
        }

        if (refN > 0) {
            refCb /= refN;
            refCr /= refN;
        }

        for (int i = 0; i < N; i++) {
            float chroma = (float)Math.sqrt(
                    (cb[i] - 128f) * (cb[i] - 128f) +
                    (cr[i] - 128f) * (cr[i] - 128f));

            boolean broad =
                    lum[i] > 25f && lum[i] < 250f &&
                    cb[i] > 70f && cb[i] < 154f &&
                    cr[i] > 124f && cr[i] < 200f &&
                    cr[i] - cb[i] > 7f && chroma > 8f;

            boolean adaptive = true;
            if (refN > 30) {
                float dc = cb[i] - refCb;
                float dr = cr[i] - refCr;
                adaptive = dc * dc + dr * dr < 31f * 31f;
            }
            skinFloat[i] = (broad && adaptive) ? 1f : 0f;
        }

        boxMeanInto(skinFloat, skinDensity, 2);
        for (int i = 0; i < N; i++) skin[i] = skinDensity[i] >= 0.60f;

        keepBestSkinComponentInPlace(skin);

        Arrays.fill(skinFloat, 0f);
        for (int i = 0; i < N; i++) if (skin[i]) skinFloat[i] = 1f;
        boxMeanInto(skinFloat, skinDensity, 4);

        int count = 0;
        for (int i = 0; i < N; i++) {
            innerSkin[i] = skin[i] && skinDensity[i] >= 0.94f;
            if (innerSkin[i]) count++;
        }
        return count;
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

            while (head < tail) {
                int p = queue[head++];
                size++;
                int x = p % W, y = p / W;
                int dx = x - cx, dy = y - cy;
                minDist2 = Math.min(minDist2, dx * dx + dy * dy);

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

            double centerBonus = 1.0 + 2.2 * Math.max(0,
                    1.0 - Math.sqrt(minDist2) / (Math.min(W, H) * 0.52));
            double score = size * centerBonus;
            if (score > bestScore) {
                bestScore = score;
                bestLabel = nextLabel;
            }
        }

        for (int i = 0; i < N; i++)
            src[i] = bestLabel != 0 && labels[i] == bestLabel;
    }

    private float principalAxis(boolean[] mask) {
        double sx = 0, sy = 0;
        int n = 0;
        for (int i = 0; i < N; i++) {
            if (mask[i]) {
                sx += i % W;
                sy += i / W;
                n++;
            }
        }
        if (n < 20) return 0f;

        double mx = sx / n, my = sy / n;
        double xx = 0, yy = 0, xy = 0;
        for (int i = 0; i < N; i++) {
            if (!mask[i]) continue;
            double dx = i % W - mx, dy = i / W - my;
            xx += dx * dx;
            yy += dy * dy;
            xy += dx * dy;
        }
        return (float)(0.5 * Math.atan2(2 * xy, xx - yy));
    }

    private float axisElongation(boolean[] mask) {
        double sx = 0, sy = 0;
        int n = 0;
        for (int i = 0; i < N; i++) {
            if (mask[i]) {
                sx += i % W;
                sy += i / W;
                n++;
            }
        }
        if (n < 20) return 1f;

        double mx = sx / n, my = sy / n;
        double xx = 0, yy = 0, xy = 0;
        for (int i = 0; i < N; i++) {
            if (!mask[i]) continue;
            double dx = i % W - mx, dy = i / W - my;
            xx += dx * dx;
            yy += dy * dy;
            xy += dx * dy;
        }
        xx /= n;
        yy /= n;
        xy /= n;

        double tr = xx + yy;
        double disc = Math.sqrt(Math.max(0,
                (xx - yy) * (xx - yy) + 4 * xy * xy));
        double l1 = Math.max(0.001, (tr + disc) * 0.5);
        double l2 = Math.max(0.001, (tr - disc) * 0.5);
        return (float)Math.sqrt(l1 / l2);
    }

    private float anatomyFactor(int dir, float skinAxis, boolean armLike) {
        if (!atlasPrior) return 1f;

        float angle = (float)(dir * Math.PI / 8.0);
        float diff = angleDiff(angle, skinAxis);
        float deg = (float)Math.toDegrees(diff);
        String r = region == null ? "AUTO" : region;

        if ("ONKOL".equals(r) || "UST KOL".equals(r)) {
            if (deg <= 32f) return 1.18f;
            if (deg >= 68f) return 0.78f;
            return 1.00f;
        }
        if ("DIRSEK".equals(r)) {
            if (deg <= 65f) return 1.08f;
            return 0.94f;
        }
        if ("EL".equals(r) || "AYAK".equals(r)) return 1.00f;

        if (armLike) {
            if (deg <= 35f) return 1.08f;
            if (deg >= 72f) return 0.90f;
        }
        return 1f;
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

            while (head < tail) {
                int p = queue[head++];
                int x = p % W, y = p / W;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                sx += x; sy += y;
                sxx += x * (double)x;
                syy += y * (double)y;
                sxy += x * (double)y;

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
            if (size < 9) continue;

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
            float fill = size / (float)Math.max(1, bw * bh);

            boolean vesselLike =
                    width <= 13.5f && fill <= 0.72f &&
                    (elongation >= 1.75f || (size >= 32 && fill <= 0.48f));

            if (vesselLike) {
                keptComponents++;
                for (int k = 0; k < tail; k++) keep[queue[k]] = true;
            }
        }
        return keptComponents;
    }

    private float computeSharpness() {
        double sum = 0;
        int count = 0;
        for (int y = 2; y < H - 2; y += 2) {
            for (int x = 2; x < W - 2; x += 2) {
                int i = y * W + x;
                float lap = 4f * lum[i] -
                        lum[i - 1] - lum[i + 1] - lum[i - W] - lum[i + W];
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
                out[y * W + x] =
                        (float)(sum / ((x1 - x0 + 1) * (y1 - y0 + 1)));
            }
        }
    }

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

    private static int clamp255(int x) {
        return x < 0 ? 0 : (x > 255 ? 255 : x);
    }
}
