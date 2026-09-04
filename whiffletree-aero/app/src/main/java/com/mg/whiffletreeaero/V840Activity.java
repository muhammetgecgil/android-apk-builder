package com.mg.whiffletreeaero;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.util.*;

/**
 * Whiffletree Aero v8.4
 * Simple reverse load-flow calculator:
 * REGION/PAD Fy -> unequal-arm whiffletree layers -> root/cylinder force.
 *
 * One planar, collinear Fy load train is intentionally kept simple.
 * Opposite-signed non-zero pad forces are rejected for one passive tree.
 */
public class V840Activity extends Activity {

  LinearLayout root, rowsPanel;
  EditText eCount, eLayers, eSpan;
  TextView status, summary, details;
  Button buildBtn, solveBtn;
  TreeView treeView;
  final ArrayList<InputRow> rows = new ArrayList<>();

  static class InputRow {
    int index;
    EditText x, fy;
  }

  static class Node {
    String id;
    double x, fy;
    int level;
    Node(String id,double x,double fy,int level){this.id=id;this.x=x;this.fy=fy;this.level=level;}
  }

  static class Beam {
    String id,leftId,rightId;
    int level;
    double leftX,rightX,pivotX,leftFy,rightFy,outFy;
    double leftArm,rightArm,span,leftShare,rightShare,momentResidual;
  }

  static class Solution {
    ArrayList<Node> pads=new ArrayList<>();
    ArrayList<Beam> beams=new ArrayList<>();
    Node root;
    int requestedLayers,effectiveLayers,minimumLayers;
    double minX,maxX;
    String warning="";
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    setTitle("Whiffletree Aero");
    buildUi();
    buildRows();
  }

  void buildUi(){
    ScrollView sv=new ScrollView(this);
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(14),dp(12),dp(28));root.setBackgroundColor(Color.rgb(5,18,28));
    sv.addView(root,new ScrollView.LayoutParams(-1,-2));setContentView(sv);

    TextView title=txt("WHIFFLETREE AERO",25,true,Color.WHITE);root.addView(title,lp());
    TextView sub=txt("TERS YÜK AKIŞI  •  BÖLGE/PAD Fy → TREE → SİLİNDİR",13,true,Color.rgb(93,211,235));root.addView(sub,lp());
    root.addView(card("Sadece gerekenleri gir:\n1) Bölge/Pad sayısı\n2) Whiffletree layer limiti\n3) Her pad için x pozisyonu ve Fy\n\nAUTO HESAPLA; kuvveti Layer-1'den başlayıp root ve silindire kadar çıkarır. 50/50 zorunluluğu yoktur; her pivot moment dengesiyle hesaplanır.",Color.rgb(10,47,63)),lp());

    LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
    eCount=num("4",false);eLayers=num("2",false);eSpan=num("3000",true);
    top.addView(field("Bölge/Pad",eCount),new LinearLayout.LayoutParams(0,-2,1));
    top.addView(field("Layer limiti",eLayers),new LinearLayout.LayoutParams(0,-2,1));
    top.addView(field("Açıklık [mm]",eSpan),new LinearLayout.LayoutParams(0,-2,1.2f));
    root.addView(top,lp());

    buildBtn=button("BÖLGELERİ OLUŞTUR",v->buildRows());root.addView(buildBtn,new LinearLayout.LayoutParams(-1,dp(54)));
    rowsPanel=new LinearLayout(this);rowsPanel.setOrientation(LinearLayout.VERTICAL);root.addView(rowsPanel,lp());

    solveBtn=button("AUTO HESAPLA  •  PAD → TREE → SİLİNDİR",v->solve());solveBtn.setTextSize(14);root.addView(solveBtn,new LinearLayout.LayoutParams(-1,dp(66)));

    status=card("Bölge kuvvetlerini girip AUTO HESAPLA'ya bas.",Color.rgb(13,48,61));root.addView(status,lp());
    summary=card("Henüz hesap yapılmadı.",Color.rgb(16,44,56));root.addView(summary,lp());

    treeView=new TreeView(this);treeView.setMinimumHeight(dp(430));root.addView(treeView,new LinearLayout.LayoutParams(-1,dp(430)));
    details=card("Katman detayları burada gösterilecek.",Color.rgb(12,35,47));root.addView(details,lp());
  }

  void buildRows(){
    int n=ival(eCount,4,1,32);
    double span=dval(eSpan,3000);
    if(span<=0)span=3000;
    rows.clear();rowsPanel.removeAllViews();
    rowsPanel.addView(txt("PAD / BÖLGE        x [mm]                 Fy [kN]",11,true,Color.rgb(249,211,83)),lp());
    for(int i=0;i<n;i++){
      InputRow r=new InputRow();r.index=i;
      double x=n==1?0:(-span/2.0+i*span/(n-1.0));
      r.x=num(String.format(Locale.US,"%.1f",x),true);
      r.fy=num("0.0",true);
      LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);
      TextView id=txt("P"+(i+1),12,true,Color.WHITE);id.setGravity(Gravity.CENTER);line.addView(id,new LinearLayout.LayoutParams(dp(58),dp(52)));
      line.addView(r.x,new LinearLayout.LayoutParams(0,dp(52),1));
      line.addView(r.fy,new LinearLayout.LayoutParams(0,dp(52),1));
      rows.add(r);rowsPanel.addView(line,lp());
    }
    status.setText("Pad tablosu hazır. x [mm] ve signed Fy [kN] değerlerini gir. Aynı pasif tree içindeki non-zero Fy kuvvetleri aynı yönde olmalı.");
    summary.setText("Henüz hesap yapılmadı.");details.setText("Katman detayları burada gösterilecek.");treeView.setSolution(null);
  }

  void solve(){
    hideKeyboard();
    int n=ival(eCount,4,1,32), requested=ival(eLayers,2,0,8);
    if(rows.size()!=n){buildRows();status.setText("Pad sayısı değiştiği için tablo yenilendi. Kuvvetleri girip tekrar AUTO HESAPLA.");return;}

    ArrayList<Node> pads=new ArrayList<>();
    int sign=0;double prevX=Double.NEGATIVE_INFINITY;
    for(int i=0;i<rows.size();i++){
      double x=dval(rows.get(i).x,Double.NaN),fyKn=dval(rows.get(i).fy,Double.NaN);
      if(!finite(x)||!finite(fyKn)){status.setText("GİRİŞ HATASI: P"+(i+1)+" x ve Fy sayısal olmalı.");return;}
      if(x<=prevX){status.setText("GEOMETRİ HATASI: x pozisyonları soldan sağa kesin artmalı. P"+(i+1)+" konumunu kontrol et.");return;}
      prevX=x;
      if(Math.abs(fyKn)>1e-12){int s=fyKn>0?1:-1;if(sign==0)sign=s;else if(sign!=s){status.setText("TEK PASİF TREE UYGUN DEĞİL: aynı ağaçta hem +Fy hem -Fy var. Zıt yönlü yükler için ayrı load-train/tree kullan.");return;}}
      pads.add(new Node("P"+(i+1),x,fyKn*1000.0,0));
    }

    int minimum=ceilLog2(n);
    if(requested<minimum){status.setText("LAYER YETERSİZ: "+n+" pad için tek root/silindire ulaşmak üzere en az "+minimum+" layer gerekir. Layer limitini artır.");return;}

    Solution s=new Solution();s.pads.addAll(pads);s.requestedLayers=requested;s.minimumLayers=minimum;
    s.minX=pads.get(0).x;s.maxX=pads.get(pads.size()-1).x;
    ArrayList<Node> current=new ArrayList<>(pads);
    int level=1,beamNo=1;
    while(current.size()>1){
      ArrayList<Node> next=new ArrayList<>();
      for(int i=0;i<current.size();i+=2){
        if(i+1>=current.size()){
          Node c=current.get(i);
          next.add(new Node(c.id,c.x,c.fy,level));
          continue;
        }
        Node L=current.get(i),R=current.get(i+1);
        double fl=Math.abs(L.fy),fr=Math.abs(R.fy),sum=fl+fr;
        double span=R.x-L.x;
        if(span<=1e-9){status.setText("GEOMETRİ HATASI: "+L.id+" ve "+R.id+" aynı uygulama konumuna geldi.");return;}
        Beam bm=new Beam();bm.id="L"+level+"-B"+(beamNo++);bm.leftId=L.id;bm.rightId=R.id;bm.level=level;bm.leftX=L.x;bm.rightX=R.x;bm.leftFy=L.fy;bm.rightFy=R.fy;bm.outFy=L.fy+R.fy;bm.span=span;
        if(sum<=1e-12){bm.pivotX=(L.x+R.x)/2.0;bm.leftArm=span/2.0;bm.rightArm=span/2.0;bm.leftShare=0;bm.rightShare=0;}
        else {bm.pivotX=(fl*L.x+fr*R.x)/sum;bm.leftArm=bm.pivotX-L.x;bm.rightArm=R.x-bm.pivotX;bm.leftShare=fl/sum;bm.rightShare=fr/sum;}
        bm.momentResidual=fl*bm.leftArm-fr*bm.rightArm;
        s.beams.add(bm);
        next.add(new Node(bm.id,bm.pivotX,bm.outFy,level));
      }
      current=next;level++;
    }
    s.effectiveLayers=Math.max(0,level-1);s.root=current.get(0);
    if(requested>s.effectiveLayers)s.warning="Girilen layer limiti "+requested+"; bu pad sayısı için fiziksel olarak "+s.effectiveLayers+" beam layer yeterli. Ek layer gerekmiyor.";
    render(s);
  }

  void render(Solution s){
    double sum=0;for(Node p:s.pads)sum+=p.fy;
    double cyl=Math.abs(sum)/1000.0;
    String direction=sum>0?"+Fy":(sum<0?"-Fy":"0");
    summary.setText(String.format(Locale.US,
      "SONUÇ\nPad: %d  •  Effective tree layer: %d  •  Layer limiti: %d\n\nGEREKLİ SİLİNDİR KUVVETİ = %.3f kN\nSigned root Fy = %+.3f kN  (%s)\nSİLİNDİR / ROOT UYGULAMA x = %+.1f mm\n\nBu x, girilen pad kuvvetlerinin eşdeğer kuvvet hattıdır. Silindir seçimi yapılmaz; yalnız gerekli kuvvet ve yük hattı hesaplanır.%s",
      s.pads.size(),s.effectiveLayers,s.requestedLayers,cyl,sum/1000.0,direction,s.root.x,s.warning.isEmpty()?"":"\n\nNOT: "+s.warning));

    TreeMap<Integer,ArrayList<Beam>> by=new TreeMap<>();for(Beam b:s.beams){if(!by.containsKey(b.level))by.put(b.level,new ArrayList<>());by.get(b.level).add(b);}
    StringBuilder d=new StringBuilder();
    d.append("KUVVET YOLU — AŞAĞIDAN YUKARI\n");
    d.append("PAD\n");for(Node p:s.pads)d.append(String.format(Locale.US,"%s  x=%+.1f mm  Fy=%+.3f kN\n",p.id,p.x,p.fy/1000.0));
    for(Map.Entry<Integer,ArrayList<Beam>> e:by.entrySet()){
      d.append("\nLAYER-").append(e.getKey()).append("\n");
      for(Beam b:e.getValue()){
        double total=Math.abs(b.leftFy)+Math.abs(b.rightFy);
        double slider=b.span>0?100.0*b.leftArm/b.span:50;
        d.append(String.format(Locale.US,
          "%s: %s %+.3f + %s %+.3f → %+.3f kN\n  span %.1f mm • pivot x=%+.1f mm • kol L/R %.1f / %.1f mm\n  yük payı L/R %.1f%% / %.1f%% • slider soldan %.1f%% • |ΣM| %.6f kN·mm\n",
          b.id,b.leftId,b.leftFy/1000.0,b.rightId,b.rightFy/1000.0,b.outFy/1000.0,b.span,b.pivotX,b.leftArm,b.rightArm,100*b.leftShare,100*b.rightShare,slider,Math.abs(b.momentResidual)/1000.0));
        if(total>1e-9 && (b.leftShare<.05||b.rightShare<.05))d.append("  ⚠ Pivot uca çok yakın; gerçek slider/beam imalatı ayrıca kontrol edilmeli.\n");
      }
    }
    d.append(String.format(Locale.US,"\nROOT → SİLİNDİR\nFy = %+.3f kN\nGerekli silindir kuvveti = %.3f kN\nSilindir ekseni x = %+.1f mm\n",sum/1000.0,cyl,s.root.x));
    details.setText(d.toString());
    status.setText("AUTO HESAP TAMAMLANDI — pad kuvvetlerinden whiffletree katmanları ve silindir kuvveti aşağıdan yukarı hesaplandı.");
    treeView.setSolution(s);
  }

  int ceilLog2(int n){if(n<=1)return 0;int l=0,p=1;while(p<n){p*=2;l++;}return l;}
  boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
  int ival(EditText e,int def,int lo,int hi){try{return Math.max(lo,Math.min(hi,Integer.parseInt(e.getText().toString().trim())));}catch(Exception x){return def;}}
  double dval(EditText e,double def){try{return Double.parseDouble(e.getText().toString().trim().replace(',','.'));}catch(Exception x){return def;}}
  void hideKeyboard(){try{((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);}catch(Exception ignored){}}

  EditText num(String s,boolean signed){EditText e=new EditText(this);e.setText(s);e.setSelectAllOnFocus(true);e.setSingleLine(true);e.setGravity(Gravity.CENTER);e.setTextColor(Color.WHITE);e.setTextSize(13);e.setBackground(box(Color.rgb(18,52,66),8));int t=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL;if(signed)t|=InputType.TYPE_NUMBER_FLAG_SIGNED;e.setInputType(t);e.setPadding(dp(6),0,dp(6),0);return e;}
  LinearLayout field(String label,EditText e){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setPadding(dp(3),dp(4),dp(3),dp(4));TextView t=txt(label,10,true,Color.rgb(187,215,226));t.setGravity(Gravity.CENTER);l.addView(t,new LinearLayout.LayoutParams(-1,dp(28)));l.addView(e,new LinearLayout.LayoutParams(-1,dp(48)));return l;}
  Button button(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(box(Color.rgb(8,103,133),12));b.setOnClickListener(l);return b;}
  TextView txt(String s,float z,boolean bold,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setPadding(dp(8),dp(6),dp(8),dp(6));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
  TextView card(String s,int c){TextView t=txt(s,12,false,Color.WHITE);t.setBackground(box(c,12));t.setPadding(dp(12),dp(12),dp(12),dp(12));return t;}
  GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));g.setStroke(dp(1),Color.rgb(42,77,91));return g;}
  LinearLayout.LayoutParams lp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));return p;}
  int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}

  class TreeView extends View {
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);Solution s;
    HashMap<String,Node> nodes=new HashMap<>();
    TreeView(Context c){super(c);setBackground(box(Color.rgb(7,28,40),12));}
    void setSolution(Solution x){s=x;invalidate();}
    float sx(double x){if(s==null)return 0;double span=Math.max(1,s.maxX-s.minX);return (float)(dp(34)+(x-s.minX)/span*(getWidth()-dp(68)));}
    float sy(int level){int max=Math.max(1,s==null?1:s.effectiveLayers);float bottom=getHeight()-dp(48),top=dp(75);return bottom-(bottom-top)*(float)level/(max+0.35f);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(s==null||s.root==null){p.setColor(Color.LTGRAY);p.setTextSize(dp(13));c.drawText("AUTO HESAP sonrası whiffletree burada çizilecek.",dp(18),dp(40),p);return;}
      nodes.clear();for(Node n:s.pads)nodes.put(n.id,n);for(Beam b:s.beams)nodes.put(b.id,new Node(b.id,b.pivotX,b.outFy,b.level));
      p.setStrokeWidth(dp(2));p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(130,202,225));
      for(Beam b:s.beams){Node L=nodes.get(b.leftId),R=nodes.get(b.rightId);float y=sy(b.level),yl=sy(L==null?0:L.level),yr=sy(R==null?0:R.level);float xl=sx(b.leftX),xr=sx(b.rightX),xp=sx(b.pivotX);c.drawLine(xl,yl,xl,y,p);c.drawLine(xr,yr,xr,y,p);c.drawLine(xl,y,xr,y,p);c.drawLine(xp,y,xp,y-dp(18),p);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(250,202,67));c.drawCircle(xp,y,dp(5),p);p.setColor(Color.WHITE);p.setTextSize(dp(9));c.drawText(b.id+"  "+String.format(Locale.US,"%.1fkN",Math.abs(b.outFy)/1000.0),xp-dp(30),y-dp(8),p);p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(130,202,225));}
      p.setStyle(Paint.Style.FILL);for(Node n:s.pads){float x=sx(n.x),y=sy(0);p.setColor(Color.rgb(76,220,166));c.drawCircle(x,y,dp(6),p);p.setColor(Color.WHITE);p.setTextSize(dp(9));c.drawText(n.id,x-dp(8),y+dp(18),p);c.drawText(String.format(Locale.US,"%+.1f",n.fy/1000.0),x-dp(15),y+dp(30),p);}
      float rx=sx(s.root.x),ry=sy(s.effectiveLayers);p.setStrokeWidth(dp(3));p.setColor(Color.rgb(239,111,88));c.drawLine(rx,ry-dp(18),rx,dp(42),p);p.setStyle(Paint.Style.FILL);c.drawRect(rx-dp(13),dp(16),rx+dp(13),dp(42),p);p.setColor(Color.WHITE);p.setTextSize(dp(11));c.drawText("SİLİNDİR",Math.max(dp(6),rx-dp(30)),dp(13),p);c.drawText(String.format(Locale.US,"%.2f kN",Math.abs(s.root.fy)/1000.0),Math.max(dp(6),rx-dp(26)),dp(58),p);
    }
  }
}
