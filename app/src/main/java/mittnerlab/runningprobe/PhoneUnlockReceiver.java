package mittnerlab.runningprobe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// This receiver just stops the beeping once the user unlocks the phone
public class PhoneUnlockReceiver extends BroadcastReceiver {
    private static final String TAG = PhoneUnlockReceiver.class.getSimpleName();

    public PhoneUnlockReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_USER_PRESENT)){
            Intent ii=new Intent(context, AlarmSoundService.class);
            context.stopService(ii);
            Log.i(TAG, intent.getAction());
        }
    }

}
