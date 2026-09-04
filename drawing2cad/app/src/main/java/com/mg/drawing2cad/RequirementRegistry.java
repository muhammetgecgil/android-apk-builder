package com.mg.drawing2cad;

import java.util.*;

public final class RequirementRegistry {
  public static final String[] CATEGORIES={
    "SYS","UX","IMP2D","IMP3D","ZIP","OCR","LINE","VIEW","DIM","TOL",
    "HOLE","FEATURE","RECON","BREP","MESH","CAD3D","MEASURE","SECTION","DRAW2D","AUTOANN",
    "EXPORT","VALID","CONF","PERF","MEM","ROBUST","SEC","OFFLINE","PROJECT","UNDO",
    "ASSEM","META","MANUF","QA","ANDROID","ACCESS","I18N","REPORT","AI","FUTURE"
  };
  public static final int TOTAL=1000;
  public static String id(int n){return String.format(Locale.US,"REQ-%04d",n);}
  public static String categoryFor(int n){int i=Math.max(1,Math.min(TOTAL,n))-1;return CATEGORIES[i/25];}
  public static String priorityFor(int n){int slot=(Math.max(1,Math.min(TOTAL,n))-1)%25;return slot<5?"P0":slot<15?"P1":"P2";}
  public static int countPriority(String p){if("P0".equals(p))return 200;if("P1".equals(p))return 400;if("P2".equals(p))return 400;return 0;}
  public static String summary(){return "1000 REQ • 40 KATEGORİ • P0:200 • P1:400 • P2:400";}
  private RequirementRegistry(){}
}
