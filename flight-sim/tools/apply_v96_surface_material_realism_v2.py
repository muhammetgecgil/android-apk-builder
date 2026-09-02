from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
REAL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/RealisticFighterMesh.java'
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'
PROFILE=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/sim/SurfaceMaterialProfile.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim/SurfaceMaterialProfileTest.java'


def require(cond,label):
    if not cond: raise SystemExit('v96 material patch anchor missing: '+label)

# -------------------- Fighter geometry / functional surface detail --------------------
r=REAL.read_text()
if 'RADOME=50f' not in r:
    require('DETAIL=29f' in r,'DETAIL material id')
    r=r.replace('DETAIL=29f','DETAIL=29f, RADOME=50f, RAM_TAPE=51f, ACCESS_PANEL=52f, FASTENER=53f',1)

# Assign only the true forward loft to the radome material. Work inside smoothLoft so
# other for-loops created by earlier geometry upgrades are untouched.
if 'z[s+1]<=-4.84f' not in r:
    a=r.find('private void smoothLoft('); b=r.find('private V section(',a)
    require(a>=0 and b>a,'smoothLoft block')
    seg=r[a:b]
    old='for(int s=0;s<n-1;s++){\n            float dz=z[s+1]-z[s];'
    new='for(int s=0;s<n-1;s++){\n            float oldPart=part;\n            if(oldPart==SKIN && z[s+1]<=-4.84f)part=RADOME;else part=oldPart;\n            float dz=z[s+1]-z[s];'
    require(old in seg,'smoothLoft loop')
    seg=seg.replace(old,new,1)
    tail='                quadSmooth(p00,p10,p11,p01);\n            }\n        }\n    }\n\n    '
    repl='                quadSmooth(p00,p10,p11,p01);\n            }\n            part=oldPart;\n        }\n    }\n\n    '
    require(tail in seg,'smoothLoft restore')
    seg=seg.replace(tail,repl,1)
    r=r[:a]+seg+r[b:]

# Call the new detail pass once near final mesh assembly; do not depend on the exact
# order of v88/v89 detail calls.
if 'b.surfaceDetailUpgrade();' not in r:
    marker='        float[] data=new float[b.out.size()];'
    require(marker in r,'mesh finalization')
    r=r.replace(marker,'        b.part=DETAIL; b.surfaceDetailUpgrade();\n\n'+marker,1)

if 'private void surfaceDetailUpgrade()' not in r:
    marker='    private void gearDetails(){'
    require(marker in r,'gearDetails insertion point')
    methods=r'''    private void surfaceDetailUpgrade(){
        // Low-observable edge treatment only on plausible structural/aerodynamic joins.
        part=RAM_TAPE;
        ribbon(new float[][]{{-.56f,.445f,-4.90f},{0f,.545f,-4.86f},{.56f,.445f,-4.90f}},.016f,.010f);
        for(float s:new float[]{-1f,1f}){
            ribbon(new float[][]{{.84f*s,.455f,-2.72f},{1.62f*s,.405f,-2.28f},{2.68f*s,.315f,-1.66f},{3.78f*s,.235f,-1.02f},{4.72f*s,.175f,-.46f}},.018f,.010f);
            ribbon(new float[][]{{1.28f*s,.715f,.72f},{2.30f*s,.715f,.71f},{3.54f*s,.675f,.67f},{4.48f*s,.505f,.50f}},.015f,.009f);
            ribbon(new float[][]{{.70f*s,.545f,1.70f},{1.38f*s,.545f,1.86f},{2.58f*s,.545f,2.17f}},.014f,.009f);
        }

        // Sparse removable/service panels; intentionally no all-over rivet lattice.
        part=ACCESS_PANEL;
        accessPanel(0f,1.020f,-.58f,.74f,.64f);
        accessPanel(-.93f,.650f,1.38f,.58f,.70f); accessPanel(.93f,.650f,1.38f,.58f,.70f);
        accessPanel(-1.34f,.470f,-1.38f,.46f,.52f); accessPanel(1.34f,.470f,-1.38f,.46f,.52f);

        // Flush fasteners only around maintainable panels.
        part=FASTENER;
        fastenerGroup(0f,1.032f,-.58f,.64f,.54f,4,3);
        fastenerGroup(-.93f,.662f,1.38f,.48f,.60f,3,4); fastenerGroup(.93f,.662f,1.38f,.48f,.60f,3,4);
        part=DETAIL;
    }

    private void accessPanel(float cx,float y,float cz,float wx,float wz){
        float x0=cx-wx*.5f,x1=cx+wx*.5f,z0=cz-wz*.5f,z1=cz+wz*.5f;
        ribbon(new float[][]{{x0,y,z0},{x1,y,z0}},.010f,.007f); ribbon(new float[][]{{x1,y,z0},{x1,y,z1}},.010f,.007f);
        ribbon(new float[][]{{x1,y,z1},{x0,y,z1}},.010f,.007f); ribbon(new float[][]{{x0,y,z1},{x0,y,z0}},.010f,.007f);
    }

    private void fastenerGroup(float cx,float y,float cz,float wx,float wz,int nx,int nz){
        for(int ix=0;ix<nx;ix++)for(int iz=0;iz<nz;iz++){
            if(!(ix==0||ix==nx-1||iz==0||iz==nz-1))continue;
            float x=cx-wx*.5f+wx*ix/Math.max(1,nx-1),z=cz-wz*.5f+wz*iz/Math.max(1,nz-1);
            ellipsoid(x,y,z,.013f,.006f,.013f,7,4);
        }
    }

'''
    r=r.replace(marker,methods+marker,1)
REAL.write_text(r)

# -------------------- Material shader --------------------
j=JET.read_text()
# Remove the procedural global seam/rivet look. Replace only the vP<.5 branch.
clean_skin='                "if(vP<.5){float upper=smoothstep(-.30,.62,N.y);float micro=(hash(floor(vPos*73.))-0.5),macro=(hash(floor(vPos*8.))-0.5);base=mix(vec3(.205,.216,.226),vec3(.292,.306,.318),upper)+vec3(micro*.006+macro*.010);rough=.49+.075*(1.-upper)+micro*.035;metal=.055;ao=.965;N=normalize(N+vec3(micro*.018,macro*.014,0.));}"+\n'
if clean_skin not in j:
    s=j.find('                "if(vP<.5){')
    e=j.find('                "else if(vP>.5&&vP<1.5){',s)
    require(s>=0 and e>s,'skin shader branch')
    j=j[:s]+clean_skin+j[e:]

# Glossy canopy: sharper environment/sun response but still transparent.
old_can='"else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=.08+.92*pow(1.-ndv,4.2);vec3 glass=mix(vec3(.010,.027,.038),envc(R),.34+.52*fr);float sun=pow(ndh,120.);glass+=vec3(.80,.88,.90)*sun*.72;glass+=vec3(.11,.075,.035)*pow(1.-ndv,2.2)*.20;gl_FragColor=vec4(glass,.20+.34*fr);return;}'
new_can='"else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=.07+.93*pow(1.-ndv,4.5);vec3 glass=mix(vec3(.008,.023,.034),envc(R),.38+.56*fr);float sun=pow(ndh,150.);glass+=vec3(.88,.94,.96)*sun*.82;glass+=vec3(.10,.062,.025)*pow(1.-ndv,2.1)*.18;gl_FragColor=vec4(glass,.18+.36*fr);return;}'
if old_can in j: j=j.replace(old_can,new_can,1)

# Nozzle and tyre material hierarchy, targeted so v93 flame/nozzle animation is retained.
j=re.sub(r'else if\(vP>1\.5&&vP<2\.5\)\{base=vec3\([^}]+?\);rough=\.[0-9]+;metal=\.[0-9]+;\}',
         'else if(vP>1.5&&vP<2.5){base=vec3(.135,.143,.150);rough=.18;metal=.97;}',j,count=1)
j=re.sub(r'else if\(vP>13\.5&&vP<14\.5\)\{base=vec3\([^}]+?\);rough=\.[0-9]+;metal=\.[0-9]+;\}',
         'else if(vP>13.5&&vP<14.5){base=vec3(.008,.009,.010);rough=.98;metal=.00;}',j,count=1)

# New semantic materials before the existing flame-core branch.
if 'vP>49.5&&vP<50.5' not in j:
    marker='"else if(vP>21.5&&vP<22.5){float ab=smoothstep(.73,.88,uThrottle);'
    require(marker in j,'new material shader insertion')
    newcases='"else if(vP>49.5&&vP<50.5){float grain=(hash(floor(vPos*34.))-0.5)*.018;base=vec3(.205,.216,.224)+vec3(grain);rough=.70;metal=.005;ao=.97;}else if(vP>50.5&&vP<51.5){base=vec3(.072,.079,.086);rough=.64;metal=.018;ao=.91;}else if(vP>51.5&&vP<52.5){base=vec3(.105,.114,.122);rough=.58;metal=.08;ao=.88;}else if(vP>52.5&&vP<53.5){base=vec3(.175,.185,.195);rough=.30;metal=.84;ao=.92;}else if(vP>21.5&&vP<22.5){float ab=smoothstep(.73,.88,uThrottle);'
    j=j.replace(marker,newcases,1)
JET.write_text(j)

# CI-lock the material hierarchy independently of the OpenGL renderer.
PROFILE.write_text('''package com.mg.fixturecockpitsim.sim;\n\npublic final class SurfaceMaterialProfile {\n public enum Kind { RAM_SKIN,RADOME,NOZZLE,CANOPY,TYRE,RAM_TAPE,ACCESS_PANEL,FASTENER }\n public static double roughness(Kind k){switch(k){case RAM_SKIN:return .53;case RADOME:return .70;case NOZZLE:return .18;case CANOPY:return .07;case TYRE:return .98;case RAM_TAPE:return .64;case ACCESS_PANEL:return .58;case FASTENER:return .30;default:return .5;}}\n public static double metallic(Kind k){switch(k){case RAM_SKIN:return .055;case RADOME:return .005;case NOZZLE:return .97;case CANOPY:return .02;case TYRE:return 0;case RAM_TAPE:return .018;case ACCESS_PANEL:return .08;case FASTENER:return .84;default:return .1;}}\n private SurfaceMaterialProfile(){}\n}\n''')
TEST.parent.mkdir(parents=True,exist_ok=True)
TEST.write_text('''package com.mg.fixturecockpitsim.sim;\nimport org.junit.Test;\nimport static org.junit.Assert.*;\npublic class SurfaceMaterialProfileTest {\n @Test public void modernFighterMaterialHierarchyIsDistinct(){\n  assertTrue(SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.TYRE)>.9);\n  assertTrue(SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.RADOME)>SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.RAM_SKIN));\n  assertTrue(SurfaceMaterialProfile.metallic(SurfaceMaterialProfile.Kind.NOZZLE)>.9);\n  assertTrue(SurfaceMaterialProfile.metallic(SurfaceMaterialProfile.Kind.RAM_SKIN)<.1);\n  assertTrue(SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.CANOPY)<.12);\n }\n}\n''')

g=GRADLE.read_text()
if 'versionCode 96' not in g:
    require('versionCode 95' in g,'version code')
    g=g.replace('versionCode 95','versionCode 96',1)
if "26.14-avm25.0-surface-material-realism" not in g:
    require("26.13-avm24.0-airfield-environment-realism" in g,'version name')
    g=g.replace("26.13-avm24.0-airfield-environment-realism","26.14-avm25.0-surface-material-realism",1)
GRADLE.write_text(g)

print('v96 v2 applied: clean semi-matte RAM skin, real radome, sparse access panels, grouped flush fasteners, RAM tape, glossy canopy, metallic nozzle and matte tyres')
