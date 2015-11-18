package freemap.opentrail;

// General role: a displayer of overlay data
// This or its subclasses (e.g. DataDisplayer in opentrail) would collect together all necessary overlays
// and display data on them


import android.graphics.drawable.Drawable;
import android.content.Context;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.view.MapView;

import freemap.data.Point;

public class LocationDisplayer implements MapLocationProcessorWithListener.LocationDisplayer {
	

	protected Context ctx;
	protected Drawable locationIcon;
	protected Marker myLocOverlayItem;
	protected MapView mv;
	protected boolean markerShowing; // to prevent exceptions when marker added twice

	public LocationDisplayer(Context ctx, MapView mv,  Drawable locationIcon)
	{
		this.mv = mv;
		this.ctx = ctx;
		this.locationIcon = locationIcon;
	}
	
	
	
	public void setLocationMarker(Point p)
	{

		myLocOverlayItem = MapsforgeUtil.makeMarker(locationIcon, new LatLong(p.y, p.x));
		
	}
	
	public void addLocationMarker()
	{
		if(mv!=null && myLocOverlayItem!=null && !markerShowing)
		{
			mv.getLayerManager().getLayers().add(myLocOverlayItem);
			markerShowing=true;
		}
	}
	
	public void moveLocationMarker(Point p)
	{
		if(myLocOverlayItem!=null)
			myLocOverlayItem.setLatLong(new LatLong(p.y, p.x));
	}
	
	public void removeLocationMarker()
	{
		if(mv!=null && myLocOverlayItem!=null && markerShowing)
		{
			mv.getLayerManager().getLayers().remove(myLocOverlayItem);
			markerShowing=false;
		}
	}	
	
	public boolean isLocationMarker()
	{
		return myLocOverlayItem != null;
	}
}
