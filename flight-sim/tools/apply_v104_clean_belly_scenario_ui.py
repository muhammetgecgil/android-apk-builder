from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
VIS=PKG/'visual'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/visual'
REAL=VIS/'RealisticFighterMesh.java'
LAUNCHER=PKG/'LauncherActivity.java'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v104 clean-belly patch anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# 1) Remove ONLY the plate-like belly overlay from the rendered aircraft.
# The smooth fuselage itself remains untouched, as do wings, tails, gear,
# cockpit, engines, hardpoints, control surfaces and all flight dynamics.
# ---------------------------------------------------------------------------
r=REAL.read_text()
r=rep(r,
'        b.bellyBlend();\n',
'        // v104: plate-like belly overlay intentionally not rendered; smooth fuselage remains the lower skin.\n',
'belly overlay build call')
REAL.write_text(r)

# ---------------------------------------------------------------------------
# 2) Strengthen mission/scenario copy without changing scenario IDs/routing.
# ---------------------------------------------------------------------------
l=LAUNCHER.read_text()
l=rep(l,
'        TextView title=new TextView(this);title.setText("GÖREV / SCENARIO SELECT");',
'        TextView title=new TextView(this);title.setText("GÖREV / SENARYO SEÇİMİ");',
'scenario title')
l=rep(l,
'        TextView sub=new TextView(this);sub.setText("Her görev aynı uçuş motorunu kullanır; hava, başlangıç fazı, ATC ve pist koşulları göreve göre ayarlanır.");',
'        TextView sub=new TextView(this);sub.setText("Her görev aynı gelişmiş uçuş motorunu kullanır; hava durumu, başlangıç fazı, ATC akışı, pist koşulları ve çevresel etkiler seçilen göreve göre ayarlanır.");',
'scenario subtitle')
l=rep(l,
'        addScenario(root,cockpitTarget,SCENARIO_FULL_DEMO,"1. FULL DEMO  —  Tam görev: kalkış • bulut • deniz • orbit • yaklaşma • iniş");',
'        addScenario(root,cockpitTarget,SCENARIO_FULL_DEMO,"1. FULL DEMO  —  Tam görev zinciri • kalkış • bulut geçişi • deniz alçak uçuş • orbit • yaklaşma • iniş");',
'full demo copy')
l=rep(l,
'        addScenario(root,cockpitTarget,SCENARIO_NIGHT_LANDING,"2. NIGHT LANDING  —  RWY 27 gece yaklaşması • tam pist ışıkları • PAPI/HUD");',
'        addScenario(root,cockpitTarget,SCENARIO_NIGHT_LANDING,"2. NIGHT LANDING  —  RWY 27 gece operasyonu • düşük görsel referans • pist ışıkları • PAPI • HUD");',
'night landing copy')
l=rep(l,
'        addScenario(root,cockpitTarget,SCENARIO_LOW_VIS_RAIN,"3. LOW VIS / RAIN APPROACH  —  Yağmur + sis • düşük RVR • ıslak pist • ILS/PAPI");',
'        addScenario(root,cockpitTarget,SCENARIO_LOW_VIS_RAIN,"3. LOW VIS / RAIN APPROACH  —  Yoğun yağmur • sis • düşük RVR • ıslak pist • ILS/PAPI hassas yaklaşma");',
'low visibility copy')
l=rep(l,
'        addScenario(root,cockpitTarget,SCENARIO_MOUNTAIN_PASS,"4. MOUNTAIN PASS  —  Dağ rotası • yüksek hızlı alçak/orta irtifa manevrası");',
'        addScenario(root,cockpitTarget,SCENARIO_MOUNTAIN_PASS,"4. MOUNTAIN PASS  —  Dağ geçidi • yüksek hızlı alçak/orta irtifa • araziye duyarlı manevra yönetimi");',
'mountain copy')
l=rep(l,
'        addScenario(root,cockpitTarget,SCENARIO_SEA_LOW_LEVEL,"5. SEA LOW LEVEL  —  Deniz üstü alçak irtifa • yüksek hız • pull-up");',
'        addScenario(root,cockpitTarget,SCENARIO_SEA_LOW_LEVEL,"5. SEA LOW LEVEL  —  Deniz üstü çok alçak irtifa • yüksek sürat • yüzey akışı • agresif pull-up");',
'sea copy')
l=rep(l,
'        addScenario(root,cockpitTarget,SCENARIO_EMERGENCY_RETURN,"6. EMERGENCY RETURN  —  Kısa acil geri dönüş • pist yakalama • öncelikli ATC");',
'        addScenario(root,cockpitTarget,SCENARIO_EMERGENCY_RETURN,"6. EMERGENCY RETURN  —  Acil geri dönüş • ATC önceliği • kısa yaklaşma • güvenli pist yakalama");',
'emergency copy')
LAUNCHER.write_text(l)

# ---------------------------------------------------------------------------
# 3) Mesh integrity regression: confirm the aircraft remains a full volumetric
# fighter after removal of the one belly overlay build call.
# ---------------------------------------------------------------------------
TEST.mkdir(parents=True,exist_ok=True)
(TEST/'AircraftMeshIntegrityV104Test.java').write_text(r'''package com.mg.fixturecockpitsim.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class AircraftMeshIntegrityV104Test {
    @Test public void aircraftRemainsFullAndFinite(){
        ProceduralFighterMesh.Mesh m=RealisticFighterMesh.build();
        assertNotNull(m);assertNotNull(m.data);assertEquals(0,m.data.length%7);
        assertTrue("aircraft vertex count collapsed",m.vertexCount()>3500);
        float minX=Float.POSITIVE_INFINITY,maxX=Float.NEGATIVE_INFINITY;
        float minY=Float.POSITIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY;
        float minZ=Float.POSITIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
        int skin=0,leftFlaperon=0,rightFlaperon=0,leftStab=0,rightStab=0,leftRudder=0,rightRudder=0;
        for(int i=0;i<m.data.length;i+=7){
            float x=m.data[i],y=m.data[i+1],z=m.data[i+2],part=m.data[i+6];
            assertFalse(Float.isNaN(x)||Float.isNaN(y)||Float.isNaN(z));
            assertFalse(Float.isInfinite(x)||Float.isInfinite(y)||Float.isInfinite(z));
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
            if(part==0f)skin++;else if(part==9f)leftFlaperon++;else if(part==10f)rightFlaperon++;else if(part==4f)leftStab++;else if(part==5f)rightStab++;else if(part==6f)leftRudder++;else if(part==7f)rightRudder++;
        }
        assertTrue("wingspan/airframe width missing",minX<-5.0f&&maxX>5.0f);
        assertTrue("fuselage length missing",minZ<-6.4f&&maxZ>3.3f);
        assertTrue("vertical geometry missing",minY<-.55f&&maxY>2.2f);
        assertTrue("main skin missing",skin>1000);
        assertTrue(leftFlaperon>20&&rightFlaperon>20);
        assertTrue(leftStab>20&&rightStab>20);
        assertTrue(leftRudder>20&&rightRudder>20);
    }
}
''')

# v104 version metadata. This executes after v103 in the workflow.
g=GRADLE.read_text()
g2=re.sub(r'versionCode\s+103\b','versionCode 104',g,count=1)
if g2==g and 'versionCode 104' not in g:
    raise SystemExit('v104 versionCode anchor missing')
g=g2
g2=re.sub(r'versionName\s+"26\.21-avm31\.0-tower-turn-dynamics"','versionName "26.22-avm32.0-clean-belly-scenario-ui"',g,count=1)
if g2==g and '26.22-avm32.0-clean-belly-scenario-ui' not in g:
    raise SystemExit('v104 versionName anchor missing')
GRADLE.write_text(g2)

print('v104 clean belly + scenario UI patch applied')
