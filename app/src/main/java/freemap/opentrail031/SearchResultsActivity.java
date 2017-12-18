package freemap.opentrail031;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

import freemap.data.POI;

/**
 * Created by nick on 03/05/17.
 */

public class SearchResultsActivity extends AbstractPOIListActivity implements SearchTask.Receiver {

    ArrayList<POI> foundPOIs;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        SearchTask task = new SearchTask(this, this);
        task.execute(intent.getStringExtra("query"));
    }

    public void receivePOIs(ArrayList<POI> pois) {
        foundPOIs = pois;
        populateList();
    }

    public ArrayList<POI> getPOIs() {
        return foundPOIs;
    }
}
