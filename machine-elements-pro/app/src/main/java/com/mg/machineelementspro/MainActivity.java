package com.mg.machineelementspro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final EditText[] inputs = new EditText[6];
    private Spinner moduleSpinner;
    private TextView resultTitle, resultBody, resultStatus, resultNote;
    private int selectedModule = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        updateFields(0);
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(248,250,252));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("MACHINE ELEMENTS PRO", 24, true, Color.rgb(15,23,42));
        root.addView(title);
        TextView sub = text("Mühendislik hesap motoru • SI birimleri", 14, false, Color.rgb(71,85,105));
        sub.setPadding(0, dp(4), 0, dp(18));
        root.addView(sub);

        moduleSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CalculationEngine.MODULES);
        moduleSpinner.setAdapter(adapter);
        root.addView(moduleSpinner, lp(-1, dp(54), 0));

        TextView hint = text("Hesap modülü", 12, true, Color.rgb(15,118,110));
        root.addView(hint, 0);

        for (int i=0; i<inputs.length; i++) {
            inputs[i] = new EditText(this);
            inputs[i].setTextSize(16);
            inputs[i].setSingleLine(true);
            inputs[i].setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
            inputs[i].setPadding(dp(12), dp(8), dp(12), dp(8));
            root.addView(inputs[i], lp(-1, dp(58), dp(8)));
        }

        Button calc = new Button(this);
        calc.setText("HESAPLA");
        calc.setTextSize(16);
        calc.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        calc.setAllCaps(false);
        calc.setTextColor(Color.WHITE);
        calc.setBackgroundColor(Color.rgb(15,118,110));
        root.addView(calc, lp(-1, dp(56), dp(14)));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(Color.WHITE);
        root.addView(card, lp(-1, -2, dp(16)));

        resultTitle = text("Sonuç", 18, true, Color.rgb(15,23,42));
        resultStatus = text("", 15, true, Color.rgb(15,118,110));
        resultStatus.setPadding(0, dp(8), 0, dp(8));
        resultBody = text("Değerleri girip hesaplayın.", 16, false, Color.rgb(30,41,59));
        resultBody.setLineSpacing(0, 1.25f);
        resultNote = text("", 12, false, Color.rgb(100,116,139));
        resultNote.setPadding(0, dp(12), 0, 0);
        card.addView(resultTitle);
        card.addView(resultStatus);
        card.addView(resultBody);
        card.addView(resultNote);

        TextView footer = text("Not: Bu sürüm ön boyutlandırma ve mühendislik kontrolü içindir. Nihai tasarımda geçerli standart, malzeme sertifikası, yük spektrumu, imalat toleransları ve doğrulama analizi ayrıca uygulanmalıdır.", 11, false, Color.rgb(100,116,139));
        footer.setPadding(0, dp(18), 0, 0);
        root.addView(footer);

        moduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModule = position;
                updateFields(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        calc.setOnClickListener(v -> calculate());
        return scroll;
    }

    private void updateFields(int module) {
        String[] labels = CalculationEngine.LABELS[module];
        for (int i=0; i<inputs.length; i++) {
            String label = labels[i];
            inputs[i].setHint(label);
            inputs[i].setVisibility(label.isEmpty() ? View.GONE : View.VISIBLE);
            inputs[i].setText("");
        }
        resultTitle.setText("Sonuç");
        resultStatus.setText("");
        resultBody.setText("Değerleri girip hesaplayın.");
        resultNote.setText("");
    }

    private void calculate() {
        try {
            double[] v = new double[6];
            for (int i=0; i<inputs.length; i++) {
                if (inputs[i].getVisibility() == View.GONE) { v[i] = 0.0; continue; }
                String s = inputs[i].getText().toString().trim().replace(',', '.');
                if (s.isEmpty()) throw new IllegalArgumentException("Eksik alan: " + CalculationEngine.LABELS[selectedModule][i]);
                v[i] = Double.parseDouble(s);
            }
            CalculationEngine.Result r = CalculationEngine.calculate(selectedModule, v);
            resultTitle.setText(r.title);
            resultStatus.setText(r.status);
            resultStatus.setTextColor(r.status.contains("UYGUN DEĞİL") ? Color.rgb(185,28,28) : r.status.contains("SINIRDA") ? Color.rgb(180,83,9) : Color.rgb(15,118,110));
            resultBody.setText(r.body);
            resultNote.setText(r.note);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage() == null ? "Girişleri kontrol edin." : e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private TextView text(String s, int sp, boolean bold, int color) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = top;
        return p;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
