package com.mg.machineelementspro;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class EngineeringReportActivity extends Activity {
    private EngineeringProject project;
    private String reportText;

    @Override protected void onCreate(Bundle b){super.onCreate(b);project=EngineeringProjectRepository.active(this);setContentView(build());}

    private View build(){
        ScrollView s=new ScrollView(this);s.setBackgroundColor(Color.rgb(248,250,252));
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(30));s.addView(r);
        TextView h=t("ENGINEERING REPORT",24,true,Color.rgb(15,23,42));r.addView(h);
        if(project==null){r.addView(t("Aktif proje yok. Önce Project Manager'dan bir proje seçin.",15,false,Color.rgb(185,28,28)));return s;}
        reportText=EngineeringReportEngine.build(project);
        TextView sub=t(project.name+" • Rev "+project.revision,14,true,Color.rgb(71,85,105));sub.setPadding(0,dp(4),0,dp(10));r.addView(sub);
        Button pdf=new Button(this);pdf.setText("PDF OLUŞTUR VE PAYLAŞ");pdf.setOnClickListener(v->exportPdf());r.addView(pdf,new LinearLayout.LayoutParams(-1,dp(56)));
        TextView body=t(reportText,13,false,Color.rgb(30,41,59));body.setTextIsSelectable(true);body.setPadding(dp(12),dp(14),dp(12),dp(14));body.setBackgroundColor(Color.WHITE);r.addView(body);
        return s;
    }

    private void exportPdf(){
        try{
            File dir=new File(getCacheDir(),"reports");if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("Rapor klasörü oluşturulamadı");
            File f=new File(dir,safe(project.name)+"_Rev"+project.revision+"_Engineering_Report.pdf");
            writePdf(f,reportText);
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",f);
            Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Mühendislik raporunu paylaş"));
        }catch(Exception e){Toast.makeText(this,e.getMessage()==null?"PDF oluşturulamadı":e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    private void writePdf(File f,String text)throws Exception{
        PdfDocument doc=new PdfDocument();Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(10f);
        final int w=595,h=842,left=42,top=48,bottom=48,lineH=14,maxChars=92;
        List<String> lines=wrap(text,maxChars);int pageNo=1,idx=0;
        while(idx<lines.size()){
            PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(w,h,pageNo).create();PdfDocument.Page page=doc.startPage(info);Canvas c=page.getCanvas();float y=top;
            while(idx<lines.size()&&y<h-bottom){c.drawText(lines.get(idx++),left,y,paint);y+=lineH;}
            doc.finishPage(page);pageNo++;
        }
        try(FileOutputStream out=new FileOutputStream(f)){doc.writeTo(out);}finally{doc.close();}
    }

    private List<String> wrap(String text,int max){List<String> out=new ArrayList<>();for(String raw:text.split("\\n",-1)){if(raw.length()==0){out.add("");continue;}String x=raw;while(x.length()>max){int cut=x.lastIndexOf(' ',max);if(cut<1)cut=max;out.add(x.substring(0,cut));x=x.substring(cut).trim();}out.add(x);}return out;}
    private String safe(String x){return x.replaceAll("[^A-Za-z0-9._-]+","_");}
    private TextView t(String x,int sp,boolean bold,int color){TextView v=new TextView(this);v.setText(x);v.setTextSize(sp);v.setTextColor(color);if(bold)v.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);return v;}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}
