package com.mark.suchforgot.app;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

import java.io.IOException;


public class ActivityAlarming extends Activity {

    /*

    This is the Activity that starts when the vehicle has parked and turned off.

    This activity will:
        - Display a message.
        - Make the device vibrate.
        - Make a sound for the alarm play.

     */

    private static final String LOG_TAG = "[ActivityAlarming]"; //Filter to see logs from this class

    MediaPlayer mMediaPlayer;
    Vibrator vibrator;
    Button stopAlarmButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarming);

        stopAlarmButton = (Button)findViewById(R.id.button_stop_alarm);
        stopAlarmButton.setOnClickListener(stopAlarmButtonListener);

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100000);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, soundUri);
            final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private View.OnClickListener stopAlarmButtonListener = new View.OnClickListener() {
        public void onClick(View v){
            TheService.isAlarming = false;
            vibrator.cancel();
            mMediaPlayer.stop();
            finish();
        }
    };
}
