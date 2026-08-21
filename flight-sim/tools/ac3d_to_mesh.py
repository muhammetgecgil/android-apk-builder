#!/usr/bin/env python3
import math, struct, sys, os
from collections import defaultdict

TEXTURES = [
    'Map__2_Mix.png','Map__4_Mix.png','Map__4_Noise.png','Map__6_Noise.png',
    'Map__8_Mix.png','Map__8_Noise.png','Map__9_Mix.png','Map__10_Mix.png',
    'Map__15_Noise.png','Map__18_Mix.png','Map__20_Mix.png','Map__24_Mix.png',
    'Map__26_Mix.png','Map__90_Mix.png','Map__96_Mix.png'
]
TEX_INDEX={n:i for i,n in enumerate(TEXTURES)}

# V18 layout stays compact and Android-friendly:
# position(3), crease-aware smooth normal(3), material color(4), atlasUV(2).
# color.a = 1 when a texture is present, 0 for material-only shading.
class Obj:
    def __init__(self):
        self.name=''; self.loc=(0.,0.,0.); self.rot=(1,0,0,0,1,0,0,0,1)
        self.verts=[]; self.surfs=[]; self.children=[]
        self.texture=''; self.texrep=(1.,1.); self.texoff=(0.,0.)

def mmul(a,b):
    return tuple(sum(a[i*3+k]*b[k*3+j] for k in range(3)) for i in range(3) for j in range(3))
def mv(m,v): return (m[0]*v[0]+m[1]*v[1]+m[2]*v[2],m[3]*v[0]+m[4]*v[1]+m[5]*v[2],m[6]*v[0]+m[7]*v[1]+m[8]*v[2])
def add(a,b): return (a[0]+b[0],a[1]+b[1],a[2]+b[2])
def sub(a,b): return (a[0]-b[0],a[1]-b[1],a[2]-b[2])
def cross(a,b): return (a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0])
def dot(a,b): return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]
def length(a): return math.sqrt(max(0.,dot(a,a)))
def norm(a):
    l=length(a) or 1.; return (a[0]/l,a[1]/l,a[2]/l)
def qstr(line):
    p=line.find('"'); q=line.rfind('"'); return line[p+1:q] if p>=0 and q>p else ''

def parse(path):
    lines=open(path,'r',encoding='utf-8',errors='ignore').read().splitlines(); mats=[]; i=1
    while i<len(lines) and lines[i].startswith('MATERIAL '):
        s=lines[i].split()
        try:
            k=s.index('rgb'); mats.append(tuple(float(x) for x in s[k+1:k+4]))
        except: mats.append((.58,.60,.62))
        i+=1
    def obj(i):
        assert lines[i].startswith('OBJECT '); o=Obj(); i+=1
        while i<len(lines):
            ln=lines[i].strip()
            if ln.startswith('name '): o.name=qstr(ln); i+=1
            elif ln.startswith('loc '): o.loc=tuple(map(float,ln.split()[1:4])); i+=1
            elif ln.startswith('rot '): o.rot=tuple(map(float,ln.split()[1:10])); i+=1
            elif ln.startswith('texture '): o.texture=os.path.basename(qstr(ln)); i+=1
            elif ln.startswith('texrep '): o.texrep=tuple(map(float,ln.split()[1:3])); i+=1
            elif ln.startswith('texoff '): o.texoff=tuple(map(float,ln.split()[1:3])); i+=1
            elif ln.startswith('data '):
                i+=1
                if i<len(lines): i+=1
            elif ln.startswith('crease ') or ln.startswith('url '): i+=1
            elif ln.startswith('numvert '):
                n=int(ln.split()[1]); i+=1
                for _ in range(n):
                    a=lines[i].split(); o.verts.append(tuple(map(float,a[:3]))); i+=1
            elif ln.startswith('numsurf '):
                n=int(ln.split()[1]); i+=1
                for _ in range(n):
                    flags=0; mat=0; refs=[]
                    if lines[i].strip().startswith('SURF '):
                        try: flags=int(lines[i].split()[1],0)
                        except: pass
                        i+=1
                    if i<len(lines) and lines[i].strip().startswith('mat '): mat=int(lines[i].split()[1]); i+=1
                    if i<len(lines) and lines[i].strip().startswith('refs '):
                        nr=int(lines[i].split()[1]); i+=1
                        for __ in range(nr):
                            a=lines[i].split(); idx=int(a[0]); u=float(a[1]) if len(a)>1 else 0.; v=float(a[2]) if len(a)>2 else 0.
                            refs.append((idx,u,v)); i+=1
                    o.surfs.append((mat,refs,flags))
            elif ln.startswith('kids '):
                n=int(ln.split()[1]); i+=1
                for _ in range(n): ch,i=obj(i); o.children.append(ch)
                return o,i
            else: i+=1
        return o,i
    root,i=obj(i); return mats,root

def atlas_uv(tex,u,v,rep,off):
    if tex not in TEX_INDEX: return 0.,0.,0.
    idx=TEX_INDEX[tex]; cellx=idx%4; celly=idx//4
    u=(u*rep[0]+off[0])%1.; v=(v*rep[1]+off[1])%1.
    inset=.004
    return (cellx+inset+u*(1-2*inset))/4., (celly+inset+(1-v)*(1-2*inset))/4., 1.

def build(mats,root):
    faces=[]; I=(1,0,0,0,1,0,0,0,1)
    # Keep the real airframe/control surfaces/canopy/nozzles, hide only ground gear.
    skip=('landinggear','landing_gear','nosegear','maingear','wheel','tire','towbar')
    def walk(o,pm,pt):
        wm=mmul(pm,o.rot); wt=add(pt,mv(pm,o.loc)); hidden=any(s in o.name.lower().replace(' ','') for s in skip)
        wv=[add(wt,mv(wm,v)) for v in o.verts]
        if not hidden:
            for mat,refs,flags in o.surfs:
                if len(refs)<3: continue
                col=mats[mat] if 0<=mat<len(mats) else (.58,.60,.62)
                mx=max(col); mn=min(col)
                if mx>.93 and mx-mn<.08: col=(.56,.58,.60)
                for j in range(1,len(refs)-1):
                    rr=(refs[0],refs[j],refs[j+1]); verts=[]; ok=True
                    for idx,u,v in rr:
                        if idx<0 or idx>=len(wv): ok=False; break
                        au,av,tm=atlas_uv(o.texture,u,v,o.texrep,o.texoff)
                        verts.append((wv[idx],au,av,tm))
                    if not ok: continue
                    a,b,c=verts[0][0],verts[1][0],verts[2][0]
                    raw=cross(sub(b,a),sub(c,a)); area=max(length(raw),1e-12); fn=norm(raw)
                    faces.append({'verts':verts,'fn':fn,'area':area,'col':col})
        for ch in o.children: walk(ch,wm,wt)
    walk(root,I,(0,0,0))

    pts=[vv[0] for f in faces for vv in f['verts']]
    lo=[min(p[k] for p in pts) for k in range(3)]; hi=[max(p[k] for p in pts) for k in range(3)]
    ctr=[(lo[k]+hi[k])*.5 for k in range(3)]; length0=hi[0]-lo[0]; scale=7.8/max(length0,1e-6)
    def mp(p): return ((p[1]-ctr[1])*scale,(p[2]-ctr[2])*scale,(p[0]-ctr[0])*scale)

    # Convert to Android axes first, then build adjacency. V16 used one face normal per
    # triangle (100% flat shading). V18 averages neighboring faces only when the angle
    # is inside a 52-degree crease, preserving stealth-panel edges while smoothing the
    # curved fuselage, canopy, inlets and nozzle shoulders.
    groups=defaultdict(list)
    cooked=[]
    for fi,f in enumerate(faces):
        pp=[mp(v[0]) for v in f['verts']]
        raw=cross(sub(pp[1],pp[0]),sub(pp[2],pp[0])); area=max(length(raw),1e-12); fn=norm(raw)
        rec={'pp':pp,'fn':fn,'area':area,'verts':f['verts'],'col':f['col']}; cooked.append(rec)
        for ci,p in enumerate(pp):
            key=(round(p[0],5),round(p[1],5),round(p[2],5)); groups[key].append(fi)

    crease_cos=math.cos(math.radians(52.0))
    out=[]
    for fi,f in enumerate(cooked):
        r,g,b=f['col']
        for ci,p in enumerate(f['pp']):
            key=(round(p[0],5),round(p[1],5),round(p[2],5))
            sx=sy=sz=0.; sw=0.
            for nfi in groups[key]:
                nf=cooked[nfi]['fn']
                if dot(f['fn'],nf)>=crease_cos:
                    w=cooked[nfi]['area']; sx+=nf[0]*w; sy+=nf[1]*w; sz+=nf[2]*w; sw+=w
            sn=norm((sx,sy,sz)) if sw>0 else f['fn']
            _,au,av,tm=f['verts'][ci]
            out.append((*p,*sn,r,g,b,tm,au,av))
    return out

def main():
    if len(sys.argv)!=3: raise SystemExit('usage: ac3d_to_mesh.py input.ac output.mesh')
    mats,root=parse(sys.argv[1]); verts=build(mats,root)
    with open(sys.argv[2],'wb') as f:
        f.write(b'F22MSH18'); f.write(struct.pack('<I',len(verts)))
        for v in verts: f.write(struct.pack('<12f',*v))
    print('V18 smooth converted vertices:',len(verts),'triangles:',len(verts)//3,'bytes:',12+len(verts)*48)
if __name__=='__main__': main()
