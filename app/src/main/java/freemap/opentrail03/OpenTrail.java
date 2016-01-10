package freemap.opentrail03;


import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.Environment;
import android.location.LocationManager;
import android.location.LocationListener;
import android.location.Location;
import android.app.AlertDialog;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import freemap.andromaps.DownloadTextFilesTask;
import freemap.mapsforgeplus.GeoJSONDataSource;
import freemap.mapsforgeplus.DownloadCache;
import org.mapsforge.core.model.LatLong;
import freemap.andromaps.HTTPCommunicationTask;
import freemap.andromaps.DialogUtils;

import java.io.File;
import java.io.IOException;





public class OpenTrail extends Activity implements LocationListener, HTTPCommunicationTask.Callback {

    MapView mv;
    TileRendererLayer tileRendererLayer;
    GeoJSONDataSource ds;
    TileCache tileCache;
    LocationManager mgr;
    String dir;
    boolean gotStyleFile;
    SavedDataFragment frag;

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
        FragmentManager fm = getFragmentManager();
        frag = (SavedDataFragment)getFragmentManager().findFragmentByTag("sdf");
        if(frag==null) {
            frag = new SavedDataFragment();
            fm.beginTransaction().add(frag,"sdf").commit();

        }

    }

    protected void onStart() {
        super.onStart();
        System.out.println("onStart()");

        mv.setCenter(new LatLong(51.05, -0.72));
        mv.setZoomLevel((byte) 14);

        createTileRendererLayer();
        File styleFile = new File(dir + "freemap_v4.xml");

        if (styleFile.exists())
            loadStyleFile(styleFile);
        else
            downloadStyleFile("http://www.free-map.org.uk/data/android/", styleFile);
    }


    public void onStop() {
        super.onStop();
        if(mgr!=null) {
            mgr.removeUpdates(this);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        mv.destroyAll();
    }


    public void onLocationChanged(Location loc) {
        mv.setCenter(new LatLong(loc.getLatitude(), loc.getLongitude()));
    }

    public void onProviderEnabled(String provider) {

    }

    public void onProviderDisabled(String provider) {

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    private void loadStyleFile(File styleFile) {

        try {
            ExternalRenderTheme theme = new ExternalRenderTheme(styleFile);

            tileRendererLayer.setXmlRenderTheme(theme);
            mv.addLayer(tileRendererLayer);

            gotStyleFile = true;

            startGPS();
        }
        catch(IOException e) {
            DialogUtils.showDialog (this, e.toString());
        }
    }

    private void downloadStyleFile(String webDir, File styleFile) {

        String url = webDir + "/" + styleFile.getName();
        /*
        frag.executeHTTPCommunicationTask
                (new DownloadTextFilesTask(this,
                        new String[] { url },
                        new String[] { filename },
                        "No Freemap style file found. Download?",
                        this,0), "Downloading...",
                        "Downloading style file...");
                        */
        DownloadTextFilesTask t = new DownloadTextFilesTask(this,
                new String[] { url },
                new String[] { styleFile.getAbsolutePath() },
                "No Freemap style file found. Download?",
                this,0);
        t.setAdditionalData(styleFile);
        t.setDialogDetails("downloading style file...", "downloading style file...");

        // something funny goes on when doing this through the SavedDataFragment, not sure why
        // download task seems to get going without the dialog appearing

        t.confirmAndExecute();

        // Literally, this line screws it up. No idea why right now
        // frag.setHTTPCommunicationTask(t);

    }

    private void startGPS() {
        mgr = (LocationManager)getSystemService(LOCATION_SERVICE);
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    private void createTileRendererLayer() {
        tileRendererLayer = new TileRendererLayer(tileCache, ds,
                mv.getModel().mapViewPosition, false, true,
                AndroidGraphicFactory.INSTANCE);

    }

    public void downloadFinished (int id, Object data) {
        switch(id) {
            case 0:
                DialogUtils.showDialog(this,((File)data).getAbsolutePath());
                loadStyleFile((File) data);
                break;
        }
    }

    public void downloadCancelled (int id) {
        DialogUtils.showDialog(this, "Download of style file cancelled");
    }

    public void downloadError (int id) {
        DialogUtils.showDialog(this, "Error downloading style file");
    }
}
