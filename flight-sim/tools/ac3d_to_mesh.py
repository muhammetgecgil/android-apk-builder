#!/usr/bin/env python3
import math, struct, sys

# Converts the downloaded FlightGear F-22 AC3D geometry into a compact
# interleaved triangle stream used by the Android renderer.
# Output vertex layout: position(3), normal(3), color(4), all float32 LE.

class Obj:
    def __init__(self):
        self.name=''; self.loc=(0.0,0.0,0.0); self.rot=(1,0,0,0,1,0,0,0,1)
        self.verts=[]; self.surfs=[]; self.children=[]

def mmul(a,b):
    r=[0.0]*9
    for i in range(3):
        for j in range(3):
            r[i*3+j]=sum(a[i*3+k]*b[k*3+j] for k in range(3))
    return tuple(r)

def mv(m,v):
    return (m[0]*v[0]+m[1]*v[1]+m[2]*v[2],
            m[3]*v[0]+m[4]*v[1]+m[5]*v[2],
            m[6]*v[0]+m[7]*v[1]+m[8]*v[2])

def add(a,b): return (a[0]+b[0],a[1]+b[1],a[2]+b[2])
def sub(a,b): return (a[0]-b[0],a[1]-b[1],a[2]-b[2])
def cross(a,b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def norm(a):
    l=math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]) or 1.0
    return (a[0]/l,a[1]/l,a[2]/l)

def qstr(line):
    p=line.find('"'); q=line.rfind('"')
    return line[p+1:q] if p>=0 and q>p else ''

def parse(path):
    lines=open(path,'r',encoding='utf-8',errors='ignore').read().splitlines()
    mats=[]; i=1
    while i<len(lines) and lines[i].startswith('MATERIAL '):
        s=lines[i].split();
        try:
            k=s.index('rgb'); mats.append(tuple(float(x) for x in s[k+1:k+4]))
        except: mats.append((0.58,0.60,0.62))
        i+=1
    def obj(i):
        assert lines[i].startswith('OBJECT '); o=Obj(); i+=1
        while i<len(lines):
            ln=lines[i].strip()
            if ln.startswith('name '): o.name=qstr(ln); i+=1
            elif ln.startswith('loc '): o.loc=tuple(map(float,ln.split()[1:4])); i+=1
            elif ln.startswith('rot '): o.rot=tuple(map(float,ln.split()[1:10])); i+=1
            elif ln.startswith('data '):
                n=int(ln.split()[1]); i+=1
                # AC3D data is byte-counted; common files store one following text line.
                if i<len(lines): i+=1
            elif ln.startswith('texture ') or ln.startswith('texrep ') or ln.startswith('texoff ') or ln.startswith('crease ') or ln.startswith('url '): i+=1
            elif ln.startswith('numvert '):
                n=int(ln.split()[1]); i+=1
                for _ in range(n):
                    a=lines[i].split(); o.verts.append((float(a[0]),float(a[1]),float(a[2]))); i+=1
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
                            a=lines[i].split(); refs.append(int(a[0])); i+=1
                    o.surfs.append((mat,refs,flags))
            elif ln.startswith('kids '):
                n=int(ln.split()[1]); i+=1
                for _ in range(n):
                    ch,i=obj(i); o.children.append(ch)
                return o,i
            else: i+=1
        return o,i
    root,i=obj(i)
    return mats,root

def build(mats,root):
    tris=[]
    skip=('landinggear','landing_gear','nosegear','maingear','wheel','tire','towbar')
    I=(1,0,0,0,1,0,0,0,1)
    def walk(o,pm,pt):
        wm=mmul(pm,o.rot); wt=add(pt,mv(pm,o.loc))
        hidden=any(s in o.name.lower().replace(' ','') for s in skip)
        wv=[add(wt,mv(wm,v)) for v in o.verts]
        if not hidden:
            for mat,refs,flags in o.surfs:
                if len(refs)<3: continue
                col=mats[mat] if 0<=mat<len(mats) else (0.58,0.60,0.62)
                # Keep very bright fuselage materials in a realistic Raptor-gray range.
                mx=max(col); mn=min(col)
                if mx>0.93 and mx-mn<0.08: col=(0.57,0.59,0.61)
                for j in range(1,len(refs)-1):
                    try: a,b,c=wv[refs[0]],wv[refs[j]],wv[refs[j+1]]
                    except: continue
                    n=norm(cross(sub(b,a),sub(c,a)))
                    if flags & 0x10: # two-sided / legacy flags can have inconsistent winding
                        pass
                    tris.append((a,b,c,n,col))
        for ch in o.children: walk(ch,wm,wt)
    walk(root,I,(0,0,0))
    # Bounds and axis conversion. AC3D: longitudinal X, lateral Y, vertical Z.
    pts=[p for t in tris for p in t[:3]]
    lo=[min(p[k] for p in pts) for k in range(3)]; hi=[max(p[k] for p in pts) for k in range(3)]
    ctr=[(lo[k]+hi[k])*0.5 for k in range(3)]
    length=hi[0]-lo[0]
    scale=7.8/max(length,1e-6)
    out=[]
    for a,b,c,n,col in tris:
        # Recompute normal after axis mapping.
        def mp(p): return ((p[1]-ctr[1])*scale,(p[2]-ctr[2])*scale,(p[0]-ctr[0])*scale)
        aa,bb,cc=mp(a),mp(b),mp(c); nn=norm(cross(sub(bb,aa),sub(cc,aa)))
        # Subtle gray harmonization while preserving canopy/nozzle/material differences.
        r,g,bv=col
        for p in (aa,bb,cc): out.append((*p,*nn,r,g,bv,1.0))
    return out

def main():
    if len(sys.argv)!=3: raise SystemExit('usage: ac3d_to_mesh.py input.ac output.mesh')
    mats,root=parse(sys.argv[1]); verts=build(mats,root)
    with open(sys.argv[2],'wb') as f:
        f.write(b'F22MSH14'); f.write(struct.pack('<I',len(verts)))
        for v in verts: f.write(struct.pack('<10f',*v))
    print('converted vertices:',len(verts),'bytes:',8+4+len(verts)*40)
if __name__=='__main__': main()
