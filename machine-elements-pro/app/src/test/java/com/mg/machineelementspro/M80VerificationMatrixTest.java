package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class M80VerificationMatrixTest {
    private static double n(String body,String label){
        int i=body.indexOf(label+":"); if(i<0) throw new AssertionError("missing "+label);
        int s=i+label.length()+1; while(s<body.length()&&body.charAt(s)==' ')s++;
        int e=s; while(e<body.length()){char c=body.charAt(e); if(Character.isDigit(c)||c=='-'||c=='+'||c=='.'||c=='e'||c=='E')e++; else break;}
        return Double.parseDouble(body.substring(s,e));
    }
    private static void near(double ex,double ac,double rel){double sc=Math.max(1.0,Math.abs(ex));assertEquals(ex,ac,rel*sc);}

    @Test public void vm001_bolt(){near(10000.0/(Math.PI*25),n(CalculationEngine.calculate(0,new double[]{10000,0,10,250,0,0}).body,"Çekme"),1e-3);}
    @Test public void vm002_shaft(){near(32*100000.0/(Math.PI*8000),n(CalculationEngine.calculate(1,new double[]{100,0,20,300,0,0}).body,"Eğilme"),1e-3);}
    @Test public void vm003_bearing(){near(8,n(CalculationEngine.calculate(2,new double[]{10000,5000,1000,3,0,0}).body,"L10"),1e-3);}
    @Test public void vm004_gear(){near(1000.0/(20*2*0.3),n(CalculationEngine.calculate(3,new double[]{1000,20,2,0.3,300,0}).body,"Diş dibi gerilmesi"),1e-3);}
    @Test public void vm005_spring(){CalculationEngine.Result r=CalculationEngine.calculate(4,new double[]{100,30,3,8,80000,700});near(100,n(r.body,"k")*n(r.body,"Sehim"),3e-3);}
    @Test public void vm006_key(){CalculationEngine.Result r=CalculationEngine.calculate(5,new double[]{500,40,12,8,50,150});assertTrue(n(r.body,"Kesme")>0&&n(r.body,"Ezilme")>0);}
    @Test public void vm007_weld(){near(10000/(0.70710678*6*100),n(CalculationEngine.calculate(6,new double[]{10000,6,100,120,0,0}).body,"Kayma"),2e-3);}
    @Test public void vm008_goodman(){near(1.0/(100.0/250+50.0/600),n(CalculationEngine.calculate(7,new double[]{100,50,250,600,0,0}).body,"FoS"),2e-3);}
    @Test public void vm009_pin(){CalculationEngine.Result a=CalculationEngine.calculate(8,new double[]{10000,10,5,1,200,300});CalculationEngine.Result b=CalculationEngine.calculate(8,new double[]{10000,10,5,2,200,300});near(n(a.body,"Pim kesme")/2,n(b.body,"Pim kesme"),2e-3);}
    @Test public void vm010_euler(){double ex=Math.PI*Math.PI*200000*100000/1000000.0;near(ex,n(CalculationEngine.calculate(9,new double[]{10000,200000,100000,1000,1,2}).body,"Pcr"),2e-3);}
    @Test public void vm011_beam(){near(250,n(CalculationEngine.calculate(10,new double[]{1000,1000,200000,1000000,10000,250}).body,"Mmax"),1e-3);}
    @Test public void vm012_torsion(){assertTrue(n(CalculationEngine.calculate(11,new double[]{100,1000,20,80000,5,0}).body,"Açı")>0);}
    @Test public void vm013_powerScrew(){assertTrue(n(CalculationEngine.calculate(12,new double[]{10000,20,4,0.15,30,0.1}).body,"Toplam tork")>0);}
    @Test public void vm014_threadStrip(){double expected=10000.0/(Math.PI*18*20*0.75);CalculationEngine.Result r=CalculationEngine.calculate(13,new double[]{10000,18,20,0.75,120,1.5});near(expected,n(r.body,"Ortalama kayma"),2e-3);}
    @Test public void vm015_preload(){CalculationEngine.Result r=CalculationEngine.calculate(14,new double[]{20000,5000,100000,400000,0.2,10});assertTrue(n(r.body,"Kalan sıkma")>0);}
    @Test public void vm016_belt(){CalculationEngine.Result r=CalculationEngine.calculate(15,new double[]{5,200,1500,0.3,180,5000});assertTrue(n(r.body,"T1")>n(r.body,"T2"));}
    @Test public void vm017_chain(){double p=5000,rpm=500,r=0.1,Ks=1.5;double omega=2*Math.PI*rpm/60.0;double expected=Ks*(p/omega)/r;CalculationEngine.Result x=CalculationEngine.calculate(16,new double[]{5,500,100,1.5,5000,0});near(expected,n(x.body,"Tasarım çekme"),2e-3);}
    @Test public void vm018_coupling(){assertTrue(n(CalculationEngine.calculate(17,new double[]{500,6,120,10,100,0}).body,"Kesme")>0);}
    @Test public void vm019_bearingEquivalent(){near(5500,n(CalculationEngine.calculate(18,new double[]{5000,1000,1,0.5,20000,3}).body,"P"),1e-3);}
    @Test public void vm020_brake(){near(700,n(CalculationEngine.calculate(19,new double[]{0.35,10000,100,2,500,0}).body,"Kapasite"),1e-3);}

    @Test public void vm021_drivetrain(){DrivetrainEngine.Input x=new DrivetrainEngine.Input();x.torqueNm=100;x.rpm=1500;x.pitchDiameterMm=100;x.pressureAngleDeg=20;x.helixAngleDeg=0;x.spanMm=400;x.gearPositionMm=200;x.shaftDiameterMm=30;x.shaftYieldMpa=530;x.bearingC1N=30000;x.bearingC2N=30000;x.bearingExponent=3;DrivetrainEngine.Result r=DrivetrainEngine.calculate(x);double ft=2000.0;double fr=ft*Math.tan(Math.toRadians(20));double expectedReaction=Math.hypot(ft/2.0,fr/2.0);near(ft,r.ft,5e-3);near(fr,r.fr,5e-3);near(expectedReaction,r.ra,5e-3);near(expectedReaction,r.rb,5e-3);}

    @Test public void vm022_assemblyShaft(){AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(0,new double[]{1000,250,1000,750,2000,40});assertTrue(r.body.contains("RA")&&r.body.contains("RB"));}
    @Test public void vm023_boltGroup(){AssemblyCalculationEngine.Result r=AssemblyCalculationEngine.calculate(1,new double[]{1000,500,200,50,10,640});assertTrue(r.body.contains("Kritik civata"));}
    @Test public void vm024_gearbox(){GearboxDesignEngine.GearboxResult r=GearboxDesignEngine.sizeSingleStage(100,1500,20,60,3,30,20,0.97,530,2,250,10000);near(3.0,r.ratio,5e-3);near(291,r.outputTorqueNm,5e-3);}
    @Test public void vm025_m80EndToEndGate(){double pKw=7.5,inputRpm=1450,outputRpm=100,eta=0.94;double tin=pKw*9550/inputRpm;double ratio=inputRpm/outputRpm;double tout=tin*ratio*eta;near(14.5,ratio,1e-6);near(49.39655,tin,5e-4);near(673.278,tout,5e-4);assertTrue(ProductCatalogEngine.gearboxMatches(pKw,inputRpm,outputRpm,tout).size()>=2);assertTrue(ProductCatalogEngine.boltMatches(11.2,"10.9").get(0).calculatedSelection.contains("M12"));}
}
