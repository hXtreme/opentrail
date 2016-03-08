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

public class POIListActivity extends ListActivity {

    String[] names,types;
    ArrayList<POI> pois;
    double projectedX, projectedY;


    public void onCreate(Bundle savedInstanceState)
    {
        try
        {
            super.onCreate(savedInstanceState);

            ArrayAdapter<String> adapter=null;
            Intent intent=getIntent();
            String[] keyval = intent.getExtras().getString("poitype").split("=");
            projectedX = intent.getExtras().getDouble("projectedX");
            projectedY = intent.getExtras().getDouble("projectedY");

            if (Shared.pois != null)
            {
                pois= Shared.pois.getPOIsByType(keyval[0],keyval[1]);
                names = new String[pois.size()];
                types = new String[pois.size()];
                Point p = new Point(projectedX, projectedY);
                POI.sortByDistanceFrom(pois,p);
                DecimalFormat df=new DecimalFormat("#.##");

                // WARNING!!! Distance assumes OSGB projection or some other projection in which units are metres
                for(int i=0; i<pois.size(); i++)
                {
                    names[i] = pois.get(i).getValue("name");
                    types[i] = pois.get(i).getValue(keyval[0])+", distance="+df.format(pois.get(i).distanceTo(p)/1000.0)
                            +"km " + pois.get(i).directionFrom(p);
                }
                adapter = new AnnotatedListAdapter(this,android.R.layout.simple_list_item_1,names,types);
            }
            else if (Shared.pois==null)
            {
                adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,new String[]{"none found"});
            }

            setListAdapter(adapter);
        }
        catch(Exception e)
        {
            Log.d("OpenTrail", e.getMessage());
        }
    }

    public void onListItemClick(ListView listView,View view,int index,long id)
    {
        String osmId = pois.get(index).getValue("osm_id");
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putString("osmId", osmId);
        intent.putExtras(extras);
        setResult(RESULT_OK,intent);
        finish();
    }


}
