import struct, pathlib
UTF8_FLAG=0x100; NO=0xffffffff
T_REF=1; T_STR=3; T_INT=0x10; T_BOOL=0x12; T_FLAGS=0x11
URI='http://schemas.android.com/apk/res/android'
strings=[]; idx={}
def S(x):
    if x not in idx: idx[x]=len(strings); strings.append(x)
    return idx[x]
# Put resource-backed attribute names early for a compact resource map.
res_ids={
 'theme':0x01010000,'label':0x01010001,'icon':0x01010002,'name':0x01010003,
 'hasCode':0x0101000c,'screenOrientation':0x0101001e,'configChanges':0x0101001f,'value':0x01010024,
 'versionCode':0x0101021b,'versionName':0x0101021c,'minSdkVersion':0x0101020c,'targetSdkVersion':0x01010270,
 'required':0x0101028e,'exported':0x0101056f,
}
for k in res_ids: S(k)
for x in ['android',URI,'manifest','package','uses-sdk','uses-permission','uses-feature','application','activity','meta-data','intent-filter','action','category',
          'com.mgecgil.skytrackv14','14.0','Hareket Gorus V14 HD','android.permission.CAMERA','android.hardware.camera.any','android.app.NativeActivity','android.app.lib_name','skytrack','android.intent.action.MAIN','android.intent.category.LAUNCHER','true','false','portrait','orientation|screenSize|keyboardHidden']:
    S(x)

def enc8(n): return bytes([n]) if n<0x80 else bytes([(n>>8)|0x80,n&0xff])
def chunk(t,h,b): return struct.pack('<HHI',t,h,8+len(b))+b
def pool():
    blob=bytearray(); offs=[]
    for x in strings:
        b=x.encode('utf-8'); offs.append(len(blob)); blob += enc8(len(x))+enc8(len(b))+b+b'\0'
    while len(blob)%4: blob+=b'\0'
    return chunk(1,28,struct.pack('<IIIII',len(strings),0,UTF8_FLAG,28+4*len(strings),0)+b''.join(struct.pack('<I',o) for o in offs)+blob)
def rmap():
    m=[0]*(max(idx[k] for k in res_ids)+1)
    for k,v in res_ids.items(): m[idx[k]]=v
    return chunk(0x180,8,b''.join(struct.pack('<I',x) for x in m))
def node(t,ext): return struct.pack('<HHIII',t,16,16+len(ext),1,NO)+ext
def attr(name,val,typ=T_STR,android=True,raw=None):
    ns=S(URI) if android else NO; ni=S(name)
    if typ==T_STR:
        data=S(val); rawi=data if raw is None else S(raw)
    else:
        data=int(val)&0xffffffff; rawi=NO if raw is None else S(raw)
    return (res_ids.get(name,0),struct.pack('<IIIHBBI',ns,ni,rawi,8,0,typ,data))
def start(tag,attrs=()):
    aa=sorted(attrs,key=lambda x:(x[0],))
    ext=struct.pack('<IIHHHHHH',NO,S(tag),20,20,len(aa),0,0,0)+b''.join(x[1] for x in aa)
    return node(0x102,ext)
def end(tag): return node(0x103,struct.pack('<II',NO,S(tag)))

x=[node(0x100,struct.pack('<II',S('android'),S(URI)))]
x += [start('manifest',[attr('package','com.mgecgil.skytrackv14',T_STR,False),attr('versionCode',1400,T_INT),attr('versionName','14.0')])]
x += [start('uses-sdk',[attr('minSdkVersion',24,T_INT),attr('targetSdkVersion',28,T_INT)]),end('uses-sdk')]
x += [start('uses-permission',[attr('name','android.permission.CAMERA')]),end('uses-permission')]
x += [start('uses-feature',[attr('name','android.hardware.camera.any'),attr('required',0,T_BOOL,raw='false')]),end('uses-feature')]
x += [start('application',[attr('theme',0x01030007,T_REF),attr('label','Hareket Gorus V14 HD'),attr('icon',0x7f010000,T_REF),attr('hasCode',0,T_BOOL,raw='false')])]
x += [start('activity',[attr('name','android.app.NativeActivity'),attr('label','Hareket Gorus V14 HD'),attr('exported',1,T_BOOL,raw='true'),attr('screenOrientation',1,T_INT,raw='portrait'),attr('configChanges',0x4a0,T_FLAGS,raw='orientation|screenSize|keyboardHidden')])]
x += [start('meta-data',[attr('name','android.app.lib_name'),attr('value','skytrack')]),end('meta-data')]
x += [start('intent-filter'),start('action',[attr('name','android.intent.action.MAIN')]),end('action'),start('category',[attr('name','android.intent.category.LAUNCHER')]),end('category'),end('intent-filter')]
x += [end('activity'),end('application'),end('manifest'),node(0x101,struct.pack('<II',S('android'),S(URI)))]
body=pool()+rmap()+b''.join(x); out=struct.pack('<HHI',3,8,8+len(body))+body
p=pathlib.Path(__file__).parent/'apk'/'AndroidManifest.xml'; p.write_bytes(out); print(p,len(out))
