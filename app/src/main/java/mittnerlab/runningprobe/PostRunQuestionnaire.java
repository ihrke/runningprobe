package mittnerlab.runningprobe;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class PostRunQuestionnaire extends AppCompatActivity {
    private static final String TAG = PostRunQuestionnaire.class.getSimpleName();

    SeekBar seekbar_q1;
    Spinner spinner_q2;
    Spinner spinner_q3;
    Spinner spinner_q4;
    SeekBar seekbar_q5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_run_questionnaire);

        // Q1
        seekbar_q1=(SeekBar) findViewById(R.id.seekBar_postq1);

        // Q2
        //------------------------------------
        spinner_q2 = (Spinner) findViewById(R.id.q2_dropdown);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.q2_options, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner_q2.setAdapter(adapter);

        // Q3
        //------------------------------------
        spinner_q3 = (Spinner) findViewById(R.id.q3_dropdown);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.q3_options, android.R.layout.simple_spinner_item);
        spinner_q3.setAdapter(adapter2);

        // Q4
        //------------------------------------
        spinner_q4 = (Spinner) findViewById(R.id.q4_dropdown);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this,
                R.array.q4_options, android.R.layout.simple_spinner_item);
        spinner_q4.setAdapter(adapter3);

        // Q5
        seekbar_q5=(SeekBar) findViewById(R.id.seekBar_postq5);
    }


    public void submitPostQuestionnaire(View view){
        String data_file=getIntent().getStringExtra("datafile");
        Log.d(TAG, "submitPostQuestionnaire()"+data_file);

        if(data_file!=null){
            File file = new File(data_file);
            try {
                Log.d(TAG, "submitPostQuestionnaire(): "+file.toString());
                FileWriter fw = new FileWriter(file, true); // true is for append
                String now = DateFormat.getTimeInstance().format(new Date());
                fw.write( String.format("# Q1=%d\n", seekbar_q1.getProgress() ));
                fw.write( String.format("# Q2=%d (%s)\n", (int)spinner_q2.getSelectedItemId(),
                        (String)spinner_q2.getSelectedItem()) );
                fw.write( String.format("# Q3=%d (%s)\n", (int)spinner_q3.getSelectedItemId(),
                        (String)spinner_q3.getSelectedItem()) );
                fw.write( String.format("# Q4=%d (%s)\n", (int)spinner_q4.getSelectedItemId(),
                        (String)spinner_q4.getSelectedItem()) );
                fw.write( String.format("# Q5=%d\n", seekbar_q5.getProgress() ));
                fw.close();
            } catch( IOException e){
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "submitPostQuestionnaire(): data_file is null, can't write data!");
            Toast.makeText(getApplicationContext(), "can't store data-file", Toast.LENGTH_SHORT).show();
        }


        AlertDialog.Builder dialog = new AlertDialog.Builder(PostRunQuestionnaire.this);
        dialog.setCancelable(false);
        dialog.setTitle(getString(R.string.final_message_title));
        dialog.setMessage( getString(R.string.final_message, data_file));
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "dismiss dialog");
                Intent ii=new Intent(getApplicationContext(), MainActivity.class);
                startActivity(ii);
            }
        });

        final AlertDialog alert = dialog.create();
        alert.show();

        /**
        new AlertDialog.Builder(getApplicationContext())
                .setTitle("Thanks!")
                .setMessage("Tusen takk for at du deltok i studien! Vær så god og del filen som ligger under 'Documents/running'")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        */

    }
}
