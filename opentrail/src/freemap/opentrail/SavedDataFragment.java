package freemap.opentrail;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;

import java.util.ArrayList;

import freemap.andromaps.DataCallbackTask;
import freemap.andromaps.HTTPCommunicationTask;
import freemap.data.Walkroute;
import freemap.datasource.FreemapDataset;

public class SavedDataFragment extends Fragment
{
	private DataCallbackTask<?,?> dataTask;
	private HTTPCommunicationTask dfTask;
	private FreemapDataset pois;
	private ArrayList<Walkroute> walkroutes;
	
	public void onCreate (Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.d("newmapsforge", "fragment onCreate()");
		setRetainInstance(true);
	}
	
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		Log.d("newmapsforge", "onActivityCreated(): dataTask = " + dataTask + " dfTask = " + dfTask);
		OpenTrail activity = (OpenTrail)getActivity();
				
		// any tasks running, connect to activity
		if(dataTask!=null)
		{
			Log.d("newmapsforge", "dataTask not null so reconnecting");
		
			dataTask.reconnect(activity, activity);
		}
		if(dfTask!=null)
		{
			Log.d("newmapsforge", "dfTask not null so reconnecting");
		
			dfTask.reconnect(activity, activity);
		}
		if(Shared.pois==null && pois!=null)
			Shared.pois = pois;
		if(Shared.walkroutes==null && walkroutes!=null)
			Shared.walkroutes = walkroutes;
	}
	
	public void setHTTPCommunicationTask (HTTPCommunicationTask dfTask, String dialogTitle, String dialogText)
	{
		this.dfTask = dfTask;
		this.dfTask.setDialogDetails(dialogTitle, dialogText);
	}
	public void executeHTTPCommunicationTask (HTTPCommunicationTask dfTask, String dialogTitle, String dialogText)
	{
		setHTTPCommunicationTask (dfTask, dialogTitle, dialogText);
		dfTask.confirmAndExecute();
	}
	
	public HTTPCommunicationTask getHTTPCommunicationTask()
	{
		return dfTask;
	}
	
	public void setDataCallbackTask (DataCallbackTask<?,?> dataTask)
	{
		this.dataTask = dataTask;
	}
	
	public DataCallbackTask<?,?> getDataCallbackTask()
	{
		return dataTask;
	}
	
	public void onDetach()
	{
		super.onDetach();
		
		Log.d("newmapsforge", "onDetach()");
		// any tasks running, disconnect from activity
		if(dataTask!=null && dataTask.getStatus()==AsyncTask.Status.RUNNING)
		{
			
			Log.d("newmapsforge", "disconnecting data task");
			
			dataTask.disconnect();
		}
		else
			dataTask = null;
		if(dfTask!=null && dfTask.getStatus()==AsyncTask.Status.RUNNING)
		{
			Log.d("newmapsforge", "disconnecting dfTask");
		
			dfTask.disconnect();
		}
		else
			dfTask = null;
		
		pois = Shared.pois;
		walkroutes = Shared.walkroutes;
	}
}
