#define F(x) __attribute__((visibility("default"))) void x(void){}
F(ACameraManager_create) F(ACameraManager_delete) F(ACameraManager_getCameraIdList) F(ACameraManager_deleteCameraIdList) F(ACameraManager_openCamera)
F(ACameraDevice_close) F(ACameraDevice_createCaptureRequest) F(ACameraDevice_createCaptureSession)
F(ACameraOutputTarget_create) F(ACameraOutputTarget_free) F(ACaptureRequest_addTarget) F(ACaptureRequest_free)
F(ACaptureSessionOutputContainer_create) F(ACaptureSessionOutputContainer_free) F(ACaptureSessionOutputContainer_add)
F(ACaptureSessionOutput_create) F(ACaptureSessionOutput_free)
F(ACameraCaptureSession_setRepeatingRequest) F(ACameraCaptureSession_stopRepeating) F(ACameraCaptureSession_close)
