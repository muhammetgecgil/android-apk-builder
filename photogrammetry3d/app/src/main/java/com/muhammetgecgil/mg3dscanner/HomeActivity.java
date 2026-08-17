package com.muhammetgecgil.mg3dscanner;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.*;import android.view.*;import android.widget.*;

public class HomeActivity extends Activity{
 private TextView txt(String s,int z){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(z);v.setPadding(22,16,22,16);return v;}
 private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(18);b.setMinHeight(72);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(12,13,16));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(24,32,24,32);r.setBackgroundColor(Color.rgb(12,13,16));TextView t=txt("MG 3D STUDIO",30);t.setGravity(Gravity.CENTER);r.addView(t);TextView s=txt("3D tarama • fotogrametri • profesyonel dosya görüntüleyici",15);s.setTextColor(Color.LTGRAY);s.setGravity(Gravity.CENTER);r.addView(s);Space sp=new Space(this);r.addView(sp,new LinearLayout.LayoutParams(1,30));Button scan=btn("📷 3D TARA / MODEL ÜRET");scan.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));r.addView(scan);Button view=btn("🧊 3D DOSYA AÇ / VIEWER");view.setOnClickListener(v->startActivity(new Intent(this,ViewerActivity.class)));r.addView(view);TextView f=txt("Desteklenen görüntüleme: OBJ • STL • PLY • XYZ • GLB • STEP/STP • IGES/IGS\n\nViewer: 360° döndürme • pinch zoom • pan benzeri sürükleme • nokta/mesh görünümü • otomatik merkezleme ve ölçekleme.",14);f.setTextColor(Color.LTGRAY);r.addView(f);setContentView(r);}
}
