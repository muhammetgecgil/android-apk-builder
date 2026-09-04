package com.mg.whiffletreeaero;

import android.os.Bundle;
import java.util.*;

/** v8 safety gate: an infeasible 6DOF least-squares result is never activated as a physical rig. */
public class V801Activity extends V800Activity {
  @Override void solveAndBuild6Dof(){
    try{
      MatrixBuild mb=readMatrix();
      double[] target=readTarget();
      boolean tensionOnly=constraintSpinner.getSelectedItemPosition()==1;
      Solve6 s=solve6(mb,target,tensionOnly);
      last6=s;
      if(s.feasible){
        apply6DofToCore(s);
        render6Dof(s);
      } else {
        renderRejected6Dof(s);
        status.setText("6DOF hedefi bu pad/yön geometrisiyle tam kapanmıyor — aktif rig değiştirilmedi.");
      }
    }catch(Exception ex){
      dofResult.setText("6DOF ÇÖZÜM HATASI\n"+ex.getMessage());
    }
  }

  void renderRejected6Dof(Solve6 s){
    StringBuilder out=new StringBuilder();
    out.append("6DOF SOLUTION — NOT ACTIVATED\n");
    out.append("Fiziksel kapanış toleransı sağlanmadığı için mevcut aktif rig korunuyor. Aşağıdaki değerler yalnızca en yakın matematiksel çözüm / redesign rehberidir.\n\n");
    out.append(String.format(Locale.US,
      "Matrix rank %d/6 • %s • active pad %d/%d\nForce error %.4f%% • Moment error %.4f%%\nRecommended family: %s\n%s\n\n",
      s.rank,s.tensionOnly?"TENSION ONLY":"TENSION/COMPRESSION",activeCount(s.active),s.q.length,
      s.forceErrorPct,s.momentErrorPct,s.family,s.warning));
    out.append("TARGET → BEST AVAILABLE → RESIDUAL\n");
    String[] nm={"Fx N","Fy N","Fz N","Mx Nmm","My Nmm","Mz Nmm"};
    for(int r=0;r<6;r++)out.append(String.format(Locale.US,"%-7s %+.3f → %+.3f  Δ %+.3f\n",nm[r],s.target[r],s.applied[r],s.residual[r]));
    out.append("\nPAD SCALARS (NOT RELEASED TO TREE)\n");
    for(int i=0;i<s.q.length;i++)out.append(String.format(Locale.US,"P%d q=%+.2f N%s\n",i+1,s.q[i],s.active[i]?"":" INACTIVE"));
    out.append("\nREDESIGN ACTIONS\n");
    out.append("• rank düşükse pad yönü/konumu ekle veya değiştir.\n");
    out.append("• tension-only çözüm kapanmıyorsa karşı yönde çekebilen ek cable/anchor ekle.\n");
    out.append("• karışık eksen yüklerde ayrı X/Y/Z tree veya multi-actuator 3D rig kullan.\n");
    out.append("• saf moment için zıt konumlu force-couple / LE-TE / fore-aft pair oluştur.\n");
    dofResult.setText(out.toString());
    familyCard.setText("TREE NOT ACTIVATED\nÖnerilen gerçek-dünya mimarisi: "+s.family+"\n\n"+s.warning);
    authorityCard.setText(String.format(Locale.US,"DOF AUTHORITY — rank %d/6 • geometry redesign required for requested target",s.rank));
  }
}
