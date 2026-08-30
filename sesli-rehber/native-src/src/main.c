/* Hareket Gorus Native V14.0
 * ARM64 NativeActivity + Camera2 NDK + AImageReader.
 * Portrait color preview, motion detection, persistent target IDs, route traces,
 * image-plane speed/acceleration and app-private CSV recording.
 */
#include "jni.h"
typedef signed char int8_t; typedef unsigned char uint8_t;
typedef signed short int16_t; typedef unsigned short uint16_t;
typedef signed int int32_t; typedef unsigned int uint32_t;
typedef signed long long int64_t; typedef unsigned long long uint64_t;
typedef unsigned long size_t; typedef signed long ssize_t; typedef unsigned long pthread_t;
typedef struct FILE FILE;
#define NULL ((void*)0)

typedef struct AAssetManager AAssetManager; typedef struct ANativeWindow ANativeWindow;
typedef struct AInputQueue AInputQueue; typedef struct AInputEvent AInputEvent; typedef struct ALooper ALooper;
typedef struct ACameraManager ACameraManager; typedef struct ACameraDevice ACameraDevice;
typedef struct ACameraCaptureSession ACameraCaptureSession; typedef struct ACaptureRequest ACaptureRequest;
typedef struct ACameraOutputTarget ACameraOutputTarget; typedef struct ACaptureSessionOutputContainer ACaptureSessionOutputContainer;
typedef struct ACaptureSessionOutput ACaptureSessionOutput; typedef struct AImageReader AImageReader; typedef struct AImage AImage;
typedef struct { int32_t left,top,right,bottom; } ARect;
typedef struct { int32_t width,height,stride,format; void* bits; uint32_t reserved[6]; } ANativeWindow_Buffer;

typedef struct ANativeActivity ANativeActivity;
typedef struct ANativeActivityCallbacks {
 void (*onStart)(ANativeActivity*); void (*onResume)(ANativeActivity*); void* (*onSaveInstanceState)(ANativeActivity*,size_t*);
 void (*onPause)(ANativeActivity*); void (*onStop)(ANativeActivity*); void (*onDestroy)(ANativeActivity*);
 void (*onWindowFocusChanged)(ANativeActivity*,int); void (*onNativeWindowCreated)(ANativeActivity*,ANativeWindow*);
 void (*onNativeWindowResized)(ANativeActivity*,ANativeWindow*); void (*onNativeWindowRedrawNeeded)(ANativeActivity*,ANativeWindow*);
 void (*onNativeWindowDestroyed)(ANativeActivity*,ANativeWindow*); void (*onInputQueueCreated)(ANativeActivity*,AInputQueue*);
 void (*onInputQueueDestroyed)(ANativeActivity*,AInputQueue*); void (*onContentRectChanged)(ANativeActivity*,const ARect*);
 void (*onConfigurationChanged)(ANativeActivity*); void (*onLowMemory)(ANativeActivity*);
} ANativeActivityCallbacks;
struct ANativeActivity { ANativeActivityCallbacks* callbacks; JavaVM* vm; JNIEnv* env; jobject clazz; const char* internalDataPath;
 const char* externalDataPath; int32_t sdkVersion; void* instance; AAssetManager* assetManager; const char* obbPath; };

typedef struct { void* context; void (*onDisconnected)(void*,ACameraDevice*); void (*onError)(void*,ACameraDevice*,int); } ACameraDevice_stateCallbacks;
typedef struct { void* context; void (*onClosed)(void*,ACameraCaptureSession*); void (*onReady)(void*,ACameraCaptureSession*); void (*onActive)(void*,ACameraCaptureSession*); } ACameraCaptureSession_stateCallbacks;
typedef struct { void* context; void (*onImageAvailable)(void*,AImageReader*); } AImageReader_ImageListener;
typedef struct { int numCameras; const char** cameraIds; } ACameraIdList;

extern void ANativeActivity_setWindowFlags(ANativeActivity*,uint32_t,uint32_t); extern void ANativeActivity_setWindowFormat(ANativeActivity*,int32_t);
extern int32_t ANativeWindow_setBuffersGeometry(ANativeWindow*,int32_t,int32_t,int32_t); extern int32_t ANativeWindow_lock(ANativeWindow*,ANativeWindow_Buffer*,ARect*); extern int32_t ANativeWindow_unlockAndPost(ANativeWindow*);
extern ALooper* ALooper_prepare(int); extern int ALooper_pollAll(int,int*,int*,void**); extern void ALooper_wake(ALooper*);
extern void AInputQueue_attachLooper(AInputQueue*,ALooper*,int,void*,void*); extern void AInputQueue_detachLooper(AInputQueue*);
extern int32_t AInputQueue_getEvent(AInputQueue*,AInputEvent**); extern int32_t AInputQueue_preDispatchEvent(AInputQueue*,AInputEvent*); extern void AInputQueue_finishEvent(AInputQueue*,AInputEvent*,int);
extern int32_t AInputEvent_getType(const AInputEvent*); extern int32_t AMotionEvent_getAction(const AInputEvent*); extern float AMotionEvent_getX(const AInputEvent*,size_t); extern float AMotionEvent_getY(const AInputEvent*,size_t);

extern ACameraManager* ACameraManager_create(void); extern void ACameraManager_delete(ACameraManager*); extern int ACameraManager_getCameraIdList(ACameraManager*,ACameraIdList**); extern void ACameraManager_deleteCameraIdList(ACameraIdList*);
extern int ACameraManager_openCamera(ACameraManager*,const char*,ACameraDevice_stateCallbacks*,ACameraDevice**);
extern int ACameraDevice_close(ACameraDevice*); extern int ACameraDevice_createCaptureRequest(const ACameraDevice*,int,ACaptureRequest**);
extern int ACameraDevice_createCaptureSession(ACameraDevice*,const ACaptureSessionOutputContainer*,const ACameraCaptureSession_stateCallbacks*,ACameraCaptureSession**);
extern int ACameraOutputTarget_create(ANativeWindow*,ACameraOutputTarget**); extern void ACameraOutputTarget_free(ACameraOutputTarget*); extern int ACaptureRequest_addTarget(ACaptureRequest*,const ACameraOutputTarget*); extern void ACaptureRequest_free(ACaptureRequest*);
extern int ACaptureSessionOutputContainer_create(ACaptureSessionOutputContainer**); extern void ACaptureSessionOutputContainer_free(ACaptureSessionOutputContainer*); extern int ACaptureSessionOutputContainer_add(ACaptureSessionOutputContainer*,const ACaptureSessionOutput*);
extern int ACaptureSessionOutput_create(ANativeWindow*,ACaptureSessionOutput**); extern void ACaptureSessionOutput_free(ACaptureSessionOutput*);
extern int ACameraCaptureSession_setRepeatingRequest(ACameraCaptureSession*,void*,int,ACaptureRequest**,int*); extern int ACameraCaptureSession_stopRepeating(ACameraCaptureSession*); extern void ACameraCaptureSession_close(ACameraCaptureSession*);

extern int AImageReader_new(int32_t,int32_t,int32_t,int32_t,AImageReader**); extern void AImageReader_delete(AImageReader*); extern int AImageReader_getWindow(AImageReader*,ANativeWindow**); extern int AImageReader_setImageListener(AImageReader*,AImageReader_ImageListener*); extern int AImageReader_acquireLatestImage(AImageReader*,AImage**);
extern void AImage_delete(AImage*); extern int AImage_getWidth(const AImage*,int32_t*); extern int AImage_getHeight(const AImage*,int32_t*); extern int AImage_getPlaneRowStride(const AImage*,int,int32_t*); extern int AImage_getPlanePixelStride(const AImage*,int,int32_t*); extern int AImage_getPlaneData(const AImage*,int,uint8_t**,int*); extern int AImage_getTimestamp(const AImage*,int64_t*);

extern void* calloc(size_t,size_t); extern void free(void*); extern void* memset(void*,int,size_t); extern void* memcpy(void*,const void*,size_t);
extern int pthread_create(pthread_t*,const void*,void*(*)(void*),void*); extern int pthread_join(pthread_t,void**); extern int snprintf(char*,size_t,const char*,...);
extern float sqrtf(float); extern float fabsf(float);
extern FILE* fopen(const char*,const char*); extern size_t fwrite(const void*,size_t,size_t,FILE*); extern int fclose(FILE*);

#define INPUT_EVENT_TYPE_MOTION 2
#define ACTION_MASK 0xff
#define ACTION_DOWN 0
#define ACTION_UP 1
#define ACTION_MOVE 2
#define ACTION_CANCEL 3
#define WINDOW_FORMAT_RGBA_8888 1
#define FLAG_KEEP_SCREEN_ON 0x80u
#define FLAG_FULLSCREEN 0x400u
#define AIMAGE_FORMAT_YUV_420_888 35
#define TEMPLATE_PREVIEW 1
#define CAM_W 1280
#define CAM_H 720
#define PRE_W 540
#define PRE_H 960
#define GRID_W 135
#define GRID_H 240
#define GRID_N (GRID_W*GRID_H)
#define CELL_W 4
#define CELL_H 4
#define MAX_DET 12
#define MAX_TRACK 12
#define ROUTE_N 120
#define RGBA(r,g,b) ((uint32_t)((r)|((uint32_t)(g)<<8)|((uint32_t)(b)<<16)|0xff000000u))
#define C_BG RGBA(4,10,20)
#define C_PANEL RGBA(13,27,45)
#define C_PANEL2 RGBA(20,38,61)
#define C_TEXT RGBA(238,247,255)
#define C_MUTED RGBA(139,164,191)
#define C_CYAN RGBA(40,223,255)
#define C_GREEN RGBA(74,235,127)
#define C_YELLOW RGBA(255,217,64)
#define C_RED RGBA(255,83,92)
#define C_ORANGE RGBA(255,162,55)
#define C_BLUE RGBA(49,130,255)

typedef struct { int x1,y1,x2,y2; } Rect;
typedef struct { int minx,miny,maxx,maxy,count; float cx,cy; } Detection;
typedef struct {
 int active,id,matched,missed;
 float cx,cy,dx,dy,speed,accel,last_speed;
 int minx,miny,maxx,maxy;
 int64_t last_ns;
 float route_x[ROUTE_N],route_y[ROUTE_N]; int route_count,route_pos;
} Track;

typedef struct {
 ANativeActivity* activity; volatile int running,window_lock,input_lock,frame_lock;
 ANativeWindow* window; AInputQueue* input; AInputQueue* attached_input; ALooper* looper; pthread_t thread; int thread_started;
 ACameraManager* cam_manager; ACameraDevice* cam_device; ACameraCaptureSession* cam_session; ACaptureRequest* cam_request;
 ACameraOutputTarget* cam_target; ACaptureSessionOutputContainer* cam_container; ACaptureSessionOutput* cam_output;
 AImageReader* reader; ANativeWindow* reader_window; int camera_started,camera_error,camera_permission,request_done;
 char camera_id[32]; int rotate_mode;
 uint8_t preview_y[PRE_W*PRE_H]; uint32_t preview_rgb[PRE_W*PRE_H]; int frame_ready; uint64_t frame_count; int64_t last_frame_ns;
 uint8_t grid_prev[GRID_N],grid_cur[GRID_N],mask[GRID_N],mask2[GRID_N],seen[GRID_N]; uint16_t queue[GRID_N]; int have_prev,camera_moving;
 Detection detections[MAX_DET]; int det_count;
 Track tracks[MAX_TRACK]; int next_id,selected_id;
 int view_w,view_h; Rect preview_rect,b_rotate,b_clear,b_target,b_record; float crop_x,crop_y,crop_w,crop_h; int pressed_button;
 int recording; FILE* csv; char csv_path[320];
} App;

static void lock(volatile int* p){while(__atomic_test_and_set(p,__ATOMIC_ACQUIRE)){} } static void unlock(volatile int* p){__atomic_clear(p,__ATOMIC_RELEASE);}
static int absi(int v){return v<0?-v:v;} static float absf(float v){return v<0?-v:v;} static int mini(int a,int b){return a<b?a:b;} static int maxi(int a,int b){return a>b?a:b;}
static int clampi(int v,int a,int b){return v<a?a:(v>b?b:v);} static int inside(Rect r,float x,float y){return x>=r.x1&&x<=r.x2&&y>=r.y1&&y<=r.y2;}
static void copy_text(char* d,const char* s,int n){int i=0;if(!d||n<1)return;while(s&&s[i]&&i<n-1){d[i]=s[i];i++;}d[i]=0;}
static int strlen0(const char* s){int n=0;while(s&&s[n])n++;return n;}

/* 5x7 ASCII font */
static const uint8_t* glyph(char c){static const uint8_t q[7]={14,17,1,2,4,0,4};
#define G(ch,a,b,c1,d,e,f,g) case ch:{static const uint8_t v[7]={a,b,c1,d,e,f,g};return v;}
switch(c){
G('A',14,17,17,31,17,17,17) G('B',30,17,17,30,17,17,30) G('C',14,17,16,16,16,17,14) G('D',30,17,17,17,17,17,30)
G('E',31,16,16,30,16,16,31) G('F',31,16,16,30,16,16,16) G('G',14,17,16,23,17,17,15) G('H',17,17,17,31,17,17,17)
G('I',31,4,4,4,4,4,31) G('J',7,2,2,2,18,18,12) G('K',17,18,20,24,20,18,17) G('L',16,16,16,16,16,16,31)
G('M',17,27,21,21,17,17,17) G('N',17,25,21,19,17,17,17) G('O',14,17,17,17,17,17,14) G('P',30,17,17,30,16,16,16)
G('Q',14,17,17,17,21,18,13) G('R',30,17,17,30,20,18,17) G('S',15,16,16,14,1,1,30) G('T',31,4,4,4,4,4,4)
G('U',17,17,17,17,17,17,14) G('V',17,17,17,17,17,10,4) G('W',17,17,17,21,21,21,10) G('X',17,17,10,4,10,17,17)
G('Y',17,17,10,4,4,4,4) G('Z',31,1,2,4,8,16,31)
G('0',14,17,19,21,25,17,14) G('1',4,12,4,4,4,4,14) G('2',14,17,1,2,4,8,31) G('3',30,1,1,14,1,1,30)
G('4',2,6,10,18,31,2,2) G('5',31,16,16,30,1,1,30) G('6',14,16,16,30,17,17,14) G('7',31,1,2,4,8,8,8)
G('8',14,17,17,14,17,17,14) G('9',14,17,17,15,1,1,14)
G('.',0,0,0,0,0,12,12) G(',',0,0,0,0,4,4,8) G(':',0,12,12,0,12,12,0) G('-',0,0,0,31,0,0,0)
G('+',0,4,4,31,4,4,0) G('/',1,2,2,4,8,8,16) G('%',17,2,4,4,8,16,17) G('=',0,31,0,31,0,0,0)
G('(',2,4,8,8,8,4,2) G(')',8,4,2,2,2,4,8) G('?',14,17,1,2,4,0,4) G('#',10,31,10,10,31,10,0)
G(' ',0,0,0,0,0,0,0)
default:return q;} }
#undef G
static void rect(ANativeWindow_Buffer* b,int x1,int y1,int x2,int y2,uint32_t c){if(x1<0)x1=0;if(y1<0)y1=0;if(x2>b->width)x2=b->width;if(y2>b->height)y2=b->height;if(x2<=x1||y2<=y1)return;for(int y=y1;y<y2;y++){uint32_t* p=(uint32_t*)b->bits+(size_t)y*b->stride+x1;for(int x=x1;x<x2;x++)*p++=c;}}
static void border(ANativeWindow_Buffer* b,int x1,int y1,int x2,int y2,int t,uint32_t c){rect(b,x1,y1,x2,y1+t,c);rect(b,x1,y2-t,x2,y2,c);rect(b,x1,y1,x1+t,y2,c);rect(b,x2-t,y1,x2,y2,c);} static uint32_t blend_px(uint32_t dst,uint32_t src,int alpha){int ia=255-alpha;int dr=dst&255,dg=(dst>>8)&255,db=(dst>>16)&255;int sr=src&255,sg=(src>>8)&255,sb=(src>>16)&255;return RGBA((dr*ia+sr*alpha)/255,(dg*ia+sg*alpha)/255,(db*ia+sb*alpha)/255);}
static void blend_rect(ANativeWindow_Buffer* b,int x1,int y1,int x2,int y2,uint32_t c,int alpha){if(x1<0)x1=0;if(y1<0)y1=0;if(x2>b->width)x2=b->width;if(y2>b->height)y2=b->height;if(x2<=x1||y2<=y1)return;for(int y=y1;y<y2;y++){uint32_t* p=(uint32_t*)b->bits+(size_t)y*b->stride+x1;for(int x=x1;x<x2;x++,p++)*p=blend_px(*p,c,alpha);}}

static int tw(const char* s,int sc){return strlen0(s)*6*sc;} static void text(ANativeWindow_Buffer* b,int x,int y,const char* s,int sc,uint32_t c){for(int k=0;s&&s[k];k++){const uint8_t* g=glyph(s[k]);for(int ry=0;ry<7;ry++)for(int rx=0;rx<5;rx++)if(g[ry]&(1<<(4-rx)))rect(b,x+rx*sc,y+ry*sc,x+(rx+1)*sc,y+(ry+1)*sc,c);x+=6*sc;}}
static void center_text(ANativeWindow_Buffer* b,int y,const char* s,int sc,uint32_t c){text(b,(b->width-tw(s,sc))/2,y,s,sc,c);}
static void line(ANativeWindow_Buffer* b,int x0,int y0,int x1,int y1,int thick,uint32_t c){int dx=absi(x1-x0),sx=x0<x1?1:-1,dy=-absi(y1-y0),sy=y0<y1?1:-1,err=dx+dy;for(;;){rect(b,x0-thick,y0-thick,x0+thick+1,y0+thick+1,c);if(x0==x1&&y0==y1)break;int e2=2*err;if(e2>=dy){err+=dy;x0+=sx;}if(e2<=dx){err+=dx;y0+=sy;}}}
static void circle(ANativeWindow_Buffer* b,int cx,int cy,int r,int thick,uint32_t c){for(int y=-r;y<=r;y++)for(int x=-r;x<=r;x++){int d=x*x+y*y;if(d<=r*r&&d>=(r-thick)*(r-thick))rect(b,cx+x,cy+y,cx+x+1,cy+y+1,c);}}
static void button(ANativeWindow_Buffer* b,Rect r,const char* label,uint32_t fill,uint32_t edge,int sc){blend_rect(b,r.x1,r.y1,r.x2,r.y2,fill,178);border(b,r.x1,r.y1,r.x2,r.y2,2,edge);text(b,(r.x1+r.x2-tw(label,sc))/2,(r.y1+r.y2-7*sc)/2,label,sc,C_TEXT);}
static uint32_t color_for_id(int id){switch(id%6){case 0:return C_CYAN;case 1:return C_YELLOW;case 2:return C_GREEN;case 3:return C_ORANGE;case 4:return C_BLUE;default:return C_RED;}}

static void draw_splash(App* a){lock(&a->window_lock);ANativeWindow* w=a->window;if(!w){unlock(&a->window_lock);return;}ANativeWindow_Buffer b;if(ANativeWindow_lock(w,&b,NULL)!=0){unlock(&a->window_lock);return;}int W=b.width,H=b.height,s=W/360;if(s<2)s=2;if(s>5)s=5;rect(&b,0,0,W,H,C_BG);int cx=W/2,cy=H/3,r=W/5;circle(&b,cx,cy,r,4,C_CYAN);circle(&b,cx,cy,(r*2)/3,3,C_BLUE);circle(&b,cx,cy,r/3,3,C_CYAN);line(&b,cx-r-20,cy,cx+r+20,cy,2,C_MUTED);line(&b,cx,cy-r-20,cx,cy+r+20,2,C_MUTED);line(&b,cx-r/2,cy+r/3,cx+r/2,cy-r/3,6,C_TEXT);line(&b,cx+r/5,cy-r/8,cx+r/2,cy-r/3,5,C_TEXT);line(&b,cx,cy,cx-r/3,cy-r/3,4,C_TEXT);center_text(&b,cy+r+90,"HAREKET GORUS",s+2,C_TEXT);center_text(&b,cy+r+145,"GOK HEDEF TAKIP SISTEMI",s,C_CYAN);center_text(&b,H-220,"UCAK  KUS  NESNE",s,C_MUTED);center_text(&b,H-165,"YORUNGE  HIZ  IVME",s,C_MUTED);center_text(&b,H-95,"NATIVE V14 HD",s,C_GREEN);ANativeWindow_unlockAndPost(w);unlock(&a->window_lock);}

static int yuv_clip(int v){return v<0?0:(v>255?255:v);}
static uint32_t yuv_rgb(int Y,int U,int V){int c=Y-16;if(c<0)c=0;int d=U-128,e=V-128;int r=(298*c+409*e+128)>>8;int g=(298*c-100*d-208*e+128)>>8;int b=(298*c+516*d+128)>>8;return RGBA(yuv_clip(r),yuv_clip(g),yuv_clip(b));}

static void clear_routes(App* a){for(int i=0;i<MAX_TRACK;i++){a->tracks[i].route_count=0;a->tracks[i].route_pos=0;}}
static Track* find_track_id(App* a,int id){for(int i=0;i<MAX_TRACK;i++)if(a->tracks[i].active&&a->tracks[i].id==id)return &a->tracks[i];return NULL;}
static Track* first_active(App* a){for(int i=0;i<MAX_TRACK;i++)if(a->tracks[i].active)return &a->tracks[i];return NULL;}
static void add_route(Track* t,float x,float y){if(t->route_count>0){int pi=(t->route_pos-1+ROUTE_N)%ROUTE_N;float dx=x-t->route_x[pi],dy=y-t->route_y[pi];if(dx*dx+dy*dy<1.5f)return;}t->route_x[t->route_pos]=x;t->route_y[t->route_pos]=y;t->route_pos=(t->route_pos+1)%ROUTE_N;if(t->route_count<ROUTE_N)t->route_count++;}

static void record_track(App* a,Track* t,int64_t ns){if(!a->recording||!a->csv||!t||!t->active)return;char row[220];double sec=(double)ns/1000000000.0;int n=snprintf(row,sizeof(row),"%.3f,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",sec,t->id,(double)t->cx,(double)t->cy,(double)t->speed,(double)t->accel,(double)t->dx,(double)t->dy);if(n>0)fwrite(row,1,(size_t)n,a->csv);}
static void toggle_record(App* a){if(a->recording){a->recording=0;if(a->csv){fclose(a->csv);a->csv=NULL;}return;}const char* base=(a->activity&&a->activity->externalDataPath)?a->activity->externalDataPath:(a->activity?a->activity->internalDataPath:NULL);if(!base)return;snprintf(a->csv_path,sizeof(a->csv_path),"%s/hareket_gorus_v14.csv",base);a->csv=fopen(a->csv_path,"wb");if(a->csv){const char* h="time_s,id,x_px,y_px,speed_px_s,accel_px_s2,dx_px,dy_px\n";fwrite(h,1,(size_t)strlen0(h),a->csv);a->recording=1;}}

static void update_tracks(App* a,Detection* ds,int n,int64_t ns){for(int i=0;i<MAX_TRACK;i++)if(a->tracks[i].active)a->tracks[i].matched=0;
 for(int d=0;d<n;d++){
  int best=-1;float bestd=999999.0f;for(int i=0;i<MAX_TRACK;i++){Track* t=&a->tracks[i];if(!t->active||t->matched)continue;float dx=ds[d].cx-t->cx,dy=ds[d].cy-t->cy;float dd=dx*dx+dy*dy;float bw=(float)(ds[d].maxx-ds[d].minx),bh=(float)(ds[d].maxy-ds[d].miny);float gate=82.0f+0.8f*(bw>bh?bw:bh);if(gate>190.0f)gate=190.0f;if(dd<gate*gate&&dd<bestd){bestd=dd;best=i;}}
  Track* t=NULL;if(best>=0)t=&a->tracks[best];else{int slot=-1;for(int i=0;i<MAX_TRACK;i++)if(!a->tracks[i].active){slot=i;break;}if(slot<0){int mm=-1,mi=0;for(int i=0;i<MAX_TRACK;i++)if(a->tracks[i].missed>mm){mm=a->tracks[i].missed;mi=i;}slot=mi;}t=&a->tracks[slot];memset(t,0,sizeof(*t));t->active=1;t->id=a->next_id++;if(a->next_id>999)a->next_id=1;t->cx=ds[d].cx;t->cy=ds[d].cy;t->last_ns=ns;if(a->selected_id<=0)a->selected_id=t->id;}
  float oldx=t->cx,oldy=t->cy;float dt=(t->last_ns>0&&ns>t->last_ns)?(float)(ns-t->last_ns)/1000000000.0f:0.0f;if(dt>0.005f&&dt<1.0f){float dx=ds[d].cx-oldx,dy=ds[d].cy-oldy;float raw=sqrtf(dx*dx+dy*dy)/dt;float old=t->speed;t->speed=(t->last_ns?0.65f*old+0.35f*raw:raw);t->accel=(t->last_ns?0.7f*t->accel+0.3f*((t->speed-old)/dt):0.0f);t->dx=dx;t->dy=dy;}else{t->dx=0;t->dy=0;}
  t->cx=ds[d].cx;t->cy=ds[d].cy;t->minx=ds[d].minx;t->miny=ds[d].miny;t->maxx=ds[d].maxx;t->maxy=ds[d].maxy;t->last_ns=ns;t->matched=1;t->missed=0;add_route(t,t->cx,t->cy);record_track(a,t,ns);
 }
 for(int i=0;i<MAX_TRACK;i++){Track* t=&a->tracks[i];if(t->active&&!t->matched){t->missed++;if(t->missed>10){int was=t->id;t->active=0;if(a->selected_id==was)a->selected_id=0;}}}
 if(a->selected_id<=0){Track* f=first_active(a);if(f)a->selected_id=f->id;}
}

static void analyze_motion(App* a,int64_t ns){int active=0;int sumdiff=0;
 for(int gy=0;gy<GRID_H;gy++)for(int gx=0;gx<GRID_W;gx++){int sum=0;int x0=gx*CELL_W,y0=gy*CELL_H;for(int y=0;y<CELL_H;y++)for(int x=0;x<CELL_W;x++)sum+=a->preview_y[(y0+y)*PRE_W+x0+x];int avg=sum/(CELL_W*CELL_H);int idx=gy*GRID_W+gx;a->grid_cur[idx]=(uint8_t)avg;if(a->have_prev)sumdiff+=absi(avg-(int)a->grid_prev[idx]);}
 if(!a->have_prev){memcpy(a->grid_prev,a->grid_cur,GRID_N);a->have_prev=1;a->det_count=0;return;}
 int mean=sumdiff/GRID_N;int threshold=mean+11;if(threshold<14)threshold=14;if(threshold>42)threshold=42;
 for(int i=0;i<GRID_N;i++){int d=absi((int)a->grid_cur[i]-(int)a->grid_prev[i]);a->mask[i]=(uint8_t)(d>threshold);if(a->mask[i])active++;}
 memcpy(a->grid_prev,a->grid_cur,GRID_N);
 if(active>GRID_N*38/100){a->camera_moving=1;a->det_count=0;for(int i=0;i<MAX_TRACK;i++)if(a->tracks[i].active){a->tracks[i].missed++;if(a->tracks[i].missed>15)a->tracks[i].active=0;}return;}a->camera_moving=0;
 memset(a->mask2,0,GRID_N);for(int gy=1;gy<GRID_H-1;gy++)for(int gx=1;gx<GRID_W-1;gx++){int i=gy*GRID_W+gx;if(!a->mask[i])continue;for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++)a->mask2[(gy+yy)*GRID_W+(gx+xx)]=1;}
 memset(a->seen,0,GRID_N);Detection tmp[MAX_DET];int tn=0;
 for(int sy=1;sy<GRID_H-1;sy++)for(int sx=1;sx<GRID_W-1;sx++){int start=sy*GRID_W+sx;if(!a->mask2[start]||a->seen[start])continue;int qh=0,qt=0;a->queue[qt++]=(uint16_t)start;a->seen[start]=1;int minx=sx,maxx=sx,miny=sy,maxy=sy,count=0,sumx=0,sumy=0;while(qh<qt){int p=a->queue[qh++],x=p%GRID_W,y=p/GRID_W;count++;sumx+=x;sumy+=y;if(x<minx)minx=x;if(x>maxx)maxx=x;if(y<miny)miny=y;if(y>maxy)maxy=y;for(int yy=-1;yy<=1;yy++)for(int xx=-1;xx<=1;xx++){if(!xx&&!yy)continue;int nx=x+xx,ny=y+yy;if(nx<1||nx>=GRID_W-1||ny<1||ny>=GRID_H-1)continue;int ni=ny*GRID_W+nx;if(a->mask2[ni]&&!a->seen[ni]){a->seen[ni]=1;if(qt<GRID_N)a->queue[qt++]=(uint16_t)ni;}}}
  if(count<5||count>4200)continue;Detection d;d.count=count;d.minx=minx*CELL_W;d.maxx=(maxx+1)*CELL_W;d.miny=miny*CELL_H;d.maxy=(maxy+1)*CELL_H;d.cx=((float)sumx/(float)count+0.5f)*CELL_W;d.cy=((float)sumy/(float)count+0.5f)*CELL_H;
  int pos=tn;if(pos<MAX_DET){tmp[pos]=d;tn++;for(int k=tn-1;k>0&&tmp[k].count>tmp[k-1].count;k--){Detection z=tmp[k];tmp[k]=tmp[k-1];tmp[k-1]=z;}}else if(d.count>tmp[MAX_DET-1].count){tmp[MAX_DET-1]=d;for(int k=MAX_DET-1;k>0&&tmp[k].count>tmp[k-1].count;k--){Detection z=tmp[k];tmp[k]=tmp[k-1];tmp[k-1]=z;}}
 }
 a->det_count=tn>MAX_DET?MAX_DET:tn;for(int i=0;i<a->det_count;i++)a->detections[i]=tmp[i];update_tracks(a,a->detections,a->det_count,ns);
}

static void process_yuv(App* a,const uint8_t* ydata,int yrow,const uint8_t* udata,int urow,int upix,const uint8_t* vdata,int vrow,int vpix,int sw,int sh,int64_t ns){if(!ydata||yrow<=0)return;lock(&a->frame_lock);
 for(int py=0;py<PRE_H;py++){for(int px=0;px<PRE_W;px++){int sx,sy;if(a->rotate_mode==0){sx=(py*(sw-1))/(PRE_H-1);sy=((PRE_W-1-px)*(sh-1))/(PRE_W-1);}else{sx=((PRE_H-1-py)*(sw-1))/(PRE_H-1);sy=(px*(sh-1))/(PRE_W-1);}int Y=ydata[sy*yrow+sx];int U=128,V=128;if(udata&&vdata&&urow>0&&vrow>0&&upix>0&&vpix>0){int ux=sx/2,uy=sy/2;U=udata[uy*urow+ux*upix];V=vdata[uy*vrow+ux*vpix];}int p=py*PRE_W+px;a->preview_y[p]=(uint8_t)Y;a->preview_rgb[p]=yuv_rgb(Y,U,V);}}
 a->frame_count++;a->last_frame_ns=ns;if((a->frame_count%2u)==0u)analyze_motion(a,ns);a->frame_ready=1;unlock(&a->frame_lock);
}

static void on_image(void* ctx,AImageReader* reader){App* a=(App*)ctx;if(!a||!a->running)return;AImage* image=NULL;if(AImageReader_acquireLatestImage(reader,&image)!=0||!image)return;int32_t w=0,h=0,yr=0,ur=0,vr=0,up=0,vp=0;uint8_t *yd=NULL,*ud=NULL,*vd=NULL;int yl=0,ul=0,vl=0;int64_t ts=0;AImage_getTimestamp(image,&ts);if(AImage_getWidth(image,&w)==0&&AImage_getHeight(image,&h)==0&&AImage_getPlaneRowStride(image,0,&yr)==0&&AImage_getPlaneData(image,0,&yd,&yl)==0&&w>0&&h>0&&yd){AImage_getPlaneRowStride(image,1,&ur);AImage_getPlaneRowStride(image,2,&vr);AImage_getPlanePixelStride(image,1,&up);AImage_getPlanePixelStride(image,2,&vp);AImage_getPlaneData(image,1,&ud,&ul);AImage_getPlaneData(image,2,&vd,&vl);if(ts<=0)ts=(int64_t)a->frame_count*33333333LL;process_yuv(a,yd,yr,ud,ur,up,vd,vr,vp,w,h,ts);}AImage_delete(image);}
static void cam_disc(void* ctx,ACameraDevice* d){(void)d;App* a=(App*)ctx;if(a)a->camera_error=31;} static void cam_err(void* ctx,ACameraDevice* d,int e){(void)d;App* a=(App*)ctx;if(a)a->camera_error=100+e;}
static void sess_closed(void* c,ACameraCaptureSession* s){(void)c;(void)s;} static void sess_ready(void* c,ACameraCaptureSession* s){(void)c;(void)s;} static void sess_active(void* c,ACameraCaptureSession* s){(void)s;App* a=(App*)c;if(a)a->camera_started=2;}

static int permission_check(App* a){JNIEnv* env=NULL;int attached=0;if(!a||!a->activity||!a->activity->vm)return 0;if((*a->activity->vm)->GetEnv(a->activity->vm,(void**)&env,JNI_VERSION_1_6)!=JNI_OK){if((*a->activity->vm)->AttachCurrentThread(a->activity->vm,(void**)&env,NULL)!=JNI_OK)return 0;attached=1;}jclass cls=(*env)->GetObjectClass(env,a->activity->clazz);jmethodID mid=cls?(*env)->GetMethodID(env,cls,"checkSelfPermission","(Ljava/lang/String;)I"):NULL;jstring p=(*env)->NewStringUTF(env,"android.permission.CAMERA");jint r=-1;if(mid&&p)r=(*env)->CallIntMethod(env,a->activity->clazz,mid,p);if((*env)->ExceptionCheck(env)){(*env)->ExceptionClear(env);r=-1;}if(p)(*env)->DeleteLocalRef(env,p);if(cls)(*env)->DeleteLocalRef(env,cls);if(attached)(*a->activity->vm)->DetachCurrentThread(a->activity->vm);return r==0;}
static void permission_request(ANativeActivity* act){if(!act||act->sdkVersion<23||!act->env||!act->clazz)return;JNIEnv* env=act->env;jclass ac=(*env)->GetObjectClass(env,act->clazz);jmethodID mid=ac?(*env)->GetMethodID(env,ac,"requestPermissions","([Ljava/lang/String;I)V"):NULL;jclass sc=(*env)->FindClass(env,"java/lang/String");if(!mid||!sc){if(ac)(*env)->DeleteLocalRef(env,ac);return;}jobjectArray arr=(*env)->NewObjectArray(env,1,sc,NULL);jstring perm=(*env)->NewStringUTF(env,"android.permission.CAMERA");(*env)->SetObjectArrayElement(env,arr,0,perm);(*env)->CallVoidMethod(env,act->clazz,mid,arr,501);if((*env)->ExceptionCheck(env))(*env)->ExceptionClear(env);if(perm)(*env)->DeleteLocalRef(env,perm);if(arr)(*env)->DeleteLocalRef(env,arr);if(sc)(*env)->DeleteLocalRef(env,sc);if(ac)(*env)->DeleteLocalRef(env,ac);}

static int camera_start(App* a){if(a->camera_started||!a->camera_permission)return 0;a->camera_error=0;a->cam_manager=ACameraManager_create();if(!a->cam_manager){a->camera_error=1;return 0;}ACameraIdList* list=NULL;if(ACameraManager_getCameraIdList(a->cam_manager,&list)!=0||!list||list->numCameras<1){a->camera_error=2;return 0;}const char* chosen=list->cameraIds[0];for(int i=0;i<list->numCameras;i++){const char* id=list->cameraIds[i];if(id&&id[0]=='0'&&id[1]==0){chosen=id;break;}}copy_text(a->camera_id,chosen,32);ACameraManager_deleteCameraIdList(list);
 if(AImageReader_new(CAM_W,CAM_H,AIMAGE_FORMAT_YUV_420_888,4,&a->reader)!=0||!a->reader){a->camera_error=3;return 0;}AImageReader_ImageListener il={a,on_image};if(AImageReader_setImageListener(a->reader,&il)!=0||AImageReader_getWindow(a->reader,&a->reader_window)!=0||!a->reader_window){a->camera_error=4;return 0;}
 ACameraDevice_stateCallbacks dc={a,cam_disc,cam_err};if(ACameraManager_openCamera(a->cam_manager,a->camera_id,&dc,&a->cam_device)!=0||!a->cam_device){a->camera_error=5;return 0;}if(ACameraDevice_createCaptureRequest(a->cam_device,TEMPLATE_PREVIEW,&a->cam_request)!=0||!a->cam_request){a->camera_error=6;return 0;}
 if(ACameraOutputTarget_create(a->reader_window,&a->cam_target)!=0||ACaptureRequest_addTarget(a->cam_request,a->cam_target)!=0){a->camera_error=7;return 0;}if(ACaptureSessionOutputContainer_create(&a->cam_container)!=0||ACaptureSessionOutput_create(a->reader_window,&a->cam_output)!=0||ACaptureSessionOutputContainer_add(a->cam_container,a->cam_output)!=0){a->camera_error=8;return 0;}
 ACameraCaptureSession_stateCallbacks sc={a,sess_closed,sess_ready,sess_active};if(ACameraDevice_createCaptureSession(a->cam_device,a->cam_container,&sc,&a->cam_session)!=0||!a->cam_session){a->camera_error=9;return 0;}ACaptureRequest* reqs[1]={a->cam_request};if(ACameraCaptureSession_setRepeatingRequest(a->cam_session,NULL,1,reqs,NULL)!=0){a->camera_error=10;return 0;}a->camera_started=1;return 1;}
static void camera_stop(App* a){if(!a)return;if(a->cam_session){ACameraCaptureSession_stopRepeating(a->cam_session);ACameraCaptureSession_close(a->cam_session);a->cam_session=NULL;}if(a->cam_device){ACameraDevice_close(a->cam_device);a->cam_device=NULL;}if(a->cam_request){ACaptureRequest_free(a->cam_request);a->cam_request=NULL;}if(a->cam_target){ACameraOutputTarget_free(a->cam_target);a->cam_target=NULL;}if(a->cam_output){ACaptureSessionOutput_free(a->cam_output);a->cam_output=NULL;}if(a->cam_container){ACaptureSessionOutputContainer_free(a->cam_container);a->cam_container=NULL;}if(a->reader){AImageReader_delete(a->reader);a->reader=NULL;}if(a->cam_manager){ACameraManager_delete(a->cam_manager);a->cam_manager=NULL;}a->camera_started=0;}

static void preview_to_screen(App* a,float px,float py,int* sx,int* sy){
 float nx=(px-a->crop_x)/a->crop_w,ny=(py-a->crop_y)/a->crop_h;
 *sx=a->preview_rect.x1+(int)(nx*(float)(a->preview_rect.x2-a->preview_rect.x1));
 *sy=a->preview_rect.y1+(int)(ny*(float)(a->preview_rect.y2-a->preview_rect.y1));
}
static const char* track_hint(Track* t){int w=t->maxx-t->minx,h=t->maxy-t->miny,area=w*h;if(w>h*2&&area>900)return "UCAK?";if(area<4800&&t->speed>16.0f)return "KUS?";return "HEDEF";}
static void draw_tracks(App* a,ANativeWindow_Buffer* b,int sc){for(int ti=0;ti<MAX_TRACK;ti++){Track* t=&a->tracks[ti];if(!t->active)continue;uint32_t col=color_for_id(t->id);if(t->route_count>1){int start=(t->route_pos-t->route_count+ROUTE_N)%ROUTE_N;for(int k=1;k<t->route_count;k++){int i0=(start+k-1)%ROUTE_N,i1=(start+k)%ROUTE_N;int x0,y0,x1,y1;preview_to_screen(a,t->route_x[i0],t->route_y[i0],&x0,&y0);preview_to_screen(a,t->route_x[i1],t->route_y[i1],&x1,&y1);if((x0>=0&&x0<b->width&&y0>=0&&y0<b->height)||(x1>=0&&x1<b->width&&y1>=0&&y1<b->height))line(b,x0,y0,x1,y1,2*sc,col);}}
 int x1,y1,x2,y2;preview_to_screen(a,(float)t->minx,(float)t->miny,&x1,&y1);preview_to_screen(a,(float)t->maxx,(float)t->maxy,&x2,&y2);border(b,x1,y1,x2,y2,2*sc,col);int cx,cy;preview_to_screen(a,t->cx,t->cy,&cx,&cy);circle(b,cx,cy,7*sc,2*sc,col);int ax=cx+(int)(t->dx*4.0f*sc),ay=cy+(int)(t->dy*4.0f*sc);line(b,cx,cy,ax,ay,2*sc,col);char lab[80];snprintf(lab,sizeof(lab),"#%d %s %.0f PX/S",t->id,track_hint(t),(double)t->speed);text(b,x1,y1-9*sc,lab,sc,col);}}

static void draw(App* a){
 lock(&a->window_lock);ANativeWindow* w=a->window;if(!w){unlock(&a->window_lock);return;}ANativeWindow_Buffer b;if(ANativeWindow_lock(w,&b,NULL)!=0){unlock(&a->window_lock);return;}
 int W=b.width,H=b.height;a->view_w=W;a->view_h=H;int sc=W/420;if(sc<2)sc=2;if(sc>5)sc=5;rect(&b,0,0,W,H,C_BG);
 a->preview_rect=(Rect){0,0,W,H};
 float screen_ar=(float)W/(float)H,src_ar=(float)PRE_W/(float)PRE_H;
 if(screen_ar>src_ar){a->crop_w=(float)PRE_W;a->crop_h=(float)PRE_W/screen_ar;a->crop_x=0.0f;a->crop_y=((float)PRE_H-a->crop_h)*0.5f;}
 else{a->crop_h=(float)PRE_H;a->crop_w=(float)PRE_H*screen_ar;a->crop_y=0.0f;a->crop_x=((float)PRE_W-a->crop_w)*0.5f;}
 lock(&a->frame_lock);
 if(a->frame_ready){for(int yy=0;yy<H;yy++){float fy=a->crop_y+((float)yy+0.5f)*a->crop_h/(float)H;int py=clampi((int)fy,0,PRE_H-1);uint32_t* dst=(uint32_t*)b.bits+(size_t)yy*b.stride;for(int xx=0;xx<W;xx++){float fx=a->crop_x+((float)xx+0.5f)*a->crop_w/(float)W;int px=clampi((int)fx,0,PRE_W-1);*dst++=a->preview_rgb[py*PRE_W+px];}}draw_tracks(a,&b,sc);}else{center_text(&b,H/2,"KAMERA GORUNTUSU BEKLENIYOR",sc,C_MUTED);}
 Track selcopy;int hassel=0;Track* sp=find_track_id(a,a->selected_id);if(sp){selcopy=*sp;hassel=1;}int cammove=a->camera_moving;unlock(&a->frame_lock);
 /* Top telemetry HUD over the live image. */
 int pad=10*sc;blend_rect(&b,pad,pad,W-pad,pad+52*sc,C_PANEL,164);text(&b,pad+10*sc,pad+8*sc,"HAREKET GORUS V14 HD",sc,C_TEXT);char top[110];snprintf(top,sizeof(top),"HEDEF %d  1280X720  KARE %llu",a->det_count,(unsigned long long)a->frame_count);text(&b,pad+10*sc,pad+29*sc,top,sc,C_CYAN);
 /* Selected-target telemetry HUD. */
 int info_h=62*sc,info_y=H-info_h-72*sc;blend_rect(&b,pad,info_y,W-pad,info_y+info_h,C_PANEL,172);char h1[150],h2[150];if(hassel){snprintf(h1,sizeof(h1),"#%d %s  X %.0f Y %.0f",selcopy.id,track_hint(&selcopy),(double)selcopy.cx,(double)selcopy.cy);snprintf(h2,sizeof(h2),"HIZ %.1f PX/S  IVME %.1f PX/S2",(double)selcopy.speed,(double)selcopy.accel);}else{snprintf(h1,sizeof(h1),"HEDEF BEKLENIYOR");snprintf(h2,sizeof(h2),"TELEFONU SABIT TUTUN");}text(&b,pad+10*sc,info_y+8*sc,h1,sc,C_TEXT);text(&b,pad+10*sc,info_y+34*sc,h2,sc,hassel?C_GREEN:C_MUTED);if(cammove)text(&b,W-tw("KAMERA HAREKETI",sc)-pad-10*sc,info_y+8*sc,"KAMERA HAREKETI",sc,C_RED);
 /* Four translucent controls float on top of the camera image. */
 int gap=6*sc,bh=48*sc,side=pad,bw=(W-2*side-3*gap)/4,by=H-bh-pad;a->b_rotate=(Rect){side,by,side+bw,by+bh};a->b_clear=(Rect){side+bw+gap,by,side+2*bw+gap,by+bh};a->b_target=(Rect){side+2*bw+2*gap,by,side+3*bw+2*gap,by+bh};a->b_record=(Rect){side+3*bw+3*gap,by,W-side,by+bh};button(&b,a->b_rotate,"DONDER",C_PANEL,C_CYAN,sc);button(&b,a->b_clear,"IZ SIL",C_PANEL,C_YELLOW,sc);button(&b,a->b_target,"HEDEF",C_PANEL,C_GREEN,sc);button(&b,a->b_record,a->recording?"DURDUR":"KAYIT",a->recording?C_RED:C_PANEL,a->recording?C_RED:C_ORANGE,sc);
 ANativeWindow_unlockAndPost(w);unlock(&a->window_lock);
}

static Rect expand_rect(Rect r,int d){Rect q={r.x1-d,r.y1-d,r.x2+d,r.y2+d};return q;}
static void cycle_target(App* a){int ids[MAX_TRACK],n=0;for(int i=0;i<MAX_TRACK;i++)if(a->tracks[i].active)ids[n++]=a->tracks[i].id;if(!n){a->selected_id=0;return;}int pos=-1;for(int i=0;i<n;i++)if(ids[i]==a->selected_id)pos=i;a->selected_id=ids[(pos+1)%n];}
static void do_button(App* a,int b){lock(&a->frame_lock);if(b==1){a->rotate_mode=!a->rotate_mode;a->have_prev=0;clear_routes(a);}else if(b==2)clear_routes(a);else if(b==3)cycle_target(a);else if(b==4)toggle_record(a);unlock(&a->frame_lock);}
static void touch_down(App* a,float x,float y){if(inside(expand_rect(a->b_rotate,18),x,y))a->pressed_button=1;else if(inside(expand_rect(a->b_clear,18),x,y))a->pressed_button=2;else if(inside(expand_rect(a->b_target,18),x,y))a->pressed_button=3;else if(inside(expand_rect(a->b_record,18),x,y))a->pressed_button=4;else{lock(&a->frame_lock);for(int i=0;i<MAX_TRACK;i++){Track* t=&a->tracks[i];if(!t->active)continue;int x1,y1,x2,y2;preview_to_screen(a,t->minx,t->miny,&x1,&y1);preview_to_screen(a,t->maxx,t->maxy,&x2,&y2);Rect r={x1-20,y1-20,x2+20,y2+20};if(inside(r,x,y)){a->selected_id=t->id;break;}}unlock(&a->frame_lock);}}
static void touch_up(App* a,float x,float y){int p=a->pressed_button;a->pressed_button=0;if(p==1&&inside(expand_rect(a->b_rotate,30),x,y))do_button(a,1);else if(p==2&&inside(expand_rect(a->b_clear,30),x,y))do_button(a,2);else if(p==3&&inside(expand_rect(a->b_target,30),x,y))do_button(a,3);else if(p==4&&inside(expand_rect(a->b_record,30),x,y))do_button(a,4);}
static void process_input(App* a){lock(&a->input_lock);AInputQueue* q=a->input;if(!q){unlock(&a->input_lock);return;}AInputEvent* e=NULL;while(AInputQueue_getEvent(q,&e)>=0){if(AInputQueue_preDispatchEvent(q,e))continue;int handled=0;if(AInputEvent_getType(e)==INPUT_EVENT_TYPE_MOTION){int ac=AMotionEvent_getAction(e)&ACTION_MASK;float x=AMotionEvent_getX(e,0),y=AMotionEvent_getY(e,0);if(ac==ACTION_DOWN)touch_down(a,x,y);else if(ac==ACTION_UP||ac==ACTION_CANCEL)touch_up(a,x,y);handled=1;}AInputQueue_finishEvent(q,e,handled);}unlock(&a->input_lock);}
static void* worker(void* p){App* a=(App*)p;a->looper=ALooper_prepare(1);lock(&a->input_lock);if(a->input&&a->looper){AInputQueue_attachLooper(a->input,a->looper,2,NULL,NULL);a->attached_input=a->input;}unlock(&a->input_lock);int tick=0,perm_tick=0;while(a->running){int id=ALooper_pollAll(25,NULL,NULL,NULL);if(id==2)process_input(a);if(++perm_tick>=16){perm_tick=0;a->camera_permission=permission_check(a);if(a->camera_permission&&!a->camera_started&&!a->camera_error)camera_start(a);}if(++tick>=2){draw(a);tick=0;}}camera_stop(a);if(a->csv){fclose(a->csv);a->csv=NULL;}lock(&a->input_lock);if(a->attached_input){AInputQueue_detachLooper(a->attached_input);a->attached_input=NULL;}unlock(&a->input_lock);return NULL;}

static void set_immersive(ANativeActivity* act){if(!act||!act->env||!act->clazz)return;JNIEnv* env=act->env;jclass ac=(*env)->GetObjectClass(env,act->clazz);if(!ac)return;jmethodID gw=(*env)->GetMethodID(env,ac,"getWindow","()Landroid/view/Window;");jobject win=gw?(*env)->CallObjectMethod(env,act->clazz,gw):NULL;if((*env)->ExceptionCheck(env)){(*env)->ExceptionClear(env);win=NULL;}if(win){jclass wc=(*env)->GetObjectClass(env,win);jmethodID gd=wc?(*env)->GetMethodID(env,wc,"getDecorView","()Landroid/view/View;"):NULL;jobject dv=gd?(*env)->CallObjectMethod(env,win,gd):NULL;if((*env)->ExceptionCheck(env)){(*env)->ExceptionClear(env);dv=NULL;}if(dv){jclass vc=(*env)->GetObjectClass(env,dv);jmethodID ss=vc?(*env)->GetMethodID(env,vc,"setSystemUiVisibility","(I)V"):NULL;if(ss)(*env)->CallVoidMethod(env,dv,ss,5894);if((*env)->ExceptionCheck(env))(*env)->ExceptionClear(env);if(vc)(*env)->DeleteLocalRef(env,vc);(*env)->DeleteLocalRef(env,dv);}if(wc)(*env)->DeleteLocalRef(env,wc);(*env)->DeleteLocalRef(env,win);}(*env)->DeleteLocalRef(env,ac);}
static void on_focus(ANativeActivity* act,int focused){if(focused)set_immersive(act);}
static App* app(ANativeActivity* a){return (App*)a->instance;}
static void on_window_created(ANativeActivity* act,ANativeWindow* w){App* a=app(act);if(!a)return;lock(&a->window_lock);a->window=w;ANativeWindow_setBuffersGeometry(w,0,0,WINDOW_FORMAT_RGBA_8888);unlock(&a->window_lock);draw_splash(a);if(!a->request_done){a->request_done=1;permission_request(act);}}
static void on_window_resized(ANativeActivity* act,ANativeWindow* w){App* a=app(act);if(!a)return;lock(&a->window_lock);a->window=w;ANativeWindow_setBuffersGeometry(w,0,0,WINDOW_FORMAT_RGBA_8888);unlock(&a->window_lock);draw(a);}static void on_window_redraw(ANativeActivity* act,ANativeWindow* w){(void)w;App* a=app(act);if(a)draw(a);}static void on_window_destroyed(ANativeActivity* act,ANativeWindow* w){(void)w;App* a=app(act);if(!a)return;lock(&a->window_lock);a->window=NULL;unlock(&a->window_lock);}
static void on_input_created(ANativeActivity* act,AInputQueue* q){App* a=app(act);if(!a)return;lock(&a->input_lock);a->input=q;if(a->looper&&a->attached_input!=q){if(a->attached_input)AInputQueue_detachLooper(a->attached_input);AInputQueue_attachLooper(q,a->looper,2,NULL,NULL);a->attached_input=q;ALooper_wake(a->looper);}unlock(&a->input_lock);}static void on_input_destroyed(ANativeActivity* act,AInputQueue* q){App* a=app(act);if(!a)return;lock(&a->input_lock);if(a->attached_input==q){AInputQueue_detachLooper(q);a->attached_input=NULL;}if(a->input==q)a->input=NULL;unlock(&a->input_lock);}
static void on_destroy(ANativeActivity* act){App* a=app(act);if(!a)return;a->running=0;if(a->looper)ALooper_wake(a->looper);if(a->thread_started)pthread_join(a->thread,NULL);act->instance=NULL;free(a);}
__attribute__((visibility("default"))) void ANativeActivity_onCreate(ANativeActivity* act,void* saved,size_t saved_size){(void)saved;(void)saved_size;App* a=(App*)calloc(1,sizeof(App));if(!a)return;a->activity=act;a->running=1;a->next_id=1;a->rotate_mode=0;act->instance=a;act->callbacks->onDestroy=on_destroy;act->callbacks->onWindowFocusChanged=on_focus;act->callbacks->onNativeWindowCreated=on_window_created;act->callbacks->onNativeWindowResized=on_window_resized;act->callbacks->onNativeWindowRedrawNeeded=on_window_redraw;act->callbacks->onNativeWindowDestroyed=on_window_destroyed;act->callbacks->onInputQueueCreated=on_input_created;act->callbacks->onInputQueueDestroyed=on_input_destroyed;ANativeActivity_setWindowFormat(act,WINDOW_FORMAT_RGBA_8888);ANativeActivity_setWindowFlags(act,FLAG_FULLSCREEN|FLAG_KEEP_SCREEN_ON,0);set_immersive(act);if(pthread_create(&a->thread,NULL,worker,a)==0)a->thread_started=1;}
