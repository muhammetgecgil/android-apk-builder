package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class AdvancedCalculationEngineTest {
    private static double numberAfter(String body,String label){int i=body.indexOf(label+":");if(i<0)throw new AssertionError("Missing "+label+" in "+body);int s=i+label.length()+1;while(s<body.length()&&body.charAt(s)==' ')s++;int e=s;while(e<body.length()){char c=body.charAt(e);if((c>='0'&&c<='9')||c=='-'||c=='+'||c=='.'||c=='e'||c=='E')e++;else break;}return Double.parseDouble(body.substring(s,e));}
    private static void near(double exp,double act,double rel){double scale=Math.max(1.0,Math.abs(exp));assertTrue("expected="+exp+" actual="+act,Math.abs(exp-act)<=rel*scale);}

    @Test public void boltLoadFractionUsesStiffnessRatio(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(0,new double[]{10000,6000,100000,300000,600,100});
        near(0.25,numberAfter(r.body,"Yük paylaşım katsayısı Φ"),1e-4);
        near(11500,numberAfter(r.body,"Civata toplam yükü"),1e-4);
        near(5500,numberAfter(r.body,"Kalan sıkma kuvveti"),1e-4);
    }

    @Test public void circularBoltGroupMomentLoadMatchesMrSum(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(1,new double[]{0,1000,4,50,50,120});
        near(5000,numberAfter(r.body,"Moment yükü/civata"),1e-4);
        near(100,numberAfter(r.body,"Kritik kayma"),1e-4);
    }

    @Test public void shaftReactionsCloseStaticBalance(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(2,new double[]{1000,400,1000,500,40,300});
        double ay=numberAfter(r.body,"A teğetsel reaksiyon");
        double by=numberAfter(r.body,"B teğetsel reaksiyon");
        double az=numberAfter(r.body,"A radyal reaksiyon");
        double bz=numberAfter(r.body,"B radyal reaksiyon");
        near(1000,ay+by,1e-4); near(500,az+bz,1e-4);
    }

    @Test public void helicalGearTangentialForceIsTwoTOverD(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(3,new double[]{100,100,20,15,0,0});
        near(2000,numberAfter(r.body,"Teğetsel Ft"),1e-4);
        assertTrue(numberAfter(r.body,"Radyal Fr")>0); assertTrue(numberAfter(r.body,"Eksenel Fa")>0);
    }

    @Test public void bearingPairUsesShorterLife(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(4,new double[]{10000,5000,12000,4000,1000,3});
        near(133.3333,numberAfter(r.body,"Rulman-1 L10h"),2e-4);
        near(133.3333,numberAfter(r.body,"Yöneten ömür"),2e-4);
    }

    @Test public void pressFitTorqueMatchesFrictionRadius(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(5,new double[]{20,40,50,0.15,80,250});
        double area=Math.PI*40*50; double expected=0.15*20*area*20/1000.0;
        near(expected,numberAfter(r.body,"Aktarılabilir tork"),2e-4);
    }

    @Test public void hollowShaftPropertiesArePositive(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(6,new double[]{100,50,40,20,300,0});
        assertTrue(numberAfter(r.body,"I")>0); assertTrue(numberAfter(r.body,"J")>0); assertTrue(numberAfter(r.body,"von Mises")>0);
    }

    @Test public void marinGoodmanAppliesModifiers(){
        AdvancedCalculationEngine.Result r=AdvancedCalculationEngine.calculate(7,new double[]{300,0.8,0.9,1.0,100,0.1});
        near(216,numberAfter(r.body,"Düzeltilmiş Se"),1e-4);
        double expected=1.0/(100.0/216.0+0.1); near(expected,numberAfter(r.body,"Yorulma FoS"),2e-4);
    }
}
