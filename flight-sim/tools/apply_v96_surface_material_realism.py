from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REAL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/RealisticFighterMesh.java'
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'
PROFILE=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/sim/SurfaceMaterialProfile.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim/SurfaceMaterialProfileTest.java'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v96 material patch anchor missing: {label}')
    return text.replace(old,new,1)

# --- Geometry: keep the modern fighter skin clean; add only functional seams. ---
r=REAL.read_text()
r=rep(r,
'            HEAT_SHIELD=28f, DETAIL=29f;\n',
'            HEAT_SHIELD=28f, DETAIL=29f, RADOME=50f, RAM_TAPE=51f, ACCESS_PANEL=52f, FASTENER=53f;\n',
'new material part ids')

# Tag the actual forward fuselage loft as radome rather than painting an arbitrary stripe.
r=rep(r,
'        for(int s=0;s<n-1;s++){\n            float dz=z[s+1]-z[s];\n',
'        for(int s=0;s<n-1;s++){\n            float oldPart=part;\n            if(oldPart==SKIN && z[s+1]<=-4.84f)part=RADOME;else part=oldPart;\n            float dz=z[s+1]-z[s];\n',
'radome loft material assignment')
r=rep(r,
'                quadSmooth(p00,p10,p11,p01);\n            }\n        }\n    }\n\n    private V section(',
'                quadSmooth(p00,p10,p11,p01);\n            }\n            part=oldPart;\n        }\n    }\n\n    private V section(',
'restore loft material')

# Add sparse service-panel seams, grouped fasteners and RAM tape at plausible joins.
r=rep(r,
'        b.part=DETAIL; b.airframeDetails(); b.gearDetails();\n',
'        b.part=DETAIL; b.airframeDetails(); b.gearDetails(); b.surfaceDetailUpgrade();\n',
'call surface detail upgrade')

surface_method=r'''    private void surfaceDetailUpgrade(){
        // Radar/radome junction and low-observable tape around major aerodynamic joins.
        part=RAM_TAPE;
        ribbon(new float[][]{{-.56f,.445f,-4.90f},{0f,.545f,-4.86f},{.56f,.445f,-4.90f}},.016f,.010f);
        for(float s:new float[]{-1f,1f}){
            ribbon(new float[][]{{.84f*s,.455f,-2.72f},{1.62f*s,.405f,-2.28f},{2.68f*s,.315f,-1.66f},{3.78f*s,.235f,-1.02f},{4.72f*s,.175f,-.46f}},.018f,.010f);
            ribbon(new float[][]{{1.28f*s,.715f,.72f},{2.30f*s,.715f,.71f},{3.54f*s,.675f,.67f},{4.48f*s,.505f,.50f}},.015f,.009f);
            ribbon(new float[][]{{.70f*s,.545f,1.70f},{1.38f*s,.545f,1.86f},{2.58f*s,.545f,2.17f}},.014f,.009f);
        }

        // Large removable/service panels only; no uniform rivet grid.
        part=ACCESS_PANEL;
        accessPanel(0f,1.020f,-.58f,.74f,.64f);
        accessPanel(-.93f,.650f,1.38f,.58f,.70f);
        accessPanel(.93f,.650f,1.38f,.58f,.70f);
        accessPanel(-1.34f,.470f,-1.38f,.46f,.52f);
        accessPanel(1.34f,.470f,-1.38f,.46f,.52f);

        // Fasteners are deliberately grouped around maintainable panels, not scattered everywhere.
        part=FASTENER;
        fastenerGroup(0f,1.032f,-.58f,.64f,.54f,4,3);
        fastenerGroup(-.93f,.662f,1.38f,.48f,.60f,3,4);
        fastenerGroup(.93f,.662f,1.38f,.48f,.60f,3,4);
        part=DETAIL;
    }

    private void accessPanel(float cx,float y,float cz,float wx,float wz){
        float x0=cx-wx*.5f,x1=cx+wx*.5f,z0=cz-wz*.5f,z1=cz+wz*.5f;
        ribbon(new float[][]{{x0,y,z0},{x1,y,z0}},.010f,.007f);
        ribbon(new float[][]{{x1,y,z0},{x1,y,z1}},.010f,.007f);
        ribbon(new float[][]{{x1,y,z1},{x0,y,z1}},.010f,.007f);
        ribbon(new float[][]{{x0,y,z1},{x0,y,z0}},.010f,.007f);
    }

    private void fastenerGroup(float cx,float y,float cz,float wx,float wz,int nx,int nz){
        for(int ix=0;ix<nx;ix++)for(int iz=0;iz<nz;iz++){
            boolean edge=ix==0||ix==nx-1||iz==0||iz==nz-1;if(!edge)continue;
            float x=cx-wx*.5f+wx*ix/Math.max(1,nx-1),z=cz-wz*.5f+wz*iz/Math.max(1,nz-1);
            ellipsoid(x,y,z,.013f,.006f,.013f,7,4);
        }
    }

'''
r=rep(r,'    private void gearDetails(){\n',surface_method+'    private void gearDetails(){\n','surface-detail methods')
REAL.write_text(r)

# --- Shader/materials: replace toy-like global panel grid with material-specific response. ---
j=JET.read_text()
old_skin='''                "if(vP<.5){float upper=smoothstep(-.28,.60,N.y);float fine=(hash(floor(vPos*95.))-0.5)*.010;float panelA=abs(fract(vPos.z*.37+vPos.x*.055)-.5),panelB=abs(fract(vPos.x*.31-vPos.z*.041)-.5);float seam=1.-smoothstep(.010,.027,min(panelA,panelB)),ram=1.-smoothstep(.010,.032,abs(fract((vPos.x+vPos.z)*.115)-.5));base=mix(vec3(.225,.238,.250),vec3(.365,.382,.400),upper)+vec3(fine);base*=1.-.050*seam-.026*ram;rough=.40+.11*seam+.035*(1.-upper);metal=.16;ao=.96-.055*seam;N=normalize(N+vec3(fine*.55,fine*.30,0.));}"+\n'''
new_skin='''                "if(vP<.5){float upper=smoothstep(-.30,.62,N.y);float micro=(hash(floor(vPos*73.))-0.5),macro=(hash(floor(vPos*8.))-0.5);base=mix(vec3(.205,.216,.226),vec3(.292,.306,.318),upper)+vec3(micro*.006+macro*.010);rough=.49+.075*(1.-upper)+micro*.035;metal=.055;ao=.965;N=normalize(N+vec3(micro*.018,macro*.014,0.));}"+\n'''
j=rep(j,old_skin,new_skin,'clean semi-matte RAM skin shader')

j=rep(j,
'                "else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=.08+.92*pow(1.-ndv,4.2);vec3 glass=mix(vec3(.010,.027,.038),envc(R),.34+.52*fr);float sun=pow(ndh,120.);glass+=vec3(.80,.88,.90)*sun*.72;glass+=vec3(.11,.075,.035)*pow(1.-ndv,2.2)*.20;gl_FragColor=vec4(glass,.20+.34*fr);return;}"+\n',
'                "else if(vP>.5&&vP<1.5){vec3 R=reflect(-V,N);float fr=.07+.93*pow(1.-ndv,4.5);vec3 glass=mix(vec3(.008,.023,.034),envc(R),.38+.56*fr);float sun=pow(ndh,150.);glass+=vec3(.88,.94,.96)*sun*.82;glass+=vec3(.10,.062,.025)*pow(1.-ndv,2.1)*.18;gl_FragColor=vec4(glass,.18+.36*fr);return;}"+\n',
'canopy high-gloss material')

j=rep(j,
'                "else if(vP>1.5&&vP<2.5){base=vec3(.145,.153,.160);rough=.24;metal=.90;}else if(vP>2.5&&vP<3.5){base=vec3(.075,.084,.091);rough=.64;metal=.30;ao=.82;}else if(vP>10.5&&vP<11.5){base=vec3(.028,.032,.036);rough=.42;metal=.52;}else if(vP>12.5&&vP<13.5){base=vec3(.50,.52,.54);rough=.22;metal=.92;}else if(vP>13.5&&vP<14.5){base=vec3(.010,.011,.012);rough=.92;metal=.01;}else if(vP>14.5&&vP<15.5){base=vec3(.205,.215,.225);rough=.34;metal=.72;}else if(vP>15.5&&vP<18.5){base=vec3(.022,.026,.030);rough=.70;metal=.16;}else if(vP>18.5&&vP<20.5){base=vec3(.042,.048,.053);rough=.76;metal=.16;ao=.80;}else if(vP>20.5&&vP<21.5){base=vec3(.105,.110,.115);rough=.27;metal=.92;}else if(vP>45.5&&vP<46.5){float rpm=smoothstep(.05,1.,uThrottle);base=mix(vec3(.075,.082,.088),vec3(.20,.215,.225),rpm*.30);rough=.20+.12*rpm;metal=.95;ao=.84;}"+\n',
'                "else if(vP>1.5&&vP<2.5){base=vec3(.135,.143,.150);rough=.18;metal=.97;}else if(vP>2.5&&vP<3.5){base=vec3(.072,.080,.087);rough=.68;metal=.24;ao=.82;}else if(vP>10.5&&vP<11.5){base=vec3(.026,.030,.034);rough=.46;metal=.42;}else if(vP>12.5&&vP<13.5){base=vec3(.50,.52,.54);rough=.20;metal=.94;}else if(vP>13.5&&vP<14.5){base=vec3(.008,.009,.010);rough=.98;metal=.00;}else if(vP>14.5&&vP<15.5){base=vec3(.200,.210,.220);rough=.38;metal=.68;}else if(vP>15.5&&vP<18.5){base=vec3(.021,.025,.029);rough=.74;metal=.12;}else if(vP>18.5&&vP<20.5){base=vec3(.040,.046,.051);rough=.79;metal=.12;ao=.80;}else if(vP>20.5&&vP<21.5){base=vec3(.095,.102,.110);rough=.20;metal=.96;}else if(vP>45.5&&vP<46.5){float rpm=smoothstep(.05,1.,uThrottle);base=mix(vec3(.070,.077,.083),vec3(.20,.215,.225),rpm*.30);rough=.18+.12*rpm;metal=.97;ao=.84;}"+\n',
'metal/nozzle/tire material differentiation')

j=rep(j,
'                "else if(vP>21.5&&vP<22.5){float ab=smoothstep(.73,.88,uThrottle);',
'                "else if(vP>49.5&&vP<50.5){float grain=(hash(floor(vPos*34.))-0.5)*.018;base=vec3(.205,.216,.224)+vec3(grain);rough=.70;metal=.005;ao=.97;}else if(vP>50.5&&vP<51.5){base=vec3(.072,.079,.086);rough=.64;metal=.018;ao=.91;}else if(vP>51.5&&vP<52.5){base=vec3(.105,.114,.122);rough=.58;metal=.08;ao=.88;}else if(vP>52.5&&vP<53.5){base=vec3(.175,.185,.195);rough=.30;metal=.84;ao=.92;}else if(vP>21.5&&vP<22.5){float ab=smoothstep(.73,.88,uThrottle);',
'radome RAM tape panel fastener materials')
JET.write_text(j)

# Small pure-Java profile used by CI to lock the intended material hierarchy.
PROFILE.write_text('''package com.mg.fixturecockpitsim.sim;\n\npublic final class SurfaceMaterialProfile {\n    public enum Kind { RAM_SKIN, RADOME, NOZZLE, CANOPY, TYRE, RAM_TAPE, FASTENER }\n    public static double roughness(Kind k){\n        switch(k){case RAM_SKIN:return .53;case RADOME:return .70;case NOZZLE:return .18;case CANOPY:return .07;case TYRE:return .98;case RAM_TAPE:return .64;case FASTENER:return .30;default:return .5;}\n    }\n    public static double metallic(Kind k){\n        switch(k){case RAM_SKIN:return .055;case RADOME:return .005;case NOZZLE:return .97;case CANOPY:return .02;case TYRE:return 0;case RAM_TAPE:return .018;case FASTENER:return .84;default:return .1;}\n    }\n    private SurfaceMaterialProfile(){}\n}\n''')
TEST.parent.mkdir(parents=True,exist_ok=True)
TEST.write_text('''package com.mg.fixturecockpitsim.sim;\n\nimport org.junit.Test;\nimport static org.junit.Assert.*;\n\npublic class SurfaceMaterialProfileTest {\n @Test public void modernFighterMaterialHierarchyIsDistinct(){\n   assertTrue(SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.TYRE) > .9);\n   assertTrue(SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.RADOME) > SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.RAM_SKIN));\n   assertTrue(SurfaceMaterialProfile.metallic(SurfaceMaterialProfile.Kind.NOZZLE) > .9);\n   assertTrue(SurfaceMaterialProfile.metallic(SurfaceMaterialProfile.Kind.RAM_SKIN) < .1);\n   assertTrue(SurfaceMaterialProfile.roughness(SurfaceMaterialProfile.Kind.CANOPY) < .12);\n }\n}\n''')

g=GRADLE.read_text()
g=rep(g,'        versionCode 95\n','        versionCode 96\n','version code')
g=rep(g,"        versionName '26.13-avm24.0-airfield-environment-realism'\n","        versionName '26.14-avm25.0-surface-material-realism'\n",'version name')
GRADLE.write_text(g)

print('v96 surface/material realism applied: clean RAM skin, true radome region, sparse maintenance panels, grouped fasteners, RAM tape and distinct fighter materials')
