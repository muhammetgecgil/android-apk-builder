from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
r=RUNTIME.read_text()

# v89 already introduced localSpeedBrake. v100 owns the final cockpit-aware
# declaration/command wiring, so temporarily normalize only the exact anchors
# that v100 replaces. No flight behavior is changed by this compatibility pass.
pairs=[
('    private double localThrottle=.10,localBrake,localSpeedBrake,localYawHold;\n',
 '    private double localThrottle=.10,localBrake,localYawHold;\n'),
('            controls.throttle=localThrottle;controls.brake=state.onGround?localBrake:0;controls.speedBrake=state.onGround?0:localSpeedBrake;controls.gearDown=localGearDown;\n',
 '            controls.throttle=localThrottle;controls.brake=localBrake;controls.gearDown=localGearDown;\n'),
('            localBrake=0;localSpeedBrake=0;localYawHold=0;imuRoll=imuPitch=imuYaw=0;seedFreeNavigation();autoRecovery=true;autoRecoveryStableSec=0;\n',
 '            localBrake=0;localYawHold=0;imuRoll=imuPitch=imuYaw=0;seedFreeNavigation();autoRecovery=true;autoRecoveryStableSec=0;\n'),
]
for old,new in pairs:
    if old in r:r=r.replace(old,new,1)
    elif new not in r:raise SystemExit('v100 anchor compat: expected v89 runtime anchor not found')
RUNTIME.write_text(r)
print('v100 anchor compat applied: v89 speed-brake anchors normalized for cockpit integration')
