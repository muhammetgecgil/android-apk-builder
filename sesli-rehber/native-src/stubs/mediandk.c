#define F(x) __attribute__((visibility("default"))) void x(void){}
F(AImageReader_new) F(AImageReader_delete) F(AImageReader_getWindow) F(AImageReader_setImageListener) F(AImageReader_acquireLatestImage)
F(AImage_delete) F(AImage_getWidth) F(AImage_getHeight) F(AImage_getPlaneRowStride) F(AImage_getPlaneData) F(AImage_getTimestamp) F(AImage_getPlanePixelStride)
