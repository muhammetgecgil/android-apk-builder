#define F(x) __attribute__((visibility("default"))) void x(void){}
F(ANativeActivity_setWindowFlags) F(ANativeActivity_setWindowFormat)
F(ANativeWindow_setBuffersGeometry) F(ANativeWindow_lock) F(ANativeWindow_unlockAndPost)
F(ALooper_prepare) F(ALooper_pollAll) F(ALooper_wake)
F(AInputQueue_attachLooper) F(AInputQueue_detachLooper) F(AInputQueue_getEvent) F(AInputQueue_preDispatchEvent) F(AInputQueue_finishEvent)
F(AInputEvent_getType) F(AMotionEvent_getAction) F(AMotionEvent_getX) F(AMotionEvent_getY)
