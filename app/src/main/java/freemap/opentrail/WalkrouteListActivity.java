/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.content.Intent;
import android.util.Log;

import java.text.DecimalFormat;

public class WalkrouteListActivity extends RecyclerViewActivity  {

    String[] titles, descriptions;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(Shared.walkroutes!=null) {
            titles = new String[Shared.walkroutes.size()];
            descriptions=new String[Shared.walkroutes.size()];
            for(int i=0; i<Shared.walkroutes.size(); i++) {
                DecimalFormat df = new DecimalFormat("#.##");
                titles[i]=Shared.walkroutes.get(i).getTitle();
                descriptions[i]=truncate(Shared.walkroutes.get(i).getDescription()) + " (" + df.format(Shared.walkroutes.get(i).getDistance()) + "km)";
                Log.d("opentrail", "Found title=" + titles[i] + " description=" + descriptions[i]);

            }
            view.setAdapter(getAdapter());
        }
    }
    public RecyclerView.Adapter getAdapter() {
        return new AnnotatedListAdapter (this,titles,descriptions,this);
    }


    public void onListItemClick(int selectedRoute)
    {
        Intent intent = new Intent();
        Bundle extras = new Bundle();
        extras.putInt("selectedRoute", selectedRoute);
        intent.putExtras(extras);
        setResult(RESULT_OK, intent);
        finish();
    }

    public static String truncate(String s)
    {
        return s.length()>=80 ? s.substring(0,79)+"...": s;
    }


}
