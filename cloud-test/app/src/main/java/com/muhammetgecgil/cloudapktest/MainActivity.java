package com.muhammetgecgil.cloudapktest;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(48, 48, 48, 48);
        root.setBackgroundColor(Color.rgb(6, 17, 24));

        TextView title = new TextView(this);
        title.setText("APK BULUT DERLEME\nBAŞARILI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("GitHub Actions → Android APK\nS24 Ultra kurulum testi");
        sub.setTextColor(Color.rgb(0, 229, 255));
        sub.setTextSize(17);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 36, 0, 0);
        root.addView(sub);

        setContentView(root);
    }
}
