package freemap.opentrail03;





import android.os.Bundle;
import android.preference.PreferenceActivity;



public class OpenTrailPreferences extends PreferenceActivity {


    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}