package freemap.opentrail03;


import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import freemap.mapsforgeplus.GeoJSONDataSource;
import freemap.mapsforgeplus.DownloadCache;
import org.mapsforge.core.model.LatLong;

import java.io.File;





public class MainActivity extends Activity implements LocationListener {

    MapView mv;
    TileRendererLayer layer;
    GeoJSONDataSource ds;
    TileCache tileCache;
    LocationManager mgr;
    String dir;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AndroidGraphicFactory.createInstance(this.getApplication());

        mv = new MapView(this);

        setContentView(mv);
        mv.setBuiltInZoomControls(true);
        mv.setClickable(true);

        tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mv.getModel().displayModel.getTileSize(), 1f,
                mv.getModel().frameBufferModel.getOverdrawFactor());

        dir = Environment.getExternalStorageDirectory().getAbsolutePath()+"/opentrail/";

        DownloadCache downloadCache = new DownloadCache(new File(dir+"geojson/"));

        ds = new GeoJSONDataSource("http://www.free-map.org.uk/fm/ws/tsvr.php",
                "way=highway,natural,waterway&poi=natural,place,amenity&ext=20&contour=1&outProj=4326");


        ds.setDownloadCache(downloadCache);

    }

    protected void onStart()
    {
        super.onStart();
        System.out.println("onStart()");
        try {

            mv.setCenter(new LatLong(51.05, -0.72));
            mv.setZoomLevel((byte)14);

            layer = new TileRendererLayer(tileCache, ds,
                    mv.getModel().mapViewPosition, false, true,
                    AndroidGraphicFactory.INSTANCE);

            ExternalRenderTheme theme = new ExternalRenderTheme
                    (new File(dir + "freemap_v4.xml"));
            layer.setXmlRenderTheme(theme);
            mv.addLayer(layer);

            mgr = (LocationManager)getSystemService(LOCATION_SERVICE);
            mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    public void onStop()
    {
        super.onStop();
        mgr.removeUpdates(this);
    }

    protected void onDestroy()
    {
        super.onDestroy();
        mv.destroyAll();
    }

    public void onLocationChanged(Location loc)
    {
        mv.setCenter(new LatLong(loc.getLatitude(), loc.getLongitude()));
    }

    public void onProviderEnabled(String provider)
    {

    }

    public void onProviderDisabled(String provider)
    {

    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {

    }
}
