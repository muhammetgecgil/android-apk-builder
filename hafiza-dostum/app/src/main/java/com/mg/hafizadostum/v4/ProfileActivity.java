package com.mg.hafizadostum.v4;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileActivity extends Activity {
    public static final String ACTION_EDIT = "com.mg.hafizadostum.v4.EDIT_PROFILE";
    private static final int NAVY = Color.rgb(13,53,86);
    private static final int TEAL = Color.rgb(23,184,151);
    private static final int BG = Color.rgb(245,249,251);

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        boolean edit = ACTION_EDIT.equals(getIntent().getAction());
        if (ProfileEngine.isSaved(this) && !edit) {
            goHome();
            return;
        }
        render();
    }

    private void render() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(36));
        root.setBackgroundColor(BG);
        sv.addView(root);
        setContentView(sv);

        TextView title = text("Seni tanıyayım", 30, NAVY, true);
        root.addView(title);
        TextView sub = text("Hafıza Dostum sana göre şekillenecek. Mesleğin + yaşam rollerin + ihtiyaç duyduğun hafıza desteği birlikte çalışır.", 16, Color.DKGRAY, false);
        sub.setPadding(0, dp(5), 0, dp(18)); root.addView(sub);

        root.addView(section("1 • MESLEĞİN"));
        Spinner profession = new Spinner(this);
        ArrayAdapter<String> pa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ProfileEngine.PROFESSIONS);
        profession.setAdapter(pa);
        profession.setSelection(ProfileEngine.isSaved(this) ? ProfileEngine.profession(this) : 0);
        root.addView(profession, full(dp(58), dp(4)));

        root.addView(section("2 • YAŞAMDA HANGİ ROLLERİN VAR?"));
        TextView roleHint = text("Birden fazlasını seçebilirsin. Örnek: Mühendis + Baba + Ev / bütçe sorumluluğu.", 14, Color.DKGRAY, false);
        roleHint.setPadding(0, 0, 0, dp(6)); root.addView(roleHint);

        boolean[] existingRoles = readRoles();
        CheckBox[] roleChecks = new CheckBox[ProfileEngine.ROLES.length];
        for (int i = 0; i < roleChecks.length; i++) {
            CheckBox c = new CheckBox(this);
            c.setText(ProfileEngine.ROLES[i]);
            c.setTextSize(17);
            c.setTextColor(NAVY);
            c.setChecked(existingRoles[i]);
            c.setPadding(dp(4), dp(5), dp(4), dp(5));
            roleChecks[i] = c;
            root.addView(c);
        }

        root.addView(section("3 • HAFIZA DESTEĞİ"));
        TextView supportInfo = text("Bu seçim tanı koymaz ve ilaç kararı vermez. Sadece ekranı, hatırlatma yoğunluğunu ve güvenlik kontrollerini sana uygunlaştırır.", 13, Color.DKGRAY, false);
        supportInfo.setPadding(0, 0, 0, dp(5)); root.addView(supportInfo);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        int selectedSupport = ProfileEngine.isSaved(this) ? ProfileEngine.support(this) : 0;
        for (int i = 0; i < ProfileEngine.SUPPORT.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(5000 + i);
            rb.setText(ProfileEngine.SUPPORT[i]);
            rb.setTextSize(16);
            rb.setTextColor(NAVY);
            rb.setPadding(dp(4), dp(5), dp(4), dp(5));
            rg.addView(rb);
            if (i == selectedSupport) rb.setChecked(true);
        }
        root.addView(rg);

        LinearLayout note = card();
        note.addView(text("Nasıl çalışacak?", 17, NAVY, true));
        note.addView(text("• Mesleğine özel 3 temel rutin\n• Seçtiğin her yaşam rolüne ek rutinler\n• Hafıza desteği seçtiysen ek güvenlik / günlük yönlendirme\n• Hepsi tek 'Şimdi ne önemli?' ekranında birleşir\n• Sonradan istediğin rutini değiştirebilir veya silebilirsin", 15, Color.DKGRAY, false));
        root.addView(note, full(-2, dp(12)));

        Button save = new Button(this);
        save.setText("✓ PROFİLİ KAYDET VE BAŞLA");
        save.setTextSize(17); save.setTypeface(Typeface.DEFAULT, Typeface.BOLD); save.setTextColor(Color.WHITE); save.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(TEAL); bg.setCornerRadius(dp(16)); save.setBackground(bg);
        root.addView(save, full(dp(62), dp(16)));

        save.setOnClickListener(v -> {
            boolean[] rs = new boolean[roleChecks.length];
            for (int i = 0; i < rs.length; i++) rs[i] = roleChecks[i].isChecked();
            int sid = rg.getCheckedRadioButtonId();
            int support = sid < 5000 ? 0 : sid - 5000;
            ProfileEngine.save(this, profession.getSelectedItemPosition(), rs, support);
            Toast.makeText(this, "Profil kaydedildi. Hatırlatmalar sana göre hazırlandı.", Toast.LENGTH_LONG).show();
            goHome();
        });

        if (ProfileEngine.isSaved(this)) {
            Button cancel = new Button(this);
            cancel.setText("Değişiklik yapmadan geri dön"); cancel.setAllCaps(false); cancel.setTextSize(15);
            cancel.setOnClickListener(v -> goHome());
            root.addView(cancel, full(dp(52), dp(5)));
        }
    }

    private boolean[] readRoles() {
        boolean[] out = new boolean[ProfileEngine.ROLES.length];
        String e = ProfileEngine.roles(this);
        for (int i = 0; i < out.length; i++) out[i] = e.contains("|" + i + "|");
        return out;
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    private TextView section(String s) {
        TextView t = text(s, 14, Color.rgb(72,102,118), true);
        t.setPadding(0, dp(18), 0, dp(7)); return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color); t.setLineSpacing(0,1.08f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(15),dp(14),dp(15),dp(14));
        GradientDrawable g = new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(18)); g.setStroke(dp(1), Color.rgb(210,225,231)); l.setBackground(g); return l;
    }

    private LinearLayout.LayoutParams full(int h, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h == -2 ? LinearLayout.LayoutParams.WRAP_CONTENT : h);
        p.setMargins(0, margin, 0, margin); return p;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
}
