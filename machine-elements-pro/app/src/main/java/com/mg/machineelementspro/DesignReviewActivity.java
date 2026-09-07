package com.mg.machineelementspro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DesignReviewActivity extends Activity {
    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(build());}

    private ScrollView build(){
        ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(30));s.addView(r);
        r.addView(t("DESIGN REVIEW",24,true,Color.rgb(15,23,42)));
        r.addView(t("M80.3 • aktif proje için kritik / sınırda / uygun / eksik veri taraması",14,false,Color.rgb(71,85,105)));
        EngineeringProject p=EngineeringProjectRepository.active(this);
        if(p==null){r.addView(t("Aktif proje yok. Önce Project Manager'dan bir proje seçin.",16,true,Color.rgb(185,28,28)));return s;}
        DesignReviewEngine.Review q=DesignReviewEngine.review(p);
        int overallColor="KRİTİK".equals(q.overall())?Color.rgb(185,28,28):"İNCELEME GEREKLİ".equals(q.overall())?Color.rgb(180,83,9):Color.rgb(5,150,105);
        TextView head=t(p.name+" • Rev "+p.revision+"\nGENEL DURUM: "+q.overall(),20,true,overallColor);head.setPadding(0,dp(14),0,dp(10));r.addView(head);
        String minF=Double.isNaN(q.minFos)?"—":String.format(java.util.Locale.US,"%.3f",q.minFos);
        String minL=Double.isNaN(q.minBearingLifeH)?"—":String.format(java.util.Locale.US,"%.0f h",q.minBearingLifeH);
        r.addView(t("Kritik: "+q.critical+"   Uyarı: "+q.warning+"   Eksik: "+q.missing+"   Uygun: "+q.ok+"\nMin FoS: "+minF+"   Min bearing life: "+minL,15,true,Color.rgb(30,41,59)));
        for(DesignReviewEngine.Finding f:q.findings){
            LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(12),dp(14),dp(12));c.setBackgroundColor(Color.WHITE);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(10);r.addView(c,cp);
            int col=f.severity==DesignReviewEngine.Severity.CRITICAL?Color.rgb(185,28,28):f.severity==DesignReviewEngine.Severity.WARNING?Color.rgb(180,83,9):f.severity==DesignReviewEngine.Severity.MISSING?Color.rgb(124,58,237):Color.rgb(5,150,105);
            c.addView(t(f.severity+" • "+f.elementId+" • "+f.type,13,true,col));
            c.addView(t(f.title,16,true,Color.rgb(15,23,42)));
            c.addView(t(f.detail,14,false,Color.rgb(71,85,105)));
        }
        return s;
    }
    private TextView t(String s,int z,boolean b,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
