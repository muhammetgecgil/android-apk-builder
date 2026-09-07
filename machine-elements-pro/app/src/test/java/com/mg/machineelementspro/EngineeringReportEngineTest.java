package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class EngineeringReportEngineTest {
    @Test public void reportContainsProjectReviewAndProducts(){
        EngineeringProject p=new EngineeringProject("P-R1","Transmission Demo");
        p.upsert("M1","MOTOR").put("powerKw",7.5).put("rpm",1450);
        p.upsert("S1","SHAFT").put("fos",2.1).put("diameterMm",40);
        p.upsert("B1","BEARING").put("lifeHours",15000).put("staticFoS",2.0).put("designation","6208");
        p.upsert("PS1","PRODUCT_SELECTION").put("region","EU").put("vendor","DemoVendor").put("selection","6208 candidate").put("catalogLabel","Official catalog").put("url","https://example.com");
        String r=EngineeringReportEngine.build(p);
        assertTrue(r.contains("Transmission Demo"));
        assertTrue(r.contains("DESIGN REVIEW SUMMARY"));
        assertTrue(r.contains("Minimum FoS"));
        assertTrue(r.contains("PRODUCT SELECTIONS"));
        assertTrue(r.contains("DemoVendor"));
    }
}
