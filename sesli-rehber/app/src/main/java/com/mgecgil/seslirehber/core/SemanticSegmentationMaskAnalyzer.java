package com.mgecgil.seslirehber.core;

import java.nio.ByteBuffer;

/** Pure mask statistics for the bundled Pascal-style DeepLab-v3 category mask. */
public final class SemanticSegmentationMaskAnalyzer {
    // Pascal VOC / DeepLab label ids used by the official model.
    static final int BACKGROUND = 0;
    static final int BICYCLE = 2;
    static final int BOTTLE = 5;
    static final int BUS = 6;
    static final int CAR = 7;
    static final int CAT = 8;
    static final int CHAIR = 9;
    static final int COW = 10;
    static final int DINING_TABLE = 11;
    static final int DOG = 12;
    static final int HORSE = 13;
    static final int MOTORBIKE = 14;
    static final int PERSON = 15;
    static final int POTTED_PLANT = 16;
    static final int SHEEP = 17;
    static final int SOFA = 18;
    static final int TRAIN = 19;

    public record Raw(
            float personRatio,
            float vehicleRatio,
            float twoWheelerRatio,
            float animalRatio,
            float furnitureRatio,
            float smallObstacleRatio,
            float foregroundRatio,
            float leftOccupancy,
            float centerOccupancy,
            float rightOccupancy,
            float farOccupancy,
            float midOccupancy,
            float nearOccupancy,
            float lowerCenterOccupancy) {}

    public Raw analyze(ByteBuffer source, int width, int height) {
        if (source == null || width <= 0 || height <= 0 || source.capacity() < width * height) {
            return new Raw(0,0,0,0,0,0,0,0,0,0,0,0,0,0);
        }
        ByteBuffer mask = source.duplicate();
        long person=0, vehicle=0, two=0, animal=0, furniture=0, small=0, foreground=0;
        long left=0, center=0, right=0, far=0, mid=0, near=0, lowerCenter=0;
        long leftN=0, centerN=0, rightN=0, farN=0, midN=0, nearN=0, lowerCenterN=0;
        long total = (long) width * height;

        for (int y = 0; y < height; y++) {
            boolean isFar = y < height / 3;
            boolean isNear = y >= (height * 2) / 3;
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                int label = mask.get(idx) & 0xff;
                boolean fg = label != BACKGROUND;
                if (fg) foreground++;
                if (label == PERSON) person++;
                if (label == BUS || label == CAR || label == TRAIN) vehicle++;
                if (label == BICYCLE || label == MOTORBIKE) two++;
                if (label == CAT || label == COW || label == DOG || label == HORSE || label == SHEEP) animal++;
                if (label == CHAIR || label == DINING_TABLE || label == SOFA) furniture++;
                if (label == BOTTLE || label == POTTED_PLANT) small++;

                if (x < width / 3) { leftN++; if (fg) left++; }
                else if (x >= (width * 2) / 3) { rightN++; if (fg) right++; }
                else { centerN++; if (fg) center++; }

                if (isFar) { farN++; if (fg) far++; }
                else if (isNear) { nearN++; if (fg) near++; }
                else { midN++; if (fg) mid++; }

                if (isNear && x >= width / 3 && x < (width * 2) / 3) {
                    lowerCenterN++;
                    if (fg) lowerCenter++;
                }
            }
        }

        return new Raw(
                ratio(person,total), ratio(vehicle,total), ratio(two,total), ratio(animal,total),
                ratio(furniture,total), ratio(small,total), ratio(foreground,total),
                ratio(left,leftN), ratio(center,centerN), ratio(right,rightN),
                ratio(far,farN), ratio(mid,midN), ratio(near,nearN), ratio(lowerCenter,lowerCenterN));
    }

    private static float ratio(long n, long d) { return d <= 0 ? 0f : n / (float) d; }
}
