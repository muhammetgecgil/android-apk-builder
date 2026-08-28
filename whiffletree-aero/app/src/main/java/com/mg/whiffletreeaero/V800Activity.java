package com.mg.whiffletreeaero;

import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

/**
 * v8.0.0 — 6DOF moment-driven aerospace whiffletree synthesis.
 *
 * Core model:
 *   A*q = [Fx Fy Fz Mx My Mz]^T
 *
 * Each pad i has a position r_i=[x y z] and an allowed unit load direction u_i.
 * The unknown scalar q_i generates:
 *   F_i = q_i*u_i
 *   M_i = r_i x F_i
 *
 * Signed mode represents tension/compression load trains.
 * Tension-only mode enforces q_i >= 0 with a simple active-set solve.
 *
 * After pad forces are synthesized they are injected into the existing
 * unequal-arm / multi-layer whiffletree engine. That engine already solves
 * passive-beam pivots from FL*a = FR*b and checks force/moment closure.
 */
public class V800Activity extends V719Activity {
  LinearLayout dofPanel,padPanel;
  TextView dofResult,authorityCard,familyCard;
  Button momentEntry,buildPresetBtn,solveBtn,backToWizardBtn;
  EditText tFx,tFy,tFz,tMx,tMy,tMz,padCount;
  Spinner presetSpinner,constraintSpinner;
  ArrayList<PadRow> padRows=new ArrayList<>();
  Solve6 last6;

  static class PadRow {
    EditText x,y,z,ux,uy,uz;
  }

  static class Solve6 {
    double[] q;
    double[] target=new double[6];
    double[] applied=new double[6];
    double[] residual=new double[6];
    double[][] rawA;
    double[][] scaledA;
    double[] ux,uy,uz,x,y,z;
    boolean[] active;
    int rank;
    boolean feasible;
    boolean tensionOnly;
    int removed;
    double forceErrorPct,momentErrorPct,maxAbsQ,directionSpread;
    String family,warning;
  }

  @Override public void onCreate(Bundle b){
    super.onCreate(b);

    momentEntry=designBtn("6DOF • MOMENT DRIVEN LOAD TREE",v->show6Dof());
    momentEntry.setTextSize(12);
    wizard.addView(momentEntry,Math.min(2,wizard.getChildCount()),new LinearLayout.LayoutParams(-1,dp(60)));

    dofPanel=new LinearLayout(this);
    dofPanel.setOrientation(LinearLayout.VERTICAL);
    dofPanel.setPadding(dp(8),dp(8),dp(8),dp(14));
    dofPanel.setBackground(bg(Color.rgb(5,25,39),14));
    dofPanel.setVisibility(View.GONE);
    wizard.addView(dofPanel,Math.min(3,wizard.getChildCount()),lp());

    build6DofPanel();
  }

  void build6DofPanel(){
    dofPanel.removeAllViews();

    dofPanel.addView(card(
      "v8 • 6DOF MOMENT-DRIVEN TREE\n"+
      "Hedef [Fx,Fy,Fz,Mx,My,Mz] ver. Her pad için konum [x,y,z] ve izin verilen yük yönünü [ux,uy,uz] tanımla. "+
      "Solver gerekli pad kuvvetlerini çözer; sonra gerçek asimetrik pivot / multi-layer tree motoruna aktarır.",
      Color.rgb(11,55,72)),lp());

    presetSpinner=new Spinner(this);
    presetSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
      "VERTICAL WING / EFT — Fz + Mx/My",
      "LE/TE TORSION PAIR — vertical",
      "FULL 6DOF MIXED-AXIS — 12 pad",
      "CUSTOM PAD DIRECTIONS"
    }));

    constraintSpinner=new Spinner(this);
    constraintSpinner.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{
      "TENSION / COMPRESSION — signed q",
      "TENSION ONLY — q >= 0"
    }));

    padCount=fresh("8");
    dofPanel.addView(selector("Gerçek-dünya load-tree şablonu",presetSpinner),lp());
    dofPanel.addView(selector("Load-train fiziksel modu",constraintSpinner),lp());
    dofPanel.addView(field("Pad sayısı",padCount,"2–24. FULL 6DOF şablonu en az 12 pad kullanır."),lp());

    tFx=fresh(hFx.getText().toString());
    tFy=fresh(hFy.getText().toString());
    tFz=fresh(hFz.getText().toString());
    tMx=fresh("0");tMy=fresh("0");tMz=fresh("0");

    dofPanel.addView(tx("HEDEF 6DOF — kuvvet [N], moment [Nmm]",12,true,Color.rgb(247,207,77)),lp());
    dofPanel.addView(pairFields("Fx",tFx,"Fy",tFy),lp());
    dofPanel.addView(pairFields("Fz",tFz,"Mx",tMx),lp());
    dofPanel.addView(pairFields("My",tMy,"Mz",tMz),lp());

    buildPresetBtn=designBtn("PAD GEOMETRİSİNİ OLUŞTUR",v->generatePreset());
    dofPanel.addView(buildPresetBtn,new LinearLayout.LayoutParams(-1,dp(56)));

    authorityCard=card(
      "DOF AUTHORITY\nPad geometrisi oluşturulduğunda sistem matrisinin rank değeri hesaplanır. "+
      "Rank < 6 ise her altı kuvvet/moment bileşeni bağımsız üretilemez; yazılım bunu çözüm diye gizlemez.",
      Color.rgb(25,45,59));
    dofPanel.addView(authorityCard,lp());

    padPanel=new LinearLayout(this);
    padPanel.setOrientation(LinearLayout.VERTICAL);
    dofPanel.addView(padPanel,lp());

    familyCard=card(
      "GERÇEK AĞAÇ AİLELERİ\n"+
      "• unequal-arm 2-way beam\n• multi-level passive tree\n• LE/TE torsion pair\n"+
      "• tension-only cable tree\n• tension/compression tree\n• separate-axis / multi-actuator 3D tree",
      Color.rgb(30,45,49));
    dofPanel.addView(familyCard,lp());

    solveBtn=designBtn("6DOF ÇÖZ • TREE'Yİ SENTEZLE",v->solveAndBuild6Dof());
    solveBtn.setTextSize(12);
    dofPanel.addView(solveBtn,new LinearLayout.LayoutParams(-1,dp(66)));

    dofResult=card("Henüz 6DOF çözümü yok.",Color.rgb(12,48,57));
    dofPanel.addView(dofResult,lp());

    backToWizardBtn=designBtn("← KLASİK WHIFFLETREE WIZARD",v->leave6Dof());
    dofPanel.addView(backToWizardBtn,new LinearLayout.LayoutParams(-1,dp(50)));

    generatePreset();
  }

  LinearLayout pairFields(String a,EditText ea,String b,EditText eb){
    LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
    r.addView(field(a,ea,""),new LinearLayout.LayoutParams(0,-2,1));
    r.addView(field(b,eb,""),new LinearLayout.LayoutParams(0,-2,1));
    return r;
  }

  void show6Dof(){
    momentEntry.setVisibility(View.GONE);
    body.setVisibility(View.GONE);navRow.setVisibility(View.GONE);auto.setVisibility(View.GONE);
    advanced.setVisibility(View.GONE);resultBox.setVisibility(View.GONE);
    dofPanel.setVisibility(View.VISIBLE);
    title.setText("6DOF AEROSPACE LOAD TREE");
    status.setText("Kuvvet + moment hedeflerinden fiziksel pad yükleri ve uygun whiffletree/load-train ailesi çözülecek.");
  }

  void leave6Dof(){
    dofPanel.setVisibility(View.GONE);
    momentEntry.setVisibility(View.VISIBLE);
    resultBox.setVisibility(View.VISIBLE);
    showStep(1);
  }

  @Override public void onBackPressed(){
    if(dofPanel!=null&&dofPanel.getVisibility()==View.VISIBLE){leave6Dof();return;}
    super.onBackPressed();
  }

  EditText cell(String s){
    EditText e=new EditText(this);
    e.setText(s);e.setTextColor(Color.WHITE);e.setTextSize(11);e.setSingleLine(true);
    e.setSelectAllOnFocus(true);e.setGravity(Gravity.CENTER);
    e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
    e.setBackground(bg(Color.rgb(21,50,66),7));
    e.setPadding(dp(3),0,dp(3),0);
    return e;
  }

  double d(EditText e){
    try{return Double.parseDouble(e.getText().toString().trim());}
    catch(Exception ex){return Double.NaN;}
  }

  int wantedPads(){
    double v=d(padCount);
    int n=Double.isNaN(v)?8:(int)Math.round(v);
    return Math.max(2,Math.min(24,n));
  }

  void generatePreset(){
    int preset=presetSpinner.getSelectedItemPosition();
    int n=wantedPads();
    if(preset==2&&n<12){n=12;padCount.setText("12");}
    if(preset==1&&n<4){n=4;padCount.setText("4");}

    double L=Math.max(.1,val(hLen))*1000.0;
    double D=Math.max(.05,val(hDia))*1000.0;
    double targetFz=d(tFz);
    double zSign=(Double.isNaN(targetFz)||Math.abs(targetFz)<1e-9)?1:Math.signum(targetFz);

    padRows.clear();padPanel.removeAllViews();
    padPanel.addView(tx("PAD GEOMETRİSİ — pozisyon [mm], yön birim vektörü",11,true,Color.rgb(247,207,77)),lp());

    for(int i=0;i<n;i++){
      double x=-L/2.0+(i+.5)*L/n;
      double y=0,z=0,ux=0,uy=0,uz=zSign;

      if(preset==0){
        y=(i%2==0?-1:1)*D*.35;
        z=0;ux=0;uy=0;uz=zSign;
      } else if(preset==1){
        int station=i/2;
        int stationCount=Math.max(2,(n+1)/2);
        x=-L/2.0+(station+.5)*L/stationCount;
        y=(i%2==0?-1:1)*D*.45;
        z=0;ux=0;uy=0;uz=zSign;
      } else if(preset==2){
        int seg=i/3;
        int segCount=Math.max(4,(int)Math.ceil(n/3.0));
        x=-L/2.0+(seg+.5)*L/segCount;
        y=(seg%2==0?-1:1)*D*.50;
        z=((seg/2)%2==0?-1:1)*D*.33;
        int axis=i%3;
        double sign=((seg+i)%2==0)?1:-1;
        ux=axis==0?sign:0;uy=axis==1?sign:0;uz=axis==2?sign:0;
      } else {
        y=(i%2==0?-1:1)*D*.30;
        z=0;ux=0;uy=0;uz=zSign;
      }

      addPadRow(i,x,y,z,ux,uy,uz);
    }

    updateAuthorityPreview();
  }

  void addPadRow(int index,double x,double y,double z,double ux,double uy,double uz){
    PadRow p=new PadRow();
    p.x=cell(f1(x));p.y=cell(f1(y));p.z=cell(f1(z));
    p.ux=cell(f3(ux));p.uy=cell(f3(uy));p.uz=cell(f3(uz));
    padRows.add(p);

    LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);
    c.setPadding(dp(6),dp(6),dp(6),dp(7));c.setBackground(bg(Color.rgb(10,38,53),9));
    c.addView(tx("P"+(index+1),10,true,Color.WHITE),lp());

    LinearLayout h1=new LinearLayout(this);h1.setOrientation(LinearLayout.HORIZONTAL);
    h1.addView(tx("x",9,true,Color.rgb(185,215,232)),new LinearLayout.LayoutParams(dp(20),dp(44)));
    h1.addView(p.x,new LinearLayout.LayoutParams(0,dp(44),1));
    h1.addView(tx("y",9,true,Color.rgb(185,215,232)),new LinearLayout.LayoutParams(dp(20),dp(44)));
    h1.addView(p.y,new LinearLayout.LayoutParams(0,dp(44),1));
    h1.addView(tx("z",9,true,Color.rgb(185,215,232)),new LinearLayout.LayoutParams(dp(20),dp(44)));
    h1.addView(p.z,new LinearLayout.LayoutParams(0,dp(44),1));
    c.addView(h1,lp());

    LinearLayout h2=new LinearLayout(this);h2.setOrientation(LinearLayout.HORIZONTAL);
    h2.addView(tx("ux",9,true,Color.rgb(247,207,77)),new LinearLayout.LayoutParams(dp(28),dp(44)));
    h2.addView(p.ux,new LinearLayout.LayoutParams(0,dp(44),1));
    h2.addView(tx("uy",9,true,Color.rgb(247,207,77)),new LinearLayout.LayoutParams(dp(28),dp(44)));
    h2.addView(p.uy,new LinearLayout.LayoutParams(0,dp(44),1));
    h2.addView(tx("uz",9,true,Color.rgb(247,207,77)),new LinearLayout.LayoutParams(dp(28),dp(44)));
    h2.addView(p.uz,new LinearLayout.LayoutParams(0,dp(44),1));
    c.addView(h2,lp());

    padPanel.addView(c,lp());
  }

  String f1(double v){return String.format(Locale.US,"%.1f",v);}
  String f3(double v){return String.format(Locale.US,"%.3f",v);}

  void updateAuthorityPreview(){
    try{
      MatrixBuild mb=readMatrix();
      int rank=matrixRank(mb.scaledA);
      String control=rank>=6?"FULL 6DOF AUTHORITY":"LIMITED AUTHORITY";
      authorityCard.setText(String.format(Locale.US,
        "DOF AUTHORITY — %s\nMatrix rank %d / 6 • %d pad\n"+
        "Rank 6: tanımlanan yön/konum kombinasyonu bağımsız Fx,Fy,Fz,Mx,My,Mz üretebilir.\n"+
        "Rank < 6: yalnız bu geometriyle bazı moment/kuvvet kombinasyonları fiziksel olarak üretilemez.",
        control,rank,padRows.size()));
    }catch(Exception ex){
      authorityCard.setText("DOF AUTHORITY — pad alanlarında geçerli x/y/z ve yön bileşenleri gerekli.");
    }
  }

  static class MatrixBuild {
    double[][] rawA,scaledA;
    double[] ux,uy,uz,x,y,z;
    double lref;
  }

  MatrixBuild readMatrix(){
    int n=padRows.size();
    if(n<2)throw new IllegalArgumentException("En az 2 pad gerekli");
    MatrixBuild m=new MatrixBuild();
    m.rawA=new double[6][n];m.scaledA=new double[6][n];
    m.ux=new double[n];m.uy=new double[n];m.uz=new double[n];
    m.x=new double[n];m.y=new double[n];m.z=new double[n];
    m.lref=Math.max(100.0,Math.max(.1,val(hLen))*1000.0);

    for(int i=0;i<n;i++){
      PadRow p=padRows.get(i);
      double x=d(p.x),y=d(p.y),z=d(p.z),ux=d(p.ux),uy=d(p.uy),uz=d(p.uz);
      if(Double.isNaN(x)||Double.isNaN(y)||Double.isNaN(z)||Double.isNaN(ux)||Double.isNaN(uy)||Double.isNaN(uz))
        throw new IllegalArgumentException("P"+(i+1)+" sayısal değil");
      double un=Math.sqrt(ux*ux+uy*uy+uz*uz);
      if(un<1e-9)throw new IllegalArgumentException("P"+(i+1)+" yönü sıfır");
      ux/=un;uy/=un;uz/=un;
      m.x[i]=x;m.y[i]=y;m.z[i]=z;m.ux[i]=ux;m.uy[i]=uy;m.uz[i]=uz;

      m.rawA[0][i]=ux;
      m.rawA[1][i]=uy;
      m.rawA[2][i]=uz;
      m.rawA[3][i]=y*uz-z*uy;
      m.rawA[4][i]=z*ux-x*uz;
      m.rawA[5][i]=x*uy-y*ux;

      for(int r=0;r<6;r++)m.scaledA[r][i]=m.rawA[r][i];
      m.scaledA[3][i]/=m.lref;
      m.scaledA[4][i]/=m.lref;
      m.scaledA[5][i]/=m.lref;
    }
    return m;
  }

  double[] readTarget(){
    double[] b=new double[]{d(tFx),d(tFy),d(tFz),d(tMx),d(tMy),d(tMz)};
    for(double v:b)if(Double.isNaN(v))throw new IllegalArgumentException("6DOF hedeflerinden biri sayısal değil");
    return b;
  }

  void solveAndBuild6Dof(){
    try{
      MatrixBuild mb=readMatrix();
      double[] target=readTarget();
      boolean tensionOnly=constraintSpinner.getSelectedItemPosition()==1;
      Solve6 s=solve6(mb,target,tensionOnly);
      last6=s;
      apply6DofToCore(s);
      render6Dof(s);
    }catch(Exception ex){
      dofResult.setText("6DOF ÇÖZÜM HATASI\n"+ex.getMessage());
    }
  }

  Solve6 solve6(MatrixBuild mb,double[] target,boolean tensionOnly){
    int n=mb.x.length;
    Solve6 s=new Solve6();
    s.rawA=mb.rawA;s.scaledA=mb.scaledA;
    s.ux=mb.ux;s.uy=mb.uy;s.uz=mb.uz;s.x=mb.x;s.y=mb.y;s.z=mb.z;
    s.target=target.clone();s.tensionOnly=tensionOnly;s.rank=matrixRank(mb.scaledA);
    s.active=new boolean[n];Arrays.fill(s.active,true);

    double[] bs=target.clone();
    bs[3]/=mb.lref;bs[4]/=mb.lref;bs[5]/=mb.lref;

    double[] q=solveMinNorm(mb.scaledA,bs,s.active);

    if(tensionOnly){
      for(int iter=0;iter<n;iter++){
        int worst=-1;double min=-1e-7;
        double scale=1;for(double v:q)scale=Math.max(scale,Math.abs(v));
        double tol=1e-7*scale;
        for(int i=0;i<n;i++)if(s.active[i]&&q[i]<-tol&&(worst<0||q[i]<min)){worst=i;min=q[i];}
        if(worst<0)break;
        s.active[worst]=false;s.removed++;
        q=solveMinNorm(mb.scaledA,bs,s.active);
      }
      for(int i=0;i<n;i++)if(!s.active[i]||q[i]<0&&Math.abs(q[i])<1e-6)q[i]=0;
    }

    s.q=q;
    for(int r=0;r<6;r++){
      double v=0;for(int i=0;i<n;i++)v+=mb.rawA[r][i]*q[i];
      s.applied[r]=v;s.residual[r]=target[r]-v;
    }

    double tf=Math.sqrt(target[0]*target[0]+target[1]*target[1]+target[2]*target[2]);
    double rf=Math.sqrt(s.residual[0]*s.residual[0]+s.residual[1]*s.residual[1]+s.residual[2]*s.residual[2]);
    double tm=Math.sqrt(target[3]*target[3]+target[4]*target[4]+target[5]*target[5]);
    double rm=Math.sqrt(s.residual[3]*s.residual[3]+s.residual[4]*s.residual[4]+s.residual[5]*s.residual[5]);
    s.forceErrorPct=100*rf/Math.max(1,tf);
    s.momentErrorPct=100*rm/Math.max(1,tm);

    for(double v:q)s.maxAbsQ=Math.max(s.maxAbsQ,Math.abs(v));
    s.directionSpread=directionSpread(s);
    s.family=selectFamily(s);
    s.warning=physicalWarning(s);

    boolean forceOK=tf<1?rf<1e-3:s.forceErrorPct<0.5;
    boolean momentOK=tm<1?rm<1e-2:s.momentErrorPct<0.5;
    boolean signOK=true;
    if(tensionOnly)for(double v:q)if(v<-1e-6)signOK=false;
    s.feasible=forceOK&&momentOK&&signOK;
    return s;
  }

  double[] solveMinNorm(double[][] A,double[] b,boolean[] active){
    int n=A[0].length;
    double[][] g=new double[6][6];
    int count=0;
    for(int i=0;i<n;i++)if(active[i]){
      count++;
      for(int r=0;r<6;r++)for(int c=0;c<6;c++)g[r][c]+=A[r][i]*A[c][i];
    }
    if(count==0)return new double[n];

    double trace=0;for(int i=0;i<6;i++)trace+=g[i][i];
    double lambda=Math.max(1e-12,trace*1e-10);
    for(int i=0;i<6;i++)g[i][i]+=lambda;

    double[] y=solveLinear6(g,b);
    double[] q=new double[n];
    for(int i=0;i<n;i++)if(active[i]){
      double v=0;for(int r=0;r<6;r++)v+=A[r][i]*y[r];
      q[i]=v;
    }
    return q;
  }

  double[] solveLinear6(double[][] a,double[] b){
    int n=6;
    double[][] m=new double[n][n+1];
    double max=0;
    for(int r=0;r<n;r++){
      for(int c=0;c<n;c++){m[r][c]=a[r][c];max=Math.max(max,Math.abs(m[r][c]));}
      m[r][n]=b[r];
    }
    double eps=Math.max(1e-14,max*1e-12);

    for(int col=0;col<n;col++){
      int piv=col;for(int r=col+1;r<n;r++)if(Math.abs(m[r][col])>Math.abs(m[piv][col]))piv=r;
      if(Math.abs(m[piv][col])<eps)continue;
      if(piv!=col){double[] tmp=m[piv];m[piv]=m[col];m[col]=tmp;}
      double p=m[col][col];
      for(int c=col;c<=n;c++)m[col][c]/=p;
      for(int r=0;r<n;r++)if(r!=col){
        double f=m[r][col];if(Math.abs(f)<eps)continue;
        for(int c=col;c<=n;c++)m[r][c]-=f*m[col][c];
      }
    }
    double[] x=new double[n];for(int i=0;i<n;i++)x[i]=m[i][n];
    return x;
  }

  int matrixRank(double[][] src){
    int rows=src.length,cols=src[0].length;
    double[][] a=new double[rows][cols];
    double max=0;
    for(int r=0;r<rows;r++)for(int c=0;c<cols;c++){a[r][c]=src[r][c];max=Math.max(max,Math.abs(a[r][c]));}
    double tol=Math.max(1e-10,max*1e-9);
    int rank=0,row=0;
    for(int col=0;col<cols&&row<rows;col++){
      int piv=row;for(int r=row+1;r<rows;r++)if(Math.abs(a[r][col])>Math.abs(a[piv][col]))piv=r;
      if(Math.abs(a[piv][col])<=tol)continue;
      double[] tmp=a[piv];a[piv]=a[row];a[row]=tmp;
      double p=a[row][col];for(int c=col;c<cols;c++)a[row][c]/=p;
      for(int r=0;r<rows;r++)if(r!=row){
        double f=a[r][col];if(Math.abs(f)<=tol)continue;
        for(int c=col;c<cols;c++)a[r][c]-=f*a[row][c];
      }
      row++;rank++;
    }
    return rank;
  }

  double directionSpread(Solve6 s){
    double sx=0,sy=0,sz=0,w=0;
    for(int i=0;i<s.q.length;i++){
      double wi=Math.abs(s.q[i]);if(wi<1e-8)continue;
      double sign=s.q[i]>=0?1:-1;
      sx+=wi*s.ux[i]*sign;sy+=wi*s.uy[i]*sign;sz+=wi*s.uz[i]*sign;w+=wi;
    }
    double nr=Math.sqrt(sx*sx+sy*sy+sz*sz);
    if(w<1e-9||nr<1e-9)return 180;
    sx/=nr;sy/=nr;sz/=nr;
    double max=0;
    for(int i=0;i<s.q.length;i++){
      if(Math.abs(s.q[i])<1e-8)continue;
      double sign=s.q[i]>=0?1:-1;
      double dot=(s.ux[i]*sign)*sx+(s.uy[i]*sign)*sy+(s.uz[i]*sign)*sz;
      dot=Math.max(-1,Math.min(1,dot));
      max=Math.max(max,Math.toDegrees(Math.acos(dot)));
    }
    return max;
  }

  String selectFamily(Solve6 s){
    boolean neg=false;for(double q:s.q)if(q<-1e-6)neg=true;
    double moment=Math.sqrt(s.target[3]*s.target[3]+s.target[4]*s.target[4]+s.target[5]*s.target[5]);
    boolean vertical=true;
    for(int i=0;i<s.q.length;i++)if(Math.abs(s.ux[i])>.05||Math.abs(s.uy[i])>.05)vertical=false;

    if(s.rank>=6&&s.directionSpread>25)
      return "MULTI-ACTUATOR 3D / SEPARATE-AXIS LOAD TREES";
    if(vertical&&moment>1)
      return neg?"LE/TE or FORE/AFT TENSION-COMPRESSION MOMENT TREE":"LE/TE or FORE/AFT UNEQUAL-ARM MOMENT TREE";
    if(s.tensionOnly)
      return "TENSION-ONLY CABLE / UNEQUAL-ARM MULTI-LAYER TREE";
    if(neg)
      return "TENSION-COMPRESSION WHIFFLETREE / DUAL LOAD TRAIN";
    return "UNEQUAL-ARM MULTI-LEVEL PASSIVE WHIFFLETREE";
  }

  String physicalWarning(Solve6 s){
    if(s.rank<6)
      return "Geometri rank "+s.rank+"/6: tüm 6DOF bağımsız değil. Yalnız hedef vektör bu alt uzaydaysa çözüm fiziksel olarak kapanır.";
    if(s.directionSpread>25)
      return "Pad kuvvet yönleri ortak pasif beam için fazla farklı. X/Y/Z için ayrı load tree veya gimbaled/multi-actuator 3D rig kullan.";
    if(s.directionSpread>8)
      return "Ortak beam mümkün olabilir fakat rod-end/clevis açıları ve secondary loads 3D fixture analiziyle doğrulanmalı.";
    return "Yük yönleri ortak pasif unequal-arm whiffletree için uyumlu görünüyor; beam/pin/clevis boyutlandırması ayrıca yapılmalı.";
  }

  void apply6DofToCore(Solve6 s){
    solved.clear();
    double stiffness=Math.max(1,qd(qStiffness));
    double gauge=Math.max(1,qd(qGaugeLength));

    double sx=0,sy=0,sz=0;
    for(int i=0;i<s.q.length;i++){
      SNode n=new SNode();
      n.section=i;n.x=s.x[i];n.y=s.y[i];n.z=s.z[i];
      n.fx=s.q[i]*s.ux[i];n.fy=s.q[i]*s.uy[i];n.fz=s.q[i]*s.uz[i];
      n.r=Math.sqrt(n.fx*n.fx+n.fy*n.fy+n.fz*n.fz);
      n.mx=n.y*n.fz-n.z*n.fy;
      n.my=n.z*n.fx-n.x*n.fz;
      n.mz=n.x*n.fy-n.y*n.fx;
      n.disp=n.r/stiffness;n.strain=n.disp/gauge*1e6;n.lc=n.r;
      solved.add(n);
      sx+=n.fx;sy+=n.fy;sz+=n.fz;
    }

    solvedValid=true;customZones=false;forcedActuatorGroups=null;
    hSections.setText(String.valueOf(s.q.length));
    hFx.setText(String.format(Locale.US,"%.6f",sx));
    hFy.setText(String.format(Locale.US,"%.6f",sy));
    hFz.setText(String.format(Locale.US,"%.6f",sz));

    buildSelectableCandidates();
    if(recommended!=null)applyDesign(recommended);else designAutomaticWhiffletree();

    if(zoneSummary!=null)zoneSummary.setText(
      "6DOF MOMENT SYNTHESIS ACTIVE — pad kuvvetleri Fx/Fy/Fz/Mx/My/Mz hedefinden çözüldü. "+
      "Klasik eşit dağılım kullanılmıyor.");
    status.setText(s.feasible?
      "6DOF hedefi kapandı — pad force synthesis ve load-tree topolojisi aktif.":
      "6DOF hedefi tam kapanmadı — aşağıdaki residual ve fiziksel mimari uyarılarını kontrol et.");
  }

  void render6Dof(Solve6 s){
    StringBuilder out=new StringBuilder();
    out.append(s.feasible?"6DOF SOLUTION — FEASIBLE\n":"6DOF SOLUTION — REVIEW / INFEASIBLE\n");
    out.append(String.format(Locale.US,
      "Matrix rank %d/6 • mode %s • active pad %d/%d\n"+
      "Force error %.4f%% • Moment error %.4f%% • direction spread %.1f°\n"+
      "REAL-WORLD FAMILY: %s\n%s\n\n",
      s.rank,s.tensionOnly?"TENSION ONLY":"TENSION/COMPRESSION",
      activeCount(s.active),s.q.length,s.forceErrorPct,s.momentErrorPct,s.directionSpread,s.family,s.warning));

    out.append("TARGET → APPLIED → RESIDUAL\n");
    String[] nm={"Fx N","Fy N","Fz N","Mx Nmm","My Nmm","Mz Nmm"};
    for(int r=0;r<6;r++)out.append(String.format(Locale.US,
      "%-7s  %+.3f  →  %+.3f  Δ %+.3f\n",nm[r],s.target[r],s.applied[r],s.residual[r]));

    out.append("\nPAD FORCE SOLUTION\n");
    for(int i=0;i<s.q.length;i++){
      double fx=s.q[i]*s.ux[i],fy=s.q[i]*s.uy[i],fz=s.q[i]*s.uz[i];
      out.append(String.format(Locale.US,
        "P%d  q=%+.2f N  F=[%+.1f,%+.1f,%+.1f] N  r=[%.1f,%.1f,%.1f] mm%s\n",
        i+1,s.q[i],fx,fy,fz,s.x[i],s.y[i],s.z[i],s.active[i]?"":"  INACTIVE"));
    }

    out.append(String.format(Locale.US,
      "\nPeak |pad q| %.1f N • tree beams %d • force closure %.6f N • worst passive-beam ΣM %.6f Nmm\n",
      s.maxAbsQ,wtBeams.size(),wtForceResidual,wtMomentResidual));

    out.append("\nFİZİKSEL TASARIM KURALI\n");
    out.append("• Aynı yönlü pad'ler: unequal-arm beam; pivot FL·a = FR·b.\n");
    out.append("• Çok pad: 2→4→8… multi-layer tree.\n");
    out.append("• Saf moment/torsion: karşılıklı force couple / LE-TE veya fore-aft pair.\n");
    out.append("• Tension-only: q<0 üreten kollar devreden çıkarılır ve sistem tekrar çözülür.\n");
    out.append("• Karışık eksen: tek pasif beam yerine ayrı X/Y/Z load train veya multi-actuator 3D rig.\n");

    dofResult.setText(out.toString());
    familyCard.setText("SEÇİLEN GERÇEK-DÜNYA AİLESİ\n"+s.family+"\n\n"+s.warning);
    authorityCard.setText(String.format(Locale.US,
      "DOF AUTHORITY — %s\nRank %d / 6 • force residual %.3f N • moment residual %.3f Nmm",
      s.rank==6?"FULL":("LIMITED "+s.rank+"/6"),s.rank,
      Math.sqrt(s.residual[0]*s.residual[0]+s.residual[1]*s.residual[1]+s.residual[2]*s.residual[2]),
      Math.sqrt(s.residual[3]*s.residual[3]+s.residual[4]*s.residual[4]+s.residual[5]*s.residual[5])));
  }

  int activeCount(boolean[] a){int n=0;for(boolean v:a)if(v)n++;return n;}
}
