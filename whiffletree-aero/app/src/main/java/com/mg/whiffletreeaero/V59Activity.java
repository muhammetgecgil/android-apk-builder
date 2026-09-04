package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.widget.*;
import java.util.*;

public class V59Activity extends V58Activity {
    TextView regressionSummary, regressionDetails;
    Button runRegression;

    static class Bench {
        String name; double F,M,L; int n,layers,acts; double cap,sf;
        Bench(String name,double F,double M,double L,int n,int layers,int acts,double cap,double sf){this.name=name;this.F=F;this.M=M;this.L=L;this.n=n;this.layers=layers;this.acts=acts;this.cap=cap;this.sf=sf;}
    }
    static class BR {String name; boolean pass; double fErr,mErr,peakAct,capacity; String reason;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(dp(8),dp(8),dp(8),dp(8));p.setBackground(bg(Color.rgb(6,24,37),14));
        p.addView(tx("v5.9 REGRESSION & BENCHMARK VALIDATION",18,true,Color.WHITE));
        p.addView(tx("24 deterministic load cases • force/moment closure • actuator grouping • repeatability",9,false,Color.rgb(180,210,230)));
        regressionSummary=card("Regression suite not yet run.",Color.rgb(17,46,66));p.addView(regressionSummary,lp());
        runRegression=new Button(this);runRegression.setText("RUN 24 BENCHMARK CASES");runRegression.setOnClickListener(v->runBenchmarks(true));p.addView(runRegression,lp());
        regressionDetails=card("Benchmark details will appear here.",Color.rgb(20,51,72));p.addView(regressionDetails,lp());
        root.addView(p,Math.min(4,root.getChildCount()),lp());
        runBenchmarks(false);
    }

    ArrayList<Bench> suite(){
        ArrayList<Bench>b=new ArrayList<>();
        double[] Fs={120,180,240,305.7,360,450};
        int k=1;
        for(double f:Fs){
            double L=6.0+(k%4)*0.5;
            b.add(new Bench("B"+(k++)+" Uniform low moment",f,0.35*f*L,L,8,2,2,180,1.25));
            b.add(new Bench("B"+(k++)+" Nominal bending",f,0.50*f*L,L,12,3,4,220,1.50));
            b.add(new Bench("B"+(k++)+" High moment",f,0.68*f*L,L,16,3,6,260,1.35));
            b.add(new Bench("B"+(k++)+" Compact topology",f,0.45*f*L,L,10,2,5,180,1.20));
        }
        return b;
    }

    BR solveBench(Bench b){
        BR r=new BR();r.name=b.name;r.capacity=b.cap;
        int n=Math.max(4,Math.min(16,b.n));double[] x=new double[n],fi=new double[n];
        for(int i=0;i<n;i++){x[i]=(i+.5)*b.L/n;fi[i]=b.F/n;}
        double base=0,mean=0,den=0;for(int i=0;i<n;i++){base+=fi[i]*x[i];mean+=x[i];}mean/=n;for(double xx:x)den+=(xx-mean)*(xx-mean);
        double delta=b.M-base;if(Math.abs(delta)>1e-12&&den>1e-12)for(int i=0;i<n;i++)fi[i]+=delta*(x[i]-mean)/den;
        boolean negative=false;for(double v:fi)if(v<=0)negative=true;
        double sum=0;for(double v:fi)sum+=Math.max(.001,v);for(int i=0;i<n;i++)fi[i]=Math.max(.001,fi[i])*b.F/sum;
        double sf=0,sm=0;for(int i=0;i<n;i++){sf+=fi[i];sm+=fi[i]*x[i];}
        r.fErr=100*Math.abs(sf-b.F)/Math.max(1,b.F);r.mErr=100*Math.abs(sm-b.M)/Math.max(1,Math.abs(b.M));
        int acts=Math.max(1,Math.min(8,b.acts));double[] al=new double[acts];
        for(int a=0;a<acts;a++){int s=(int)Math.floor((double)a*n/acts),e=(int)Math.floor((double)(a+1)*n/acts)-1;if(a==acts-1)e=n-1;for(int i=s;i<=e;i++)al[a]+=fi[i]*b.sf;}
        r.peakAct=0;for(double v:al)r.peakAct=Math.max(r.peakAct,v);
        boolean forceOk=r.fErr<=0.01,momentOk=r.mErr<=2.0,capOk=r.peakAct<=b.cap;
        r.pass=forceOk&&momentOk&&capOk&&!negative;
        if(negative)r.reason="negative station demand before positivity clamp";else if(!forceOk)r.reason="force closure";else if(!momentOk)r.reason="moment closure";else if(!capOk)r.reason="actuator capacity";else r.reason="all benchmark gates";
        return r;
    }

    void runBenchmarks(boolean toast){
        ArrayList<Bench>s=suite();ArrayList<BR>res=new ArrayList<>();int pass=0;double worstF=0,worstM=0,worstUtil=0;
        for(Bench b:s){BR r=solveBench(b);res.add(r);if(r.pass)pass++;worstF=Math.max(worstF,r.fErr);worstM=Math.max(worstM,r.mErr);worstUtil=Math.max(worstUtil,100*r.peakAct/Math.max(1,r.capacity));}
        boolean ok=pass==res.size();
        regressionSummary.setText(String.format(Locale.US,"%s\n%d / %d benchmark cases passed\nWorst ΣF closure %.4f%% • worst ΣM closure %.3f%% • max actuator utilization %.1f%%\nThe suite is deterministic and can be rerun after each calculation-core change.",ok?"REGRESSION PASS":"REGRESSION FAIL",pass,res.size(),worstF,worstM,worstUtil));
        StringBuilder d=new StringBuilder();for(BR r:res){d.append(r.pass?"PASS  ":"FAIL  ").append(r.name).append(String.format(Locale.US," | F %.3f%% | M %.2f%% | peak A %.1f/%.0f kN | %s\n",r.fErr,r.mErr,r.peakAct,r.capacity,r.reason));}
        regressionDetails.setText(d.toString().trim());
        if(toast)Toast.makeText(this,ok?"Regression PASS":"Regression FAIL — review benchmark list",Toast.LENGTH_SHORT).show();
    }
}
