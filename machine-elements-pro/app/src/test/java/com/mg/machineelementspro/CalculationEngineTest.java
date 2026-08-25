package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class CalculationEngineTest {
    private static double numberAfter(String body, String label) {
        int i = body.indexOf(label + ":");
        if (i < 0) throw new AssertionError("Missing label: " + label + " in " + body);
        int s = i + label.length() + 1;
        while (s < body.length() && body.charAt(s) == ' ') s++;
        int e=s;
        while(e<body.length()){
            char c=body.charAt(e);
            if((c>='0'&&c<='9')||c=='-'||c=='+'||c=='.'||c=='e'||c=='E') e++; else break;
        }
        return Double.parseDouble(body.substring(s,e));
    }
    private static void near(double ex,double ac,double tol){ double sc=Math.max(1.0,Math.abs(ex)); assertTrue(Math.abs(ex-ac)<=tol*sc); }

    @Test public void bolt(){ CalculationEngine.Result r=CalculationEngine.calculate(0,new double[]{10000,0,10,250,0,0}); near(127.32395,numberAfter(r.body,"Çekme"),1e-4); }
    @Test public void shaft(){ CalculationEngine.Result r=CalculationEngine.calculate(1,new double[]{100,0,20,300,0,0}); near(127.32395,numberAfter(r.body,"Eğilme"),1e-4); }
    @Test public void bearing(){ CalculationEngine.Result r=CalculationEngine.calculate(2,new double[]{10000,5000,1000,3,0,0}); near(8.0,numberAfter(r.body,"L10"),1e-5); }
    @Test public void gear(){ CalculationEngine.Result r=CalculationEngine.calculate(3,new double[]{1000,20,2,0.3,300,0}); near(83.333333,numberAfter(r.body,"Diş dibi gerilmesi"),1e-4); }
    @Test public void springConsistency(){ CalculationEngine.Result r=CalculationEngine.calculate(4,new double[]{100,30,3,8,80000,700}); near(100,numberAfter(r.body,"k")*numberAfter(r.body,"Sehim"),2e-3); }
    @Test public void pinDoubleShear(){ CalculationEngine.Result a=CalculationEngine.calculate(8,new double[]{10000,10,5,1,200,300}); CalculationEngine.Result b=CalculationEngine.calculate(8,new double[]{10000,10,5,2,200,300}); near(numberAfter(a.body,"Pim kesme")/2,numberAfter(b.body,"Pim kesme"),1e-4); }
    @Test public void euler(){ CalculationEngine.Result r=CalculationEngine.calculate(9,new double[]{10000,200000,100000,1000,1,2}); double ex=Math.PI*Math.PI*200000*100000/1000000.0; near(ex,numberAfter(r.body,"Pcr"),2e-4); }
    @Test public void beam(){ CalculationEngine.Result r=CalculationEngine.calculate(10,new double[]{1000,1000,200000,1000000,10000,250}); near(250,numberAfter(r.body,"Mmax"),1e-5); near(25,numberAfter(r.body,"Gerilme"),1e-5); }
    @Test public void torsion(){ CalculationEngine.Result r=CalculationEngine.calculate(11,new double[]{100,1000,20,80000,5,0}); assertTrue(numberAfter(r.body,"Açı")>0); }
    @Test public void powerScrew(){ CalculationEngine.Result r=CalculationEngine.calculate(12,new double[]{10000,20,4,0.15,30,0.1}); assertTrue(numberAfter(r.body,"Toplam tork")>0); }
    @Test public void preloadSeparation(){ CalculationEngine.Result r=CalculationEngine.calculate(14,new double[]{20000,5000,100000,400000,0.2,10}); assertTrue(numberAfter(r.body,"Kalan sıkma")>0); }
    @Test public void beltPower(){ CalculationEngine.Result r=CalculationEngine.calculate(15,new double[]{5,200,1500,0.3,180,5000}); assertTrue(numberAfter(r.body,"T1")>numberAfter(r.body,"T2")); }
    @Test public void chain(){ CalculationEngine.Result r=CalculationEngine.calculate(16,new double[]{5,500,100,1.5,5000,0}); assertTrue(numberAfter(r.body,"Tasarım çekme")>0); }
    @Test public void couplingBolts(){ CalculationEngine.Result r=CalculationEngine.calculate(17,new double[]{500,6,120,10,100,0}); assertTrue(numberAfter(r.body,"Kesme")>0); }
    @Test public void bearingEquivalent(){ CalculationEngine.Result r=CalculationEngine.calculate(18,new double[]{5000,1000,1,0.5,20000,3}); near(5500,numberAfter(r.body,"P"),1e-5); }
    @Test public void brake(){ CalculationEngine.Result r=CalculationEngine.calculate(19,new double[]{0.35,10000,100,2,500,0}); near(700,numberAfter(r.body,"Kapasite"),1e-5); }
    @Test(expected=IllegalArgumentException.class) public void invalidPinPlane(){ CalculationEngine.calculate(8,new double[]{1000,10,5,3,100,200}); }
}
