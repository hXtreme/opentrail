package freemap.opentrail;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Marker;


import android.graphics.drawable.Drawable;


public class FeatureUtil
{
	public static Marker makeMarker (Drawable drawable, double lat, double lon)
	{
		
	
		Bitmap b = AndroidGraphicFactory.convertToBitmap(drawable);
		Marker marker = new Marker(new LatLong(lat,lon), b, 0, -b.getHeight()/2)
		{
			public boolean onTap(LatLong point, org.mapsforge.core.model.Point viewPosition,
						org.mapsforge.core.model.Point tapPoint)
			{
				if (contains(viewPosition,tapPoint))
				{
					
					return true;
				}
				return false;
			}
		};
		return marker;
	}
	
	
	public static Paint makePaint (Color colour, int width, Style style)
	{
		Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
		p.setColor(colour);
		p.setStrokeWidth(width);
		p.setStyle(style);
		return p;
	}
}
