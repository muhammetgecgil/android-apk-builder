package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class V56Activity extends V55Activity {
    TextView workspaceInfo, selectionInfo, loadLabel, layerLabel;
    SeekBar loadSeek, layerSeek;
    CheckBox expertMode;
    EFTWorkspaceView workspace;
    EditText hpX, hpY, hpZ;
    Button applyHp;
    int selectedStation = 0;
    int loadPct = 100;
    int layerFilter = 0;
    double[] hpXv, hpYv, hpZv;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8),dp(8),dp(8),dp(8));
        panel.setBackground(bg(Color.rgb(8,24,38),14));
        panel.addView(tx("EFT VISUAL WORKSPACE",18,true,Color.WHITE));
        panel.addView(tx("Tank • Station • Pad • Beam • Pivot • Load Cell • Actuator",9,false,Color.rgb(180,210,230)));

        workspaceInfo = card("Visual workspace preparing...", Color.rgb(17,46,66));
        panel.addView(workspaceInfo, lp());

        loadLabel = tx("LOAD LEVEL 100%",10,true,Color.rgb(247,207,77));
        panel.addView(loadLabel, lp());
        loadSeek = new SeekBar(this); loadSeek.setMax(100); loadSeek.setProgress(100);
        panel.addView(loadSeek, lp());

        layerLabel = tx("LAYER VIEW: ALL",10,true,Color.rgb(51,205,220));
        panel.addView(layerLabel, lp());
        layerSeek = new SeekBar(this); layerSeek.setMax(4); layerSeek.setProgress(0);
        panel.addView(layerSeek, lp());

        expertMode = new CheckBox(this); expertMode.setText("Expert Mode — equations / coordinates / margins"); expertMode.setTextColor(Color.WHITE);
        panel.addView(expertMode, lp());

        workspace = new EFTWorkspaceView();
        panel.addView(workspace,new LinearLayout.LayoutParams(-1,dp(980)));

        selectionInfo = card("Tank üzerindeki bir station/pad/actuator bölgesine dokun.", Color.rgb(20,51,72));
        panel.addView(selectionInfo, lp());

        panel.addView(tx("SELECTED HARDPOINT COORDINATES",12,true,Color.WHITE));
        hpX = field("Selected station X [mm]","0");
        hpY = field("Selected pad Y [mm]","0");
        hpZ = field("Selected pad Z [mm]","0");
        applyHp = new Button(this); applyHp.setText("APPLY HARDPOINT TO VISUAL MODEL");
        applyHp.setOnClickListener(v -> applyHardpoint());
        panel.addView(applyHp, lp());

        root.addView(panel,36,lp());

        loadSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){ loadPct=p; loadLabel.setText("LOAD LEVEL "+p+"%"); refreshWorkspace(); }
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        layerSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean from){ layerFilter=p; layerLabel.setText(p==0?"LAYER VIEW: ALL":"LAYER VIEW: L"+p); refreshWorkspace(); }
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
        expertMode.setOnCheckedChangeListener((bttn,checked)->refreshSelection());
        refreshWorkspace();
    }

    void ensureHardpoints(Calc c){
        if(hpXv!=null && hpXv.length==c.n) return;
        hpXv=new double[c.n]; hpYv=new double[c.n]; hpZv=new double[c.n];
        double yo=d(yOff); double z=d(D)*500.0;
        for(int i=0;i<c.n;i++){ hpXv[i]=c.x[i]*1000.0; hpYv[i]=(i%2==0?yo:-yo); hpZv[i]=z; }
        selectedStation=Math.min(selectedStation,c.n-1); updateHpFields();
    }

    void updateHpFields(){
        if(hpXv==null||hpXv.length==0) return;
        int i=Math.max(0,Math.min(selectedStation,hpXv.length-1));
        hpX.setText(String.format(Locale.US,"%.1f",hpXv[i]));
        hpY.setText(String.format(Locale.US,"%.1f",hpYv[i]));
        hpZ.setText(String.format(Locale.US,"%.1f",hpZv[i]));
    }

    void applyHardpoint(){
        try{
            int i=Math.max(0,Math.min(selectedStation,hpXv.length-1));
            hpXv[i]=d(hpX); hpYv[i]=d(hpY); hpZv[i]=d(hpZ);
            refreshWorkspace(); refreshSelection();
        }catch(Exception e){ selectionInfo.setText("Hardpoint coordinate update failed — check numeric inputs."); }
    }

    void refreshWorkspace(){
        try{
            Calc c=compute(false); ensureHardpoints(c);
            double peak=0,sum=0; for(double f:c.fi){double v=f*loadPct/100.0;peak=Math.max(peak,v);sum+=v;}
            workspaceInfo.setText(String.format(Locale.US,
                "EFT LIVE VIEW — %d stations | %d actuators | %d layers\nApplied load %.0f%% → ΣF %.1f kN | peak station %.1f kN\nTap the tank/load path to inspect the physics. Green=healthy, amber=near limit, red=review.",
                c.n,c.nAct,Math.max(1,(int)Math.round(d(layers))), (double)loadPct,sum,peak));
            workspace.c=c; workspace.invalidate(); refreshSelection();
        }catch(Exception e){ workspaceInfo.setText("EFT visual workspace could not evaluate current inputs."); }
    }

    void refreshSelection(){
        try{
            Calc c=compute(false); ensureHardpoints(c); int i=Math.max(0,Math.min(selectedStation,c.n-1));
            double station=c.fi[i]*loadPct/100.0; int a=c.stationAct[i]+1;
            double padArea=Math.max(1,d(padLength)*d(padWidth));
            double pmpa=(station*1000.0)/(padArea)*1.0;
            String basic=String.format(Locale.US,
                "SELECTED S%d → A%d\nStation force %.1f kN at %d%% load\nHardpoint X %.1f | Y %.1f | Z %.1f mm\nLoad path: EFT skin → pad → link → beam/pivot → load cell → actuator → strongback\nPurpose: distribute the commanded aircraft-equivalent load without creating local overload or actuator side-load.",
                i+1,a,station,loadPct,hpXv[i],hpYv[i],hpZv[i]);
            if(expertMode.isChecked()) basic += String.format(Locale.US,
                "\nEXPERT\nNominal pad area %.0f mm² | average contact pressure %.3f MPa\nStation design load with SF %.1f kN | actuator capacity %.1f kN\nCoordinate editor changes the visual hardpoint model; detailed release still requires actual drawing/CAD coordinates.",
                padArea,pmpa,station*d(sf),d(actCapacity));
            selectionInfo.setText(basic);
        }catch(Exception e){}
    }

    class EFTWorkspaceView extends View {
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG), t=new Paint(Paint.ANTI_ALIAS_FLAG);
        Calc c;
        EFTWorkspaceView(){ super(V56Activity.this); setBackgroundColor(Color.rgb(3,14,24)); t.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD)); setFocusable(true); }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(e.getAction()!=android.view.MotionEvent.ACTION_DOWN || c==null) return true;
            float left=dp(35), right=getWidth()-dp(35), tankY=dp(205);
            int best=0; float bestD=Float.MAX_VALUE;
            for(int i=0;i<c.n;i++){
                float x=left+(right-left)*(i+.5f)/c.n;
                float dd=Math.abs(e.getX()-x)+Math.abs(e.getY()-tankY);
                if(dd<bestD){bestD=dd;best=i;}
            }
            selectedStation=best; updateHpFields(); refreshSelection(); invalidate(); return true;
        }

        @Override protected void onDraw(Canvas cn){
            super.onDraw(cn); if(c==null)return; int W=getWidth(); float left=dp(32),right=W-dp(32),tankY=dp(205);
            t.setColor(Color.WHITE);t.setTextSize(dp(13));cn.drawText("INTERACTIVE EFT LOAD PATH",dp(16),dp(30),t);
            t.setTextSize(dp(7));t.setColor(Color.rgb(160,190,210));cn.drawText("Tap a station. Load slider animates force level. Layer slider isolates the tree.",dp(16),dp(52),t);

            p.setColor(Color.rgb(72,82,92)); cn.drawRoundRect(new RectF(left,tankY-dp(42),right,tankY+dp(42)),dp(42),dp(42),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(150,170,185));cn.drawRoundRect(new RectF(left,tankY-dp(42),right,tankY+dp(42)),dp(42),dp(42),p);p.setStyle(Paint.Style.FILL);

            double max=1;for(double f:c.fi)max=Math.max(max,f);
            float beamY=tankY+dp(145), actY=tankY+dp(430);
            for(int i=0;i<c.n;i++){
                float x=left+(right-left)*(i+.5f)/c.n; double f=c.fi[i]*loadPct/100.0; double util=100*f*d(sf)/Math.max(1,d(actCapacity));
                int col=util>100?Color.rgb(229,82,74):(util>80?Color.rgb(247,207,77):Color.rgb(67,190,113));
                p.setColor(i==selectedStation?Color.WHITE:col);cn.drawCircle(x,tankY,dp(i==selectedStation?8:5),p);
                float h=(float)(dp(90)*(f/max));p.setStrokeWidth(dp(3));p.setColor(col);cn.drawLine(x,tankY-dp(50),x,tankY-dp(50)-h,p);
                t.setTextSize(dp(6));t.setColor(col);cn.drawText("S"+(i+1),x-dp(7),tankY-dp(58)-h,t);
                if(layerFilter==0 || layerFilter==1){p.setColor(Color.rgb(195,210,220));p.setStrokeWidth(dp(2));cn.drawLine(x,tankY+dp(48),x,beamY-dp(18),p);}
            }

            int nAct=Math.max(1,c.nAct);
            for(int a=0;a<nAct;a++){
                float ax=left+(right-left)*(a+.5f)/nAct;
                if(layerFilter==0 || layerFilter<=2){
                    p.setColor(Color.rgb(51,205,220));cn.drawRoundRect(new RectF(ax-dp(42),beamY-dp(12),ax+dp(42),beamY+dp(12)),dp(5),dp(5),p);
                    p.setColor(Color.rgb(247,207,77));cn.drawCircle(ax,beamY,dp(7),p);
                }
                if(layerFilter==0 || layerFilter>=2){p.setColor(Color.rgb(120,145,165));p.setStrokeWidth(dp(3));cn.drawLine(ax,beamY+dp(15),ax,actY-dp(60),p);}
                double af=(a<c.actLoads.length?c.actLoads[a]:0)*loadPct/100.0; double au=100*af/Math.max(1,d(actCapacity));
                int acol=au>100?Color.rgb(229,82,74):(au>85?Color.rgb(247,207,77):Color.rgb(67,190,113));
                p.setColor(acol);cn.drawRoundRect(new RectF(ax-dp(18),actY-dp(60),ax+dp(18),actY+dp(35)),dp(7),dp(7),p);
                t.setColor(Color.WHITE);t.setTextSize(dp(6));cn.drawText("A"+(a+1),ax-dp(7),actY-dp(68),t);cn.drawText(String.format(Locale.US,"%.0f kN",af),ax-dp(15),actY+dp(55),t);
            }

            p.setColor(Color.rgb(45,55,65));cn.drawRect(left,actY+dp(85),right,actY+dp(105),p);
            t.setColor(Color.rgb(180,200,215));t.setTextSize(dp(7));cn.drawText("STRONGBACK / GROUND REACTION",left,actY+dp(128),t);

            float ly=actY+dp(185);t.setTextSize(dp(7));
            int[] cols={Color.rgb(67,190,113),Color.rgb(247,207,77),Color.rgb(229,82,74)};String[] labs={"PASS / healthy margin","WARN / near limit","FAIL / review geometry or capacity"};
            for(int i=0;i<3;i++){p.setColor(cols[i]);cn.drawCircle(dp(28),ly+i*dp(28),dp(5),p);t.setColor(Color.WHITE);cn.drawText(labs[i],dp(42),ly+dp(3)+i*dp(28),t);}
        }
    }
}
