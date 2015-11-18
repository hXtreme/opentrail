package freemap.opentrail;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.View;
import android.content.Intent;
import java.text.DecimalFormat;

public class WalkrouteListActivity extends ListActivity {

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if(Shared.walkroutes!=null)
		{
			String[] titles = new String[Shared.walkroutes.size()], descriptions=new String[Shared.walkroutes.size()];
			for(int i=0; i<Shared.walkroutes.size(); i++)
			{
				DecimalFormat df = new DecimalFormat("#.##");
				titles[i]=Shared.walkroutes.get(i).getTitle();
				descriptions[i]=truncate(Shared.walkroutes.get(i).getDescription()) + " (" + df.format(Shared.walkroutes.get(i).getDistance()) + "km)";
			}
			ArrayAdapter<String> adapter = new AnnotatedListAdapter (this,android.R.layout.simple_list_item_1,titles,descriptions);
			setListAdapter(adapter);
		}
	}
	
	public void onListItemClick(ListView lv, View v, int selectedRoute, long id)
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
