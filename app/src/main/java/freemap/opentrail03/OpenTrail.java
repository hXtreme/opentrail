package freemap.opentrail03;


import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;


import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.core.model.LatLong;

import freemap.mapsforgeplus.GeoJSONDataSource;
import freemap.mapsforgeplus.DownloadCache;
import freemap.andromaps.DialogUtils;
import freemap.andromaps.MapLocationProcessor;
import freemap.andromaps.MapLocationProcessorWithListener;

import java.io.File;
import java.io.IOException;


public class OpenTrail extends Activity implements  ConditionalLoader.Callback,
                                MapLocationProcessor.LocationReceiver {

    MapView mv;
    TileRendererLayer tileRendererLayer;
    GeoJSONDataSource ds;
    TileCache tileCache;
    String dir;
    boolean gotStyleFile;
    //SavedDataFragment frag;
    SavedData frag;
    MapLocationProcessorWithListener locationListener;
    LocationDisplayer locationDisplayer;

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

        // http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
        /*
        FragmentManager fm = getFragmentManager();
        frag = (SavedDataFragment)getFragmentManager().findFragmentByTag("sdf");
        if(frag==null) {
            frag = new SavedDataFragment();
            fm.beginTransaction().add(frag,"sdf").commit();

        }
        */


    }

    protected void onStart() {
        super.onStart();
        System.out.println("onStart()");

        mv.setCenter(new LatLong(51.05, -0.72));
        mv.setZoomLevel((byte) 14);

        createTileRendererLayer();
        locationDisplayer = new LocationDisplayer(this, mv, getResources().getDrawable(R.drawable.person));

        File styleFile = new File(dir + "freemap_v4.xml");

        ConditionalLoader loader = new ConditionalLoader(this, 0, "http://www.free-map.org.uk/data/android/",
                                                            styleFile, this);
        loader.downloadOrLoad();



    }

    public void onStop() {
        super.onStop();
        if(locationListener!=null) {
            locationListener.stopUpdates();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        mv.destroyAll();
    }


    private void startGPS() {
        locationListener = new MapLocationProcessorWithListener(this, this, locationDisplayer);
        locationListener.startUpdates(0, 0);
    }

    private void createTileRendererLayer() {
        tileRendererLayer = new TileRendererLayer(tileCache, ds,
                mv.getModel().mapViewPosition, false, true,
                AndroidGraphicFactory.INSTANCE);

    }

    public void receiveLoadedData(int id, Object data) {
        switch(id) {
            case 0:
                loadStyleFile((File)data);
                break;
        }
    }

    private void loadStyleFile(File styleFile) {

        try {
            ExternalRenderTheme theme = new ExternalRenderTheme(styleFile);


            tileRendererLayer.setXmlRenderTheme(theme);
            mv.addLayer(tileRendererLayer);

            gotStyleFile = true;

            // critical : start GPS only once tile renderer layer setup otherwise
            // my location marker might get added before tile renderer layer
            startGPS();
        }
        catch(IOException e) {
            DialogUtils.showDialog(this, e.toString());
        }
    }

    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected (MenuItem item) {

        switch(item.getItemId()) {

        }
        return false;
    }

    public void noGPS() {

    }

    public void receiveLocation (double lon, double lat, boolean refresh) {
        mv.setCenter(new LatLong(lat, lon));
    }
}
