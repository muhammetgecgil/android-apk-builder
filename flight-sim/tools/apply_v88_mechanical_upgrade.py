from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
ADV=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/AdvancedAirframeOverlay.java'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v88 mechanical patch anchor missing: {label}')
    return text.replace(old,new,1)

# Renderer integration: add the new mechanical geometry as a normal opaque buffer.
j=JET.read_text()
j=rep(j,
'import com.mg.fixturecockpitsim.visual.EngineDynamicsOverlay;\n',
'import com.mg.fixturecockpitsim.visual.EngineDynamicsOverlay;\nimport com.mg.fixturecockpitsim.visual.MechanicalDynamicsOverlay;\n',
'mechanical import')
j=rep(j,
'FloatBuffer vbOpaque,vbCanopy,detailBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;\n',
'FloatBuffer vbOpaque,vbCanopy,detailBuffer,mechanicalBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;\n',
'mechanical buffer field')
j=rep(j,
'int opaqueCount,canopyCount,detailCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;\n',
'int opaqueCount,canopyCount,detailCount,mechanicalCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;\n',
'mechanical count field')
j=rep(j,
'            float[] detail=AdvancedAirframeOverlay.build();detailBuffer=buffer(detail);detailCount=detail.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();',
'            float[] detail=AdvancedAirframeOverlay.build();detailBuffer=buffer(detail);detailCount=detail.length/7;\n            float[] mech=MechanicalDynamicsOverlay.build();mechanicalBuffer=buffer(mech);mechanicalCount=mech.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();',
'mechanical mesh construction')
j=rep(j,
'            bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(engineSolidBuffer,engineSolidCount);',
'            bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(mechanicalBuffer,mechanicalCount);bindAndDraw(engineSolidBuffer,engineSolidCount);',
'mechanical draw call')

# Replace the old single-stage gear transform with a staged mechanical sequence.
old='''                \"if((aPart>12.5&&aPart<15.5)||(aPart>23.5&&aPart<24.5)){float nose=step(p.z,-2.),r=1.-uGear,fold=smoothstep(.08,.82,r);vR=r;if(aPart>13.5&&aPart<14.5&&uGear>.70){vec2 ctr=nose>.5?vec2(-1.62,-3.78):vec2(-1.67,1.18);p.yz=rr(uWheelSpin)*(p.yz-ctr)+ctr;}if(nose>.5){vec2 piv=vec2(-.46,-3.76),q=p.yz-piv;q=rr(-1.34*fold)*q;p.yz=piv+q;p.x*=mix(1.,.55,fold);p.y=min(p.y,-.40);}else{float s=p.x<0.?-1.:1.;vec2 piv=vec2(1.34*s,-.38),q=vec2(p.x,p.y)-piv;q=rr(-s*1.14*fold)*q;p.x=piv.x+q.x;p.y=piv.y+q.y;p.x=mix(p.x,.72*s,smoothstep(.68,.96,r));p.z=mix(p.z,.94+(p.z-.94)*.42,fold);p.y=min(p.y,-.38);}if(aPart>14.5&&aPart<15.5){float op=smoothstep(.03,.18,r)*(1.-smoothstep(.62,.92,r));p.x+=(p.x<0.?-.24:.24)*op;p.y-=.08*op;}}\"+\n'''
new='''                \"if((aPart>12.5&&aPart<15.5)||(aPart>23.5&&aPart<24.5)){float nose=step(p.z,-2.),r=1.-uGear;float fold=nose>.5?smoothstep(.10,.78,r):smoothstep(.18,.90,r);vR=fold;if(aPart>13.5&&aPart<14.5&&uGear>.70){vec2 ctr=nose>.5?vec2(-1.62,-3.78):vec2(-1.67,1.18);p.yz=rr(uWheelSpin)*(p.yz-ctr)+ctr;}if(uGear>.72&&aPart>12.5&&aPart<14.5){float comp=nose>.5?uNoseComp:uMainComp;float lower=clamp((-p.y-1.02)/.62,0.,1.);p.y+=comp*lower*(nose>.5?.18:.22);}if(nose>.5&&uGear>.72&&aPart>12.5&&aPart<14.5){float steer=(uRudderL+uRudderR)*.31*d;vec2 st=vec2(p.x,p.z+3.78);st=rr(steer)*st;p.x=st.x;p.z=st.y-3.78;}if(nose>.5){vec2 piv=vec2(-.46,-3.76),q=p.yz-piv;q=rr(-1.34*fold)*q;p.yz=piv+q;p.x*=mix(1.,.55,fold);p.y=min(p.y,-.40);}else{float s=p.x<0.?-1.:1.;vec2 piv=vec2(1.34*s,-.38),q=vec2(p.x,p.y)-piv;q=rr(-s*1.14*fold)*q;p.x=piv.x+q.x;p.y=piv.y+q.y;p.x=mix(p.x,.72*s,smoothstep(.70,.97,r));p.z=mix(p.z,.94+(p.z-.94)*.42,fold);p.y=min(p.y,-.38);}if(aPart>14.5&&aPart<15.5){float op=smoothstep(.025,.16,r)*(1.-smoothstep(.74,.96,r));float seq=nose>.5?1.:.88;p.x+=(p.x<0.?-.25:.25)*op*seq;p.y-=.09*op;}}\"+\n'''
j=rep(j,old,new,'staged gear shader')
JET.write_text(j)

# Existing airframe hardware should move with its parent surfaces rather than remain static.
a=ADV.read_text()
old='''    private void controlSurfaceHardware(){\n        part=DETAIL;\n        for(float s:new float[]{-1f,1f}){\n            // Stabilator root hinge drum and two rudder hinge fairings.\n            cylinderBetween(.74f*s,.33f,1.88f,.92f*s,.33f,1.88f,.075f,14);\n            ellipsoid(.95f*s,1.38f,2.42f,.055f,.105f,.080f,12,8);\n            ellipsoid(1.05f*s,1.88f,2.53f,.050f,.100f,.075f,12,8);\n            // Flaperon drive fairing.\n            ellipsoid(2.10f*s,.37f,.86f,.080f,.050f,.235f,14,8);\n        }\n    }\n'''
new='''    private void controlSurfaceHardware(){\n        for(float s:new float[]{-1f,1f}){\n            // v88: hardware inherits the same moving part ID as the parent surface.\n            part=s<0f?4f:5f;\n            cylinderBetween(.74f*s,.33f,1.88f,.92f*s,.33f,1.88f,.075f,14);\n            part=s<0f?6f:7f;\n            ellipsoid(.95f*s,1.38f,2.42f,.055f,.105f,.080f,12,8);\n            ellipsoid(1.05f*s,1.88f,2.53f,.050f,.100f,.075f,12,8);\n            part=s<0f?9f:10f;\n            ellipsoid(2.10f*s,.37f,.86f,.080f,.050f,.235f,14,8);\n        }\n        part=DETAIL;\n    }\n'''
a=rep(a,old,new,'control-surface hardware parenting')
a=rep(a,
'''            part=DETAIL;\n            // Two concentric actuator families create visible mechanical depth.\n''',
'''            part=NOZZLE_PETAL;\n            // v88: actuator families follow vectoring and nozzle aperture motion.\n''',
'nozzle actuator parenting')
ADV.write_text(a)

print('v88 mechanical upgrade applied: staged gear, oleo compression, nose steering, moving surface/nozzle hardware')
