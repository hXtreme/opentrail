package freemap.opentrail03;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

import freemap.data.POI;

/**
 * Created by nick on 03/05/17.
 */

public class SearchActivity extends ListActivity implements SearchTask.Receiver {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        if(intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            SearchTask task = new SearchTask(this, this);
            task.execute(query);
        }
    }

    public void receivePOIs(ArrayList<POI> pois) {

    }
}
