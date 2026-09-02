from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
RUNTIME=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/FlightRuntimeActivity.java'
JET=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/Jet3DView.java'

# v91 initializes the lighting overlay on the same line as world/weather/jet.
# Move only that initialization after the proven 3-view block so the v94 patch can
# add its supersonic layer without losing or duplicating the v91 lighting overlay.
r=RUNTIME.read_text()
old='''        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);lightingOverlay=new FighterLightingOverlayView(this,lighting);lightingOverlay.bindAircraft(jet);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(lightingOverlay,new FrameLayout.LayoutParams(-1,-1));\n'''
new='''        world=new AirfieldWorldView(this);weather=new WeatherEffectsView(this);jet=new Jet3DView(this);\n        root.addView(world,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(weather,new FrameLayout.LayoutParams(-1,-1));\n        root.addView(jet,new FrameLayout.LayoutParams(-1,-1));\n        lightingOverlay=new FighterLightingOverlayView(this,lighting);lightingOverlay.bindAircraft(jet);\n        root.addView(lightingOverlay,new FrameLayout.LayoutParams(-1,-1));\n'''
if old in r:
    r=r.replace(old,new,1)
elif new not in r:
    raise SystemExit('v94 runtime compat: v91 lighting initialization layout not found')
RUNTIME.write_text(r)

# v91 inserted getLightingAnchors between getCameraMode and onTouchEvent. Reorder
# that one method so v94 can replace the camera-mode block atomically, then the
# lighting-anchor API remains immediately after it.
j=JET.read_text()
oldj='''    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}\n    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(3,m));}\n    public int getCameraMode(){return r.cam;}\n    public boolean getLightingAnchors(float[] out){return r.copyLightingAnchors(out);}\n    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%4;return true;}\n'''
newj='''    public void setWheelSpeed(float v){r.ws=Math.max(0,v);}\n    public void setCameraMode(int m){r.cam=Math.max(0,Math.min(3,m));}\n    public int getCameraMode(){return r.cam;}\n    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_UP)r.cam=(r.cam+1)%4;return true;}\n    public boolean getLightingAnchors(float[] out){return r.copyLightingAnchors(out);}\n'''
if oldj in j:
    j=j.replace(oldj,newj,1)
elif newj not in j:
    raise SystemExit('v94 runtime compat: v91 Jet3DView camera/lighting anchor layout not found')
JET.write_text(j)
print('v94 anchor compat applied: v91 lighting preserved; stable runtime layering and camera blocks exposed')
