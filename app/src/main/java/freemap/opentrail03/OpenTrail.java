package freemap.opentrail03;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;


import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.core.model.LatLong;

import freemap.andromaps.HTTPUploadTask;
import freemap.data.Annotation;
import freemap.data.POI;
import freemap.data.Point;
import freemap.data.Walkroute;
import freemap.datasource.CachedTileDeliverer;
import freemap.datasource.FreemapDataHandler;
import freemap.datasource.FreemapDataset;
import freemap.datasource.FreemapFileFormatter;
import freemap.datasource.WebDataSource;
import freemap.datasource.WalkrouteCacheManager;
import freemap.datasource.XMLDataInterpreter;
import freemap.mapsforgeplus.GeoJSONDataSource;
import freemap.mapsforgeplus.DownloadCache;
import freemap.andromaps.DialogUtils;
import freemap.andromaps.MapLocationProcessor;
import freemap.andromaps.MapLocationProcessorWithListener;
import freemap.datasource.AnnotationCacheManager;
import freemap.proj.OSGBProjection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

// Problems:


public class OpenTrail extends Activity implements ConditionalLoader.Callback,
                                MapLocationProcessor.LocationReceiver {

    MapView mv;
    TileRendererLayer tileRendererLayer;
    GeoJSONDataSource ds;
    ExternalRenderTheme theme;
    TileCache tileCache;
    String dir;
    boolean gotStyleFile;

    MapLocationProcessorWithListener locationListener;

    HTTPCallback httpCallback;
    OverlayManager overlayManager;
    OSGBProjection proj;
    LatLong location, initPos;
    AnnotationCacheManager annCacheMgr;
    CachedTileDeliverer poiDeliverer;
    WalkrouteCacheManager wrCacheMgr;
    DataReceiver dataReceiver;
    AlertDisplay alertDisplay;
    AlertDisplayManager alertDisplayMgr;

    boolean recordingWalkroute, waitingForNewPOIData;
    boolean prefGPSTracking, prefAutoDownload, prefAnnotations;
    String cachedir;

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

        proj = new OSGBProjection();
        cachedir = makeCacheDir(proj.getID());
        
        float lat = 50.9f, lon = -1.4f;
        int zoom = 14;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(savedInstanceState!=null) {
            lat = savedInstanceState.getFloat("lat", 51.05f);
            lon = savedInstanceState.getFloat("lon", -0.72f);
            zoom = savedInstanceState.getInt("zoom", 14);
            recordingWalkroute = savedInstanceState.getBoolean("recordingWalkroute", false);
            waitingForNewPOIData = savedInstanceState.getBoolean("waitingForNewPOIData");
        } else if (prefs!=null) {
            lat = prefs.getFloat("lat", 51.05f);
            lon = prefs.getFloat("lon", -0.72f);
            zoom = prefs.getInt("zoom", 14);
            waitingForNewPOIData = prefs.getBoolean("waitingForNewPOIData", false);
        }
        initPos = new LatLong(lat, lon);

        mv.setCenter(initPos);
        mv.setZoomLevel((byte) zoom);

        createTileRendererLayer();

        httpCallback = new HTTPCallback(this);

        Shared.initSavedDataInstance();

        // do something about reconnecting asynctasks
        Shared.savedData.reconnect(this, httpCallback);



        overlayManager = new OverlayManager (this, mv, getResources().getDrawable(R.mipmap.person),
                                                 getResources().getDrawable(R.mipmap.marker),
                                                 getResources().getDrawable(R.mipmap.annotation),
                                                 proj);

        annCacheMgr = new AnnotationCacheManager(dir+"/annotations");
        Log.d("OpenTrail","Projection ID: " + proj.getID());



        FreemapFileFormatter formatter=new FreemapFileFormatter(this.proj.getID());
        formatter.setScript("bsvr.php");
        formatter.selectPOIs("place,amenity,natural");
        formatter.selectAnnotations(true);
        WebDataSource wds=new WebDataSource("http://www.free-map.org.uk/fm/ws/",formatter);
        poiDeliverer=new CachedTileDeliverer("poi",wds, new XMLDataInterpreter
                (new FreemapDataHandler()),5000,5000,this.proj,cachedir);
        poiDeliverer.setCache(true);
        poiDeliverer.setReprojectCachedData(true);

        if (Shared.pois==null) {
            Shared.pois = new FreemapDataset();
            Shared.pois.setProjection(proj);
        }



        /* all this stuff seems a bit messy
        File wrDir = new File(dir+"/walkroutes/");
        if(!wrDir.exists())
            wrDir.mkdir();
        if(Shared.walkroutes!=null && walkrouteIdx > -1 && walkrouteIdx < Shared.walkroutes.size() && Shared.walkroutes.get(walkrouteIdx)!=null )
        {
            // temporarily remove
            //alertDisplayMgr.setWalkroute(Shared.walkroutes.get(walkrouteIdx));

            overlayManager.setWalkroute(Shared.walkroutes.get(walkrouteIdx));
        }
        */
        wrCacheMgr = new WalkrouteCacheManager(dir+"/walkroutes/");

        dataReceiver = new DataReceiver() {
            public void receivePOIs(FreemapDataset ds)
            {
                Log.d("OpenTrail", "POIs received");
                if(ds!=null)
                {
                    Shared.pois = ds;

                    alertDisplayMgr.setPOIs(Shared.pois);

                    /* comment this lot out for the moment

                     */
                    waitingForNewPOIData=false;
                    loadAnnotationOverlay();

                }
            }

            public void receiveWalkroutes(ArrayList<Walkroute> walkroutes)
            {
                Shared.walkroutes = walkroutes;
            }

            public void receiveWalkroute(int idx, Walkroute walkroute)
            {
                Log.d("OpenTrail", "received walkroute: index " + idx + " ID: " + walkroute.getId());
                // again this whole walkroute handling stuff is messy and needs to be cleaned up??
                setWalkroute(idx,walkroute);


                try
                {
                    wrCacheMgr.addWalkrouteToCache(walkroute);
                }
                catch(IOException e)
                {
                    DialogUtils.showDialog(OpenTrail.this, "Downloaded walk route, but unable to save to cache: " +
                            e.getMessage());
                }
            }
        };

        alertDisplay = new AlertDisplay()
        {
            public void displayAnnotationInfo(String msg, int type, int alertId)
            {
                DialogUtils.showDialog(OpenTrail.this, msg);

                /*
                Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if(ringtoneUri!=null)
                {
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                    r.play();
                }
                String summary = (type==AlertDisplay.ANNOTATION) ? "New walk note" : "New walk route stage";
                NotificationManager mgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

                Notification.Builder nBuilder = new Notification.Builder(this).setSmallIcon(R.drawable.marker).setContentTitle(summary).
                        setContentText(msg);
                Intent intent = new Intent(this,OpenTrail.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                nBuilder.setContentIntent(pIntent);
                mgr.notify(alertId, nBuilder.getNotification()); // deprecated api level 16 - use build() instead
                */
            }
        };

        alertDisplayMgr = new AlertDisplayManager(alertDisplay, 50);
        alertDisplayMgr.setPOIs(Shared.pois);

    }

    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        prefGPSTracking = prefs.getBoolean("prefGPSTracking", true);
        prefAnnotations = prefs.getBoolean("prefAnnotations", true);
        prefAutoDownload = prefs.getBoolean("prefAutoDownload", false);

    }

    public void onResume() {

        super.onResume();

        File styleFile = new File(dir + "freemap_v4.xml");



        ConditionalLoader loader = new ConditionalLoader(this, 0, "http://www.free-map.org.uk/data/android/",
                styleFile, this);
        loader.downloadOrLoad();



    }

    public void onStop() {
        super.onStop();
    }

    public void onPause() {
        super.onPause();

        // remove all markers
        overlayManager.removeAllOverlays(false);
        overlayManager.removeTileRendererLayer();

        if(locationListener!=null) {
            locationListener.stopUpdates();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Shared.savedData.disconnect();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        if(location!=null) {
            editor.putFloat("lat", (float) location.latitude);
            editor.putFloat("lon", (float) location.longitude);
        }
        editor.putInt("zoom", mv.getModel().mapViewPosition.getZoomLevel());
        editor.commit();
        mv.destroyAll();

    }

    private void startGPS() {
        locationListener = new MapLocationProcessorWithListener(this, this, overlayManager);
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
                loadStyleFile((File) data);
                doOverlays();

                // critical : start GPS only once tile renderer layer setup otherwise
                // my location marker might get added before tile renderer layer
                if(prefGPSTracking==true) {
                    startGPS();
                }
                break;
        }
    }

    private void loadStyleFile(File styleFile) {

        try {
            if (theme == null) {
                theme = new ExternalRenderTheme(styleFile);
            }

            Log.d("OpenTrail", "Loading style");
            tileRendererLayer.setXmlRenderTheme(theme);

            gotStyleFile = true;

        } catch (IOException e) {
            DialogUtils.showDialog(this, e.toString());
        }
    }

    private void doOverlays() {
        // 250116 Originally we added the tile renderer layer directly to the mapview here
        // however this call does it anyway
        overlayManager.addTileRendererLayer(tileRendererLayer);
        overlayManager.addAllOverlays();

        //mv.addLayer(tileRendererLayer);

        // This is a bit messy, we are forced to give the location marker an arbitrary location

        if(prefGPSTracking==true) {
            Point markerPos = (location == null ? new Point(-0.72, 51.05) :
                    new Point(location.longitude, location.latitude));
            overlayManager.addLocationMarker(markerPos);
        }
    }


    public boolean onCreateOptionsMenu (Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected (MenuItem item) {
        LatLong loc = this.location != null ? this.location : initPos;
        boolean retcode=true;
        if(item.getItemId()==R.id.aboutMenuItem) {
            about();
        }else {
            Intent intent = null;
            switch(item.getItemId()) {
                case R.id.myLocationMenuItem:
                    if(this.location!=null) {
                        gotoMyLocation();
                    } else {
                        DialogUtils.showDialog(this,"Location not known yet");
                    }
                    break;

                case R.id.inputAnnotationMenuItem:
                    if(this.location!=null) {
                        launchInputAnnotationActivity(this.location.latitude,
                                    this.location.longitude);
                    } else {

                            // TEST??? might produce unwanted null object if commented out
                            // this.location = new LatLong(50.9, 1.4);

                            DialogUtils.showDialog(this,"Location not known yet");
                    }

                    break;


                case R.id.settingsMenuItem:

                    intent = new Intent(this,OpenTrailPreferences.class);
                    startActivity(intent);

                    break;

                case R.id.poisMenuItem:
                    if(this.location!=null) {
                        SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        startPOIDownload(true, sprefs.getBoolean("prefForcePOIDownload", false));
                    } else {
                        DialogUtils.showDialog(this, "Location not known yet");
                    }
                    break;

                case R.id.findPoisMenuItem:

                    if(loc==null) {
                        DialogUtils.showDialog(this, "Location not known yet");
                    }else {
                        intent = new Intent(this,POITypesListActivity.class);
                        Point p = this.proj.project(new Point(loc.longitude,loc.latitude));
                        intent.putExtra("projectedX", p.x);
                        intent.putExtra("projectedY", p.y);
                        startActivityForResult(intent,2);
                    }
                    break;

                case R.id.walkroutesMenuItem:
                     if(loc==null) {
                         DialogUtils.showDialog(this, "Location not known");
                     } else {

                            Shared.savedData.setDataCallbackTask(new DownloadWalkroutesTask(this, dataReceiver, loc ));
                            ((DownloadWalkroutesTask)Shared.savedData.getDataCallbackTask()).execute();
                     }
                    break;

                case R.id.findWalkroutesMenuItem:
                    if(Shared.walkroutes!=null) {
                        intent = new Intent(this,WalkrouteListActivity.class);
                        startActivityForResult(intent,3);
                    } else {
                        DialogUtils.showDialog(this, "No walk routes downloaded yet");
                    }
                    break;

                case R.id.uploadAnnotationsMenuItem:
                    uploadCachedAnnotations();
                    break;

                case R.id.recordWalkrouteMenuItem:
                    recordingWalkroute = !recordingWalkroute;
                    item.setTitle(recordingWalkroute ? "Stop recording" : "Record walk route");

                    /* temporary comment out
                    if(gpsService.getRecordingWalkroute()!=null)
                        Log.d("OpenTrail", "recording/stop recording: walkroute stage size="  + gpsService.getRecordingWalkroute().getStages().size());
                    */

                    if(recordingWalkroute) {
                        overlayManager.removeWalkroute(true);
                        mv.invalidate();


                        Intent startLoggingBroadcast = new Intent("freemap.opentrail.startlogging");
                        sendBroadcast(startLoggingBroadcast);

                    } else {
                        overlayManager.removeWalkroute(true);
                        Intent stopLoggingBroadcast = new Intent("freemap.opentrail.stoplogging");
                        sendBroadcast(stopLoggingBroadcast);
                        showWalkrouteDetailsActivity();
                    }
                    break;
                 case R.id.uploadWalkrouteMenuItem:
                     showRecordedWalkroutesActivity();
                     break;
                default:
                    retcode=false;
                    break;
                }
            }
            return retcode;

    }


    public void onActivityResult(int request, int result, Intent i)
    {
        Bundle extras;
        if(result==RESULT_OK)
        {
            switch(request)
            {
                case 0:
                    // mapfiles - not relevant anymore
                    break;
                case 1:
                    extras = i.getExtras();
                    if(extras.getBoolean("success"))
                    {
                        boolean isWalkrouteAnnotation = extras.getBoolean("walkrouteAnnotation");

                        String id=extras.getString("ID"),description=extras.getString("description");
                        LatLong gp = new LatLong(extras.getDouble("lat"),extras.getDouble("lon"));
                        Point p = new Point(extras.getDouble("lon"),extras.getDouble("lat"));

                        Marker item = MapsforgeUtil.makeTappableMarker(this, isWalkrouteAnnotation ?
                                getResources().getDrawable(R.drawable.marker) :
                                getResources().getDrawable(R.drawable.annotation) , gp, description);

                        // 290116 this seems a bad idea as it will add it before the map layer
                     //   mv.addLayer(item);

                        int idInt = id.equals("0")? -(annCacheMgr.size()+1):Integer.parseInt(id);

                        mv.invalidate(); // 280116 needed?

                    //    Walkroute curWR = gpsService.getRecordingWalkroute();
                        Walkroute curWR = null;

                        if(isWalkrouteAnnotation && this.recordingWalkroute && curWR!=null)
                        {
                            curWR.addStage(p, description);

                            Log.d("OpenTrail", "Added walkroute annotation. Size of stages=" + curWR.getStages().size());
                        }

                        else if(idInt<0)
                        {
                            try
                            {
                                Annotation an=new Annotation(idInt,p.x,p.y,description);
                                annCacheMgr.addAnnotation(an); // adding in wgs84 latlon
                                Shared.pois.add(an); // this reprojects it

                                // 290116 add the annotation to the layer
                                overlayManager.addAnnotation(an, false);
                            }
                            catch(IOException e)
                            {
                                DialogUtils.showDialog(this,"Could not save annotation, please enable upload");
                            }
                        }
                    }
                    break;
                case 2:
                    extras = i.getExtras();
                    POI poi = Shared.pois.getPOIById(Integer.parseInt(extras.getString("osmId")));

                    if(poi!=null)
                        overlayManager.addPOI(poi);
                    break;

                case 3:
                    extras = i.getExtras();
                    int idx = extras.getInt("selectedRoute"), wrId = Shared.walkroutes.get(idx).getId();
                    boolean loadSuccess=false;

                    if(wrCacheMgr.isInCache(wrId))
                    {
                        try
                        {
                            Walkroute wr= wrCacheMgr.getWalkrouteFromCache(wrId);
                            loadSuccess=true;
                               setWalkroute(idx,wr);

                        }
                        catch(Exception e)
                        {
                            DialogUtils.showDialog(this,"Unable to retrieve route from cache: " +
                                    e.getMessage()+". Loading from network");
                        }
                    }
                    if(!loadSuccess)
                    {

                        Shared.savedData.setDataCallbackTask(new DownloadWalkrouteTask(this, dataReceiver));
                        ((DownloadWalkrouteTask)Shared.savedData.getDataCallbackTask()).execute(wrId, idx);
                    }
                    break;


                case 4:
                    extras = i.getExtras();
                    String title = extras.getString("freemap.opentrail.wrtitle"),
                            description = extras.getString("freemap.opentrail.wrdescription"),
                            fname = extras.getString("freemap.opentrail.wrfilename");


                 //   Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();
                    Walkroute recordingWalkroute = null;
                    if(recordingWalkroute!=null) {
                        recordingWalkroute.setTitle(title);
                        recordingWalkroute.setDescription(description);



                        AsyncTask<String,Void,Boolean> addToCacheTask = new AsyncTask<String,Void,Boolean>()
                        {

                            Walkroute recWR;
                            String errMsg;
                            public Boolean doInBackground(String...fname)
                            {



                                try
                                {
                                    wrCacheMgr.addRecordingWalkroute(recWR);
                                    wrCacheMgr.renameRecordingWalkroute(fname[0]);
                           //         gpsService.clearRecordingWalkroute();

                                }
                                catch(IOException e)
                                {
                                    errMsg = e.toString();
                                    return false;
                                }
                                return true;
                            }

                            protected void onPostExecute(Boolean result)
                            {
                                if(!result)
                                    DialogUtils.showDialog(OpenTrail.this, "Unable to save walk route: error=" + errMsg);
                                else
                                {
                                    overlayManager.removeWalkroute(true);

                                    DialogUtils.showDialog(OpenTrail.this, "Successfully saved walk route.");
                                }
                            }
                        };
                        addToCacheTask.execute(fname);

                    } else {
                        DialogUtils.showDialog(this, "No recorded walk route");
                    }

                    break;

                case 5:
                    extras = i.getExtras();
                    String filename = extras.getString("freemap.opentrail.gpxfile");
                    uploadRecordedWalkroute(filename);
                    break;
            }
        }
    }

    public void launchInputAnnotationActivity(double lat, double lon)
    {
        if(this.location!=null)
        {
            Intent intent = new Intent(this,InputAnnotationActivity.class);
            Bundle extras = new Bundle();
            extras.putDouble("lat", lat);
            extras.putDouble("lon", lon);
            extras.putBoolean("recordingWalkroute", recordingWalkroute);
            intent.putExtras(extras);
            startActivityForResult(intent,1);
        }
        else
        {
            DialogUtils.showDialog(this,"Location unknown");
        }

    }

    private void startPOIDownload(boolean showDialog, boolean forceWebDownload)
    {
        LatLong loc = this.location != null ? this.location : initPos;
        if (loc!=null)
        {
            if(Shared.savedData.getDataCallbackTask()==null || Shared.savedData.getDataCallbackTask().getStatus()!= AsyncTask.Status.RUNNING)
            {
                Shared.savedData.setDataCallbackTask(new DownloadPOIsTask(this, poiDeliverer, dataReceiver, showDialog, forceWebDownload, location));
                ((DownloadPOIsTask)Shared.savedData.getDataCallbackTask()).execute();
            }
        }
        else
        {
            DialogUtils.showDialog(this,"Location unknown");
        }
    }

    public void uploadCachedAnnotations()
    {
        if(annCacheMgr.isEmpty())
        {
            DialogUtils.showDialog(this, "No annotations to upload");
        }
        else
        {
            try
            {
                String xml = annCacheMgr.getAllAnnotationsXML();
                ArrayList<NameValuePair> postData = new ArrayList<NameValuePair>();
                postData.add(new BasicNameValuePair("action","createMulti"));
                postData.add(new BasicNameValuePair("inProj","4326"));
                postData.add(new BasicNameValuePair("data",xml));


                Shared.savedData.executeHTTPCommunicationTask(new HTTPUploadTask
                        (this, "http://www.free-map.org.uk/fm/ws/annotation.php",
                                postData,
                                "Upload annotations?", httpCallback, 2), "Uploading...", "Uploading annotations...");
            }
            catch(IOException e)
            {
                DialogUtils.showDialog(this,"Error retrieving cached annotations: " + e.getMessage());
            }
        }
    }

    public void showWalkrouteDetailsActivity()
    {
        Intent intent = new Intent (this, WalkrouteDetailsActivity.class);

        /* temporary comment out
        if(gpsService.getRecordingWalkroute()!=null)
            intent.putExtra("distance", gpsService.getRecordingWalkroute().getDistance());
            */
        startActivityForResult(intent, 4);
    }

    public void showRecordedWalkroutesActivity()
    {
        Intent intent = new Intent(this, RecordedWalkroutesListActivity.class);
        startActivityForResult(intent, 5);
    }

    public void uploadRecordedWalkroute(String wrFile)
    {
        try
        {
            final Walkroute walkroute = this.wrCacheMgr.getRecordedWalkroute(wrFile);
            if(walkroute!=null && walkroute.getPoints().size()>0)
            {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if(prefs.getString("prefUsername", "").equals("") || prefs.getString("prefPassword","").equals(""))
                {
                    new AlertDialog.Builder(this).setMessage("WARNING: Username and password not specified in the preferences." +
                            " Walk route will still be uploaded but will need to be authorised before becoming "+
                            "visible.").setPositiveButton("OK",

                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface i, int which) {
                                    doUploadRecordedWalkroute(walkroute);
                                }
                            }

                    ).setNegativeButton("Cancel",null).show();
                }
                else
                    doUploadRecordedWalkroute(walkroute);
            }
        }
        catch(Exception e)
        {
            DialogUtils.showDialog(this,"Error obtaining walk route: " + e.getMessage());
        }
    }

    private void doUploadRecordedWalkroute(Walkroute walkroute)
    {

        if(walkroute!=null)
        {

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String username = prefs.getString("prefUsername",""), password = prefs.getString("prefPassword", "");
            float dpDist = Float.parseFloat(prefs.getString("prefDPDist", "5.0"));
            Shared.savedData.setHTTPCommunicationTask (new WRUploadTask(OpenTrail.this,walkroute,
                    "http://www.free-map.org.uk/fm/ws/wr.php",
                    "Upload walk route?", httpCallback, 3, dpDist), "Uploading...", "Uploading walk route...");


            if(!(username.equals("")) && !(password.equals("")))
                ((HTTPUploadTask)Shared.savedData.getHTTPCommunicationTask()).setLoginDetails(username, password);

            Shared.savedData.getHTTPCommunicationTask().confirmAndExecute();
        }
    }

    private void setWalkroute(int idx, Walkroute walkroute) {
     //   walkrouteIdx = idx; // 080316 ??? needed ???
        Log.d("OpenTrail", "setWalkroute: idx=" + idx + " walkroute: " + walkroute.getId());
        Shared.walkroutes.set(idx, walkroute);
        alertDisplayMgr.setWalkroute(walkroute);
        overlayManager.addWalkroute(walkroute);
    }

    public void noGPS() {

    }

    public void receiveLocation (double lon, double lat, boolean refresh) {
        location = new LatLong (lat, lon);
        Point pt = new Point(lon,lat);
        alertDisplayMgr.update(pt);

        if(prefAutoDownload && poiDeliverer.needNewData(pt)) {
            if(poiDeliverer.isCache(pt))
                Toast.makeText(this, "Loading data from cache", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Loading data from web", Toast.LENGTH_SHORT).show();
            startPOIDownload(false, false);

        }
        if(prefGPSTracking) {
            gotoMyLocation();
        }
    }

    public void gotoMyLocation() {

        mv.setCenter(location);

    }

    private String makeCacheDir(String projID) {
        String cachedir = dir+"/cache/" + projID.toLowerCase().replace("epsg:", "")+"/";
        File dir = new File(cachedir);
        if(!dir.exists())
            dir.mkdirs();
        return cachedir;
    }

    public void loadAnnotationOverlay()
    {
        if (overlayManager!=null) {
            Log.d("OpenTrail", "operating on annotations...");
            Shared.pois.operateOnAnnotations(overlayManager);
            overlayManager.showAnnotations();
            overlayManager.requestRedraw();
        }
    }

    public void about() {

        DialogUtils.showDialog(this, "OpenTrail 0.3 (beta), using Mapsforge 0.5. Uses OpenStreetMap data, copyright 2004-16 " +
                "OpenStreetMap contributors, Open Database Licence. Uses " +
                "Ordnance Survey OpenData LandForm Panorama contours, Crown Copyright." +
                "Person icon taken from the osmdroid project. Annotation icon based on " +
                "OpenStreetMap viewpoint icon.");
    }

    public void onSaveInstanceState(Bundle state)  {
        super.onSaveInstanceState(state);
        if (this.location!=null) {

            state.putDouble("lat", this.location.latitude);
            state.putDouble("lon", this.location.longitude);

        }
        state.putInt("zoom", mv.getModel().mapViewPosition.getZoomLevel());
        //     state.putInt("walkrouteIdx", walkrouteIdx);
        state.putBoolean("recordingWalkroute", recordingWalkroute);
        state.putBoolean("waitingForNewPOIData",waitingForNewPOIData);
        //   state.putInt("recordingWalkrouteId", recordingWalkrouteId);
    }
}
