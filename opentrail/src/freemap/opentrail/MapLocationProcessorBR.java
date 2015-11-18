package freemap.opentrail;

import android.content.BroadcastReceiver;
import android.os.Bundle;

import android.content.Context;

import android.content.Intent;
import freemap.andromaps.MapLocationProcessor;



// Broadcast receiver for gps stuff

public class MapLocationProcessorBR extends BroadcastReceiver
{
	
	MapLocationProcessor processor;
	
	public MapLocationProcessorBR(MapLocationProcessor.LocationReceiver rec,Context ctx,
			MapLocationProcessor.LocationDisplayer displayer)
	{
		
		processor=new MapLocationProcessor(rec,ctx,displayer);
	}
	
	public void onReceive(Context ctx, Intent broadcast)
	{
		String action = broadcast.getAction();
		Bundle extras = broadcast.getExtras();
		if(action.equals("freemap.opentrail.locationchanged"))
		{
			processor.onLocationChanged(extras.getDouble("lon"), extras.getDouble("lat"),
								extras.getBoolean("refresh"));
		}
		else if (action.equals("freemap.opentrail.providerenabled"))
		{
			boolean enabled = extras.getBoolean("enabled");
			if(enabled)
				processor.onProviderEnabled(extras.getString("provider"));
			else
				processor.onProviderDisabled(extras.getString("provider"));
		}
		else if (action.equals("freemap.opentrail.statuschanged"))
		{
			processor.onStatusChanged(extras.getString("provider"),extras.getInt("status"),new Bundle());
		}
		else if (action.equals("freemap.opentrail.startupdates"))
			processor.startUpdates();
		else if (action.equals("freemap.opentrail.stopupdates"))
			processor.stopUpdates();
	}
	
	public MapLocationProcessor getProcessor()
	{
		return processor;
	}
}