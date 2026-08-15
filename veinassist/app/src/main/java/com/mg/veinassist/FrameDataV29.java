package com.mg.veinassist;

import android.media.Image;
import java.nio.ByteBuffer;

final class FrameDataV29 {
    final int width, height;
    final byte[] y, u, v;
    final int yRowStride, uvRowStride, uvPixelStride;

    FrameDataV29(int width, int height, byte[] y, byte[] u, byte[] v,
              int yRowStride, int uvRowStride, int uvPixelStride) {
        this.width = width;
        this.height = height;
        this.y = y;
        this.u = u;
        this.v = v;
        this.yRowStride = yRowStride;
        this.uvRowStride = uvRowStride;
        this.uvPixelStride = uvPixelStride;
    }

    static FrameDataV29 copy(Image image) {
        Image.Plane[] p = image.getPlanes();
        ByteBuffer yb = p[0].getBuffer().duplicate();
        ByteBuffer ub = p[1].getBuffer().duplicate();
        ByteBuffer vb = p[2].getBuffer().duplicate();

        byte[] y = new byte[yb.remaining()];
        byte[] u = new byte[ub.remaining()];
        byte[] v = new byte[vb.remaining()];
        yb.get(y);
        ub.get(u);
        vb.get(v);

        return new FrameDataV29(
                image.getWidth(), image.getHeight(), y, u, v,
                p[0].getRowStride(), p[1].getRowStride(), p[1].getPixelStride());
    }
}
