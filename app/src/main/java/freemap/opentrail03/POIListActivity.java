package freemap.opentrail03;

import android.app.ListActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.ArrayAdapter;

import android.content.Intent;
import freemap.data.POI;
import java.util.ArrayList;

import android.util.Log;
import freemap.data.Point;
import java.text.DecimalFormat;
import android.widget.ListView;

public class POIListActivity extends AbstractPOIListActivity {

    String[] names,types;
    double projectedX, projectedY;
    String[] keyval;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        keyval = intent.getExtras().getString("poitype").split("=");
        populateList();
    }

    public ArrayList<POI> getPOIs() {
        return Shared.pois.getPOIsByType(keyval[0],keyval[1]);
    }
}
