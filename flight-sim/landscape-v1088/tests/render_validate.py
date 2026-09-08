"""Compile the actual APK GLSL and render each terrain under EGL/GLES2."""
import os
os.environ['PYOPENGL_PLATFORM'] = 'egl'
os.environ['EGL_PLATFORM'] = 'surfaceless'
os.environ['LIBGL_ALWAYS_SOFTWARE'] = '1'
from pathlib import Path
import ctypes, json, math, sys
import numpy as np
from PIL import Image, ImageDraw
from OpenGL import EGL
from OpenGL import GL as gl

root = Path(__file__).resolve().parents[1]
out = Path(sys.argv[1]); out.mkdir(parents=True, exist_ok=True)
W, H = 1280, 720
display = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY)
major, minor = ctypes.c_int(), ctypes.c_int()
assert EGL.eglInitialize(display, major, minor), 'EGL initialization'
assert EGL.eglBindAPI(EGL.EGL_OPENGL_ES_API)
attrs = (ctypes.c_int * 15)(EGL.EGL_SURFACE_TYPE, EGL.EGL_PBUFFER_BIT, EGL.EGL_RENDERABLE_TYPE,
    EGL.EGL_OPENGL_ES2_BIT, EGL.EGL_RED_SIZE,8,EGL.EGL_GREEN_SIZE,8,EGL.EGL_BLUE_SIZE,8,
    EGL.EGL_DEPTH_SIZE,24,EGL.EGL_ALPHA_SIZE,8,EGL.EGL_NONE)
config = (EGL.EGLConfig * 1)(); count = ctypes.c_int()
assert EGL.eglChooseConfig(display, attrs, config, 1, count) and count.value
pattrs = (ctypes.c_int * 5)(EGL.EGL_WIDTH,W,EGL.EGL_HEIGHT,H,EGL.EGL_NONE)
surface = EGL.eglCreatePbufferSurface(display, config[0], pattrs)
cattrs = (ctypes.c_int * 3)(EGL.EGL_CONTEXT_CLIENT_VERSION,2,EGL.EGL_NONE)
context = EGL.eglCreateContext(display, config[0], EGL.EGL_NO_CONTEXT, cattrs)
assert EGL.eglMakeCurrent(display,surface,surface,context)

def compile_shader(kind, source):
    shader = gl.glCreateShader(kind); gl.glShaderSource(shader, source); gl.glCompileShader(shader)
    ok = ctypes.c_int(); gl.glGetShaderiv(shader,gl.GL_COMPILE_STATUS,ctypes.byref(ok))
    if not ok.value:
        length=ctypes.c_int();gl.glGetShaderiv(shader,gl.GL_INFO_LOG_LENGTH,ctypes.byref(length))
        log=ctypes.create_string_buffer(length.value);gl.glGetShaderInfoLog(shader,length.value,None,log)
        raise AssertionError(log.value.decode())
    return shader

pg=gl.glCreateProgram()
for kind,name in [(gl.GL_VERTEX_SHADER,'aircraft_and_terrain.vert'),(gl.GL_FRAGMENT_SHADER,'aircraft_and_terrain.frag')]:
    gl.glAttachShader(pg,compile_shader(kind,(root/'shaders'/name).read_text()))
gl.glLinkProgram(pg);ok=ctypes.c_int();gl.glGetProgramiv(pg,gl.GL_LINK_STATUS,ctypes.byref(ok))
assert ok.value, 'Shader link failed'
gl.glUseProgram(pg)

def uniform(name): return gl.glGetUniformLocation(pg,name.encode())
def look(eye,target):
    e=np.array(eye,dtype=np.float32);z=e-np.array(target);z/=np.linalg.norm(z)
    x=np.cross([0,1,0],z);x/=np.linalg.norm(x);y=np.cross(z,x)
    m=np.eye(4,dtype=np.float32);m[:3,:3]=np.array([x,y,z]);m[:3,3]=-m[:3,:3]@e;return m
def perspective(fov):
    f=1/math.tan(math.radians(fov/2));n=.08;far=480
    return np.array([[f/(W/H),0,0,0],[0,f,0,0],[0,0,(far+n)/(n-far),2*far*n/(n-far)],[0,0,-1,0]],dtype=np.float32)
def matrix(name,m):
    gl.glUniformMatrix4fv(uniform(name),1,False,np.ascontiguousarray(m.T,dtype=np.float32))

results=[]
for kind in range(5):
    vertices=np.fromfile(out/f'terrain{kind}.bin',dtype='<f4')
    vbo=ctypes.c_uint();gl.glGenBuffers(1,ctypes.byref(vbo));gl.glBindBuffer(gl.GL_ARRAY_BUFFER,vbo.value)
    gl.glBufferData(gl.GL_ARRAY_BUFFER,vertices.nbytes,vertices,gl.GL_STATIC_DRAW)
    for name,size,offset in [('aPos',3,0),('aNormal',3,12),('aPart',1,24)]:
        loc=gl.glGetAttribLocation(pg,name.encode());gl.glEnableVertexAttribArray(loc)
        gl.glVertexAttribPointer(loc,size,gl.GL_FLOAT,False,28,ctypes.c_void_p(offset))
    for cam in range(4):
        eye=([0,4.72,19.2],[0,1.5,13.8],[12,4.55,13.5],[-12,4.55,13.5])[cam]
        target=[0,.02,1.42] if cam==1 else [0,.1,-.8]
        gl.glViewport(0,0,W,H);gl.glClearColor(.53,.64,.72,0);gl.glClear(gl.GL_COLOR_BUFFER_BIT|gl.GL_DEPTH_BUFFER_BIT)
        gl.glEnable(gl.GL_DEPTH_TEST);gl.glDisable(gl.GL_CULL_FACE);gl.glDisable(gl.GL_BLEND)
        matrix('uMvp',perspective(38)@look(eye,target));matrix('uModel',np.eye(4))
        gl.glUniform3f(uniform('uCameraPos'),*eye);gl.glUniform3f(uniform('uLightDir'),-.38,.86,-.33)
        gl.glUniform4f(uniform('uColor'),.34,.36,.38,1);gl.glUniform1f(uniform('uGear'),1)
        gl.glUniform1f(uniform('uThrottle'),.7);gl.glUniform1f(uniform('uTime'),2)
        gl.glDrawArrays(gl.GL_TRIANGLES,0,len(vertices)//7);gl.glFinish()
        err=gl.glGetError();assert err==gl.GL_NO_ERROR, f'GL error: {err}'
        pixels=np.empty((H,W,4),dtype=np.uint8);gl.glReadPixels(0,0,W,H,gl.GL_RGBA,gl.GL_UNSIGNED_BYTE,pixels)
        pixels=pixels[::-1];coverage=float(np.mean(pixels[:,:,3]>0))
        assert coverage>.15, f'No useful visible terrain: {kind}/{cam} {coverage}'
        # Only the background for presentation is composited; terrain pixels are exact GLSL output.
        sky=np.linspace([63,112,153],[167,184,191],H)[:,None,:];rgb=pixels[:,:,:3].copy()
        rgb=np.where(pixels[:,:,3,None]>0,rgb,sky).astype(np.uint8)
        if cam==0 or kind==0:Image.fromarray(rgb).save(out/f'terrain-{kind}-camera-{cam}.png')
        results.append({'kind':kind,'camera':cam,'terrain_coverage':round(coverage,4),'gl_error':int(err)})
    gl.glDeleteBuffers(1,ctypes.byref(vbo))
(out/'render-results.json').write_text(json.dumps({'shader_compile_link':'passed','renders':results},indent=2))
print(json.dumps(results))
EGL.eglMakeCurrent(display,EGL.EGL_NO_SURFACE,EGL.EGL_NO_SURFACE,EGL.EGL_NO_CONTEXT)
EGL.eglDestroyContext(display,context);EGL.eglDestroySurface(display,surface);EGL.eglTerminate(display)
