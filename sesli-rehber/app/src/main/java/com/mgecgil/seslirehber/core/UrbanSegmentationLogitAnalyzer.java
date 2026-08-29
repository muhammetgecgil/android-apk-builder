package com.mgecgil.seslirehber.core;

/** Pure post-processing for PIDNet-S Cityscapes logits [19,H,W]. */
public final class UrbanSegmentationLogitAnalyzer {
    public static final int CLASSES = 19;

    public record Raw(
            float roadRatio,
            float sidewalkRatio,
            float buildingWallRatio,
            float fencePoleRatio,
            float trafficControlRatio,
            float vegetationRatio,
            float terrainRatio,
            float personRiderRatio,
            float vehicleRatio,
            float twoWheelerRatio,
            float skyRatio,
            float leftObstacleOccupancy,
            float centerObstacleOccupancy,
            float rightObstacleOccupancy,
            float lowerCenterRoadRatio,
            float lowerCenterSidewalkRatio,
            float lowerCenterObstacleRatio) {}

    public Raw analyze(float[] logits, int width, int height) {
        int pixels = width * height;
        if (logits == null || width <= 0 || height <= 0 || logits.length < CLASSES * pixels) {
            return zero();
        }

        int[] classCount = new int[CLASSES];
        int[] sectorPixels = new int[3];
        int[] sectorObstacle = new int[3];
        int lowerCenterPixels = 0;
        int lowerRoad = 0;
        int lowerSidewalk = 0;
        int lowerObstacle = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p = y * width + x;
                int label = argmax(logits, p, pixels);
                classCount[label]++;

                int sector = Math.min(2, x * 3 / width);
                sectorPixels[sector]++;
                if (isObstacleLike(label)) sectorObstacle[sector]++;

                boolean lower = y >= Math.round(height * 0.52f);
                boolean center = x >= width / 3 && x < (width * 2) / 3;
                if (lower && center) {
                    lowerCenterPixels++;
                    if (label == 0) lowerRoad++;
                    if (label == 1) lowerSidewalk++;
                    if (isObstacleLike(label)) lowerObstacle++;
                }
            }
        }

        float n = pixels;
        return new Raw(
                classCount[0] / n,
                classCount[1] / n,
                (classCount[2] + classCount[3]) / n,
                (classCount[4] + classCount[5]) / n,
                (classCount[6] + classCount[7]) / n,
                classCount[8] / n,
                classCount[9] / n,
                (classCount[11] + classCount[12]) / n,
                (classCount[13] + classCount[14] + classCount[15] + classCount[16]) / n,
                (classCount[17] + classCount[18]) / n,
                classCount[10] / n,
                ratio(sectorObstacle[0], sectorPixels[0]),
                ratio(sectorObstacle[1], sectorPixels[1]),
                ratio(sectorObstacle[2], sectorPixels[2]),
                ratio(lowerRoad, lowerCenterPixels),
                ratio(lowerSidewalk, lowerCenterPixels),
                ratio(lowerObstacle, lowerCenterPixels));
    }

    static int argmax(float[] logits, int pixelIndex, int pixelsPerClass) {
        int best = 0;
        float bestValue = logits[pixelIndex];
        for (int c = 1; c < CLASSES; c++) {
            float value = logits[c * pixelsPerClass + pixelIndex];
            if (value > bestValue) {
                bestValue = value;
                best = c;
            }
        }
        return best;
    }

    static boolean isObstacleLike(int label) {
        return switch (label) {
            case 2, 3, 4, 5, 6, 7, 11, 12, 13, 14, 15, 16, 17, 18 -> true;
            default -> false;
        };
    }

    private static float ratio(int a, int b) { return b <= 0 ? 0f : a / (float) b; }

    private static Raw zero() {
        return new Raw(0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f,0f);
    }
}
