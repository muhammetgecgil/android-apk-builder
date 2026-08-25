package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class SelectionCatalogTest {
    @Test public void boltClassLookupReturnsExpectedStrength(){
        SelectionCatalog.BoltClass b=SelectionCatalog.findBoltClass("10.9");
        assertEquals(900.0,b.sy,1e-9); assertEquals(1000.0,b.sut,1e-9);
    }
    @Test public void bearingSelectorChoosesFirstAdequate(){
        SelectionCatalog.Bearing b=SelectionCatalog.selectBearing(20,14000);
        assertEquals("6205",b.code);
    }
    @Test public void preferredMaterialComparisonReturnsAllMaterials(){
        assertEquals(MaterialLibrary.MATERIALS.length,SelectionCatalog.compareMaterialsForShaft(100,50,2.0).size());
    }
    @Test public void requiredShaftDiameterIncreasesWithFos(){
        double a=SelectionCatalog.requiredSolidShaftDiameter(100,50,500,1.5);
        double b=SelectionCatalog.requiredSolidShaftDiameter(100,50,500,2.0);
        assertTrue(b>a);
    }
}
