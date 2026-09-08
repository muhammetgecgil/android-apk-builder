package com.mg.fixturecockpitsim.visual;

/** Offline scenery: warped ridgelines, sheltered valleys and smooth terrain normals.
 * Vertex ABI is unchanged: position.xyz, normal.xyz, material id (7 floats).
 * Generation runs once per terrain kind, never in the frame loop.
 */
public final class CinematicTerrainMesh {
    private static final int NX = 161, NZ = 129;
    private CinematicTerrainMesh() {}

    public static float[] build(int kind) {
        if (kind < 0 || kind > 4) throw new IllegalArgumentException("terrain kind");
        float[] points = new float[NX * NZ * 6];
        float part = kind == 4 ? 65f : 60f + kind;
        for (int iz = 0; iz < NZ; iz++) {
            float depth = (float) Math.pow(iz / (float) (NZ - 1), 1.45);
            float z = 22f - 427f * depth;
            for (int ix = 0; ix < NX; ix++) {
                float u = ix * 2f / (NX - 1) - 1f;
                float x = Math.copySign(112f * (float) Math.pow(Math.abs(u), 1.2), u);
                float y = height(kind, x, z);
                int p = (iz * NX + ix) * 6;
                points[p] = x; points[p + 1] = y; points[p + 2] = z;
            }
        }
        // Normals follow the sampled mesh, avoiding high-frequency shading that
        // cannot be represented by distant geometry at this level of detail.
        for (int iz = 0; iz < NZ; iz++) for (int ix = 0; ix < NX; ix++) {
            int p = (iz * NX + ix) * 6;
            int l = (iz * NX + Math.max(0, ix - 1)) * 6;
            int r = (iz * NX + Math.min(NX - 1, ix + 1)) * 6;
            int n = (Math.max(0, iz - 1) * NX + ix) * 6;
            int f = (Math.min(NZ - 1, iz + 1) * NX + ix) * 6;
            float dx = (points[r + 1] - points[l + 1]) / (points[r] - points[l]);
            float dz = (points[f + 1] - points[n + 1]) / (points[f + 2] - points[n + 2]);
            float inv = 1f / (float) Math.sqrt(dx * dx + dz * dz + 1f);
            points[p + 3] = -dx * inv; points[p + 4] = inv; points[p + 5] = -dz * inv;
        }
        float[] out = new float[(NX - 1) * (NZ - 1) * 6 * 7];
        int cursor = 0;
        for (int iz = 0; iz < NZ - 1; iz++) for (int ix = 0; ix < NX - 1; ix++) {
            int a = (iz * NX + ix) * 6, b = a + 6, c = a + NX * 6, d = c + 6;
            // Alternating diagonals remove long regular diagonal seams.
            if (((ix + iz) & 1) == 0) {
                cursor = vertex(out, cursor, points, a, part);
                cursor = vertex(out, cursor, points, b, part);
                cursor = vertex(out, cursor, points, d, part);
                cursor = vertex(out, cursor, points, a, part);
                cursor = vertex(out, cursor, points, d, part);
                cursor = vertex(out, cursor, points, c, part);
            } else {
                cursor = vertex(out, cursor, points, a, part);
                cursor = vertex(out, cursor, points, b, part);
                cursor = vertex(out, cursor, points, c, part);
                cursor = vertex(out, cursor, points, b, part);
                cursor = vertex(out, cursor, points, d, part);
                cursor = vertex(out, cursor, points, c, part);
            }
        }
        return out;
    }

    private static int vertex(float[] out, int i, float[] points, int p, float part) {
        System.arraycopy(points, p, out, i, 6);
        out[i + 6] = part;
        return i + 7;
    }

    public static float height(int kind, float x, float z) {
        float depth = -z;
        float far = smooth(18f, 175f, depth);
        float wx = x + 15f * (noise(x * .014f + 31f, z * .013f) - .5f);
        float wz = z + 13f * (noise(x * .013f, z * .016f + 19f) - .5f);
        float broad = fbm(wx * .024f + 7f, wz * .021f - 5f);
        float ridges = ridge(wx * .033f, wz * .028f);
        float detail = fbm(wx * .15f + 17f, wz * .13f + 3f) - .5f;
        if (kind == 0) {
            float center = 18f * (noise(z * .018f, 4.3f) - .5f);
            float valley = 1f - smooth(4f, 25f, Math.abs(x - center));
            float mountain = (10f * broad + 30f * ridges + 1.1f * detail);
            return -10.8f + (0.06f + .94f * far) * mountain * (1f - .72f * valley);
        }
        if (kind == 1) {
            float shore = -12f + 9f * (noise(z * .020f, 8.1f) - .5f);
            float inland = smooth(shore - 2f, shore + 24f, x);
            float hills = (4.5f + 20f * far) * (.30f * broad + .70f * ridges);
            return -10.1f + inland * (2f + hills + detail * far);
        }
        if (kind == 2) {
            float phase = wx * .088f + wz * .035f + 2.8f * broad;
            float wave = .5f + .5f * (float) Math.sin(phase);
            float dune = (float) Math.pow(wave, 1.8);
            return -10.1f + (2f + 4.6f * far) * dune + 1.7f * broad + detail * .28f;
        }
        if (kind == 3) {
            float mesa = smooth(.35f, .70f, broad);
            float eroded = 1f - .45f * ridge(wx * .12f + 8f, wz * .105f);
            return -10.3f + 2.5f * broad + far * (12f * mesa * eroded + 4f * ridges) + detail * .4f;
        }
        float inlet = smooth(-16f, 20f, x + 8f * (noise(z * .015f, 22f) - .5f));
        return -10.3f + inlet * (2f + far * (9f * broad + 15f * ridges)) + detail * .30f;
    }

    private static float ridge(float x, float z) {
        float sum = 0f, weight = .56f;
        for (int i = 0; i < 3; i++) {
            float n = 1f - Math.abs(2f * noise(x, z) - 1f);
            sum += n * n * weight;
            float oldX = x;
            x = 1.73f * x + 1.17f * z + 13.1f;
            z = -1.17f * oldX + 1.73f * z + 7.9f;
            weight *= .47f;
        }
        return sum;
    }

    private static float fbm(float x, float z) {
        float sum = 0f, weight = .57f;
        for (int i = 0; i < 4; i++) {
            sum += noise(x, z) * weight;
            float oldX = x;
            x = 1.62f * x + 1.27f * z + 9.2f;
            z = -1.27f * oldX + 1.62f * z + 15.7f;
            weight *= .46f;
        }
        return sum;
    }

    private static float noise(float x, float z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        float fx = x - ix, fz = z - iz;
        float sx = fx * fx * fx * (fx * (fx * 6f - 15f) + 10f);
        float sz = fz * fz * fz * (fz * (fz * 6f - 15f) + 10f);
        float a = hash(ix, iz), b = hash(ix + 1, iz), c = hash(ix, iz + 1), d = hash(ix + 1, iz + 1);
        return (a + (b - a) * sx) * (1f - sz) + (c + (d - c) * sx) * sz;
    }

    private static float hash(int x, int z) {
        int v = x * 0x1f1f1f1f ^ z * 0x5f356495 ^ 0x6749ba31;
        v = (v ^ (v >>> 16)) * 0x7feb352d;
        v = (v ^ (v >>> 15)) * 0x846ca68b;
        v ^= v >>> 16;
        return (v & 0x00ffffff) / 16777215f;
    }

    private static float smooth(float a, float b, float x) {
        float t = Math.max(0f, Math.min(1f, (x - a) / (b - a)));
        return t * t * (3f - 2f * t);
    }
}
