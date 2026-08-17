package com.muhammetgecgil.nesnesayarai;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.net.Uri;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.regex.*;

public class MainActivityV9 extends Activity {
    EditText left,right; TextView info;
    static final int OA=11, OB=12;
    static final int MAX_FILE_BYTES=8*1024*1024;
    static final int LOOKAHEAD=48;
    final int green=Color.rgb(205,245,214), red=Color.rgb(255,215,215);

    public void onCreate(Bundle b){ super.onCreate(b); setContentView(ui()); }

    View ui(){
        LinearLayout root=col(); root.setPadding(dp(14),dp(14),dp(14),dp(24)); root.setBackgroundColor(Color.rgb(247,248,252));
        TextView t=tx("MG Diff",28,Color.rgb(25,45,90)); t.setTypeface(null,1); root.addView(t);
        root.addView(tx("Sadece gerçek değişiklikleri gösterir",13,Color.DKGRAY));

        LinearLayout bar=row(); Button cmp=bt("Farkları Bul",true), sw=bt("Yer Değiştir",false), cl=bt("Temizle",false);
        bar.addView(cmp,w()); bar.addView(sw,w()); bar.addView(cl,w()); root.addView(bar);

        LinearLayout opens=row(); Button oa=bt("Orijinal Dosya Aç",false), ob=bt("Revize Dosya Aç",false);
        opens.addView(oa,w()); opens.addView(ob,w()); root.addView(opens);

        LinearLayout labels=row(); TextView la=tx("ORİJİNAL METİN • YEŞİL",14,Color.rgb(25,120,55)), lb=tx("REVİZE METİN • KIRMIZI",14,Color.rgb(190,45,45));
        la.setTypeface(null,1); lb.setTypeface(null,1); labels.addView(la,w()); labels.addView(lb,w()); root.addView(labels);

        LinearLayout ed=row(); left=edit(); right=edit();
        ed.addView(left,new LinearLayout.LayoutParams(0,dp(500),1));
        View divider=new View(this); divider.setBackgroundColor(Color.rgb(70,92,150)); LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(dp(2),dp(500)); dl.setMargins(dp(5),0,dp(5),0); ed.addView(divider,dl);
        ed.addView(right,new LinearLayout.LayoutParams(0,dp(500),1)); root.addView(ed);

        info=tx("Hazır • Büyük belge güvenli modu aktif",14,Color.rgb(45,70,150)); info.setPadding(0,dp(10),0,0); root.addView(info);

        cmp.setOnClickListener(v->startDiff());
        sw.setOnClickListener(v->{String a=plain(left),c=plain(right);left.setText(c);right.setText(a);});
        cl.setOnClickListener(v->{left.setText("");right.setText("");info.setText("Hazır");});
        oa.setOnClickListener(v->open(OA)); ob.setOnClickListener(v->open(OB));
        return root;
    }

    void startDiff(){
        final String a=plain(left), b=plain(right);
        if(a.equals(b)){left.setText(a);right.setText(b);info.setText("Fark yok");return;}
        info.setText("Karşılaştırılıyor…");
        new Thread(()->{
            try{
                final Result r=smartDiff(a,b);
                runOnUiThread(()->{left.setText(r.a);right.setText(r.b);info.setText(r.count+" fark • yalnız değişen kısımlar boyandı");});
            }catch(Throwable e){
                runOnUiThread(()->info.setText("Karşılaştırma güvenli şekilde durduruldu: "+e.getClass().getSimpleName()));
            }
        }).start();
    }

    Result smartDiff(String a,String b){
        SpannableStringBuilder A=new SpannableStringBuilder(), B=new SpannableStringBuilder();
        int prefix=commonPrefix(a,b), suffix=commonSuffix(a,b,prefix);
        String pre=a.substring(0,prefix); A.append(pre); B.append(pre);
        String am=a.substring(prefix,a.length()-suffix), bm=b.substring(prefix,b.length()-suffix);
        int count=appendWindowed(am,bm,A,B);
        if(suffix>0){String s=a.substring(a.length()-suffix);A.append(s);B.append(s);}
        return new Result(A,B,count);
    }

    int commonPrefix(String a,String b){int n=Math.min(a.length(),b.length()),i=0;while(i<n&&a.charAt(i)==b.charAt(i))i++;return i;}
    int commonSuffix(String a,String b,int prefix){int max=Math.min(a.length(),b.length())-prefix,i=0;while(i<max&&a.charAt(a.length()-1-i)==b.charAt(b.length()-1-i))i++;return i;}

    int appendWindowed(String a,String b,SpannableStringBuilder outA,SpannableStringBuilder outB){
        List<String> A=tokens(a), B=tokens(b); int i=0,j=0,count=0;
        while(i<A.size()||j<B.size()){
            if(i<A.size()&&j<B.size()&&A.get(i).equals(B.get(j))){outA.append(A.get(i));outB.append(B.get(j));i++;j++;continue;}
            if(i<A.size()&&j<B.size()){
                int[] sync=findSync(A,B,i,j);
                if(sync!=null){
                    int ai=sync[0], bj=sync[1];
                    if(ai==i+1&&bj==j+1){count+=appendFine(A.get(i),B.get(j),outA,outB);i++;j++;continue;}
                    while(i<ai){appendMarked(outA,A.get(i++),green);count++;}
                    while(j<bj){appendMarked(outB,B.get(j++),red);count++;}
                    continue;
                }
                count+=appendFine(A.get(i),B.get(j),outA,outB); i++;j++;
            }else if(i<A.size()){appendMarked(outA,A.get(i++),green);count++;}
            else {appendMarked(outB,B.get(j++),red);count++;}
        }
        return count;
    }

    int[] findSync(List<String>A,List<String>B,int i,int j){
        int best=Integer.MAX_VALUE,ba=-1,bb=-1;
        int ae=Math.min(A.size(),i+LOOKAHEAD), be=Math.min(B.size(),j+LOOKAHEAD);
        HashMap<String,Integer> pos=new HashMap<>();
        for(int y=j;y<be;y++) if(!pos.containsKey(B.get(y))) pos.put(B.get(y),y);
        for(int x=i;x<ae;x++){
            Integer y=pos.get(A.get(x)); if(y!=null){int cost=(x-i)+(y-j); if(cost<best){best=cost;ba=x;bb=y;if(cost<=1)break;}}
        }
        return ba<0?null:new int[]{ba,bb};
    }

    int appendFine(String a,String b,SpannableStringBuilder A,SpannableStringBuilder B){
        int p=commonPrefix(a,b), s=commonSuffix(a,b,p);
        if(p>0){A.append(a.substring(0,p));B.append(a.substring(0,p));}
        String am=a.substring(p,a.length()-s), bm=b.substring(p,b.length()-s);
        int c=0;
        if(!am.isEmpty()){appendMarked(A,am,green);c++;}
        if(!bm.isEmpty()){appendMarked(B,bm,red);c++;}
        if(s>0){String z=a.substring(a.length()-s);A.append(z);B.append(z);}
        return c;
    }

    List<String> tokens(String s){
        ArrayList<String> r=new ArrayList<>(); Matcher m=Pattern.compile("\\s+|[\\p{L}\\p{N}_]+|[^\\p{L}\\p{N}_\\s]").matcher(s);
        while(m.find())r.add(m.group()); return r;
    }

    void appendMarked(SpannableStringBuilder s,String x,int color){int st=s.length();s.append(x);s.setSpan(new BackgroundColorSpan(color),st,s.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}

    static class Result{SpannableStringBuilder a,b;int count;Result(SpannableStringBuilder a,SpannableStringBuilder b,int c){this.a=a;this.b=b;this.count=c;}}

    LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setPadding(0,dp(5),0,dp(5));return l;}
    LinearLayout.LayoutParams w(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),0,dp(3),0);return p;}
    TextView tx(String x,int z,int c){TextView v=new TextView(this);v.setText(x);v.setTextSize(z);v.setTextColor(c);return v;}
    Button bt(String x,boolean p){Button b=new Button(this);b.setText(x);b.setAllCaps(false);if(p)b.setTextColor(Color.WHITE);b.setBackgroundColor(p?Color.rgb(45,92,220):Color.rgb(235,238,246));return b;}
    EditText edit(){EditText e=new EditText(this);e.setGravity(Gravity.TOP|Gravity.START);e.setTextSize(17);e.setPadding(dp(10),dp(10),dp(10),dp(10));e.setBackgroundColor(Color.WHITE);e.setInputType(131073);e.setHorizontallyScrolling(false);return e;}
    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    String plain(EditText e){return e.getText().toString();}

    void open(int r){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/plain","application/vnd.openxmlformats-officedocument.wordprocessingml.document"});startActivityForResult(i,r);}
    protected void onActivityResult(int q,int r,Intent d){super.onActivityResult(q,r,d);if(r!=RESULT_OK||d==null||d.getData()==null)return;try{Uri u=d.getData();String n=name(u);String x=n.toLowerCase(Locale.ROOT).endsWith(".docx")?docx(u):txt(u);if(q==OA)left.setText(x);else right.setText(x);info.setText(n+" açıldı • "+(x.length()/1024)+" KB metin");}catch(Exception e){info.setText("Dosya açılamadı: "+e.getMessage());}}
    String name(Uri u){String n="belge";try(android.database.Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int k=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(k>=0)n=c.getString(k);}}return n;}
    String txt(Uri u)throws Exception{try(InputStream in=getContentResolver().openInputStream(u)){return readLimited(in);}}
    String readLimited(InputStream in)throws Exception{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[]z=new byte[16384];int n,total=0;while((n=in.read(z))>0){total+=n;if(total>MAX_FILE_BYTES)throw new IOException("Dosya 8 MB sınırını aşıyor");o.write(z,0,n);}return o.toString("UTF-8");}
    String docx(Uri u)throws Exception{try(ZipInputStream z=new ZipInputStream(getContentResolver().openInputStream(u))){ZipEntry e;while((e=z.getNextEntry())!=null)if("word/document.xml".equals(e.getName())){String xml=readLimited(z);return xml.replaceAll("</w:p>","\n").replaceAll("<[^>]+>","").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").trim();}}throw new IOException("DOCX metni yok");}
}
