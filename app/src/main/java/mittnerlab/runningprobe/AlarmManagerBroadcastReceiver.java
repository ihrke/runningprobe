package mittnerlab.runningprobe;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

/**
 * Created by mittner on 1/25/17.
 */

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = AlarmManagerBroadcastReceiver.class.getSimpleName();
    private int alarm_sec_min=4*60; // at least X secs between probes
    private int alarm_sec_max=6*60; // at most X secs between probes
    Random rng = new Random();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Waking up");
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);

        //Acquire lock
        wl.acquire();

        // this can be used to programmatically unlock the phone??
        //KeyguardManager keyguardManager = (KeyguardManager) context.getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        //KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        //keyguardLock.disableKeyguard();

        // play notification tone for new thought probe
        Intent intent_sound = new Intent(context, AlarmSoundService.class);
        context.startService(intent_sound);
        /*
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            MediaPlayer player = MediaPlayer.create(context, notification);
            player.setLooping(true);
            player.start();
            //Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
            //r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        // open or re-open thought-probe activity
        Intent ii = new Intent(context, ThoughtProbeActivity.class);
        Log.d(TAG, String.format("onReceive()"));
        ///Log.d(TAG, String.format("onReceive()"));
        ii.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // start new activity or start earlier and re-open here?
        //ii.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(ii);

        Log.d(TAG, "onReceive(): back here");

        // do not set alarm before user has responded
        // setAlarm(context);

        //Release the lock
        wl.release();

    }

    public void cancelAlarm(Context context){
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }


    public void setAlarm(Context context)
    {

        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);

        // next alarm in X seconds
        int next_alarm = rng.nextInt(alarm_sec_max - alarm_sec_min + 1) + alarm_sec_min;
        Log.d(TAG, String.format("next_alarm=%d s", next_alarm));


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            // only for kitkat and newer versions
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+next_alarm*1000, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+next_alarm*1000, pi);
        }

        //am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+next_alarm*1000, pi);

        //Toast.makeText(context, "Alarm set +10 sec", Toast.LENGTH_SHORT).show();
    }
}
