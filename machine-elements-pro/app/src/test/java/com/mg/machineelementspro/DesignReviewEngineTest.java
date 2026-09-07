package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class DesignReviewEngineTest {
    @Test public void flagsCriticalShaftAndLowLife(){
        EngineeringProject p=new EngineeringProject("P1","Review");
        p.upsert("S1","SHAFT").put("fos",0.9);
        p.upsert("B1","BEARING").put("designation","6206").put("lifeHours",800).put("staticFoS",1.2);
        p.upsert("PS1","PRODUCT_SELECTION").put("vendor","FAG");
        DesignReviewEngine.Review r=DesignReviewEngine.review(p);
        assertTrue(r.critical>=2);
        assertEquals(0.9,r.minFos,1e-9);
        assertEquals(800.0,r.minBearingLifeH,1e-9);
        assertEquals("KRİTİK",r.overall());
    }

    @Test public void flagsMissingProductAndTorque(){
        EngineeringProject p=new EngineeringProject("P2","Missing");
        p.upsert("BJ1","BOLT_JOINT").put("diameterMm",12);
        DesignReviewEngine.Review r=DesignReviewEngine.review(p);
        assertTrue(r.missing>=2);
        assertEquals("İNCELEME GEREKLİ",r.overall());
    }

    @Test public void passesHealthyProject(){
        EngineeringProject p=new EngineeringProject("P3","Good");
        p.upsert("S1","SHAFT").put("fos",2.2);
        p.upsert("B1","BEARING").put("designation","6306").put("lifeHours",12000).put("staticFoS",2.0);
        p.upsert("PS1","PRODUCT_SELECTION").put("vendor","Schaeffler");
        DesignReviewEngine.Review r=DesignReviewEngine.review(p);
        assertEquals(0,r.critical);
        assertEquals(0,r.warning);
        assertEquals(0,r.missing);
        assertEquals("UYGUN",r.overall());
    }
}
