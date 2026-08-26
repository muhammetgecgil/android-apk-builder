package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;

/** v7.15 — apply Design A/B/C directly to active rig topology. */
public class V715Activity extends V714Activity {
  AltDesign altA,altB,altC,recommended,activeDesign;
  LinearLayout designButtons;
  TextView activeDesignSummary;

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    activeDesignSummary=card("ACTIVE DESIGN: hesap sonrası önerilen A/B/C topolojisi otomatik uygulanır; istersen başka tasarımı seçebilirsin.",Color.rgb(17,49,60));
    designButtons=new LinearLayout(this);designButtons.setOrientation(LinearLayout.HORIZONTAL);
    designButtons.addView(designBtn("DESIGN A",v->applyDesign(altA)),new LinearLayout.LayoutParams(0,dp(50),1));
    designButtons.addView(designBtn("DESIGN B",v->applyDesign(altB)),new LinearLayout.LayoutParams(0,dp(50),1));
    designButtons.addView(designBtn("DESIGN C",v->applyDesign(altC)),new LinearLayout.LayoutParams(0,dp(50),1));
    proHome.addView(activeDesignSummary,Math.min(9,proHome.getChildCount()),lp());
    proHome.addView(designButtons,Math.min(10,proHome.getChildCount()),lp());
  }

  Button designBtn(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setTextSize(10);b.setAllCaps(false);b.setOnClickListener(l);return b;}

  @Override void calculateProfessional(){
    super.calculateProfessional();
    if(!solvedValid)return;
    buildSelectableCandidates();
    if(recommended!=null)applyDesign(recommended);
  }

  void buildSelectableCandidates(){
    ArrayList<SNode> s=new ArrayList<>(solved);Collections.sort(s,(a,b)->Double.compare(a.x,b.x));
    int n=s.size(),k=Math.min(Math.max(1,ival(hActs,1,12)),n),nl=Math.max(1,ival(hLayers,1,4));
    double cap=Math.max(1,val(hCap)),len=Math.max(1,val(hLen)*1000.0);
    altA=partitionCandidate("A","BALANCED ACTUATOR","Min actuator imbalance + overload",s,k,cap,len,nl,0);
    altB=partitionCandidate("B","COMPACT GEOMETRY","Min group span + lever complexity",s,k,cap,len,nl,1);
    altC=partitionCandidate("C","LOAD FIDELITY","Min equivalent root moment mismatch",s,k,cap,len,nl,2);
    recommended=altA;if(altB.score>recommended.score)recommended=altB;if(altC.score>recommended.score)recommended=altC;
  }

  void applyDesign(AltDesign d){
    if(d==null||solved==null||solved.isEmpty()){Toast.makeText(this,"Önce HESAPLA VE GÖSTER çalıştır.",Toast.LENGTH_SHORT).show();return;}
    activeDesign=d;
    forcedActuatorGroups=d.groupOf.clone();
    designAutomaticWhiffletree();

    // Rebuild all modules using the newly selected station→actuator mapping.
    buildConnections();
    if(connection2D!=null)connection2D.invalidate();if(connection3D!=null)connection3D.invalidate();
    if(structural3D!=null){structural3D.nodes=new ArrayList<>(solved);structural3D.invalidate();}
    update2DResult();refreshVisualGuide();updateFocusedResults();refreshEquipment();refreshMatrixSolver();refreshSimulation();
    if(signed2D!=null)signed2D.invalidate();if(poster!=null)poster.invalidate();if(rigPoster70!=null)rigPoster70.invalidate();if(matrixView!=null)matrixView.invalidate();if(rigAssembly!=null)rigAssembly.invalidate();if(autoWtView!=null)autoWtView.invalidate();

    int na=Math.max(1,ival(hActs,1,12));double cap=Math.max(1,val(hCap));double[] ax=new double[na],ay=new double[na],az=new double[na];
    for(SNode s:solved){int a=Math.max(0,Math.min(na-1,s.act));ax[a]+=s.fx;ay[a]+=s.fy;az[a]+=s.fz;}
    double peak=0,maxUtil=0;StringBuilder roots=new StringBuilder();
    for(int a=0;a<na;a++){double r=Math.sqrt(ax[a]*ax[a]+ay[a]*ay[a]+az[a]*az[a]);peak=Math.max(peak,r);maxUtil=Math.max(maxUtil,r/cap);if(a>0)roots.append(" | ");roots.append("A").append(a+1).append(" ").append(String.format(Locale.US,"%.0fN",r));}
    activeDesignSummary.setText(String.format(Locale.US,
      "ACTIVE DESIGN %s — %s\nScore %.1f/100 • %s\nPeak actuator %.0f N • max utilization %.1f%% • required layer %d / available %d\nForce closure %.6f N • worst beam ΣM residual %.6f Nmm\nActuator roots: %s\nBu seçim artık 2D/3D rig, proof, component ve test ekranlarının aktif station→actuator topolojisidir.",
      d.id,d.name,d.score,d.status,peak,100*maxUtil,d.requiredLayers,Math.max(1,ival(hLayers,1,4)),wtForceResidual,wtMomentResidual,roots.toString()));
    statusCard.setText("DESIGN "+d.id+" AKTİF — whiffletree beam/pivot ağı ve actuator mapping seçilen tasarıma göre yeniden kuruldu.");
  }
}
