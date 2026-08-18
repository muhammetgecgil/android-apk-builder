package com.muhammetgecgil.mg3dscanner;

import android.content.Intent;
import android.net.Uri;

public class RobustPoseSurfaceActivityV251 extends RobustPoseSurfaceActivityV25 {
 @Override Uri save25(Mesh m) throws Exception {
   Uri u = super.save25(m);
   runOnUiThread(() -> {
     try {
       Intent i = new Intent(this, AutoObjCadSurfaceActivity.class);
       i.setData(u);
       i.putExtra("generated_format","obj");
       i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
       startActivity(i);
     } catch (Throwable ignored) {}
   });
   return u;
 }
}
