
/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;




import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CheckBox;

import android.net.Uri;
import android.view.KeyEvent;

import android.view.inputmethod.InputMethodManager;
import android.app.AlertDialog;

import android.content.Context;

import android.preference.PreferenceManager;

import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.widget.Spinner;


// TODO really need to deal with saving the AsyncTask so it can be restored on orientation change:
// as a quickfix have locked this activity to portrait.

public class InputAnnotationActivity extends AppCompatActivity implements InputAnnotationTask.Receiver
{

    double lat, lon;
    InputAnnotationTask iaTask;
    String postData;
    Intent resultIntent;
    boolean recordingWalkroute;
    Spinner spAnnotationType;
    String annotationType;

    public class OKListener implements OnClickListener
    {
        public void onClick(View view)
        {
            addAnnotation();
        }
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.inputannotation);
        Button ok1 = (Button)findViewById(R.id.btnOkInputAnnotation);
        ok1.setOnClickListener(new OKListener());
        Button cancel1=(Button)findViewById(R.id.btnCancelInputAnnotation);
        Intent intent = this.getIntent();
        lat=intent.getExtras().getDouble("lat",91);
        lon=intent.getExtras().getDouble("lon",181);
        recordingWalkroute = intent.getExtras().getBoolean("isRecordingWalkroute", false);

        spAnnotationType = (Spinner)findViewById(R.id.annotationType);

        CheckBox chkbxWalkroute = (CheckBox)findViewById(R.id.chkbxWalkroute);
        chkbxWalkroute.setChecked(recordingWalkroute);
        chkbxWalkroute.setVisibility(recordingWalkroute ? View.VISIBLE: View.GONE);

        spAnnotationType.setVisibility(recordingWalkroute ? View.GONE: View.VISIBLE);

        chkbxWalkroute.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
                spAnnotationType.setVisibility(isChecked ? View.GONE: View.VISIBLE);
            }
        });

        if(lat<180 && lon<90)
        {
            cancel1.setOnClickListener(new OnClickListener() {
                public void onClick(View v)
                {
                    Intent resultIntent = new Intent();
                    InputAnnotationActivity.this.setResult(RESULT_CANCELED,resultIntent);
                    finish();
                }
            });
            EditText et = (EditText)findViewById(R.id.etAnnotation);
            et.setOnKeyListener (new OnKeyListener() {

                public boolean onKey(View v, int keyCode, KeyEvent event)
                {
                    if(event.getAction()==KeyEvent.ACTION_DOWN)
                    {
                        switch(keyCode)
                        {

                            case KeyEvent.KEYCODE_ENTER:
                                // hide the soft keyboard
                                InputMethodManager imm=(InputMethodManager)getSystemService
                                        (Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.etAnnotation)).
                                        getWindowToken(),0);
                                addAnnotation();
                                return true;
                        }
                    }
                    return false;
                }
            });

        }
        else
        {
            new AlertDialog.Builder(this).setMessage("Location not known yet").setCancelable(false).
                    setPositiveButton("OK",null).show();
        }
    }

    public void addAnnotation()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean wrAnnotation = ((CheckBox)findViewById(R.id.chkbxWalkroute)).isChecked();
        annotationType = String.valueOf(spAnnotationType.getSelectedItemPosition() + 1);
        if(wrAnnotation || prefs.getBoolean("prefNoUpload", false) == true)
            done("0",wrAnnotation ? "Added to walk route"  :   "Annotation will be stored on device", true);
        else
            sendAnnotation();
    }

    public void sendAnnotation()
    {
        EditText text=(EditText)findViewById(R.id.etAnnotation);
        String annText = Uri.encode(text.getText().toString());


        postData = "action=create&lon="+lon+"&lat="+lat+"&annotationType="+annotationType
                    +"&text="+annText;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        iaTask=new InputAnnotationTask(this, this);
        iaTask.setDialogDetails("Sending...", "Sending annotation");
        iaTask.setShowDialogOnFinish(false);
        String username=prefs.getString("prefUsername",""), password=prefs.getString("prefPassword","");
        if(username.equals("") || password.equals(""))
        {
            new AlertDialog.Builder(this).setMessage("You have not supplied a username and password in the " +
                    "preferences. Your annotation will be sent but will need to " +
                    "be authorised.").setPositiveButton
                    ("OK", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface i, int which)
                        {
                            iaTask.execute(postData);
                        }
                    } ).setNegativeButton("Cancel",null).show();
        }
        else
        {
            iaTask.setLoginDetails(username,password);
            iaTask.execute(postData);
        }
    }

    public void receiveResponse(String response)
    {
        boolean success = iaTask.isSuccess() && response!=null;
        done(response, iaTask.getResultMsg(), success);
    }

    public void done(String id, String msg, boolean success)
    {
        resultIntent = new Intent();
        Bundle extras = new Bundle();

        extras.putBoolean("success", success);
        if(success)
        {
            extras.putString("ID", id);
            extras.putString("description", ((EditText)findViewById(R.id.etAnnotation)).getText().toString());
            extras.putBoolean("walkrouteAnnotation" ,((CheckBox)findViewById(R.id.chkbxWalkroute)).isChecked());
            extras.putDouble("lon", lon);
            extras.putDouble("lat", lat);
            extras.putString("annotationType", annotationType);
        }
        resultIntent.putExtras(extras);

        new AlertDialog.Builder(this).setPositiveButton("OK",

                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface i, int which)
                    {
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                }

        ).setMessage("Successfully uploaded").setCancelable(false).show();
        //finish();
    }

    public void onDestroy() {
        super.onDestroy();
        if(iaTask != null && iaTask.getStatus() == AsyncTask.Status.RUNNING) {
            iaTask.disconnect();
        }
    }
}
