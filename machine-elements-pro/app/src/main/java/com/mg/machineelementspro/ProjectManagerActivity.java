package com.mg.machineelementspro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Map;

public class ProjectManagerActivity extends Activity {
    private LinearLayout list;
    private EditText name;
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());refresh();}
    private ScrollView build(){
        ScrollView s=new ScrollView(this);LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(30));s.addView(r);
        r.addView(txt("PROJECT MANAGER",24,true));r.addView(txt("M80.2 • çoklu proje, revizyon, aktif proje ve bağlı teknik elemanlar",14,false));
        name=new EditText(this);name.setHint("Yeni proje adı");r.addView(name,new LinearLayout.LayoutParams(-1,dp(56)));
        Button add=new Button(this);add.setText("YENİ PROJE OLUŞTUR");add.setOnClickListener(v->createProject());r.addView(add,new LinearLayout.LayoutParams(-1,dp(56)));
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);r.addView(list);return s;
    }
    private void createProject(){String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"Proje adı girin",Toast.LENGTH_SHORT).show();return;}String id="P-"+System.currentTimeMillis();EngineeringProject p=new EngineeringProject(id,n);EngineeringProjectRepository.save(this,p);EngineeringProjectRepository.setActive(this,id);name.setText("");refresh();}
    private void refresh(){list.removeAllViews();List<EngineeringProject> ps=EngineeringProjectRepository.list(this);if(ps.isEmpty()){list.addView(txt("Henüz proje yok.",15,false));return;}String active=EngineeringProjectRepository.activeId(this);for(EngineeringProject p:ps)list.addView(card(p,p.id.equals(active)));}
    private LinearLayout card(EngineeringProject p,boolean active){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);c.setLayoutParams(cp);
        c.addView(txt((active?"★ AKTİF • ":"")+p.name+" • Rev "+p.revision,17,true));c.addView(txt("ID: "+p.id+" • Eleman: "+p.elements.size(),13,false));
        if(p.elements.isEmpty())c.addView(txt("Bağlı eleman yok",13,false));
        else for(EngineeringProject.Element e:p.elements){TextView h=txt(e.type+" • "+e.id,14,true);h.setPadding(0,dp(8),0,0);c.addView(h);StringBuilder d=new StringBuilder();int shown=0;for(Map.Entry<String,String> x:e.values.entrySet()){if(shown++>=6){d.append("…\n");break;}d.append(x.getKey()).append(": ").append(x.getValue()).append('\n');}if(d.length()>0)c.addView(txt(d.toString().trim(),12,false));}
        Button select=new Button(this);select.setText(active?"AKTİF PROJE":"AKTİF PROJE YAP");select.setEnabled(!active);select.setOnClickListener(v->{EngineeringProjectRepository.setActive(this,p.id);refresh();});c.addView(select,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout row=new LinearLayout(this);Button rev=new Button(this);rev.setText("REVİZYON +1");rev.setOnClickListener(v->{p.bumpRevision();EngineeringProjectRepository.save(this,p);refresh();});Button demo=new Button(this);demo.setText("M80 DEMO ELEMANLARI");demo.setOnClickListener(v->{seed(p);EngineeringProjectRepository.save(this,p);refresh();});row.addView(rev,new LinearLayout.LayoutParams(0,dp(50),1));row.addView(demo,new LinearLayout.LayoutParams(0,dp(50),1));c.addView(row);return c;
    }
    private void seed(EngineeringProject p){p.upsert("M1","MOTOR").put("powerKw",7.5).put("rpm",1450);p.upsert("GB1","GEARBOX").put("ratio",14.5).put("outputRpm",100);p.upsert("S1","SHAFT").put("material","AISI 1045");p.upsert("B1","BEARING").put("shaft","S1");p.upsert("BJ1","BOLT_JOINT").put("propertyClass","10.9");p.upsert("C1","COUPLING").put("shaft","S1");p.upsert("PS1","PRODUCT_SELECTION").put("region","TR+EU");}
    private TextView txt(String x,int sp,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(sp);t.setTextColor(Color.rgb(30,41,59));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
