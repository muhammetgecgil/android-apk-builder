package com.mg.drawing2cad;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    static final int PICK_FILE=101;
    final int BG=Color.rgb(3,12,22), PANEL=Color.rgb(5,20,35), PANEL2=Color.rgb(8,29,48), TEXT=Color.rgb(235,244,252), MUTED=Color.rgb(158,177,193), CYAN=Color.rgb(58,205,255), BLUE=Color.rgb(0,117,255), GRASS=Color.rgb(64,214,38);
    CadSurface cad;
    TextView modelState, explodeValue, sectionValue;
    Button penButton, minusButton, plusButton;
    SeekBar explodeSeek, sectionSeek;
    Uri currentPdf;
    boolean penActive=false;
    int sectionPercent=50;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
    }

    void buildUi(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildHeader(),new LinearLayout.LayoutParams(-1,dp(58)));

        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout stage=new FrameLayout(this);
        cad=new CadSurface(this);
        stage.addView(cad,new FrameLayout.LayoutParams(-1,-1));
        addPenAndCoords(stage);
        addAnalysisPanel(stage);
        addMeasureButtons(stage);
        body.addView(stage,new LinearLayout.LayoutParams(0,-1,1f));
        body.addView(buildRightPanel(),new LinearLayout.LayoutParams(dp(355),-1));
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1f));
        root.addView(buildBottomBar(),new LinearLayout.LayoutParams(-1,dp(84)));
        setContentView(root);
    }

    View buildHeader(){
        LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);h.setPadding(dp(15),dp(6),dp(10),dp(6));
        TextView brand=txt("MG CAD PRO",22,CYAN,true);h.addView(brand);
        modelState=txt("  Model seçilmedi",19,TEXT,true);h.addView(modelState,new LinearLayout.LayoutParams(0,-2,1f));
        String[] labels={"GERİ AL (1)","DOSYA AÇ","SIĞDIR","ISO","ÖN","ÜST","SAĞ","III⌄"};
        for(String s:labels){Button b=topButton(s);h.addView(b);if(s.equals("DOSYA AÇ"))b.setOnClickListener(v->pickFile());}
        return h;
    }

    void addPenAndCoords(FrameLayout stage){
        LinearLayout left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);left.setGravity(Gravity.CENTER_HORIZONTAL);
        penButton=new Button(this);penButton.setText("✎");penButton.setTextSize(30);penButton.setTextColor(TEXT);penButton.setPadding(0,0,0,0);round(penButton,PANEL2,CYAN,999);
        penButton.setOnClickListener(v->{penActive=!penActive;penButton.setTextColor(penActive?GRASS:TEXT);round(penButton,PANEL2,penActive?GRASS:CYAN,999);cad.penActive=penActive;cad.invalidate();});
        left.addView(penButton,new LinearLayout.LayoutParams(dp(76),dp(76)));
        LinearLayout coords=new LinearLayout(this);coords.setPadding(dp(12),0,dp(12),0);coords.setGravity(Gravity.CENTER_VERTICAL);round(coords,PANEL,Color.rgb(21,67,99),12);
        TextView x=txt("X",17,Color.rgb(255,55,70),true), xr=txt(" kırmızı",16,MUTED,true), z=txt("   Z",17,Color.rgb(25,130,255),true), zr=txt(" mavi",16,MUTED,true);
        coords.addView(x);coords.addView(xr);coords.addView(z);coords.addView(zr);
        left.addView(coords,new LinearLayout.LayoutParams(dp(220),dp(52)));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(240),dp(140),Gravity.TOP|Gravity.LEFT);lp.setMargins(dp(18),dp(18),0,0);stage.addView(left,lp);
    }

    void addAnalysisPanel(FrameLayout stage){
        LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(24),dp(18),dp(18),dp(14));round(p,Color.argb(220,3,16,28),Color.rgb(16,66,98),14);
        LinearLayout titleRow=new LinearLayout(this);titleRow.setGravity(Gravity.CENTER_VERTICAL);TextView t=txt("MODEL ANALİZİ",23,CYAN,true);titleRow.addView(t,new LinearLayout.LayoutParams(0,-2,1));Button a=smallButton("ANALİZ ◀");titleRow.addView(a,new LinearLayout.LayoutParams(dp(146),dp(48)));p.addView(titleRow);
        p.addView(txt("CAD ÇALIŞMA ALANI",15,TEXT,true));p.addView(txt("Model yüklenmedi",14,MUTED,false));
        TextView formats=txt("DOSYA AÇ • ZIP • STEP/STP • IGES/IGS • BREP • OBJ\nPLY • GLTF/GLB • 3MF • DAE • FBX • DXF • X3D • OFF",12,CYAN,false);formats.setPadding(0,dp(8),0,0);p.addView(formats);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(565),dp(190),Gravity.BOTTOM|Gravity.LEFT);lp.setMargins(dp(18),0,0,dp(18));stage.addView(p,lp);
    }

    void addMeasureButtons(FrameLayout stage){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        Button b1=actionButton("ÖLÇÜLERİ KAPAT"),b2=actionButton("ÖLÇÜ KAPAT");row.addView(b1,new LinearLayout.LayoutParams(dp(230),dp(50)));row.addView(space(12));row.addView(b2,new LinearLayout.LayoutParams(dp(180),dp(50)));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(440),dp(58),Gravity.BOTTOM|Gravity.RIGHT);lp.setMargins(0,0,dp(24),dp(22));stage.addView(row,lp);
    }

    View buildRightPanel(){
        LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(18),dp(16),dp(18),dp(12));round(p,Color.rgb(3,17,29),Color.rgb(18,66,97),18);
        LinearLayout r1=new LinearLayout(this);Button hide=sideButton("GİZLE"),cadB=sideButton("CAD ▶");r1.addView(hide,new LinearLayout.LayoutParams(0,dp(54),1));r1.addView(space(12));r1.addView(cadB,new LinearLayout.LayoutParams(0,dp(54),1));p.addView(r1);
        Button all=sideButton("TÜMÜ");LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(dp(160),dp(54));alp.setMargins(0,dp(8),0,0);p.addView(all,alp);
        divider(p);
        p.addView(txt("PATLATILMIŞ GÖRÜNÜM",20,CYAN,true));explodeValue=txt("0%",18,CYAN,true);p.addView(explodeValue);
        explodeSeek=new SeekBar(this);explodeSeek.setMax(100);explodeSeek.setProgress(0);explodeSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(BLUE));explodeSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(BLUE));p.addView(explodeSeek,new LinearLayout.LayoutParams(-1,dp(54)));explodeSeek.setOnSeekBarChangeListener(simpleSeek(v->{explodeValue.setText(v+"%");cad.explode=v;cad.invalidate();}));
        divider(p);
        p.addView(txt("KESİT",20,CYAN,true));
        LinearLayout secRow=new LinearLayout(this);secRow.setGravity(Gravity.CENTER_VERTICAL);Spinner spinner=new Spinner(this);String[] axes={"X","Y","Z"};spinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,axes));secRow.addView(spinner,new LinearLayout.LayoutParams(0,dp(54),1));secRow.addView(space(10));
        minusButton=miniButton("−");plusButton=miniButton("+");secRow.addView(minusButton,new LinearLayout.LayoutParams(dp(64),dp(54)));secRow.addView(space(8));secRow.addView(plusButton,new LinearLayout.LayoutParams(dp(46),dp(46)));p.addView(secRow);
        sectionSeek=new SeekBar(this);sectionSeek.setMax(100);sectionSeek.setProgress(sectionPercent);sectionSeek.setProgressTintList(android.content.res.ColorStateList.valueOf(BLUE));sectionSeek.setThumbTintList(android.content.res.ColorStateList.valueOf(BLUE));p.addView(sectionSeek,new LinearLayout.LayoutParams(-1,dp(54)));
        sectionValue=txt("Kesit: 50%",12,MUTED,false);p.addView(sectionValue);
        View.OnClickListener adjust=v->{sectionPercent+=v==plusButton?5:-5;sectionPercent=Math.max(0,Math.min(100,sectionPercent));sectionSeek.setProgress(sectionPercent);};minusButton.setOnClickListener(adjust);plusButton.setOnClickListener(adjust);sectionSeek.setOnSeekBarChangeListener(simpleSeek(v->{sectionPercent=v;sectionValue.setText("Kesit: "+v+"%");cad.section=v;cad.invalidate();}));
        divider(p);p.addView(txt("ÖLÇÜM",20,CYAN,true));
        return p;
    }

    View buildBottomBar(){
        HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(4),dp(4),dp(4),dp(4));
        String[] a={"▧\nPDF\nTEKNİK RESİM","◇\n3D GÖRÜNÜM","✹\nPATLAT","↔\nÖLÇÜLENDİR","⌖\nKESİT AL","▱\nMALZEME","▥\nANALİZ","▤\nNOT EKLE","⚙\nAYARLAR","?\nYARDIM"};
        for(int i=0;i<a.length;i++){Button b=bottomButton(a[i]);row.addView(b,new LinearLayout.LayoutParams(dp(155),dp(74)));if(i==0)b.setOnClickListener(v->openTechnicalDrawingPdf());if(i==1)b.setOnClickListener(v->{cad.mode3d=!cad.mode3d;cad.invalidate();});if(i==2)b.setOnClickListener(v->{explodeSeek.setProgress(Math.min(100,explodeSeek.getProgress()+20));});}
        hs.addView(row);return hs;
    }

    void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_FILE);}
    void openTechnicalDrawingPdf(){
        if(currentPdf==null){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,PICK_FILE);return;}
        try{Intent v=new Intent(Intent.ACTION_VIEW);v.setDataAndType(currentPdf,"application/pdf");v.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(v);}catch(Exception e){Toast.makeText(this,"PDF görüntüleyici bulunamadı",Toast.LENGTH_SHORT).show();}
    }

    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req!=PICK_FILE||res!=RESULT_OK||data==null)return;Uri u=data.getData();if(u==null)return;try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String type=getContentResolver().getType(u);String s=u.toString().toLowerCase();if("application/pdf".equals(type)||s.endsWith(".pdf")){currentPdf=u;modelState.setText("  Teknik resim yüklendi");Toast.makeText(this,"PDF yüklendi. TEKNİK RESİM/PDF ile açabilirsiniz.",Toast.LENGTH_SHORT).show();}else{modelState.setText("  Model seçildi");}}

    interface IntAction{void run(int v);} SeekBar.OnSeekBarChangeListener simpleSeek(IntAction a){return new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){a.run(p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}};}

    TextView txt(String s,int z,int c,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextColor(c);t.setTextSize(z);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    Button topButton(String s){Button b=baseButton(s,13);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(46));lp.setMargins(dp(4),0,dp(4),0);b.setLayoutParams(lp);return b;}
    Button smallButton(String s){return baseButton(s,14);} Button sideButton(String s){return baseButton(s,14);} Button actionButton(String s){Button b=baseButton(s,15);round(b,Color.rgb(4,43,67),CYAN,12);return b;} Button miniButton(String s){return baseButton(s,22);} Button bottomButton(String s){Button b=baseButton(s,12);b.setGravity(Gravity.CENTER);return b;}
    Button baseButton(String s,int size){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(size);b.setPadding(dp(10),0,dp(10),0);b.setMinHeight(0);b.setMinimumHeight(0);round(b,PANEL2,Color.rgb(24,78,115),12);return b;}
    View space(int w){Space s=new Space(this);s.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1));return s;} void divider(LinearLayout p){View v=new View(this);v.setBackgroundColor(Color.rgb(17,59,85));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(14),0,dp(14));p.addView(v,lp);} void round(View v,int fill,int stroke,int rad){GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(rad));g.setStroke(dp(1),stroke);v.setBackground(g);} int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}

    class CadSurface extends View{
        Paint p=new Paint(3);boolean penActive=false,mode3d=false;int explode=0,section=50;
        CadSurface(Context c){super(c);setBackgroundColor(BG);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);drawGrid(c);drawAxes(c);if(mode3d)drawDemoPart(c);if(penActive){p.setColor(GRASS);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));c.drawCircle(getWidth()*.50f,getHeight()*.42f,dp(28),p);}}
        void drawGrid(Canvas c){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(Color.rgb(18,64,91));float horizon=getHeight()*.11f,center=getWidth()*.62f;for(int i=-12;i<=12;i++){float x=center+i*getWidth()*.06f;c.drawLine(center,horizon,x,getHeight(),p);}for(int j=0;j<14;j++){float y=horizon+(float)Math.pow(j/13f,1.55)*getHeight()*.89f;c.drawLine(0,y,getWidth(),y,p);}}
        void drawAxes(Canvas c){float cx=getWidth()*.62f,cy=getHeight()*.51f;p.setStrokeWidth(dp(2));p.setColor(GRASS);c.drawLine(cx,cy,cx,getHeight()*.08f,p);p.setColor(Color.rgb(0,105,255));c.drawLine(cx,cy,getWidth()*.36f,getHeight()*.84f,p);}
        void drawDemoPart(Canvas c){float cx=getWidth()*.54f,cy=getHeight()*.46f,w=dp(180),h=dp(95);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(31,115,150));Path q=new Path();q.moveTo(cx-w/2,cy);q.lineTo(cx,cy-h/2);q.lineTo(cx+w/2,cy);q.lineTo(cx,cy+h/2);q.close();c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(CYAN);c.drawPath(q,p);}
    }
}
