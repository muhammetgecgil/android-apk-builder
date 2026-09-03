from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
VIS=PKG/'visual/VisualOrdnanceMesh.java'
JET=PKG/'Jet3DView.java'
DYN=SIM/'FlightDynamicsEngine.java'
STATE=SIM/'FlightState.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v102 hardpoint patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Generic external-store aero model. No weapon employment logic: this only
# models rack/pylon/tank presence, parasite drag and the already-existing tank
# mass/fuel state from AVM-29.
# ---------------------------------------------------------------------------
(SIM/'HardpointExternalStoreModel.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

/** AVM-30 generic pylon/adapter/external-fuel-tank structural/aero coupling. */
public final class HardpointExternalStoreModel {
    private HardpointExternalStoreModel(){}
    public static final double PYLON_EQ_DRAG_AREA_M2=.055;
    public static final double EXTERNAL_TANK_EQ_DRAG_AREA_M2=.285;

    public static double machDragFactor(double mach){
        double m=Math.max(0,mach);
        if(m<.78)return 1.0;
        if(m<1.08)return 1.0+.38*((m-.78)/.30);
        if(m<1.55)return 1.38-.18*((m-1.08)/.47);
        return 1.20;
    }

    public static double equivalentDragAreaM2(FlightState s){
        double a=PYLON_EQ_DRAG_AREA_M2;
        if(s.externalTankPresent)a+=EXTERNAL_TANK_EQ_DRAG_AREA_M2;
        return a*machDragFactor(s.mach);
    }

    public static double dragN(FlightState s){
        double q=Math.max(0,s.dynamicPressurePa);
        return q*equivalentDragAreaM2(s);
    }
}
''')

(TEST/'sim').mkdir(parents=True,exist_ok=True)
(TEST/'sim/HardpointExternalStoreModelTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class HardpointExternalStoreModelTest {
    @Test public void externalTankAddsParasiteDrag(){
        FlightState s=new FlightState();s.dynamicPressurePa=26000;s.mach=.72;s.externalTankPresent=false;
        double pylon=HardpointExternalStoreModel.dragN(s);
        s.externalTankPresent=true;double tank=HardpointExternalStoreModel.dragN(s);
        assertTrue(tank>pylon*4.0);
    }
    @Test public void transonicRackDragRises(){
        FlightState s=new FlightState();s.dynamicPressurePa=22000;s.externalTankPresent=true;s.mach=.65;
        double low=HardpointExternalStoreModel.dragN(s);s.mach=.98;double trans=HardpointExternalStoreModel.dragN(s);
        assertTrue(trans>low*1.15);
    }
}
''')

# FlightState engineering telemetry for store drag.
s=STATE.read_text()
s=rep(s,
'    public double fuelFlowKgSec, fuelTransferKgSec, fuelImbalanceKg;\n',
'    public double fuelFlowKgSec, fuelTransferKgSec, fuelImbalanceKg;\n    public double externalStoreDragN, externalStoreEqDragAreaM2;\n',
'external store state')
s=rep(s,
'        c.fuelFlowKgSec=fuelFlowKgSec;c.fuelTransferKgSec=fuelTransferKgSec;c.fuelImbalanceKg=fuelImbalanceKg;\n',
'        c.fuelFlowKgSec=fuelFlowKgSec;c.fuelTransferKgSec=fuelTransferKgSec;c.fuelImbalanceKg=fuelImbalanceKg;\n        c.externalStoreDragN=externalStoreDragN;c.externalStoreEqDragAreaM2=externalStoreEqDragAreaM2;\n',
'external store state copy')
STATE.write_text(s)

# Dynamics: pylon/tank parasite drag participates in acceleration and telemetry.
d=DYN.read_text()
d=rep(d,
'            double accel=(forwardThrust-aero.dragN-rollingResistance)/mass;\n',
'            double storeDrag=HardpointExternalStoreModel.dragN(s);\n            double accel=(forwardThrust-aero.dragN-storeDrag-rollingResistance)/mass;\n',
'ground store drag')
d=rep(d,
'            double longitudinal=(aero.thrustN*Math.cos(alphaRad)-aero.dragN-mass*G*Math.sin(gammaRad))/mass;\n',
'            double storeDrag=HardpointExternalStoreModel.dragN(s);\n            double longitudinal=(aero.thrustN*Math.cos(alphaRad)-aero.dragN-storeDrag-mass*G*Math.sin(gammaRad))/mass;\n',
'air store drag')
d=rep(d,
'        s.angleOfAttackDeg=a.aoaDeg;s.dynamicPressurePa=a.dynamicPressurePa;s.liftCoefficient=a.cl;s.dragCoefficient=a.cd;\n        s.liftN=a.liftN;s.dragN=a.dragN;s.thrustN=a.thrustN;s.loadFactor=clamp(a.loadFactor,-4.5,12.0);\n',
'''        s.angleOfAttackDeg=a.aoaDeg;s.dynamicPressurePa=a.dynamicPressurePa;s.liftCoefficient=a.cl;s.dragCoefficient=a.cd;
        s.externalStoreEqDragAreaM2=HardpointExternalStoreModel.equivalentDragAreaM2(s);
        s.externalStoreDragN=HardpointExternalStoreModel.dragN(s);
        s.liftN=a.liftN;s.dragN=a.dragN+s.externalStoreDragN;s.thrustN=a.thrustN;s.loadFactor=clamp(a.loadFactor,-4.5,12.0);
''',
'published store drag telemetry')
DYN.write_text(d)

# ---------------------------------------------------------------------------
# Visual hardpoint: explicit upper mounting shoe, stand-off, pylon web, rack
# hooks, four adjustable sway braces and visible contact pads. The store is
# separated from the wing by real geometry rather than appearing glued on.
# ---------------------------------------------------------------------------
v=VIS.read_text()
v=rep(v,
'    public static final float PART_ADAPTER=38f;\n    public static final float PART_SWAY_BRACE=39f;\n',
'    public static final float PART_ADAPTER=38f;\n    public static final float PART_SWAY_BRACE=39f;\n    public static final float PART_MOUNT_LUG=40f;\n',
'mount lug material id')
pat=r'    private void hardpointAdapterRack\(float x,float wingY,float z,float s\)\{.*?\n    \}\n\n'
new_method=r'''    private void hardpointAdapterRack(float x,float wingY,float z,float s){
        // Upper shoe follows the wing contour; a visible stand-off keeps the
        // pylon/store mechanically separated from the wing skin.
        part=PART_ADAPTER;
        box(x,wingY-.018f*s,z-.02f*s,.300f*s,.030f*s,.54f*s);
        box(x,wingY-.070f*s,z+.00f*s,.215f*s,.050f*s,.70f*s);
        box(x,wingY-.125f*s,z+.025f*s,.155f*s,.045f*s,.86f*s);

        // Four flush mounting lugs / fastener bosses at the wing interface.
        part=PART_MOUNT_LUG;
        for(float zz:new float[]{-.22f,.23f})for(float side:new float[]{-1f,1f}){
            box(x+side*.205f*s,wingY-.040f*s,z+zz*s,.038f*s,.018f*s,.060f*s);
            cylinderBetween(x+side*.205f*s,wingY-.024f*s,z+zz*s,x+side*.205f*s,wingY-.080f*s,z+zz*s,.013f*s,8);
        }

        // Pylon web + lower rack body, leaving a readable air gap to the store.
        part=PART_PYLON;
        box(x,wingY-.260f*s,z+.035f*s,.118f*s,.120f*s,1.18f*s);
        box(x,wingY-.375f*s,z+.045f*s,.145f*s,.045f*s,.88f*s);

        // Four independent adjustable sway-brace screw arms and contact pads.
        part=PART_SWAY_BRACE;
        for(float zz:new float[]{-.225f,.255f})for(float side:new float[]{-1f,1f}){
            cylinderBetween(x+side*.182f*s,wingY-.300f*s,z+zz*s,x+side*.095f*s,wingY-.490f*s,z+(zz+.012f)*s,.017f*s,10);
            cylinderBetween(x+side*.112f*s,wingY-.450f*s,z+(zz+.010f)*s,x+side*.092f*s,wingY-.505f*s,z+(zz+.012f)*s,.023f*s,10);
            box(x+side*.084f*s,wingY-.515f*s,z+(zz+.012f)*s,.052f*s,.020f*s,.078f*s);
        }

        // Fore/aft suspension hook housings and service umbilical geometry.
        part=PART_MOUNT_LUG;
        for(float zz:new float[]{-.30f,.31f}){
            box(x-.066f*s,wingY-.420f*s,z+zz*s,.050f*s,.040f*s,.110f*s);
            box(x+.066f*s,wingY-.420f*s,z+zz*s,.050f*s,.040f*s,.110f*s);
        }
        part=PART_DETAIL;
        cylinderBetween(x+.115f*s,wingY-.355f*s,z+.13f*s,x+.145f*s,wingY-.485f*s,z+.235f*s,.011f*s,8);
    }

'''
v2,n=re.subn(pat,new_method,v,count=1,flags=re.S)
if n!=1:
    raise SystemExit('v102 hardpoint patch anchor missing: hardpointAdapterRack method')
VIS.write_text(v2)

(TEST/'visual').mkdir(parents=True,exist_ok=True)
(TEST/'visual/VisualHardpointStructuralTest.java').write_text(r'''package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisualHardpointStructuralTest {
    @Test public void mountLugsAndSwayBracesAreExplicitGeometry(){
        float[] d=VisualOrdnanceMesh.build();int lugs=0,braces=0,adapters=0;
        for(int i=0;i+6<d.length;i+=7){
            float p=d[i+6];
            if(Math.abs(p-VisualOrdnanceMesh.PART_MOUNT_LUG)<.1f)lugs++;
            if(Math.abs(p-VisualOrdnanceMesh.PART_SWAY_BRACE)<.1f)braces++;
            if(Math.abs(p-VisualOrdnanceMesh.PART_ADAPTER)<.1f)adapters++;
        }
        assertTrue(lugs>80);assertTrue(braces>140);assertTrue(adapters>40);
    }
}
''')

# Distinct dark steel response for the mounting lugs.
j=JET.read_text()
old='else if(vP>38.5&&vP<39.5){base=vec3(.095,.102,.108);rough=.25;metal=.92;ao=.84;}else if(vP>7.5&&vP<8.5)'
new='else if(vP>38.5&&vP<39.5){base=vec3(.095,.102,.108);rough=.25;metal=.92;ao=.84;}else if(vP>39.5&&vP<40.5){base=vec3(.070,.076,.082);rough=.22;metal=.95;ao=.82;}else if(vP>7.5&&vP<8.5)'
j=rep(j,old,new,'mount lug shader')
JET.write_text(j)

# Version bump.
g=GRADLE.read_text()
g=rep(g,'        versionCode 101\n','        versionCode 102\n','version code')
g=rep(g,"        versionName '26.19-avm29.0-fuel-management'\n","        versionName '26.20-avm30.0-hardpoint-external-tank-realism'\n",'version name')
GRADLE.write_text(g)

print('v102 hardpoint/external-tank realism applied: structural adapter/pylon/sway-braces + external tank parasite drag coupling')
