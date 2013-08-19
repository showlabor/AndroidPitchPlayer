package de.showlabor.example.pitchplayer;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import java.io.IOException;

public class MainActivity extends Activity {
    private final static String STREAM_DLF = "http://dradio_mp3_dlf_m.akacast.akamaistream.net/7/249/142684/v1/gnl.akacast.akamaistream.net/dradio_mp3_dlf_m";
    private PitchPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button togglePlayButton = (Button) findViewById(R.id.togglePlayButton);
        togglePlayButton.setText(R.string.play);
        togglePlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer != null) {
                    if (!mPlayer.isPlaying()) {
                        togglePlayButton.setText(R.string.stop);
                        mPlayer.start();
                    } else {
                        togglePlayButton.setText(R.string.play);
                        mPlayer.stop();
                    }
                }
            }
        });

        final SeekBar speedBar = (SeekBar) findViewById(R.id.speedBar);
        speedBar.setProgress(speedBar.getMax() / 2);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = 1.f / ((float) speedBar.getMax());
                if (mPlayer != null) {
                    mPlayer.setRelativePlaybackSpeed(0.5f + scale * (float)(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mPlayer = new PitchPlayer(STREAM_DLF);

        final SeekBar volBar = (SeekBar) findViewById(R.id.volBar);
        volBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float scale = 1.f / ((float) speedBar.getMax());
                float vol = scale * (float)(progress);
                if (mPlayer != null) {
                    mPlayer.setVolume(vol);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        volBar.setProgress(volBar.getMax());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
