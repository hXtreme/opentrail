/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.DecimalFormat;
import java.util.ArrayList;

import freemap.data.POI;
import freemap.data.Point;

public abstract class AbstractPOIListActivity extends ListActivity {

    String[] names,types;

    double projectedX, projectedY;
    boolean hasLocation;


    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle.containsKey("projectedX") && bundle.containsKey("projectedY")) {
            projectedX = intent.getExtras().getDouble("projectedX");
            projectedY = intent.getExtras().getDouble("projectedY");
            hasLocation = true;
        }
    }

    protected void populateList() {

            ArrayList<POI> pois = getPOIs();
            ArrayAdapter<String> adapter;


            if (pois != null) {
                if (pois.size() > 0) {
                    names = new String[pois.size()];
                    types = new String[pois.size()];
                    Point p = null;
                    if (hasLocation) {
                        p = new Point(projectedX, projectedY);
                        POI.sortByDistanceFrom(pois, p);
                    }
                    DecimalFormat df = new DecimalFormat("#.##");

                    // WARNING!!! Distance assumes OSGB projection or some other projection in which units are metres
                    for (int i = 0; i < pois.size(); i++) {
                        String featureType = pois.get(i).getValue("featuretype"),
                                isIn = pois.get(i).getValue("is_in");
                        names[i] = pois.get(i).getValue("name");
                        types[i] = (featureType == null ? "unknown" : featureType) +
                                 (isIn == null ? "" : ", " + isIn) +
                                (hasLocation ?
                                        ", distance=" + df.format(pois.get(i).distanceTo(p) / 1000.0)
                                                + "km " + pois.get(i).directionFrom(p) : "");
                    }
                    adapter = new AnnotatedListAdapter(this, android.R.layout.simple_list_item_1, names, types);
                    setListAdapter(adapter);
                } else {
                    new AlertDialog.Builder(this).setMessage("No matching places found").
                            setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface i, int which) {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            }).show();
                }
            }
    }

    public void onListItemClick(ListView listView,View view,int index,long id) {
        POI poi = getPOIs().get(index);
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putDouble("foundX", poi.getPoint().x);
        extras.putDouble("foundY", poi.getPoint().y);
        intent.putExtras(extras);
        setResult(RESULT_OK,intent);
        finish();
    }

    public abstract ArrayList<POI> getPOIs();
}
