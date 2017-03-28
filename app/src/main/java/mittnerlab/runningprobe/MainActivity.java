package mittnerlab.runningprobe;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_REQUEST_ALL_PERMISSIONS=1;

    private AlarmManagerBroadcastReceiver alarm_scheduler; // the thought-probe scheduler

    //
    int current_intensity_zone=0; // 0=None, 1=low, 2=medium, 3=high
    Random rng = new Random();

    // location API
    private GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected String mLastLocationUpdateTime;
    protected LocationRequest mLocationRequest;
    private boolean location_updates_started=false;

    // data file
    private String data_filename;
    private long start_time=0;  // time when run started first
    private long resume_time=0; // time when run was last resumed
    private long total_duration=0; // total running time
    private int num_received_responses=0; // number of received responses

    // UI elements
    TextView textview_running_time;
    TextView textview_latlon;
    TextView textview_latlon_time;
    TextView textview_filename;
    TextView textview_intensity_zone;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - resume_time + total_duration;
            //Log.d(TAG, String.format("millis=%s", Long.toString(millis)));
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = seconds/(60*60);

            seconds = seconds % 60;
            minutes = minutes % 60;

            setTimer(hours,minutes,seconds);


            timerHandler.postDelayed(this, 500);
        }
    };

    // receive probe-responses
    @Override
    protected void onNewIntent(Intent intent) {
        // this overrides the original intent on creation when you return here from ThoughtprobeActivity
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent()");
        setIntent(intent);
        //Intent intent=getIntent();
        if(intent.getExtras()!=null){
            int response1 = intent.getIntExtra("thought_probe_response1", -1);
            int response2 = intent.getIntExtra("thought_probe_response2", -1);
            int response3 = intent.getIntExtra("thought_probe_response3", -1);
            int num_alarms = getNumAlarms();
            num_received_responses+=1;
            Log.d(TAG, String.format("onstart(): thought_probe_response1=%d, response2=%d, num_alarms=%d", response1, response2,num_alarms));
            saveProbeResponse(response1, response2, response3, num_alarms);

            // set the next alarm after user has responded to a thought probe
            alarm_scheduler.setAlarm(getApplicationContext());
            setRandomIntensityZone();
        }
    }


    /* -------------- LOCATION CONNECTION -------------------------------------------*/
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "onConnected(): connection opened");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            updateLocationUI();
        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient,
                        builder.build());


        startLocationUpdates(true);
    }

    protected void startLocationUpdates(boolean force) {
        if(!location_updates_started | force) {
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                Log.d(TAG, "startLocationUpdates(): successfully started location updates");
                location_updates_started = true;
            } catch (Exception ex) {
                Log.d(TAG, "startLocationUpdates(): failed to start location updates");
                location_updates_started = false;
            }
        } else {
            Log.d(TAG, "startLocationUpdates(): updates already started");
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        mLastLocationUpdateTime= DateFormat.getTimeInstance().format(new Date());
        updateLocationUI();
    }

    private void updateLocationUI() {
        textview_latlon.setText(getString(R.string.position)+String.valueOf(mLastLocation.getLatitude())+", "+String.valueOf(mLastLocation.getLongitude()) );
        textview_latlon_time.setText(getString(R.string.last_update)+mLastLocationUpdateTime);
    }
    /*@Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode() + ", "+result.getErrorMessage());
    }*/


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "onConnectionSuspended(): Connection suspended");
        mGoogleApiClient.connect();
    }
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }
    /* -------------- /LOCATION CONNECTION -------------------------------------------*/

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");

        // start location services
        mGoogleApiClient.connect();

        super.onStart();
    }


    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
    }


    @Override
    protected void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");

        // stop location services
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    private void saveProbeResponse(int response1, int response2, int response3, int num_response){
        // file format is
        // time,runtime,gps_lon,gps_lat,intensity,num_probe,num_response,response1,response2,response3
        Log.d(TAG, "saveProbeResponse(): data_filename="+data_filename);
        File file = new File(data_filename);
        Log.d(TAG, String.format("saveProbeResponse(): response1=%d, response2=%d, num_response=%d", response1, response2, num_response));
        try {
            Log.d(TAG, "saveProbeResponse(): "+file.toString());
            FileWriter fw = new FileWriter(file, true); // true is for append
            String now = DateFormat.getTimeInstance().format(new Date());
            long millis = System.currentTimeMillis() - resume_time + total_duration;
            String lat, lon;
            if(mLastLocation!=null) {
                lat = String.valueOf(mLastLocation.getLatitude());
                lon = String.valueOf(mLastLocation.getLongitude());
            } else {
                lat = "NA";
                lon = "NA";
            }
            fw.write(String.format("%s,%d,%s,%s,%d,%d,%d,%d,%d,%d\n",
                    now, millis, lat, lon, current_intensity_zone, num_received_responses,
                    num_response,response1,response2,response3));
            fw.close();
        } catch( IOException e){
            e.printStackTrace();
        }

    }

    protected int getNumAlarms(){
        SharedPreferences settings = getSharedPreferences("RunningProbePrefs", 0);
        int num_alarms=settings.getInt("num_alarms", 0);
        return num_alarms;
    }

    protected void resetNumAlarms(){
        SharedPreferences settings = getSharedPreferences("RunningProbePrefs", 0);
        int num_alarms=settings.getInt("num_alarms", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("num_alarms", 0);
        // Commit the edits!
        editor.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        alarm_scheduler = new AlarmManagerBroadcastReceiver();

        textview_running_time = (TextView) findViewById(R.id.textview_running_time);
        textview_latlon = (TextView) findViewById(R.id.textview_latlon);
        textview_latlon_time = (TextView) findViewById(R.id.textview_latlon_time);
        textview_filename = (TextView) findViewById(R.id.textview_filename);
        textview_intensity_zone = (TextView) findViewById(R.id.textview_intensity_zone);

        if(!isExternalStorageWritable()) {
            Toast.makeText(getApplicationContext(), "can't store data-file", Toast.LENGTH_SHORT).show();
        }

        // location services
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        checkAndRequestPermissions();

        // new instance
        if(savedInstanceState==null) {
            current_intensity_zone = 0;
        } else {
            Log.d(TAG, "onCreate(): restore from savedInstanceState");
            // saved instance restored
            start_time     = savedInstanceState.getLong("start_time");
            resume_time    = savedInstanceState.getLong("resume_time");
            total_duration = savedInstanceState.getLong("total_duration");
            num_received_responses = savedInstanceState.getInt("num_received_responses");
            current_intensity_zone = savedInstanceState.getInt("current_intensity_zone");
            data_filename = savedInstanceState.getString("data_filename");
            setIntensityZone(current_intensity_zone);
            startLocationUpdates(true);
        }

    }

    // save instance variables
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(TAG, "onsSaveInstanceState()");
        // TODO: continue save/restore MainActivity
        if(start_time>0){ // user is currently in a run
            savedInstanceState.putLong("start_time", start_time);
            savedInstanceState.putLong("resume_time", resume_time);
            savedInstanceState.putLong("total_duration", total_duration);
            savedInstanceState.putInt("num_received_responses", num_received_responses);
            savedInstanceState.putInt("current_intensity_zone", current_intensity_zone);
            savedInstanceState.putString("data_filename", data_filename);
        }


        //savedInstanceState.putString(STATE_USER, mUser);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    private void setIntensityZone(int zone){
        // zone must be one of 0,1,2,3
        current_intensity_zone=zone;
        if(current_intensity_zone==0){
            textview_intensity_zone.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            textview_intensity_zone.setText("");
        } else if(current_intensity_zone==1){
            textview_intensity_zone.setBackgroundColor(ContextCompat.getColor(this,R.color.low_intensity));
            textview_intensity_zone.setText(R.string.low_intensity);
        } else if(current_intensity_zone==2){
            textview_intensity_zone.setBackgroundColor(ContextCompat.getColor(this,R.color.medium_intensity));
            textview_intensity_zone.setText(R.string.medium_intensity);
        } else if(current_intensity_zone==3){
            textview_intensity_zone.setBackgroundColor(ContextCompat.getColor(this, R.color.high_intensity));
            textview_intensity_zone.setText(R.string.high_intensity);
        }

    }

    private void setRandomIntensityZone(){
        // randomly pick intensity zone
        //int zone=rng.nextInt(3) + 1;
        int zone= (( current_intensity_zone+rng.nextInt(2) ) % 3)+1;
        Log.d(TAG, String.format("setRandomIntensityZone(): going from %d to %d", current_intensity_zone,zone));
        setIntensityZone(zone);
    }

    // starting or stopping a run
    public void startStopRun(View view) {
        Log.d(TAG, "startStopRun()");
        checkAndRequestPermissions();
        startLocationUpdates(true);
        Button start_stop_button = (Button) findViewById(R.id.start_stop_button);
        Button finalize_button = (Button) findViewById(R.id.finalize_button);

        final String status = (String) view.getTag();
        Log.d(TAG, status);

        if (status.equals("pause")) { // start run
            Log.d(TAG, "starting/resuming run");
            // switch Button to be STOP button
            start_stop_button.setText(R.string.stop_run);
            start_stop_button.setBackgroundColor( ContextCompat.getColor(getApplicationContext(), R.color.runRed) );

            if(start_time==0){ //------------------------ new run gets started
                resetNumAlarms();

                setRandomIntensityZone();

                // start timer
                start_time=System.currentTimeMillis();
                resume_time=start_time;
                timerHandler.postDelayed(timerRunnable, 0);

                // create data-file for this run
                num_received_responses=0;
                SimpleDateFormat formatter=new SimpleDateFormat("yyyy_MM_dd_hh_mm");
                String datestring=formatter.format(new Date(start_time));
                String datadir=String.format("%s/running",
                        getDatadir());

                data_filename=String.format("%s/run_%s.csv", datadir, datestring);
                textview_filename.setText(getString(R.string.file)+String.format("run_%s.csv", datestring));
                Log.d(TAG, String.format("data_filename='%s'", data_filename));
                File file = new File(datadir);

                if (!file.mkdirs()) {
                    Log.e(TAG, "Directory not created "+file.toString());
                }

                // start thought-probe timer
                if(alarm_scheduler != null){
    		        alarm_scheduler.setAlarm(getApplicationContext());
    	        } else {
                    Toast.makeText(getApplicationContext(), "Failed to set alarm!", Toast.LENGTH_SHORT).show();
    	        }

            } else { //--------------------------------- run is resumed
                // resume timer
                resume_time=System.currentTimeMillis();
                timerHandler.postDelayed(timerRunnable, 0);
                alarm_scheduler.setAlarm(getApplicationContext());
            }

            finalize_button.setEnabled(false);
            view.setTag("running");

        } else { // stop run
            Log.d(TAG, "pausing run");

            // stop timer
            total_duration=total_duration+(System.currentTimeMillis()-resume_time);
            timerHandler.removeCallbacks(timerRunnable);

            // remove alarms
            if(alarm_scheduler != null){
                alarm_scheduler.cancelAlarm(getApplicationContext());
            } else {
                Toast.makeText(getApplicationContext(), "Failed to cancel alarm!", Toast.LENGTH_SHORT).show();
            }


            // switch Button to be START button
            start_stop_button.setText(R.string.start_run);
            start_stop_button.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.runBlue));

            finalize_button.setEnabled(true);
            view.setTag("pause");

        }
    }

    private String getDatadir(){
        String datadir;

        // API<19 does not have the DIRECTOR_DOCUMENTS variable (found on Gerit's phone)
        if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            datadir=Environment.getExternalStorageDirectory() + "/Documents";
        } else {
            datadir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        }
        return datadir;
    }

    // finalize a run
    public void finalizeRun(View view) {
        Log.d(TAG, "finalizeRun()");
        Intent intent=new Intent(this, PostRunQuestionnaire.class);
        intent.putExtra("datafile", data_filename);
        startActivity(intent);

        view.setTag("pause");
        start_time=0;
        total_duration=0;
        resume_time=0;
        data_filename="";
        textview_filename.setText(R.string.file);
        num_received_responses=0;
        setTimer(0,0,0);
        setIntensityZone(0);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    public  boolean checkAndRequestPermissions() {
        boolean location_permission_granted=false;
        boolean write_permission_granted=false;
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "checkAndRequestPermissions(): location permission is granted");
                location_permission_granted=true;
            } else {
                Log.v(TAG, "checkAndRequestPermissions(): location permission is not granted");
                location_permission_granted=false;
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "checkAndRequestPermissions(): write permission is granted");
                write_permission_granted=true;
            } else {
                Log.v(TAG, "checkAndRequestPermissions(): write permission is not granted");
                write_permission_granted=false;
            }
            if( write_permission_granted & location_permission_granted ){
                return true;
            } else {
                Log.v(TAG, "checkAndRequestPermissions(): requesting permissions");
                requestPermissions(new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_REQUEST_ALL_PERMISSIONS);
                Log.v(TAG, "checkAndRequestPermissions(): requesting permissions done");
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "checkAndRequestPermissions(): SKD<23, all permissions granted");
            return true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_REQUEST_ALL_PERMISSIONS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    startLocationUpdates(true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "sorry, can't operate without these permissions", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void setTimer(int hours, int minutes, int seconds){
        textview_running_time.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

}