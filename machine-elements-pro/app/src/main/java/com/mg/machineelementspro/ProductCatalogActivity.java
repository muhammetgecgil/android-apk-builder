package com.mg.machineelementspro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public class ProductCatalogActivity extends Activity {
    private final EditText[] in=new EditText[4];
    private LinearLayout results;
    private Spinner type;
    private final String[][] labels={
            {"Bore / mil çapı (mm)","Gerekli dinamik C (N)","",""},
            {"Hesaplanan min. nominal çap (mm)","Property class (örn. 10.9)","",""},
            {"Tasarım torku (Nm)","Mil çapı / gerekli bore (mm)","",""},
            {"Motor gücü (kW)","Giriş devri (rpm)","Çıkış devri (rpm)","Gerekli çıkış torku (Nm)"}
    };

    @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(ui());}

    private View ui(){
        ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(32));s.addView(r);
        r.addView(text("TÜRKİYE + AVRUPA ÜRÜN SEÇİMİ",22,true,Color.rgb(15,23,42)));
        TextView sub=text("Hesap sonucunu standart ürüne çevirir ve resmi üretici kataloglarına yönlendirir. Fiyat/stok canlı doğrulama gerektirir.",13,false,Color.rgb(71,85,105));sub.setPadding(0,dp(5),0,dp(12));r.addView(sub);
        type=new Spinner(this);String[] types={"Rulman","Civata","Kaplin","Redüktör"};type.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,types));r.addView(type,lp(-1,dp(54),0));
        for(int i=0;i<4;i++){in[i]=new EditText(this);in[i].setTextSize(16);in[i].setSingleLine(true);in[i].setPadding(dp(12),dp(8),dp(12),dp(8));r.addView(in[i],lp(-1,dp(58),dp(8)));}
        Button calc=new Button(this);calc.setText("TEKNİK ADAYLARI BUL");calc.setAllCaps(false);calc.setTypeface(Typeface.DEFAULT,Typeface.BOLD);calc.setTextColor(Color.WHITE);calc.setBackgroundColor(Color.rgb(5,150,105));r.addView(calc,lp(-1,dp(56),dp(14)));
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);r.addView(results,lp(-1,-2,dp(14)));
        type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){update(pos);}public void onNothingSelected(AdapterView<?> p){}});
        calc.setOnClickListener(v->calculate());update(0);return s;
    }

    private void update(int pos){
        for(int i=0;i<4;i++){
            in[i].setHint(labels[pos][i]);in[i].setText("");in[i].setVisibility(labels[pos][i].isEmpty()?View.GONE:View.VISIBLE);
            if(pos==1&&i==1){in[i].setInputType(InputType.TYPE_CLASS_TEXT);in[i].setText("10.9");}
            else in[i].setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        results.removeAllViews();
    }

    private double d(int i){String x=in[i].getText().toString().trim().replace(',','.');if(x.isEmpty())throw new IllegalArgumentException("Eksik alan: "+in[i].getHint());return Double.parseDouble(x);}

    private void calculate(){
        try{
            int p=type.getSelectedItemPosition();List<ProductCatalogEngine.CatalogMatch> m;
            if(p==0)m=ProductCatalogEngine.bearingMatches(d(0),d(1));
            else if(p==1)m=ProductCatalogEngine.boltMatches(d(0),in[1].getText().toString());
            else if(p==2)m=ProductCatalogEngine.couplingMatches(d(0),d(1));
            else m=ProductCatalogEngine.gearboxMatches(d(0),d(1),d(2),d(3));
            show(m);
        }catch(Exception e){Toast.makeText(this,e.getMessage()==null?"Girdileri kontrol edin.":e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void show(List<ProductCatalogEngine.CatalogMatch> list){
        results.removeAllViews();
        for(ProductCatalogEngine.CatalogMatch m:list){
            LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(15),dp(14),dp(15),dp(14));c.setBackgroundColor(Color.WHITE);results.addView(c,lp(-1,-2,dp(10)));
            c.addView(text(m.region+" • "+m.vendor,17,true,Color.rgb(15,23,42)));
            TextView sel=text(m.calculatedSelection,15,true,Color.rgb(5,150,105));sel.setPadding(0,dp(6),0,dp(4));c.addView(sel);
            c.addView(text(m.note,13,false,Color.rgb(71,85,105)));
            Button open=new Button(this);open.setText("RESMİ KATALOĞU AÇ → "+m.catalogLabel);open.setAllCaps(false);open.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(m.url))));c.addView(open,lp(-1,dp(52),dp(9)));
        }
    }

    private TextView text(String x,int sp,boolean bold,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private LinearLayout.LayoutParams lp(int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.topMargin=top;return p;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
