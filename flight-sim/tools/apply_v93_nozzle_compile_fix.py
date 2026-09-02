from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REAL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/RealisticFighterMesh.java'

r=REAL.read_text()

# RealisticFighterMesh deliberately has a small primitive set. Keep the v93
# nozzle geometry inside that proven set instead of importing the heavier
# AdvancedAirframeOverlay helpers.
r=r.replace(
'''            double edge=c+half;
            float ex0=x+(r0+.012f)*(float)Math.cos(edge),ey0=-.10f+(r0+.012f)*ys*(float)Math.sin(edge);
            float ex1=x+(r1+.008f)*(float)Math.cos(edge),ey1=-.10f+(r1+.008f)*ys*(float)Math.sin(edge);
            cylinderBetween(ex0,ey0,z0,ex1,ey1,z1,.0075f,6);
''',
'''            // The deliberate angular overlap itself forms the visible petal seam.
''')

r=r.replace(
'''        ellipticRing(x,-.10f,3.63f,.285f,.60f,.014f,36);
        ellipticRing(x,-.10f,3.82f,.252f,.60f,.012f,36);
        ellipticRing(x,-.10f,4.00f,.218f,.60f,.011f,36);
        ellipsoid(x,-.10f,4.03f,.044f,.030f,.060f,14,8);
        for(int i=0;i<10;i++){
            double a=2*Math.PI*i/10.0;float ca=(float)Math.cos(a),sa=(float)Math.sin(a);
            cylinderBetween(x+.050f*ca,-.10f+.050f*.60f*sa,4.02f,x+.205f*ca,-.10f+.205f*.60f*sa,4.02f,.0065f,6);
        }
''',
'''        // Short coaxial liner bands create visible depth without extra helper primitives.
        tubeSurface(x,-.10f,3.620f,3.640f,.285f,.285f,.60f,36);
        tubeSurface(x,-.10f,3.810f,3.830f,.252f,.252f,.60f,36);
        tubeSurface(x,-.10f,3.990f,4.010f,.218f,.218f,.60f,36);
        // Flame-holder/spider hardware is rendered by AdvancedAirframeOverlay.
''')

REAL.write_text(r)
print('v93 nozzle compile fix applied: overlap seams retained; liner bands use tubeSurface; detailed actuators/spider remain in AdvancedAirframeOverlay')
