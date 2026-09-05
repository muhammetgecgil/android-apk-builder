from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
PKG=ROOT/'app/src/main/java/com/mg/fixturecockpitsim'
SIM=PKG/'sim'
LAUNCHER=PKG/'LauncherActivity.java'
RUNTIME=PKG/'FlightRuntimeActivity.java'
JOURNEY=SIM/'CinematicJourneyState.java'
TEST=ROOT/'app/src/test/java/com/mg/fixturecockpitsim/sim'
GRADLE=ROOT/'app/build.gradle'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v111 Toros/Ege/Patara anchor missing: {label}')
    return text.replace(old,new,1)

# ---------------------------------------------------------------------------
# Scenario selector: three real Türkiye regions already supported by the v108+
# 3D terrain renderer. The old/v107 aircraft renderer is intentionally untouched.
# ---------------------------------------------------------------------------
l=LAUNCHER.read_text()
l=rep(l,
'''    public static final int SCENARIO_FULL_DEMO=1;
    public static final int SCENARIO_NIGHT_LANDING=2;
    public static final int SCENARIO_LOW_VIS_RAIN=3;
    public static final int SCENARIO_MOUNTAIN_PASS=4;
    public static final int SCENARIO_SEA_LOW_LEVEL=5;
    public static final int SCENARIO_EMERGENCY_RETURN=6;
''',
'''    public static final int SCENARIO_TOROSLAR=1;
    public static final int SCENARIO_EGE=2;
    public static final int SCENARIO_PATARA=3;
''','scenario constants')

l=rep(l,
'        TextView title=new TextView(this);title.setText("GÖREV / SCENARIO SELECT");title.setTextColor(Color.rgb(132,255,180));title.setTextSize(24);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(42)));\n',
'        TextView title=new TextView(this);title.setText("TÜRKİYE UÇUŞ BÖLGESİ");title.setTextColor(Color.rgb(132,255,180));title.setTextSize(24);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(42)));\n','scenario title')

l=rep(l,
'        TextView sub=new TextView(this);sub.setText("Aynı uçuş motoru • farklı görev profili • hava/pist/ATC bağlamı • görev odaklı briefing");sub.setTextColor(Color.rgb(190,194,197));sub.setTextSize(11);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));\n',
'        TextView sub=new TextView(this);sub.setText("Eski v107 uçak sunumu • gerçek 3D arazi • seçilen bölge uçuş boyunca aktif");sub.setTextColor(Color.rgb(190,194,197));sub.setTextSize(11);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));\n','scenario subtitle')

old='''        addScenario(root,cockpitTarget,SCENARIO_FULL_DEMO,"1. FULL DEMO  —  Kalkış • bulut geçişi • deniz alçak uçuş • orbit • yaklaşma • iniş  |  ODAK: tam sistem turu");
        addScenario(root,cockpitTarget,SCENARIO_NIGHT_LANDING,"2. NIGHT LANDING  —  RWY 27 • gece • PAPI/HUD • centerline • flare/sink-rate  |  ODAK: hassas iniş");
        addScenario(root,cockpitTarget,SCENARIO_LOW_VIS_RAIN,"3. LOW VIS / RAIN  —  Yağmur + sis • düşük RVR • ıslak pist • ILS/PAPI • frenleme  |  ODAK: kötü hava yaklaşması");
        addScenario(root,cockpitTarget,SCENARIO_MOUNTAIN_PASS,"4. MOUNTAIN PASS  —  Vadi/geçit • terrain clearance • yüksek hız • enerji/G yönetimi  |  ODAK: arazi manevrası");
        addScenario(root,cockpitTarget,SCENARIO_SEA_LOW_LEVEL,"5. SEA LOW LEVEL  —  Deniz üstü çok alçak • yüksek hız • görsel akış • pull-up  |  ODAK: alçak irtifa enerji yönetimi");
        addScenario(root,cockpitTarget,SCENARIO_EMERGENCY_RETURN,"6. EMERGENCY RETURN  —  Kısa geri dönüş • pist yakalama • öncelikli ATC • güvenli iniş  |  ODAK: acil durum toparlama");
'''
new='''        addScenario(root,cockpitTarget,SCENARIO_TOROSLAR,"1. TOROSLAR  —  Yüksek dağ sırtları • vadi/geçit • terrain clearance • yüksek hızlı manevra  |  ODAK: dağ uçuşu");
        addScenario(root,cockpitTarget,SCENARIO_EGE,"2. EGE  —  Kıyı hattı • deniz ve adalar • alçak irtifa • açık görüş  |  ODAK: kıyı/deniz uçuşu");
        addScenario(root,cockpitTarget,SCENARIO_PATARA,"3. PATARA  —  Kumullar • Akdeniz kıyısı • alçak geçiş • deniz-kara geçişi  |  ODAK: sahil uçuşu");
'''
l=rep(l,old,new,'three Türkiye scenario rows')
LAUNCHER.write_text(l)

# ---------------------------------------------------------------------------
# Make region selection functional, not just a label. Existing automatic long
# journey remains available when reset() is used; resetForScenario() locks one
# of the existing TOROS / AEGEAN / PATARA stages for the selected mission.
# ---------------------------------------------------------------------------
j=JOURNEY.read_text()
j=rep(j,
'    private static volatile int stage=RUNWAY;\n',
'    private static volatile int stage=RUNWAY;\n    private static volatile int lockedScenarioStage=-1;\n','locked scenario field')

j=rep(j,
'    public static synchronized void reset(){airborneSec=0;progress01=0;stage=RUNWAY;flightPhase="RUNWAY_HOLD";}\n',
'''    public static synchronized void reset(){lockedScenarioStage=-1;resetProgress();}
    public static synchronized void resetForScenario(int scenarioId){lockedScenarioStage=scenarioStageForId(scenarioId);resetProgress();}
    private static void resetProgress(){airborneSec=0;progress01=0;stage=RUNWAY;flightPhase="RUNWAY_HOLD";}
    public static int scenarioStageForId(int scenarioId){
        if(scenarioId==1)return TOROS;
        if(scenarioId==2)return AEGEAN;
        if(scenarioId==3)return PATARA;
        return -1;
    }
    public static int getLockedScenarioStage(){return lockedScenarioStage;}
''','scenario reset/configure')

j=rep(j,
'''        airborneSec+=dt;
        progress01=clamp(airborneSec/112.0,0,1);
        if(airborneSec<13)stage=TOROS;
''',
'''        airborneSec+=dt;
        if(lockedScenarioStage>=0){
            stage=lockedScenarioStage;
            progress01=clamp(airborneSec/60.0,0,1);
            return;
        }
        progress01=clamp(airborneSec/112.0,0,1);
        if(airborneSec<13)stage=TOROS;
''','locked region update')

j=rep(j,
'''    public static float getStageBlend01(){
        double t=airborneSec,a=0,b=1;
''',
'''    public static float getStageBlend01(){
        if(lockedScenarioStage>=0&&stage==lockedScenarioStage)return (float)clamp(airborneSec/28.0,0,1);
        double t=airborneSec,a=0,b=1;
''','locked region blend')
JOURNEY.write_text(j)

# ---------------------------------------------------------------------------
# Runtime reads the selected region before the journey reset. Crash/reset keeps
# the same selected region. Aircraft geometry/camera code is not modified here.
# ---------------------------------------------------------------------------
r=RUNTIME.read_text()
r=rep(r,
'    private boolean demoMode=true,localManual,crashed,freeNavSeeded,hangarDeparted=true,autoRecovery;\n',
'    private boolean demoMode=true,localManual,crashed,freeNavSeeded,hangarDeparted=true,autoRecovery;\n    private int selectedScenarioId=LauncherActivity.SCENARIO_TOROSLAR;\n','selected scenario field')

r=rep(r,
'        demoMode=getIntent()==null||getIntent().getBooleanExtra(LauncherActivity.EXTRA_DEMO_MODE,true);\n',
'        demoMode=getIntent()==null||getIntent().getBooleanExtra(LauncherActivity.EXTRA_DEMO_MODE,true);\n        if(getIntent()!=null)selectedScenarioId=getIntent().getIntExtra(LauncherActivity.EXTRA_SCENARIO_ID,LauncherActivity.SCENARIO_TOROSLAR);\n','read scenario intent')

r=rep(r,
'        mission.reset(state);com.mg.fixturecockpitsim.sim.CinematicJourneyState.reset();controls.gearDown=true;hangarDeparted=true;\n',
'        mission.reset(state);com.mg.fixturecockpitsim.sim.CinematicJourneyState.resetForScenario(selectedScenarioId);controls.gearDown=true;hangarDeparted=true;\n','initial scenario journey reset')

r=rep(r,
'mission.reset(state);controls.gearDown=true;localManual=false;',
'mission.reset(state);com.mg.fixturecockpitsim.sim.CinematicJourneyState.resetForScenario(selectedScenarioId);controls.gearDown=true;localManual=false;',
'crash reset keeps scenario')
RUNTIME.write_text(r)

# ---------------------------------------------------------------------------
# Tests: selected button id must select the corresponding existing 3D region.
# ---------------------------------------------------------------------------
TEST.mkdir(parents=True,exist_ok=True)
(TEST/'CinematicJourneyScenarioTest.java').write_text(r'''package com.mg.fixturecockpitsim.sim;

import org.junit.Test;
import static org.junit.Assert.*;

public class CinematicJourneyScenarioTest {
    private static void airborne(){CinematicJourneyState.update(170,.05,false,"ROTATE_CLIMB");}

    @Test public void torosScenarioLocksTorosTerrain(){
        CinematicJourneyState.resetForScenario(1);airborne();
        assertEquals(CinematicJourneyState.TOROS,CinematicJourneyState.getStage());
        for(int i=0;i<1600;i++)CinematicJourneyState.update(190,.05,false,"ORBIT");
        assertEquals(CinematicJourneyState.TOROS,CinematicJourneyState.getStage());
        CinematicJourneyState.reset();
    }

    @Test public void egeScenarioLocksAegeanTerrain(){
        CinematicJourneyState.resetForScenario(2);airborne();
        assertEquals(CinematicJourneyState.AEGEAN,CinematicJourneyState.getStage());
        assertEquals(CinematicJourneyState.AEGEAN,CinematicJourneyState.getLockedScenarioStage());
        CinematicJourneyState.reset();
    }

    @Test public void pataraScenarioLocksPataraTerrain(){
        CinematicJourneyState.resetForScenario(3);airborne();
        assertEquals(CinematicJourneyState.PATARA,CinematicJourneyState.getStage());
        assertTrue(CinematicJourneyState.desert01()>.9f);
        CinematicJourneyState.reset();
    }

    @Test public void approachStillTransitionsToReturnScene(){
        CinematicJourneyState.resetForScenario(2);airborne();
        CinematicJourneyState.update(120,.05,false,"APPROACH");
        assertEquals(CinematicJourneyState.RETURN,CinematicJourneyState.getStage());
        CinematicJourneyState.reset();
    }

    @Test public void plainResetKeepsLegacyAutomaticJourneyAvailable(){
        CinematicJourneyState.reset();
        assertEquals(-1,CinematicJourneyState.getLockedScenarioStage());
        for(int i=0;i<18*20;i++)CinematicJourneyState.update(170,.05,false,"ORBIT");
        assertEquals(CinematicJourneyState.AEGEAN,CinematicJourneyState.getStage());
    }
}
''')

# Version only; no aircraft renderer edits in v111.
g=GRADLE.read_text()
g=re.sub(r'versionCode\s+\d+','versionCode 111',g,count=1)
g=re.sub(r"versionName\s+['\"][^'\"]+['\"]","versionName '26.29-avm36.2-toros-ege-patara-old-aircraft'",g,count=1)
GRADLE.write_text(g)

print('v111 applied: Toroslar/Ege/Patara selectable 3D regions; v107 aircraft presentation preserved')
