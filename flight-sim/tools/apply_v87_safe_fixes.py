from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
REAL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/RealisticFighterMesh.java'
ADV=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/AdvancedAirframeOverlay.java'
ORD=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/VisualOrdnanceMesh.java'


def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v87 safe patch anchor missing: {label}')
    return text.replace(old,new,1)

# Runtime: keep the proven v85 renderer, remove the long startup Toast and add Java-only runway final capture.
r=RUNTIME.read_text()
r=rep(r,
'''        String imuText=rotationSensor==null?"IMU sensörü bulunamadı — AUTO/BT kullanılabilir.":"MANUEL IMU: havada telefon yatışının yaklaşık 2 katına kadar bank; güvenli sınır ±64°.";\n        Toast.makeText(this,"Kalkış ivmelenmesi kademeli. HUD arka planı kaldırıldı. "+imuText,Toast.LENGTH_LONG).show();\n''',
'''        // v87: no blocking/confusing startup Toast; the flight screen opens immediately.\n''','startup toast')
r=rep(r,
'else if(demoMode){mission.update(state,controls,dt);hangarDeparted=true;}',
'else if(demoMode){mission.update(state,controls,dt);hangarDeparted=true;if(isDemoLandingPhase())applyDemoRunwayFinalCapture(dt);}',
'demo final hook')
method='''    private boolean isDemoLandingPhase(){\n        AutonomousFlightMission.Phase p=mission.getPhase();\n        return p==AutonomousFlightMission.Phase.APPROACH||p==AutonomousFlightMission.Phase.FLARE||p==AutonomousFlightMission.Phase.ROLLOUT||p==AutonomousFlightMission.Phase.TAXI_IN;\n    }\n\n    private void applyDemoRunwayFinalCapture(double dt){\n        seedFreeNavigation();\n        // Geometric final director: converge cross-track before allowing the aircraft to reach the ground.\n        double rate=state.altitudeM<180?2.5:1.35;\n        double blend=1.0-Math.exp(-dt*rate);\n        runwayCrossTrackM+=(0-runwayCrossTrackM)*blend;\n        double correction=clampd(runwayCrossTrackM*.62,-42,42);\n        double target=RUNWAY_HDG-correction;\n        double err=wrap180(target-state.headingDeg);\n        controls.roll=clampd(controls.roll+err/52.0-runwayCrossTrackM/1700.0,-.34,.34);\n        controls.yaw=clampd(controls.yaw+err*.026-runwayCrossTrackM*.0018,-.55,.55);\n        controls.gearDown=true;\n        if(state.altitudeM<145&&Math.abs(runwayCrossTrackM)>70){\n            controls.pitch=Math.max(controls.pitch,.055);\n            controls.throttle=Math.max(controls.throttle,.31);\n        }\n        if(state.altitudeM<26&&Math.abs(runwayCrossTrackM)<40)runwayCrossTrackM*=Math.exp(-dt*5.0);\n        controls.clamp();\n    }\n\n'''
r=rep(r,'    private void applyAutoRunwayRecovery(double dt){\n',method+'    private void applyAutoRunwayRecovery(double dt){\n','final director method')
RUNTIME.write_text(r)

# Main mesh: gear-only hardware must use a gear part ID, never DETAIL, so it retracts completely.
x=REAL.read_text()
x=rep(x,
'        b.part=DETAIL; b.airframeDetails(); b.gearDetails();\n',
'        b.part=DETAIL; b.airframeDetails();\n        b.part=GEAR_STRUT; b.gearDetails();\n',
'main gear detail tagging')
REAL.write_text(x)

# Advanced overlay: calipers/hoses were tagged DETAIL and remained below the aircraft after gear-up.
a=ADV.read_text()
a=rep(a,
'            part=DETAIL;\n            box(x+.13f*s,-1.65f,1.06f,.045f,.070f,.16f); // caliper block\n            cylinderBetween(x-.04f*s,-1.15f,.84f,x-.04f*s,-1.49f,1.12f,.010f,7); // hose\n',
'            part=GEAR_STRUT;\n            box(x+.13f*s,-1.65f,1.06f,.045f,.070f,.16f); // caliper block retracts with gear\n            cylinderBetween(x-.04f*s,-1.15f,.84f,x-.04f*s,-1.49f,1.12f,.010f,7); // hose retracts with gear\n',
'advanced gear residue tagging')
ADV.write_text(a)

# Stores: move wing stations to the real lower-wing envelope instead of leaving a visual air gap.
o=ORD.read_text()
o=rep(o,
'''        b.station(-1.72f,-.30f,-.42f,1.00f);\n        b.station( 1.72f,-.30f,-.42f,1.00f);\n        b.station(-2.82f,-.245f,-.02f,.83f);\n        b.station( 2.82f,-.245f,-.02f,.83f);\n''',
'''        b.station(-1.72f,.14f,-.42f,1.00f);\n        b.station( 1.72f,.14f,-.42f,1.00f);\n        b.station(-2.82f,.13f,-.02f,.83f);\n        b.station( 2.82f,.13f,-.02f,.83f);\n''','wing store attachment height')
ORD.write_text(o)

print('v87 safe fixes applied: immediate launch, runway final, clean gear, attached stores')
