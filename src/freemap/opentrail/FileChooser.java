package freemap.opentrail;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.view.View;
import android.os.Environment;
import java.io.File;
import java.io.FilenameFilter;

import android.content.Intent;


public class FileChooser extends ListActivity {

	ArrayAdapter<String> listAdapter;
	String[] mapFiles;
	
	public void onCreate (Bundle savedInstanceState)
	{
		File dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath()+"/opentrail");
		File[] files = dir.listFiles(
					new FilenameFilter()
					{
						public boolean accept(File dir, String filename)
						{
							return filename.endsWith(".map");
						}
					}
				);
		mapFiles = new String[files.length];
		for(int i=0; i<files.length; i++)
			mapFiles[i] = files[i].getName();
			
		super.onCreate(savedInstanceState);
		listAdapter =new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,mapFiles);
		setListAdapter(listAdapter);
	}
	
	public void onListItemClick(ListView list,View view,int index, long id)
	{
		
			String selectedFile = mapFiles[index];
		
		
			Intent i = new Intent();
			Bundle extras = new Bundle();
		
			extras.putString("mapFile", selectedFile);
			i.putExtras(extras);
			setResult(RESULT_OK, i);
			finish();
		
		

	}
}
