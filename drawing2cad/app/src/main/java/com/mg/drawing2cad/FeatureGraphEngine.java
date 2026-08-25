package com.mg.drawing2cad;

import java.util.*;

/** v3.2 engineering feature graph. Converts drawing-intelligence candidates and
 * parsed callouts into explicit CAD-like feature nodes. It never fabricates a
 * missing dimension; unresolved parameters remain null/UNKNOWN. */
public final class FeatureGraphEngine {
  private FeatureGraphEngine(){}

  public static final class Node {
    public final String id,type,source;
    public final Float value1,value2;
    public final float confidence;
    public final boolean verified;
    Node(String i,String t,String s,Float v1,Float v2,float c,boolean v){id=i;type=t;source=s;value1=v1;value2=v2;confidence=c;verified=v;}
    public String label(){
      StringBuilder b=new StringBuilder(type).append(" • ").append(Math.round(confidence*100f)).append("%");
      if(value1!=null)b.append(" • ").append(String.format(Locale.US,"%.2f",value1));
      if(value2!=null)b.append(" × ").append(String.format(Locale.US,"%.2f",value2));
      if(!verified)b.append(" • doğrulanmamış");
      return b.toString();
    }
  }

  public static final class Graph {
    public final ArrayList<Node> nodes=new ArrayList<>();
    public int holes,slots,pockets,radii,chamfers,threads,dimensions,unknown;
    public float confidence;
    public String summary(){return "Feature graph "+nodes.size()+" • delik "+holes+" • slot "+slots+" • cep "+pockets+" • R "+radii+" • pah "+chamfers+" • diş "+threads+" • güven "+Math.round(confidence*100f)+"%";}
  }

  public static Graph build(TechnicalDrawingIntelligence.Report report, Collection<String> textTokens){
    Graph g=new Graph();int seq=1;float sum=0f;
    if(report!=null){
      for(TechnicalDrawingIntelligence.Feature f:report.features){
        if("HOLE_OR_CIRCLE".equals(f.type)){
          g.nodes.add(new Node(id(seq++),"HOLE_CANDIDATE",f.note,null,null,f.confidence,false));g.holes++;sum+=f.confidence;
        }
      }
    }
    ArrayList<String> parsed=TechnicalDrawingIntelligence.parseCallouts(textTokens);
    for(String s:parsed){
      String up=s.toUpperCase(Locale.ROOT);String type="UNKNOWN";Float a=null,b=null;boolean verified=true;float conf=.96f;
      if(up.startsWith("HOLE:")){type="HOLE";g.holes++;a=firstNumber(s);}
      else if(up.startsWith("RADIUS:")){type="FILLET_RADIUS";g.radii++;a=firstNumber(s);}
      else if(up.startsWith("THREAD:")){type="THREAD";g.threads++;a=firstNumber(s);}
      else if(up.startsWith("CHAMFER:")){type="CHAMFER";g.chamfers++;a=firstNumber(s);}
      else if(up.startsWith("TOLERANCE:")){type="TOLERANCE";g.dimensions++;}
      else {g.unknown++;verified=false;conf=.4f;}
      g.nodes.add(new Node(id(seq++),type,s,a,b,conf,verified));sum+=conf;
    }
    // Geometric placeholders for later exact B-Rep operations. They carry no invented values.
    if(report!=null && report.longLineCandidates>=4){g.nodes.add(new Node(id(seq++),"BASE_EXTRUDE_CANDIDATE","closed orthographic profile",null,null,.68f,false));sum+=.68f;}
    g.confidence=g.nodes.isEmpty()?(report==null?0f:report.geometryConfidence):Math.min(.99f,sum/g.nodes.size());
    return g;
  }

  public static Graph withManualCallouts(TechnicalDrawingIntelligence.Report report,String raw){
    ArrayList<String> tokens=new ArrayList<>();if(raw!=null)for(String x:raw.split("[;\\n]+"))if(!x.trim().isEmpty())tokens.add(x.trim());return build(report,tokens);
  }

  static String id(int n){return String.format(Locale.US,"F%03d",n);}
  static Float firstNumber(String s){
    if(s==null)return null;StringBuilder b=new StringBuilder();boolean started=false;
    for(int i=0;i<s.length();i++){char c=s.charAt(i);if((c>='0'&&c<='9')||((c=='.'||c==',')&&started)){b.append(c==','?'.':c);started=true;}else if(started)break;}
    try{return b.length()==0?null:Float.parseFloat(b.toString());}catch(Exception e){return null;}
  }
}
