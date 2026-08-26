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
}
