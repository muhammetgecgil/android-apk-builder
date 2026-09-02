from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REAL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/RealisticFighterMesh.java'
ADV=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/AdvancedAirframeOverlay.java'
ENG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/EngineDynamicsOverlay.java'
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v93 nozzle patch anchor missing: {label}')
    return text.replace(old,new,1)


def method_replace(text,start_sig,next_sig,new_method,label):
    a=text.find(start_sig)
    if a<0: raise SystemExit(f'v93 nozzle method missing: {label}')
    b=text.find(next_sig,a)
    if b<0: raise SystemExit(f'v93 nozzle next method missing: {label}')
    return text[:a]+new_method+'\n    '+text[b:]

# Main nozzle geometry: 18 mechanically readable overlapping petals plus a
# separate dark inner-petal layer and a visible variable throat.
r=REAL.read_text()
new_petals=r'''private void nozzlePetals(float x){
        final int petals=18;
        final float z0=3.30f,zm=3.60f,z1=3.90f,r0=.455f,rm=.382f,r1=.305f,ys=.61f;
        part=NOZZLE_PETAL;
        for(int i=0;i<petals;i++){
            double c=2*Math.PI*(i+.5)/petals;
            double half=Math.PI/petals;
            double a0=c-half-.018,a1=c+half+.018;
            float crown=.010f+.006f*(float)Math.cos(c*2.0);
            float[] A={x+(r0+crown)*(float)Math.cos(a0),-.10f+(r0+crown)*ys*(float)Math.sin(a0),z0};
            float[] B={x+(rm+crown*.55f)*(float)Math.cos(a0),-.10f+(rm+crown*.55f)*ys*(float)Math.sin(a0),zm};
            float[] C={x+(rm+crown*.55f)*(float)Math.cos(a1),-.10f+(rm+crown*.55f)*ys*(float)Math.sin(a1),zm};
            float[] D={x+(r0+crown)*(float)Math.cos(a1),-.10f+(r0+crown)*ys*(float)Math.sin(a1),z0};
            quad(A,B,C,D);
            float[] E={x+r1*(float)Math.cos(a0),-.10f+r1*ys*(float)Math.sin(a0),z1};
            float[] F={x+r1*(float)Math.cos(a1),-.10f+r1*ys*(float)Math.sin(a1),z1};
            quad(B,E,F,C);
            double edge=c+half;
            float ex0=x+(r0+.012f)*(float)Math.cos(edge),ey0=-.10f+(r0+.012f)*ys*(float)Math.sin(edge);
            float ex1=x+(r1+.008f)*(float)Math.cos(edge),ey1=-.10f+(r1+.008f)*ys*(float)Math.sin(edge);
            cylinderBetween(ex0,ey0,z0,ex1,ey1,z1,.0075f,6);
        }
        part=NOZZLE_INNER;
        final float iz0=3.47f,iz1=3.94f,ir0=.335f,ir1=.235f;
        for(int i=0;i<petals;i++){
            double c=2*Math.PI*(i+1.0)/petals,half=Math.PI/petals*.83;
            double a0=c-half,a1=c+half;
            quad(new float[]{x+ir0*(float)Math.cos(a0),-.10f+ir0*.60f*(float)Math.sin(a0),iz0},
                 new float[]{x+ir1*(float)Math.cos(a0),-.10f+ir1*.60f*(float)Math.sin(a0),iz1},
                 new float[]{x+ir1*(float)Math.cos(a1),-.10f+ir1*.60f*(float)Math.sin(a1),iz1},
                 new float[]{x+ir0*(float)Math.cos(a1),-.10f+ir0*.60f*(float)Math.sin(a1),iz0});
        }
        part=NOZZLE_PETAL;
    }'''
r=method_replace(r,'private void nozzlePetals(float x){','private void nozzleInner(float x)',new_petals,'outer/inner petal iris')

new_inner=r'''private void nozzleInner(float x){
        part=NOZZLE_INNER;
        tubeSurface(x,-.10f,3.54f,4.06f,.292f,.214f,.60f,40);
        ellipticRing(x,-.10f,3.63f,.285f,.60f,.014f,36);
        ellipticRing(x,-.10f,3.82f,.252f,.60f,.012f,36);
        ellipticRing(x,-.10f,4.00f,.218f,.60f,.011f,36);
        ellipsoid(x,-.10f,4.03f,.044f,.030f,.060f,14,8);
        for(int i=0;i<10;i++){
            double a=2*Math.PI*i/10.0;float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
            cylinderBetween(x+.050f*ca,-.10f+.050f*.60f*sa,4.02f,x+.205f*ca,-.10f+.205f*.60f*sa,4.02f,.0065f,6);
        }
    }'''
r=method_replace(r,'private void nozzleInner(float x)','private void afterburner(float x)',new_inner,'deep exhaust liner')

new_ab=r'''private void afterburner(float x){
        part=AFTERBURNER;
        tubeSurface(x,-.10f,3.98f,4.25f,.175f,.120f,.55f,28);
        tubeSurface(x,-.10f,4.38f,4.62f,.115f,.082f,.55f,26);
        tubeSurface(x,-.10f,4.78f,5.02f,.078f,.038f,.55f,24);
    }'''
r=method_replace(r,'private void afterburner(float x)','private void flameCore(float x)',new_ab,'segmented afterburner shell')

new_core=r'''private void flameCore(float x){
        part=FLAME_CORE;
        tubeSurface(x,-.10f,4.00f,4.82f,.072f,.010f,.52f,20);
    }'''
r=method_replace(r,'private void flameCore(float x)','private void tubeSurface(',new_core,'slender blue flame core')
REAL.write_text(r)

# Mechanical nozzle hardware: synchronizing ring + 18 primary/secondary links.
a=ADV.read_text()
new_act=r'''private void nozzleActuators(){
        for(float cx:new float[]{-.72f,.72f}){
            part=HEAT_SHIELD;
            ellipticRing(cx,-.10f,3.16f,.545f,.61f,.032f,40);
            ellipticRing(cx,-.10f,3.27f,.505f,.61f,.020f,40);
            ellipticRing(cx,-.10f,3.41f,.455f,.61f,.014f,38);
            final int links=18;
            part=NOZZLE_PETAL;
            for(int i=0;i<links;i++){
                double ang=2*Math.PI*i/links;float ca=(float)Math.cos(ang),sa=(float)Math.sin(ang);
                cylinderBetween(cx+.495f*ca,-.10f+.495f*.61f*sa,3.27f,
                        cx+.365f*ca,-.10f+.365f*.61f*sa,3.63f,.011f,7);
                ellipsoid(cx+.472f*ca,-.10f+.472f*.61f*sa,3.31f,.020f,.016f,.026f,8,5);
            }
            for(int i=0;i<links;i++){
                double ang=2*Math.PI*(i+.5)/links;float ca=(float)Math.cos(ang),sa=(float)Math.sin(ang);
                cylinderBetween(cx+.430f*ca,-.10f+.430f*.61f*sa,3.39f,
                        cx+.305f*ca,-.10f+.305f*.61f*sa,3.78f,.0075f,6);
            }
            part=NOZZLE_INNER;
            ellipticRing(cx,-.10f,3.86f,.260f,.60f,.016f,36);
            ellipticRing(cx,-.10f,4.00f,.214f,.60f,.013f,34);
        }
        part=DETAIL;
    }'''
a=method_replace(a,'private void nozzleActuators(){','private void gearBayAndBrakeDetails(){',new_act,'18-link actuator ring')
ADV.write_text(a)

# 3D shock-diamond cells and longer, softer heat-haze shell.
e=ENG.read_text()
new_shock=r'''private void shockDiamonds(float cx){
        float[] centers={4.18f,4.58f,5.02f,5.50f};
        float[] radii={.155f,.132f,.105f,.078f};
        for(int i=0;i<centers.length;i++){
            float c=centers[i],len=.28f+i*.025f,r=radii[i];
            shellSegment(cx,-.10f,c-len,c,.038f,r,.56f,28);
            shellSegment(cx,-.10f,c,c+len,r,.026f,.56f,28);
        }
        annulus(cx,-.10f,4.06f,.010f,.050f,.56f,24);
    }'''
e=method_replace(e,'private void shockDiamonds(float cx){','private void heatHaze(float cx)',new_shock,'3D shock-diamond cells')
new_haze=r'''private void heatHaze(float cx){
        float[] z={3.83f,4.16f,4.58f,5.10f,5.72f,6.44f,7.20f};
        float[] rx={.215f,.230f,.250f,.278f,.310f,.345f,.385f};
        float[] ry={.133f,.144f,.157f,.174f,.194f,.216f,.241f};
        int sides=24;
        for(int s=0;s<z.length-1;s++){
            for(int i=0;i<sides;i++){
                double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
                float[] A={cx+rx[s]*(float)Math.cos(a0),-.10f+ry[s]*(float)Math.sin(a0),z[s]};
                float[] B={cx+rx[s+1]*(float)Math.cos(a0),-.10f+ry[s+1]*(float)Math.sin(a0),z[s+1]};
                float[] C={cx+rx[s+1]*(float)Math.cos(a1),-.10f+ry[s+1]*(float)Math.sin(a1),z[s+1]};
                float[] D={cx+rx[s]*(float)Math.cos(a1),-.10f+ry[s]*(float)Math.sin(a1),z[s]};
                quad(A,B,C,D);
            }
        }
    }'''
e=method_replace(e,'private void heatHaze(float cx){','private void annulus(',new_haze,'longer heat haze')
helper=r'''private void shellSegment(float cx,float cy,float z0,float z1,float r0,float r1,float ys,int sides){
        for(int i=0;i<sides;i++){
            double a0=2*Math.PI*i/sides,a1=2*Math.PI*(i+1)/sides;
            float[] A=ep(cx,cy,z0,r0,a0,ys),B=ep(cx,cy,z1,r1,a0,ys),C=ep(cx,cy,z1,r1,a1,ys),D=ep(cx,cy,z0,r0,a1,ys);
            quad2(A,B,C,D);
        }
    }

    '''
e=rep(e,'    private void annulus(', '    '+helper+'private void annulus(', 'shock shell helper')
ENG.write_text(e)

# Dynamic iris schedule: open at idle, close toward dry/military power, then
# reopen with afterburner. Outer, inner and heat-shield layers follow together.
j=JET.read_text()
old='                "if(aPart>20.5&&aPart<21.5){float op=mix(.92,1.115,smoothstep(.08,1.,uThrottle));float cx=p.x<0.?-.72:.72;float aft=smoothstep(3.28,3.88,p.z);float sc=mix(1.,op,aft);p.x=cx+(p.x-cx)*sc;p.y=-.10+(p.y+.10)*sc;}"+\n'
new='                "if(aPart>1.5&&aPart<2.5){float dry=smoothstep(.08,.72,uThrottle),ab=smoothstep(.78,.94,uThrottle);float op=mix(1.06,.91,dry);op=mix(op,1.10,ab);float cx=p.x<0.?-.72:.72;float aft=smoothstep(3.08,3.56,p.z);float sc=mix(1.,op,aft);p.x=cx+(p.x-cx)*sc;p.y=-.10+(p.y+.10)*sc;}"+\n                "if(aPart>11.5&&aPart<12.5){float dry=smoothstep(.08,.72,uThrottle),ab=smoothstep(.78,.94,uThrottle);float op=mix(1.05,.87,dry);op=mix(op,1.13,ab);float cx=p.x<0.?-.72:.72;float aft=smoothstep(3.48,4.06,p.z);float sc=mix(1.,op,aft);p.x=cx+(p.x-cx)*sc;p.y=-.10+(p.y+.10)*sc;}"+\n                "if(aPart>20.5&&aPart<21.5){float dry=smoothstep(.08,.72,uThrottle),ab=smoothstep(.78,.94,uThrottle);float op=mix(1.08,.86,dry);op=mix(op,1.14,ab);float cx=p.x<0.?-.72:.72;float aft=smoothstep(3.27,3.94,p.z);float sc=mix(1.,op,aft);p.x=cx+(p.x-cx)*sc;p.y=-.10+(p.y+.10)*sc;}"+\n'
j=rep(j,old,new,'idle-dry-AB nozzle iris schedule')
j=rep(j,
'                "if(aPart>27.5&&aPart<28.5){float op=mix(.93,1.08,smoothstep(.18,1.,uThrottle));float cx=p.x<0.?-.72:.72;p.x=cx+(p.x-cx)*op;p.y=-.10+(p.y+.10)*op;}"+\n',
'                "if(aPart>27.5&&aPart<28.5){float dry=smoothstep(.10,.72,uThrottle),ab=smoothstep(.78,.94,uThrottle);float op=mix(1.025,.965,dry);op=mix(op,1.045,ab);float cx=p.x<0.?-.72:.72;p.x=cx+(p.x-cx)*op;p.y=-.10+(p.y+.10)*op;}"+\n',
'heat-shield follow schedule')

# Blue/white transparent flame core + orange shock cells + heat distortion;
# remove the opaque pink-tube appearance.
j=rep(j,
'                "if(vP>47.5&&vP<48.5){float ab=smoothstep(.72,.89,uThrottle),f=.72+.28*sin(uTime*28.+vPos.z*18.);vec3 c=mix(vec3(.10,.34,1.75),vec3(1.65,.24,.018),smoothstep(4.18,5.45,vPos.z));gl_FragColor=vec4(c*(1.45+1.15*f)*ab,ab*(.18+.55*f));return;}"+\n',
'                "if(vP>47.5&&vP<48.5){float ab=smoothstep(.76,.91,uThrottle),f=.72+.28*sin(uTime*25.+vPos.z*17.);float warm=smoothstep(4.32,5.70,vPos.z);vec3 c=mix(vec3(.08,.62,1.95),vec3(2.00,.50,.045),warm);gl_FragColor=vec4(c*(1.05+.90*f)*ab,ab*(.10+.34*f));return;}"+\n',
'orange shock diamonds with transparent blue core')
j=rep(j,
'                "else if(vP>21.5&&vP<22.5){float ab=smoothstep(.73,.88,uThrottle);float flick=.72+.28*sin(uTime*27.+vPos.z*19.),diamond=.78+.22*sin(vPos.z*10.5-uTime*5.);base=vec3(.025,.018,.012);vec3 core=mix(vec3(.08,.28,1.45),vec3(1.35,.30,.025),smoothstep(3.7,6.3,vPos.z));emitc=core*ab*(2.0+1.25*flick)*diamond;rough=.10;metal=.08;alpha=ab*(.16+.58*flick);}"+\n',
'                "else if(vP>21.5&&vP<22.5){float ab=smoothstep(.76,.91,uThrottle);float flick=.78+.22*sin(uTime*24.+vPos.z*17.);base=vec3(.012,.020,.032);vec3 core=mix(vec3(.08,.55,1.65),vec3(.35,.78,1.35),smoothstep(4.05,4.85,vPos.z));emitc=core*ab*(1.25+.75*flick);rough=.08;metal=.04;alpha=ab*(.08+.20*flick);}"+\n',
'slender transparent flame core')
j=rep(j,
'else if(vP>7.5&&vP<8.5){float flick=.75+.25*sin(uTime*23.+vPos.z*15.);base=vec3(.045,.018,.010);emitc=mix(vec3(.05,.18,.82),vec3(1.,.13,.006),uThrottle)*uThrottle*(1.5+.8*flick);rough=.18;metal=.14;alpha=.45+.40*uThrottle;}',
'else if(vP>7.5&&vP<8.5){float ab=smoothstep(.76,.91,uThrottle),flick=.78+.22*sin(uTime*21.+vPos.z*14.);base=vec3(.012,.020,.030);emitc=mix(vec3(.07,.48,1.55),vec3(.42,.67,1.20),smoothstep(4.0,5.1,vPos.z))*ab*(.85+.55*flick);rough=.10;metal=.05;alpha=ab*(.07+.18*flick);}',
'afterburner translucent blue shell')
JET.write_text(j)

g=GRADLE.read_text()
g=rep(g,'        versionCode 92\n','        versionCode 93\n','version code')
g=rep(g,"        versionName '26.10-avm21.0-fighter-audio'\n","        versionName '26.11-avm22.0-variable-nozzle'\n",'version name')
GRADLE.write_text(g)

print('v93 variable nozzle applied: 18 overlapping outer/inner petals, actuator sync ring, idle-dry-AB iris schedule, deep liner, blue core, orange shock diamonds and expanded heat haze')
