/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import android.content.Intent;
import android.os.Bundle;
import java.io.File;
import android.support.v7.widget.RecyclerView;
import android.os.Environment;

public class RecordedWalkroutesListActivity extends RecyclerViewActivity implements ListAdapter.ListClickListener{

    String[] gpxfiles;
    RecyclerView view;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
        gpxfiles = new File(sdcard+"/opentrail/walkroutes/rec").list();

    }

    public RecyclerView.Adapter getAdapter() {
        return new BasicListAdapter (this, gpxfiles, this);
    }


    public void onListItemClick(int index)
    {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("freemap.opentrail.gpxfile", gpxfiles[index]);
        intent.putExtras(extras);
        setResult(RESULT_OK, intent);
        finish();
    }
}
