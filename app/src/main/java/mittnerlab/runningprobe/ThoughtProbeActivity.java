package mittnerlab.runningprobe;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class ThoughtProbeActivity extends AppCompatActivity {
    private static final String TAG = ThoughtProbeActivity.class.getSimpleName();


    // receive probe-responses
    @Override
    protected void onNewIntent(Intent intent) {
        // this overrides the original intent on creation when you return here from ThoughtprobeActivity
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");
        setIntent(intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thought_probe);
        incrementNumAlarms();
    }

    protected void incrementNumAlarms(){
        SharedPreferences settings = getSharedPreferences("RunningProbePrefs", 0);
        int num_alarms=settings.getInt("num_alarms", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("num_alarms", num_alarms+1);
        // Commit the edits!
        editor.commit();
    }

    public void submitProbe(View view){
        SeekBar seekbar1=(SeekBar) findViewById(R.id.seekBar_probe1);
        SeekBar seekbar2=(SeekBar) findViewById(R.id.seekBar_probe2);
        int response1=seekbar1.getProgress();
        int response2=seekbar2.getProgress();

        // store probe + meta-data
        Log.d(TAG, String.format("submitProbe(): response1=%d, response2=%d", response1, response2));
        Intent ii = new Intent(getApplicationContext(),MainActivity.class);
        ii.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT); // bring the activity to front of stack
        ii.putExtra("thought_probe_response1", response1);
        ii.putExtra("thought_probe_response2", response2);
        startActivity(ii);
    }
}
