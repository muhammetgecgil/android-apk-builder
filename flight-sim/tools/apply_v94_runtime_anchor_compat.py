from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'

r=RUNTIME.read_text()
old='''        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);lightingOverlay=new FighterLightingOverlayView(this,lighting);lightingOverlay.bindAircraft(jet);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(lightingOverlay,new FrameLayout.LayoutParams(-1,-1));\n'''
new='''        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        lightingOverlay=new FighterLightingOverlayView(this,lighting);lightingOverlay.bindAircraft(jet);\n        root.addView(lightingOverlay,new FrameLayout.LayoutParams(-1,-1));\n'''
if old in r:
    r=r.replace(old,new,1)
elif new not in r:
    raise SystemExit('v94 runtime compat: v91 lighting initialization layout not found')
RUNTIME.write_text(r)
print('v94 runtime compat applied: preserved v91 lighting while exposing stable v94 world/weather/jet anchor')
