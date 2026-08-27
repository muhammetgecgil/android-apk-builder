package com.mg.machineelementspro;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public final class EngineeringReportEngine {
    private EngineeringReportEngine(){}

    public static String build(EngineeringProject p){
        if(p==null)throw new IllegalArgumentException("Active project required");
        DesignReviewEngine.Review r=DesignReviewEngine.review(p);
        StringBuilder b=new StringBuilder();
        b.append("MACHINE ELEMENTS PRO - ENGINEERING REPORT\n");
        b.append("Project: ").append(p.name).append("\n");
        b.append("Project ID: ").append(p.id).append("\n");
        b.append("Revision: ").append(p.revision).append("\n");
        b.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date())).append("\n\n");
        b.append("1. DESIGN REVIEW SUMMARY\n");
        b.append("Overall: ").append(r.overall()).append("\n");
        b.append("Critical: ").append(r.critical).append(" | Warning: ").append(r.warning).append(" | OK: ").append(r.ok).append(" | Missing: ").append(r.missing).append("\n");
        if(Double.isFinite(r.minFos))b.append(String.format(Locale.US,"Minimum FoS: %.3f\n",r.minFos));
        if(Double.isFinite(r.minBearingLifeH))b.append(String.format(Locale.US,"Minimum bearing life: %.0f h\n",r.minBearingLifeH));
        b.append('\n');
        b.append("2. PROJECT ELEMENTS\n");
        for(EngineeringProject.Element e:p.elements){
            b.append("\n[").append(e.type).append("] ").append(e.id).append("\n");
            for(Map.Entry<String,String> x:e.values.entrySet())b.append("  ").append(x.getKey()).append(": ").append(x.getValue()).append("\n");
        }
        b.append("\n3. REVIEW FINDINGS\n");
        if(r.findings.isEmpty())b.append("No review findings.\n");
        else for(DesignReviewEngine.Finding f:r.findings)b.append(f.severity).append(" | ").append(f.elementId).append(" | ").append(f.title).append(" | ").append(f.detail).append("\n");
        b.append("\n4. PRODUCT SELECTIONS\n");
        int products=0;
        for(EngineeringProject.Element e:p.elements)if("PRODUCT_SELECTION".equals(e.type)){
            products++;
            b.append("\n").append(e.id).append("\n");
            b.append("  Region: ").append(value(e,"region","-")).append("\n");
            b.append("  Vendor: ").append(value(e,"vendor","-")).append("\n");
            b.append("  Selection: ").append(value(e,"selection","-")).append("\n");
            b.append("  Catalog: ").append(value(e,"catalogLabel","-")).append("\n");
            b.append("  URL: ").append(value(e,"url","-")).append("\n");
        }
        if(products==0)b.append("No product selection recorded.\n");
        b.append("\n5. ENGINEERING NOTE\n");
        b.append("This report records application inputs, calculated design outputs, review findings and selected catalog candidates. Final design approval remains subject to applicable standards, detailed geometry, manufacturing data and independent engineering review.\n");
        return b.toString();
    }

    private static String value(EngineeringProject.Element e,String k,String d){String v=e.get(k);return v==null||v.isEmpty()?d:v;}
}
