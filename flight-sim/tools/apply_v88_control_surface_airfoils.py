from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
REAL=ROOT/'app/src/main/java/com/mg/fixturecockpitsim/visual/RealisticFighterMesh.java'


def rep(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v88 control-airfoil patch anchor missing: {label}')
    return text.replace(old, new, 1)

r=REAL.read_text()

# Remove the fixed-wing geometry from the leading-edge-flap volume so the movable
# surface is physically separate instead of being drawn over a duplicate wing skin.
r=rep(r,
'''    private void fixedWing(float side){\n        wingSurface(side,0f,1f,0f);\n    }\n''',
'''    private void fixedWing(float side){\n        // v88 control-airfoil pass: leave a real LE-flap cavity in the main wing.\n        wingSurface(side,.19f,1f,0f);\n    }\n''','main wing LE cavity')

r=rep(r,
'''    /** Independent leading-edge flap shell. It sits flush at neutral and exposes the hinge cavity when deflected. */\n    private void leadingEdgeFlap(float side){\n        wingSurface(side,0f,.175f,.006f);\n    }\n''',
'''    /** Independent leading-edge flap with its own rounded-nose / thin-tail airfoil. */\n    private void leadingEdgeFlap(float side){\n        horizontalControlAirfoil(side,\n                new float[]{.82f,1.30f,2.26f,3.38f,4.47f,5.24f},\n                new float[]{-2.72f,-2.48f,-1.94f,-1.30f,-.66f,-.18f},\n                new float[]{-2.14f,-1.78f,-1.32f,-.86f,-.46f,-.12f},\n                new float[]{.340f,.330f,.300f,.255f,.210f,.170f},\n                new float[]{.070f,.066f,.056f,.044f,.030f,.012f},.006f);\n    }\n''','LE flap airfoil')

r=rep(r,
'''    private void flaperon(float side){\n        // Small real hinge gap is left ahead of the flaperon; the cavity is generated separately.\n        airfoil(new float[][]{{1.28f*side,.795f},{2.24f*side,.795f},{3.52f*side,.755f},{4.48f*side,.555f},{4.15f*side,1.12f},{3.22f*side,1.40f},{1.40f*side,1.48f}},.245f,.052f);\n    }\n''',
'''    private void flaperon(float side){\n        // True tapered control-surface section: rounded nose, light camber and knife-edge tail.\n        horizontalControlAirfoil(side,\n                new float[]{1.30f,2.25f,3.55f,4.48f},\n                new float[]{.80f,.80f,.76f,.56f},\n                new float[]{1.46f,1.40f,1.18f,.70f},\n                new float[]{.245f,.235f,.215f,.180f},\n                new float[]{.052f,.048f,.038f,.022f},.0045f);\n    }\n''','flaperon airfoil')

r=rep(r,
'''    private void stabilator(float side){\n        airfoil(new float[][]{{.68f*side,1.70f},{1.38f*side,1.86f},{2.98f*side,2.28f},{2.56f*side,3.27f},{1.36f*side,3.09f},{.70f*side,2.87f}},.29f,.065f);\n    }\n''',
'''    private void stabilator(float side){\n        // All-moving tail uses a nearly symmetric thin airfoil appropriate to a stabilator.\n        horizontalControlAirfoil(side,\n                new float[]{.70f,1.36f,2.20f,2.95f},\n                new float[]{1.72f,1.86f,2.08f,2.28f},\n                new float[]{2.86f,3.08f,3.25f,3.28f},\n                new float[]{.300f,.290f,.250f,.205f},\n                new float[]{.065f,.060f,.045f,.025f},0f);\n    }\n''','stabilator airfoil')

r=rep(r,
'''    private void rudder(float side){\n        finVolume(side,new float[][]{{2.43f,2.56f},{.83f,2.73f},{.56f,3.14f},{1.18f,3.00f},{2.34f,2.72f}},.065f);\n    }\n''',
'''    private void rudder(float side){\n        // Symmetric vertical-tail control airfoil; thickness tapers toward both ends.\n        verticalControlAirfoil(side,\n                new float[]{.62f,.88f,1.20f,1.58f,1.98f,2.34f},\n                new float[]{3.10f,2.78f,2.68f,2.60f,2.56f,2.56f},\n                new float[]{3.14f,3.08f,3.00f,2.89f,2.78f,2.69f},\n                new float[]{.028f,.045f,.060f,.065f,.055f,.035f});\n    }\n''','rudder airfoil')

# Add reusable high-resolution control-surface loft helpers immediately before the
# existing finVolume helper. These use a NACA-like thickness distribution whose
# thickness is exactly zero at the trailing edge.
anchor='''    private void finVolume(float side,float[][] yz,float halfThick){\n'''
helpers='''    private void horizontalControlAirfoil(float side,float[] sx,float[] le,float[] te,float[] yc,float[] ht,float camber){\n        final float[] q={0f,.010f,.025f,.050f,.085f,.130f,.190f,.270f,.370f,.490f,.610f,.720f,.815f,.890f,.945f,.980f,1f};\n        int n=Math.min(sx.length,Math.min(le.length,Math.min(te.length,Math.min(yc.length,ht.length))));\n        for(int s=0;s<n-1;s++)for(int i=0;i<q.length-1;i++){\n            V A=controlHV(side,sx[s],le[s],te[s],yc[s],ht[s],camber,q[i],true);\n            V B=controlHV(side,sx[s+1],le[s+1],te[s+1],yc[s+1],ht[s+1],camber,q[i],true);\n            V C=controlHV(side,sx[s+1],le[s+1],te[s+1],yc[s+1],ht[s+1],camber,q[i+1],true);\n            V D=controlHV(side,sx[s],le[s],te[s],yc[s],ht[s],camber,q[i+1],true);\n            quadSmooth(A,B,C,D);\n            V a=controlHV(side,sx[s],le[s],te[s],yc[s],ht[s],camber,q[i],false);\n            V d=controlHV(side,sx[s],le[s],te[s],yc[s],ht[s],camber,q[i+1],false);\n            V c=controlHV(side,sx[s+1],le[s+1],te[s+1],yc[s+1],ht[s+1],camber,q[i+1],false);\n            V b=controlHV(side,sx[s+1],le[s+1],te[s+1],yc[s+1],ht[s+1],camber,q[i],false);\n            quadSmooth(a,d,c,b);\n        }\n        // Close only the spanwise ends. Nose/tail meet naturally through the zero-thickness law.\n        for(int end:new int[]{0,n-1})for(int i=0;i<q.length-1;i++){\n            V A=controlHV(side,sx[end],le[end],te[end],yc[end],ht[end],camber,q[i],true);\n            V B=controlHV(side,sx[end],le[end],te[end],yc[end],ht[end],camber,q[i+1],true);\n            V C=controlHV(side,sx[end],le[end],te[end],yc[end],ht[end],camber,q[i+1],false);\n            V D=controlHV(side,sx[end],le[end],te[end],yc[end],ht[end],camber,q[i],false);\n            quad(new float[]{A.x,A.y,A.z},new float[]{B.x,B.y,B.z},new float[]{C.x,C.y,C.z},new float[]{D.x,D.y,D.z});\n        }\n    }\n\n    private V controlHV(float side,float span,float le,float te,float yc,float ht,float camber,float f,boolean top){\n        f=Math.max(0f,Math.min(1f,f));\n        float z=le+(te-le)*f,sg=top?1f:-1f;\n        float c=camber*4f*f*(1f-f);\n        float y=yc+c+sg*ht*controlThickness(f);\n        float e=.002f,fa=Math.max(0f,f-e),fb=Math.min(1f,f+e);\n        float ya=camber*4f*fa*(1f-fa)+sg*ht*controlThickness(fa);\n        float yb=camber*4f*fb*(1f-fb)+sg*ht*controlThickness(fb);\n        float dydz=(yb-ya)/Math.max(.0005f,(te-le)*(fb-fa));\n        return new V(span*side,y,z,0f,sg,-sg*dydz);\n    }\n\n    private void verticalControlAirfoil(float side,float[] ys,float[] le,float[] te,float[] ht){\n        final float[] q={0f,.012f,.030f,.060f,.100f,.155f,.225f,.315f,.420f,.535f,.650f,.755f,.845f,.915f,.965f,1f};\n        int n=Math.min(ys.length,Math.min(le.length,Math.min(te.length,ht.length)));\n        for(int s=0;s<n-1;s++)for(int i=0;i<q.length-1;i++){\n            V A=controlVV(side,ys[s],le[s],te[s],ht[s],q[i],true);\n            V B=controlVV(side,ys[s+1],le[s+1],te[s+1],ht[s+1],q[i],true);\n            V C=controlVV(side,ys[s+1],le[s+1],te[s+1],ht[s+1],q[i+1],true);\n            V D=controlVV(side,ys[s],le[s],te[s],ht[s],q[i+1],true);quadSmooth(A,B,C,D);\n            V a=controlVV(side,ys[s],le[s],te[s],ht[s],q[i],false);\n            V d=controlVV(side,ys[s],le[s],te[s],ht[s],q[i+1],false);\n            V c=controlVV(side,ys[s+1],le[s+1],te[s+1],ht[s+1],q[i+1],false);\n            V b=controlVV(side,ys[s+1],le[s+1],te[s+1],ht[s+1],q[i],false);quadSmooth(a,d,c,b);\n        }\n    }\n\n    private V controlVV(float side,float y,float le,float te,float ht,float f,boolean face){\n        f=Math.max(0f,Math.min(1f,f));float z=le+(te-le)*f,sg=face?1f:-1f;\n        float center=.96f*side+side*(y-.50f)*.17f;\n        float x=center+sg*ht*controlThickness(f);\n        float e=.002f,fa=Math.max(0f,f-e),fb=Math.min(1f,f+e);\n        float xa=sg*ht*controlThickness(fa),xb=sg*ht*controlThickness(fb);\n        float dxdz=(xb-xa)/Math.max(.0005f,(te-le)*(fb-fa));\n        return new V(x,y,z,sg,0f,-sg*dxdz);\n    }\n\n    private float controlThickness(float x){\n        x=Math.max(0f,Math.min(1f,x));\n        // Closed-tail NACA-like distribution: rounded nose, max thickness near 30% chord, zero tail.\n        float t=5f*(.2969f*(float)Math.sqrt(x)-.1260f*x-.3516f*x*x+.2843f*x*x*x-.1036f*x*x*x*x);\n        return Math.max(0f,Math.min(1.04f,2.03f*t));\n    }\n\n'''
r=rep(r,anchor,helpers+anchor,'control airfoil helpers')
REAL.write_text(r)
print('v88 control-surface airfoils applied: LE flap, flaperon, stabilator and rudder')
