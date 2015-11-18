package freemap.opentrail;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class PressableTileRendererLayer extends TileRendererLayer
{

	public interface Callback
	{
		public void launchInputAnnotationActivity(double lat, double lon);
	}
	
	LatLong tp;
	Callback cb;
	Context ctx;
	
	public PressableTileRendererLayer(Context ctx, Callback cb, TileCache tileCache, MapViewPosition tilePosition, boolean transparent, boolean labels,
			AndroidGraphicFactory factory)
	{
		super(tileCache, tilePosition, transparent, labels, factory);
		this.cb = cb;
		this.ctx = ctx;
	}
	
	public boolean onLongPress(LatLong tapPos, org.mapsforge.core.model.Point layerXY, 
						org.mapsforge.core.model.Point xy)
	{
		tp = tapPos;
		
		new AlertDialog.Builder(ctx).setMessage("Add an annotation at this location?").
			setNegativeButton("Cancel",null).
			setPositiveButton("OK", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface i, int which)
				{
					cb.launchInputAnnotationActivity(tp.latitude, tp.longitude);
				}
			}).show();

		return true;
	}
}

