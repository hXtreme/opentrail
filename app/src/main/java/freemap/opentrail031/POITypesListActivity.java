package freemap.opentrail031;


import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.View;
import android.content.Intent;


public class POITypesListActivity extends ListActivity {

    String[] types={"Pubs","Restaurants","Hills","Populated places"},
            typeDetails = {"amenity=pub","amenity=restaurant","natural=peak","place=*"};
    double projectedX, projectedY;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>
                (this,android.R.layout.simple_list_item_1, types);
        setListAdapter(adapter);
        Intent intent = getIntent();
        projectedX = intent.getExtras().getDouble("projectedX");
        projectedY = intent.getExtras().getDouble("projectedY");
    }

    protected void onListItemClick(ListView lv,View v,int pos, long id)
    {
        Intent intent = new Intent(this,POIListActivity.class);
        Bundle extras = new Bundle();
        extras.putString("poitype",typeDetails[pos]);
        extras.putDouble("projectedX", projectedX);
        extras.putDouble("projectedY", projectedY);
        intent.putExtras(extras);
        startActivityForResult(intent,0);
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent intent)
    {
        if(requestCode==0 && resultCode==RESULT_OK)
        {
            setResult(RESULT_OK,intent);
            finish();
        }
    }
}
