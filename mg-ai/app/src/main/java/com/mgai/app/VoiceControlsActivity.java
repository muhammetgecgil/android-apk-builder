package com.mgai.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class VoiceControlsActivity extends Activity {
    private TextView status;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(26),dp(20),dp(20));
        root.setBackgroundColor(Color.rgb(244,246,248));

        TextView title=new TextView(this);
        title.setText("MG-AI Ses Kontrolleri");
        title.setTextSize(28);
        root.addView(title);

        status=new TextView(this);
        status.setPadding(0,dp(10),0,dp(12));
        root.addView(status);

        CheckBox enabled=new CheckBox(this);
        enabled.setText("Cevapları sesli oku");
        enabled.setChecked(LocalVoiceOutput.enabled(this));
        enabled.setOnCheckedChangeListener((v,on)->{LocalVoiceOutput.setEnabled(this,on);refresh();});
        root.addView(enabled);

        TextView speedLabel=new TextView(this);
        speedLabel.setText("Konuşma hızı");
        speedLabel.setPadding(0,dp(12),0,0);
        root.addView(speedLabel);

        SeekBar speed=new SeekBar(this);
        speed.setMax(100);
        speed.setProgress(Math.round((LocalVoiceOutput.speechRate(this)-0.55f)*100f));
        speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean fromUser){
                if(fromUser){float r=0.55f+(p/100f);LocalVoiceOutput.setSpeechRate(VoiceControlsActivity.this,r);refresh();}
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        root.addView(speed);

        Button repeat=new Button(this);
        repeat.setText("Son cevabı tekrar oku");repeat.setAllCaps(false);
        repeat.setOnClickListener(v->{LocalVoiceOutput.repeatLast(this);refresh();});
        root.addView(repeat);

        Button stop=new Button(this);
        stop.setText("Sesi durdur");stop.setAllCaps(false);
        stop.setOnClickListener(v->{ContinuousDialogManager.suppressNextAutoListen();LocalVoiceOutput.stop();refresh();});
        root.addView(stop);

        CheckBox continuous=new CheckBox(this);
        continuous.setText("Sürekli diyalog modu");
        continuous.setChecked(ContinuousDialogManager.enabled(this));
        continuous.setOnCheckedChangeListener((v,on)->{ContinuousDialogManager.setEnabled(this,on);refresh();});
        root.addView(continuous);

        TextView note=new TextView(this);
        note.setText("Sürekli diyalog açıkken MG-AI cevabını sesli okumayı bitirdiğinde yerel Whisper yeniden açılır ve bir sonraki konuşmanı dinlemeye hazır olur. Kapatınca otomatik dinleme durur.");
        note.setTextSize(13);note.setPadding(0,dp(16),0,0);root.addView(note);

        setContentView(root);refresh();
    }

    private void refresh(){
        status.setText("Sesli cevap: "+(LocalVoiceOutput.enabled(this)?"AÇIK":"KAPALI")+
                " • Hız: "+String.format(java.util.Locale.US,"%.2fx",LocalVoiceOutput.speechRate(this))+
                " • Sürekli diyalog: "+(ContinuousDialogManager.enabled(this)?"AÇIK":"KAPALI"));
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
