package com.mg.machineelementspro;

import org.junit.Test;
import static org.junit.Assert.*;

public class EngineeringProjectTest {
    @Test public void roundTripKeepsLinkedElements(){
        EngineeringProject p=new EngineeringProject("P-001","7.5 kW Transmission");
        p.upsert("M1","MOTOR").put("powerKw",7.5).put("rpm",1450);
        p.upsert("GB1","GEARBOX").put("ratio",14.5).put("outputRpm",100);
        p.upsert("S1","SHAFT").put("diameterMm",40);
        p.upsert("B1","BEARING").put("designation","6208").put("shaft","S1");
        p.upsert("C1","COUPLING").put("shaft","S1").put("designTorqueNm",700);
        String raw=p.encode();EngineeringProject q=EngineeringProject.decode(raw);
        assertEquals("7.5 kW Transmission",q.name);assertEquals(5,q.elements.size());
        assertEquals(7.5,q.find("M1").getDouble("powerKw"),1e-9);
        assertEquals("S1",q.find("B1").get("shaft"));
    }
    @Test public void upsertDoesNotDuplicate(){EngineeringProject p=new EngineeringProject("P","X");p.upsert("S1","SHAFT").put("d",30);p.upsert("S1","SHAFT").put("d",35);assertEquals(1,p.elements.size());assertEquals(35,p.find("S1").getDouble("d"),1e-9);}
    @Test public void revisionIncrements(){EngineeringProject p=new EngineeringProject("P","X");int r=p.revision;p.bumpRevision();assertEquals(r+1,p.revision);}
}
