/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

import android.os.Bundle;

import android.content.Intent;
import freemap.data.POI;
import java.util.ArrayList;

public class POIListActivity extends AbstractPOIListActivity {
    String[] keyval;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        keyval = intent.getExtras().getString("poitype").split("=");
        populateList();
    }


    public ArrayList<POI> getDatasourcePOIs() {
        return Shared.pois.getPOIsByType(keyval[0],keyval[1]);
    }

}
