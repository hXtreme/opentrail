/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

import freemap.data.POI;

/**
 * Created by nick on 03/05/17.
 */

public class SearchResultsActivity extends AbstractPOIListActivity implements SearchTask.Receiver {


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        SearchTask task = new SearchTask(this, this);
        task.execute(intent.getStringExtra("query"));
    }

    public void receivePOIs(ArrayList<POI> pois) {
        viewMatchingPOIs = pois;
        populateList();
    }

    public ArrayList<POI> getDatasourcePOIs() {
        return viewMatchingPOIs;
    }
}
