package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class CustomGeometryEngineTest {
    private static double after(String body,String label){int i=body.indexOf(label+":");if(i<0)throw new AssertionError(label);int s=i+label.length()+1;while(s<body.length()&&body.charAt(s)==' ')s++;int e=s;while(e<body.length()){char c=body.charAt(e);if((c>='0'&&c<='9')||c=='-'||c=='+'||c=='.'||c=='e'||c=='E')e++;else break;}return Double.parseDouble(body.substring(s,e));}
    private static void near(double a,double b,double rel){assertTrue(Math.abs(a-b)<=rel*Math.max(1.0,Math.abs(a)));}

    @Test public void customShaftClosesReactionBalance(){
        CustomGeometryEngine.Result r=CustomGeometryEngine.shaft("200,1000,0\n600,500,400",1000,40,300,50);
        near(1500,after(r.body,"Ay")+after(r.body,"By"),1e-4);
        near(400,after(r.body,"Az")+after(r.body,"Bz"),1e-4);
        assertTrue(after(r.body,"von Mises")>0);
    }

    @Test public void symmetricBoltGroupFindsFiniteCriticalLoad(){
        String pts="-50,-40\n50,-40\n50,40\n-50,40";
        CustomGeometryEngine.Result r=CustomGeometryEngine.boltGroup(pts,1000,500,200,10,120);
        assertTrue(after(r.body,"Critical resultant")>0);
        assertTrue(after(r.body,"Bolt shear")>0);
    }

    @Test public void preferredShaftMeetsTarget(){
        CustomGeometryEngine.Result r=CustomGeometryEngine.selectPreferredShaft(200,100,300,2.0);
        assertTrue(after(r.body,"Achieved FoS")>=2.0);
        assertTrue(after(r.body,"Selected diameter")>0);
    }
}
