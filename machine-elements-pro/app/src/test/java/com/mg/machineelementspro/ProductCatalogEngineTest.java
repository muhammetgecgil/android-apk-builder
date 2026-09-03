package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProductCatalogEngineTest {
    @Test public void bearingMatchReturnsTurkeyAndEurope(){
        java.util.List<ProductCatalogEngine.CatalogMatch> r=ProductCatalogEngine.bearingMatches(25,12000);
        assertTrue(r.size()>=2);
        assertTrue(r.get(0).calculatedSelection.contains("6205"));
    }
    @Test public void boltMatchRoundsToMetricSeries(){
        java.util.List<ProductCatalogEngine.CatalogMatch> r=ProductCatalogEngine.boltMatches(11.2,"10.9");
        assertTrue(r.get(0).calculatedSelection.contains("M12"));
    }
    @Test public void couplingMatchKeepsRequiredTorque(){
        java.util.List<ProductCatalogEngine.CatalogMatch> r=ProductCatalogEngine.couplingMatches(420,38);
        assertTrue(r.get(0).calculatedSelection.contains("420"));
    }
    @Test public void gearboxMatchCalculatesRatio(){
        java.util.List<ProductCatalogEngine.CatalogMatch> r=ProductCatalogEngine.gearboxMatches(5.5,1450,95,480);
        assertTrue(r.get(0).calculatedSelection.contains("15.26"));
    }
    @Test public void beltMatchReturnsProfileAndTwoRegions(){
        java.util.List<ProductCatalogEngine.CatalogMatch> r=ProductCatalogEngine.beltMatches(8.0,140,1450,1200);
        assertTrue(r.size()>=2);
        assertTrue(r.get(0).calculatedSelection.contains("SPA"));
        assertTrue(r.get(0).calculatedSelection.contains("1200"));
    }
    @Test public void chainMatchScalesFamilyWithDesignPull(){
        java.util.List<ProductCatalogEngine.CatalogMatch> low=ProductCatalogEngine.chainMatches(3,500,2500,1.2);
        java.util.List<ProductCatalogEngine.CatalogMatch> high=ProductCatalogEngine.chainMatches(8,500,10000,1.5);
        assertTrue(low.get(0).calculatedSelection.contains("08B"));
        assertTrue(high.get(0).calculatedSelection.contains("16B"));
        assertTrue(high.size()>=2);
    }
}
