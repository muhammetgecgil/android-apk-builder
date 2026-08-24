package com.mg.testriganchor;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private final LinkedHashMap<String, EditText> f = new LinkedHashMap<>();
    private LinearLayout root;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,40);
        sv.addView(root); setContentView(sv);

        TextView title = tv("TEST RIG ANCHOR", 24, true); title.setTextColor(Color.rgb(160,0,0)); root.addView(title);
        root.addView(tv("Taban plakası / ankraj grubu ön boyutlandırma", 16, true));
        root.addView(tv("Birimler: kN, kN·m, mm. Sonuçlar ön tasarım içindir; gerçek ankraj ETA/üretici verileri ve beton kırılma kontrolleri ayrıca doğrulanmalıdır.", 13, false));

        add("Fx (kN)", "100"); add("Fy (kN)", "0"); add("Fz yukarı + (kN)", "20");
        add("Mx (kN·m)", "0"); add("My (kN·m)", "140"); add("Mz (kN·m)", "0");
        add("Ankraj adedi", "4"); add("Grup eni X (mm)", "600"); add("Grup boyu Y (mm)", "600");
        add("Ankraj çapı d (mm)", "20"); add("Çelik sınıfı fu (MPa)", "800"); add("Emniyet katsayısı", "1.5");

        Button calc = new Button(this); calc.setText("HESAPLA"); calc.setOnClickListener(v -> calculate()); root.addView(calc);
        TextView out = tv("",15,false); out.setId(9001); out.setPadding(0,20,0,20); root.addView(out);
    }

    private void add(String name, String def){
        TextView l=tv(name,14,true); root.addView(l);
        EditText e=new EditText(this); e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED); e.setText(def); root.addView(e); f.put(name,e);
    }
    private TextView tv(String s,int sp,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); if(bold)t.setTypeface(null,1); return t; }
    private double v(String k){ return Double.parseDouble(f.get(k).getText().toString().replace(',','.')); }

    private void calculate(){
        try {
            double Fx=v("Fx (kN)"), Fy=v("Fy (kN)"), Fz=v("Fz yukarı + (kN)");
            double Mx=v("Mx (kN·m)"), My=v("My (kN·m)"), Mz=v("Mz (kN·m)");
            int n=(int)Math.round(v("Ankraj adedi")); double bx=v("Grup eni X (mm)")/1000.0, by=v("Grup boyu Y (mm)")/1000.0;
            double d=v("Ankraj çapı d (mm)"), fu=v("Çelik sınıfı fu (MPa)"), gamma=v("Emniyet katsayısı");
            if(n<4 || bx<=0 || by<=0 || d<=0 || gamma<=0) throw new Exception();

            int cols=(int)Math.ceil(Math.sqrt(n)); int rows=(int)Math.ceil((double)n/cols);
            ArrayList<double[]> pts=new ArrayList<>();
            for(int r=0;r<rows && pts.size()<n;r++) for(int c=0;c<cols && pts.size()<n;c++) {
                double x = cols==1?0:-bx/2 + bx*c/(cols-1.0);
                double y = rows==1?0:-by/2 + by*r/(rows-1.0);
                pts.add(new double[]{x,y});
            }
            double sx2=0, sy2=0, sr2=0; for(double[] p:pts){sx2+=p[0]*p[0]; sy2+=p[1]*p[1]; sr2+=p[0]*p[0]+p[1]*p[1];}
            double V=Math.hypot(Fx,Fy), maxT=0,maxV=0; int critT=0,critV=0;
            StringBuilder detail=new StringBuilder();
            for(int i=0;i<pts.size();i++){
                double x=pts.get(i)[0], y=pts.get(i)[1];
                double t=Fz/n;
                if(sy2>0) t += Mx*y/sy2;
                if(sx2>0) t += My*x/sx2;
                if(t<0) t=0;
                double vx=Fx/n, vy=Fy/n;
                if(sr2>0){ vx += -Mz*y/sr2; vy += Mz*x/sr2; }
                double vv=Math.hypot(vx,vy);
                if(t>maxT){maxT=t;critT=i+1;} if(vv>maxV){maxV=vv;critV=i+1;}
                detail.append(String.format(Locale.US,"A%d  T=%.1f kN   V=%.1f kN\n",i+1,t,vv));
            }
            double area=Math.PI*d*d/4.0; // mm2
            double Nrd=0.9*area*fu/1000.0/gamma; // kN, simplified steel tensile resistance
            double Vrd=0.6*area*fu/1000.0/gamma; // kN, simplified steel shear resistance
            double utilT=maxT/Nrd, utilV=maxV/Vrd;
            double interaction=Math.sqrt(utilT*utilT+utilV*utilV);
            String status=interaction<=1.0?"PASS (yalnız çelik ön kontrolü)":"FAIL / çap veya ankraj düzenini büyüt";
            String s=String.format(Locale.US,
                    "\nSONUÇ: %s\n\nToplam yatay yük: %.1f kN\nKritik çekme: A%d = %.1f kN\nKritik kesme: A%d = %.1f kN\nYaklaşık çelik çekme kapasitesi: %.1f kN\nYaklaşık çelik kesme kapasitesi: %.1f kN\nÇekme kullanımı: %.0f %%\nKesme kullanımı: %.0f %%\nBirleşik kullanım: %.0f %%\n\nANKRAJ DAĞILIMI\n%s\nNOT: Beton konisi, pull-out, pry-out, kenar kırılması, çatlaklı beton, gömme derinliği, kenar mesafesi, taban plakası ve grout bu ilk sürümde otomatik nihai onay değildir.",
                    status,V,critT,maxT,critV,maxV,Nrd,Vrd,utilT*100,utilV*100,interaction*100,detail.toString());
            ((TextView)findViewById(9001)).setText(s);
        } catch(Exception e){ Toast.makeText(this,"Girdileri kontrol edin.",Toast.LENGTH_SHORT).show(); }
    }
}
