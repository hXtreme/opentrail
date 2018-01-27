/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.content.Intent;


public class POITypesListActivity extends RecyclerViewActivity implements ListAdapter.ListClickListener {

    String[] types={"Pubs","Restaurants","Hills","Populated places"},
            typeDetails = {"amenity=pub","amenity=restaurant","natural=peak","place=*"};
    double projectedX, projectedY;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        projectedX = intent.getExtras().getDouble("projectedX");
        projectedY = intent.getExtras().getDouble("projectedY");
        view.setAdapter(getAdapter());
    }

    public RecyclerView.Adapter getAdapter() {
       return new BasicListAdapter (this, types, this);
    }

    public void onListItemClick(int pos) {
        Intent intent = new Intent(this,POIListActivity.class);
        Bundle extras = new Bundle();
        extras.putString("poitype",typeDetails[pos]);
        extras.putDouble("projectedX", projectedX);
        extras.putDouble("projectedY", projectedY);
        intent.putExtras(extras);
        startActivityForResult(intent,0);
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent intent) {
        if(requestCode==0 && resultCode==RESULT_OK) {
            setResult(RESULT_OK,intent);
            finish();
        }
    }
}
