package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class AssemblyCalculationEngineTest {
    private static double numberAfter(String body,String label){int i=body.indexOf(label+":");if(i<0)throw new AssertionError("Missing "+label+" in "+body);int s=i+label.length()+1;while(s<body.length()&&body.charAt(s)==' ')s++;int e=s;while(e<body.length()){char c=body.charAt(e);if((c>='0'&&c<='9')||c=='-'||c=='+'||c=='.'||c=='e'||c=='E')e++;else break;}return Double.parseDouble(body.substring(s,e));}
    private static void near(double exp,double act,double rel){double scale=Math.max(1.0,Math.abs(exp));assertTrue("expected="+exp+" actual="+act,Math.abs(exp-act)<=rel*scale);}

    @Test public void steppedShaftReactionsCloseBalance(){
        AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(0,new double[]{1000,300,1000,700,500,40});
        double ra=numberAfter(r.body,"RA"),rb=numberAfter(r.body,"RB");
        near(1500,ra+rb,1e-4); assertTrue(numberAfter(r.body,"Nominal eğilme")>0);
    }

    @Test public void boltGroupZeroMomentSharesDirectLoad(){
        AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(1,new double[]{4000,0,0,50,10,300});
        near(1000,numberAfter(r.body,"Kritik civata yükü"),1e-4);
    }

    @Test public void locatingBearingTakesAxialLoad(){
        AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(2,new double[]{1000,2000,2000,10000,10000,1000});
        assertTrue(numberAfter(r.body,"P_A")>numberAfter(r.body,"P_B"));
        assertTrue(numberAfter(r.body,"L10h A")<numberAfter(r.body,"L10h B"));
    }

    @Test public void selectorReturnsPassingPreferredDiameter(){
        AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(3,new double[]{100,50,300,1.5,10,100});
        double d=numberAfter(r.body,"Seçilen çap");
        assertTrue(d>=10&&d<=100); assertTrue(numberAfter(r.body,"FoS")>=1.5);
    }

    @Test public void rankingFindsLowestFos(){
        AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(4,new double[]{2.0,1.8,1.2,2.2,1.7,1.9});
        assertTrue(r.body.contains("KRİTİK: Rulman"));
        assertTrue(r.status.contains("SINIRDA")||r.status.contains("UYGUN"));
    }
}
