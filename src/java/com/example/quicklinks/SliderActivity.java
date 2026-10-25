package com.example.quicklinks;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Floating panel with a REAL draggable SeekBar, opened by tapping a level
 * bar in the Beauty Calendar widget. Widgets themselves can never receive
 * drag gestures (the launcher only forwards taps), so the drag happens
 * here, in a small translucent activity styled to match the widget.
 */
public class SliderActivity extends Activity {

    public static final String EXTRA_STREAM = "stream";

    private AudioManager audio;
    private int stream;
    private TextView valueLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slider);
        setFinishOnTouchOutside(true);

        // Shrink the window itself to a centered floating panel (92% wide,
        // height wraps the card). Previously the window filled the whole
        // screen, so there was no real "outside" to tap — the root layout
        // swallowed every touch and only Back could dismiss the panel.
        // With a genuinely smaller window, outside taps hit the window's
        // outside region and finish the activity, as intended.
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
        getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.dimAmount = 0.35f;
        getWindow().setAttributes(lp);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Belt and braces: any tap reaching the root (e.g. the padding
        // margin inside the window) also closes. The card is clickable,
        // so taps on the card itself do not fall through to this.
        View root = findViewById(R.id.slider_root);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        stream = getIntent().getIntExtra(EXTRA_STREAM, AudioManager.STREAM_MUSIC);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);

        boolean music = stream == AudioManager.STREAM_MUSIC;
        TextView title = findViewById(R.id.slider_title);
        title.setText(music ? "Media Volume" : "Ring Volume");
        valueLabel = findViewById(R.id.slider_value);

        SeekBar bar = findViewById(R.id.slider_bar);
        int tint = music ? 0xFF2DD4BF : 0xFFE879F9;
        bar.setProgressTintList(ColorStateList.valueOf(tint));
        bar.setThumbTintList(ColorStateList.valueOf(tint));

        final int max = audio.getStreamMaxVolume(stream);
        bar.setProgress(max <= 0 ? 0
                : Math.round(audio.getStreamVolume(stream) * 100f / max));
        showValue(bar.getProgress());

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && max > 0) {
                    int index = Math.round(progress * max / 100f);
                    try {
                        audio.setStreamVolume(stream, index, 0);
                    } catch (Exception ignored) {
                        // e.g. ring changes while DND policy blocks them.
                    }
                }
                showValue(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void showValue(int percent) {
        valueLabel.setText(percent + "%");
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Tell the widget to redraw its level bars with the new values.
        Intent refresh = new Intent(this, BeautyCalendarWidget.class);
        refresh.setAction(BeautyCalendarWidget.ACTION_REFRESH);
        sendBroadcast(refresh);
    }
}
