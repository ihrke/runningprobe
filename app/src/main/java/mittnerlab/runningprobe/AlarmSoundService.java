package mittnerlab.runningprobe;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

public class AlarmSoundService extends Service {

    private static final String TAG = AlarmSoundService.class.getSimpleName();

    protected AudioManager audiomanager;
    protected MediaPlayer mplayer;
    protected Vibrator vibrator;


    @Override
    public void onCreate(){
        Log.d(TAG, "onCreate()");

        // configure sound
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // audio-manager for setting volume up
        audiomanager = (AudioManager)getSystemService(getApplicationContext().AUDIO_SERVICE);
        audiomanager.setMode(AudioManager.MODE_NORMAL);
        int maxval = audiomanager.getStreamMaxVolume(audiomanager.STREAM_NOTIFICATION);
        audiomanager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, maxval, 0);


        // mplayer created this way to be able to access STREAM_NOTIFICATION
        mplayer = new MediaPlayer();
        try {
            mplayer.setDataSource(getApplicationContext(), notification);
            mplayer.setLooping(true);
            mplayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            mplayer.setVolume(1, 1);
            mplayer.prepare();
        } catch (Exception e){
            e.printStackTrace();
        }

        // easier way but does not allow STREAM_NOTIFICATION
        //mplayer = MediaPlayer.create(getApplicationContext(), notification);


        // vibration
        vibrator = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        try {
            if(!mplayer.isPlaying()) {
                mplayer.start();
            }
            // Vibration code
            // Start without a delay
            // Vibrate for 100 milliseconds
            // Sleep for 1000 milliseconds
            long[] pattern = {0, 1000, 1000};

            // The '0' here means to repeat indefinitely
            // '0' is actually the index at which the pattern keeps repeating from (the start)
            // To repeat the pattern from any other point, you could increase the index, e.g. '1'
            vibrator.vibrate(pattern, 0);
        } catch (Exception e) {
            // Restore interrupt status.
            e.printStackTrace();
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
          // We don't provide binding, so return null
          return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mplayer.stop();
        mplayer.release();
        vibrator.cancel();
        super.onDestroy();
    }

}
