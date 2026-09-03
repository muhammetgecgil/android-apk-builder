package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class DrivetrainEngineTest {
    private static void near(double e,double a,double rel){double s=Math.max(1.0,Math.abs(e));assertTrue("expected="+e+" actual="+a,Math.abs(e-a)<=rel*s);}
    private static DrivetrainEngine.Input base(){DrivetrainEngine.Input x=new DrivetrainEngine.Input();x.torqueNm=100;x.rpm=1000;x.pitchDiameterMm=100;x.pressureAngleDeg=20;x.helixAngleDeg=0;x.spanMm=1000;x.gearPositionMm=400;x.shaftDiameterMm=40;x.shaftYieldMpa=300;x.bearingC1N=20000;x.bearingC2N=20000;x.bearingExponent=3;return x;}
    @Test public void tangentialForceIsTwoTOverD(){DrivetrainEngine.Result r=DrivetrainEngine.calculate(base());near(2000,r.ft,1e-9);}
    @Test public void reactionsSumToRadialResultantForSpurGear(){DrivetrainEngine.Result r=DrivetrainEngine.calculate(base());double total=Math.hypot(r.ft,r.fr);near(total,r.ra+r.rb,2e-4);}
    @Test public void zeroHelixCreatesNoAxialLoad(){DrivetrainEngine.Result r=DrivetrainEngine.calculate(base());near(0,r.fa,1e-10);}
    @Test public void helicalGearCreatesAxialLoad(){DrivetrainEngine.Input x=base();x.helixAngleDeg=20;DrivetrainEngine.Result r=DrivetrainEngine.calculate(x);assertTrue(r.fa>0);}
    @Test public void strongerBearingIncreasesLife(){DrivetrainEngine.Input a=base();DrivetrainEngine.Result r1=DrivetrainEngine.calculate(a);a.bearingC1N=40000;DrivetrainEngine.Result r2=DrivetrainEngine.calculate(a);assertTrue(r2.l10h1>r1.l10h1);}
    @Test(expected=IllegalArgumentException.class) public void rejectsGearOutsideSpan(){DrivetrainEngine.Input x=base();x.gearPositionMm=1000;DrivetrainEngine.calculate(x);}
}
