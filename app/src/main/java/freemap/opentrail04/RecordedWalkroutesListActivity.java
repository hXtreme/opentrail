/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import java.io.File;
import android.view.View;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.os.Environment;

public class RecordedWalkroutesListActivity extends ListActivity {

    String[] gpxfiles;


    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        gpxfiles = new File(sdcard+"/opentrail/walkroutes/rec").list();
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String> (this,android.R.layout.simple_list_item_1, gpxfiles);
        setListAdapter(adapter);
    }


    public void onListItemClick(ListView listView, View view, int index, long id)
    {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("freemap.opentrail.gpxfile", gpxfiles[index]);
        intent.putExtras(extras);
        setResult(RESULT_OK, intent);
        finish();
    }
}
