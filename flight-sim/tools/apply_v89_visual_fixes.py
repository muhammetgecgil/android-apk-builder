from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'
MECH=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/MechanicalDynamicsOverlay.java'
ADV=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/AdvancedAirframeOverlay.java'
ORD=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/VisualOrdnanceMesh.java'
AIR=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/AirfieldWorldView.java'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v89 patch anchor missing: {label}')
    return text.replace(old,new,1)

# 1) Remove the v88 mechanical pieces that read as detached/floating surfaces.
# Keep the gear and pylon mechanics; the nozzle is replaced by a dedicated solid shell.
m=MECH.read_text()
m=rep(m,
'''        b.noseGearMechanics();\n        b.mainGearMechanics();\n        b.controlSurfaceMechanics();\n        b.nozzleMechanics();\n        b.pylonMechanics();\n        b.canopyMechanics();\n''',
'''        b.noseGearMechanics();\n        b.mainGearMechanics();\n        b.pylonMechanics();\n''','mechanical residue removal')
m=m.replace('{{-1.72f,.14f,-.42f,1.0f},{1.72f,.14f,-.42f,1.0f},{-2.82f,.13f,-.02f,.83f},{2.82f,.13f,-.02f,.83f}}',
            '{{-1.72f,.225f,-.42f,1.0f},{1.72f,.225f,-.42f,1.0f},{-2.82f,.215f,-.02f,.83f},{2.82f,.215f,-.02f,.83f}}')
MECH.write_text(m)

# Advanced overlay already has base canopy/nozzle hardware; remove the duplicated v88-visible
# control/canopy/nozzle auxiliary geometry that caused the highlighted floating remnants.
a=ADV.read_text()
a=rep(a,'        b.canopyHardware();\n','        // v89: base mesh canopy frame is sufficient; omit duplicate external hinge overlay.\n','duplicate canopy overlay')
a=rep(a,'        b.controlSurfaceHardware();\n','        // v89: omit auxiliary control-surface fairings that could separate at large deflection.\n','duplicate control hardware')
a=rep(a,'        b.nozzleActuators();\n','        // v89: replaced by SolidNozzleOverlay; no exposed wire-wheel actuator cage.\n','wire nozzle overlay')
ADV.write_text(a)

# 2) Raise all wing pylons into the actual lower-wing envelope. A small intentional
# overlap makes the chain read as wing -> pylon -> store without an air gap.
o=ORD.read_text()
o=rep(o,
'''        b.station(-1.72f,.14f,-.42f,1.00f);\n        b.station( 1.72f,.14f,-.42f,1.00f);\n        b.station(-2.82f,.13f,-.02f,.83f);\n        b.station( 2.82f,.13f,-.02f,.83f);\n''',
'''        b.station(-1.72f,.225f,-.42f,1.00f);\n        b.station( 1.72f,.225f,-.42f,1.00f);\n        b.station(-2.82f,.215f,-.02f,.83f);\n        b.station( 2.82f,.215f,-.02f,.83f);\n''','wing pylon contact')
ORD.write_text(o)

# 3) Add the solid nozzle mesh to the proven renderer after v88 mechanical integration.
j=JET.read_text()
j=rep(j,
'import com.mg.fixturecockpitsim.visual.MechanicalDynamicsOverlay;\n',
'import com.mg.fixturecockpitsim.visual.MechanicalDynamicsOverlay;\nimport com.mg.fixturecockpitsim.visual.SolidNozzleOverlay;\n','solid nozzle import')
j=rep(j,
'FloatBuffer vbOpaque,vbCanopy,detailBuffer,mechanicalBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;',
'FloatBuffer vbOpaque,vbCanopy,detailBuffer,mechanicalBuffer,nozzleSolidBuffer,engineSolidBuffer,engineTransparentBuffer,obOpaque,obGlass,vortexBuffer,birdBuffer;','solid nozzle buffer')
j=rep(j,
'int opaqueCount,canopyCount,detailCount,mechanicalCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;',
'int opaqueCount,canopyCount,detailCount,mechanicalCount,nozzleSolidCount,engineSolidCount,engineTransparentCount,ordnanceCount,glassCount,vortexCount,birdCount;','solid nozzle count')
j=rep(j,
'            float[] mech=MechanicalDynamicsOverlay.build();mechanicalBuffer=buffer(mech);mechanicalCount=mech.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();',
'            float[] mech=MechanicalDynamicsOverlay.build();mechanicalBuffer=buffer(mech);mechanicalCount=mech.length/7;\n            float[] nozzle=SolidNozzleOverlay.build();nozzleSolidBuffer=buffer(nozzle);nozzleSolidCount=nozzle.length/7;\n            float[] es=EngineDynamicsOverlay.buildSolid();','solid nozzle build')
j=rep(j,
'bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(mechanicalBuffer,mechanicalCount);bindAndDraw(engineSolidBuffer,engineSolidCount);',
'bindAndDraw(vbOpaque,opaqueCount);bindAndDraw(detailBuffer,detailCount);bindAndDraw(mechanicalBuffer,mechanicalCount);bindAndDraw(nozzleSolidBuffer,nozzleSolidCount);bindAndDraw(engineSolidBuffer,engineSolidCount);','solid nozzle draw')
JET.write_text(j)

# 4) Control tower: approach for the first three seconds after movement begins, then
# remain in world-space behind the aircraft and slide/fade aft instead of following it.
w=AIR.read_text()
w=rep(w,'    private float runwayFlow,groundFlow;\n','    private float runwayFlow,groundFlow;\n    private long towerMoveStartNs;\n    private float towerElapsedSec;\n','tower timer fields')
w=rep(w,
'''        crossTrackM=(float)crossTrack;alongTrackM=(float)alongTrack;crashed=crash;crashReason=reason==null?"":reason;\n        sharedAltitudeM=altitudeM;''',
'''        crossTrackM=(float)crossTrack;alongTrackM=(float)alongTrack;crashed=crash;crashReason=reason==null?"":reason;\n        boolean departure=phase.contains("TAXI_OUT")||phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||phase.contains("ROTATE_CLIMB");\n        long towerNow=System.nanoTime();\n        if(departure&&speedMps>.6f){if(towerMoveStartNs==0L)towerMoveStartNs=towerNow;towerElapsedSec=(towerNow-towerMoveStartNs)/1.0e9f;}\n        else if(!departure||(phase.contains("TAXI_OUT")&&speedMps<.25f)){towerMoveStartNs=0L;towerElapsedSec=0f;}\n        sharedAltitudeM=altitudeM;''','tower movement timer')
w=rep(w,
'''    private boolean towerVisible(){\n        if(!(phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||phase.contains("ROTATE_CLIMB")))return false;\n        if(altitudeM>150f)return false;\n        float rel=TOWER_ALONG_M-alongTrackM;\n        return rel>-250f&&rel<620f;\n    }\n''',
'''    private boolean towerVisible(){\n        if(!(phase.contains("TAXI_OUT")||phase.contains("RUNWAY_HOLD")||phase.contains("TAKEOFF_ROLL")||phase.contains("ROTATE_CLIMB")))return false;\n        if(altitudeM>150f)return false;\n        return towerElapsedSec<5.7f;\n    }\n''','tower visibility')
w=rep(w,
'''        float rel=TOWER_ALONG_M-alongTrackM;\n        float near=clamp(1f-Math.max(0f,rel)/560f,.18f,1f);\n        float passed=Math.max(0f,-rel),fade=1f-clamp(passed/230f,0f,1f);\n        int a=(int)(255f*fade);if(a<=3)return;\n        float x=w*(.80f+.08f*(1f-near))-passed*w/720f;\n''',
'''        float elapsed=Math.max(0f,towerElapsedSec);\n        float approach=clamp(elapsed/3f,0f,1f);\n        float passedT=Math.max(0f,elapsed-3f);\n        float near=clamp(.34f+.66f*approach-passedT*.12f,.24f,1f);\n        float fade=1f-clamp(passedT/2.7f,0f,1f);\n        int a=(int)(255f*fade);if(a<=3)return;\n        // At t=3 s the aircraft reaches the tower. Afterwards the tower moves aft\n        // across the view and disappears, making the aircraft visibly pass it.\n        float x=w*(.88f-.075f*approach)-passedT*w*.34f;\n''','tower pass-by motion')
AIR.write_text(w)

print('v89 fixes applied: residual overlays removed, pylons seated, solid nozzles, 3-second tower pass-by')
