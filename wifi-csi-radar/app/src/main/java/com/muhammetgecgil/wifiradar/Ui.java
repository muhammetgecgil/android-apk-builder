package com.muhammetgecgil.wifiradar;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class Ui {
    static final int BG=Color.rgb(6,16,26), PANEL=Color.rgb(13,33,48), PANEL2=Color.rgb(17,43,61);
    static final int GREEN=Color.rgb(37,230,167), CYAN=Color.rgb(76,201,240), YELLOW=Color.rgb(255,209,102), RED=Color.rgb(255,99,99), WHITE=Color.rgb(240,248,250), MUTED=Color.rgb(139,177,185);
    private Ui() {}
    static int dp(Activity a,float v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    static TextView text(Activity a,String s,float sp,int color,boolean bold){TextView t=new TextView(a);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.08f);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    static GradientDrawable round(Activity a,int fill,int stroke,float radius){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(a,radius));if(stroke!=0)d.setStroke(dp(a,1),stroke);return d;}
    static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(a,2),dp(a,2),dp(a,2),dp(a,18));return l;}
    static LinearLayout row(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);l.setPadding(0,dp(a,3),0,dp(a,3));return l;}
    static ScrollView scroll(Activity a){ScrollView s=new ScrollView(a);s.setFillViewport(true);s.setClipToPadding(false);return s;}
    static Button button(Activity a,String label,int color,View.OnClickListener click){Button b=new Button(a);b.setText(label);b.setTextSize(12);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(color==GREEN||color==CYAN||color==YELLOW?a.getColor(android.R.color.black):WHITE);b.setBackground(round(a,color,0,12));b.setOnClickListener(click);return b;}
    static LinearLayout.LayoutParams weight(Activity a){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(a,48),1);p.setMargins(dp(a,3),0,dp(a,3),0);return p;}
    static TextView metric(Activity a){TextView t=text(a,"—",13,WHITE,false);t.setPadding(dp(a,12),dp(a,10),dp(a,12),dp(a,10));t.setBackground(round(a,PANEL2,0,12));return t;}
    static View card(Activity a,String title,String body,int accent,View.OnClickListener click){LinearLayout box=column(a);box.setPadding(dp(a,16),dp(a,14),dp(a,16),dp(a,14));box.setBackground(round(a,PANEL,accent,16));box.addView(text(a,title,18,accent,true));TextView b=text(a,body,13,WHITE,false);b.setPadding(0,dp(a,5),0,0);box.addView(b);box.setOnClickListener(click);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.setMargins(0,dp(a,8),0,dp(a,8));box.setLayoutParams(lp);return box;}
    static LinearLayout shell(Activity a,String title){
        a.getWindow().setStatusBarColor(Color.TRANSPARENT);a.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(dp(a,14),dp(a,8),dp(a,14),dp(a,8));
        root.setOnApplyWindowInsetsListener((v,insets)->{int top,bottom;if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());top=bars.top;bottom=bars.bottom;}else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}v.setPadding(dp(a,14),top+dp(a,8),dp(a,14),bottom+dp(a,8));return insets;});
        TextView t=text(a,title,21,WHITE,true);t.setGravity(Gravity.CENTER_VERTICAL);root.addView(t,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(a,48)));return root;
    }
    static TextView note(Activity a,String s){TextView t=text(a,s,12,MUTED,false);t.setPadding(dp(a,12),dp(a,10),dp(a,12),dp(a,10));t.setBackground(round(a,PANEL2,0,12));return t;}
}
