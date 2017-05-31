package freemap.opentrail03;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.util.Log;
import android.widget.SearchView;
import android.widget.Toast;


import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import freemap.andromaps.ConfigChangeSafeTask;
import freemap.andromaps.HTTPUploadTask;
import freemap.data.Annotation;
import freemap.data.Point;
import freemap.data.Walkroute;
import freemap.data.WalkrouteSummary;
import freemap.datasource.CachedTileDeliverer;
import freemap.datasource.FreemapDataHandler;
import freemap.datasource.FreemapDataset;
import freemap.datasource.FreemapFileFormatter;
import freemap.datasource.WebDataSource;
import freemap.datasource.WalkrouteCacheManager;
import freemap.datasource.XMLDataInterpreter;
import freemap.mapsforgegeojson.GeoJSONDataSource;
import freemap.mapsforgegeojson.DownloadCache;
import freemap.andromaps.DialogUtils;
import freemap.andromaps.MapLocationProcessor;
import freemap.datasource.AnnotationCacheManager;
import freemap.proj.OSGBProjection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class OpenTrail extends Activity {

    MapView mv;
    TileRendererLayer tileRendererLayer;
    GeoJSONDataSource ds;
    XmlRenderTheme theme;
    TileCache tileCache;
    String opentrailDir;
    boolean loadedRenderTheme;

    MapLocationProcessorBR mapLocationProcessor;

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
    ServiceConnection gpsServiceConn;
    GPSService gpsService;
    Walkroute curDownloadedWalkroute;
    DownloadCache downloadCache;
    long lastCacheClearTime;

    IntentFilter filter;


    boolean isRecordingWalkroute, waitingForNewPOIData;
    boolean prefGPSTracking, prefAutoDownload, prefAnnotations, prefNoUpload;
    String cachedir;

    SearchView searchView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        AndroidGraphicFactory.createInstance(this.getApplication());
        setContentView(R.layout.activity_main);
        mv = (MapView) findViewById(R.id.mapView);


        mv.setBuiltInZoomControls(true);
        mv.setClickable(true);

        tileCache = AndroidUtil.createTileCache(this, "mapcache",
                mv.getModel().displayModel.getTileSize(), 1f,
                mv.getModel().frameBufferModel.getOverdrawFactor());

        SearchManager sMgr = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setSearchableInfo(sMgr.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);

        opentrailDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/opentrail/";

        downloadCache = new DownloadCache(new File(opentrailDir + "geojson/"));

        ds = new GeoJSONDataSource("http://www.free-map.org.uk/fm/ws/tsvr.php",
                "way=highway,natural,waterway,railway,power,barrier,landuse&poi=all&ext=20&contour=1&coastline=1&outProj=4326");

        ds.setDownloadCache(downloadCache);

        proj = new OSGBProjection();
        cachedir = makeCacheDir(proj.getID());

        float lat = 50.9f, lon = -1.4f;
        int zoom = 14;

        int cacheClearFreq = 0;
        lastCacheClearTime = System.currentTimeMillis();

        wrCacheMgr = new WalkrouteCacheManager(opentrailDir + "/walkroutes/");

        overlayManager = new OverlayManager(this, mv, getResources().getDrawable(R.mipmap.person),
                getResources().getDrawable(R.mipmap.flag),
                new Drawable[] {getResources().getDrawable(R.mipmap.caution),
                        getResources().getDrawable(R.mipmap.directions),
                        getResources().getDrawable(R.mipmap.interest)},
                proj);

        mapLocationProcessor = new MapLocationProcessorBR(new MapLocationProcessor.LocationReceiver() {
            public void noGPS() {

            }

            public void receiveLocation(double lon, double lat, boolean refresh) {
                OpenTrail.this.location = new LatLong(lat, lon);
                Point pt = new Point(lon, lat);


                try {
                    Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();

                    if (recordingWalkroute != null && recordingWalkroute.getPoints().size() != 0) {

                        if (overlayManager.hasRenderedWalkroute()) {
                            overlayManager.addPointToWalkroute(pt);
                        } else {
                            overlayManager.addWalkroute(recordingWalkroute, false);
                        }
                    }
                } catch (Exception e) {
                    DialogUtils.showDialog(OpenTrail.this, "Unable to read GPS track for drawing");
                }

                alertDisplayMgr.update(pt);

                if (prefAutoDownload && poiDeliverer.needNewData(pt)) {
                    if (poiDeliverer.isCache(pt))
                        Toast.makeText(OpenTrail.this, "Loading data from cache", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(OpenTrail.this, "Loading data from web", Toast.LENGTH_SHORT).show();
                    startPOIDownload(false, false, OpenTrail.this.location);

                }
                if (prefGPSTracking) {
                    gotoMyLocation();
                    tileRendererLayer.requestRedraw();
                    overlayManager.requestRedraw();
                }
            }
        }, this, overlayManager);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (savedInstanceState != null) {
            lat = savedInstanceState.getFloat("lat", 51.05f);
            lon = savedInstanceState.getFloat("lon", -0.72f);
            zoom = savedInstanceState.getInt("zoom", 14);
            isRecordingWalkroute = savedInstanceState.getBoolean("isRecordingWalkroute", false);
            waitingForNewPOIData = savedInstanceState.getBoolean("waitingForNewPOIData");
            lastCacheClearTime = savedInstanceState.getLong("lastCacheClearTime", System.currentTimeMillis());
            int curWalkrouteId = savedInstanceState.getInt("curWalkrouteId", 0);
            if (curWalkrouteId > 0) {
                try {
                    curDownloadedWalkroute = wrCacheMgr.getWalkrouteFromCache(curWalkrouteId);
                } catch (Exception e) {
                    DialogUtils.showDialog(this, "Could not retrieve current walk route from cache:  " + e);
                }
            }
        } else if (prefs != null) {
            lat = prefs.getFloat("lat", 51.05f);
            lon = prefs.getFloat("lon", -0.72f);
            zoom = prefs.getInt("zoom", 14);
            isRecordingWalkroute = prefs.getBoolean("isRecordingWalkroute", false);
            waitingForNewPOIData = prefs.getBoolean("waitingForNewPOIData", false);
            lastCacheClearTime = prefs.getLong("lastCacheClearTime", System.currentTimeMillis());
            cacheClearFreq = Integer.parseInt(prefs.getString("prefCacheClearFreq", "0"));
            boolean prefGPSTrackingTest = prefs.getBoolean("prefGPSTracking", false);
            if (prefGPSTrackingTest) {
                LocationManager mgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    DialogUtils.showDialog(this, "GPS not enabled, please enable it to see current location");
                } else {
                    mapLocationProcessor.getProcessor().showGpsWaiting("Waiting for GPS");
                }
            }
        }

        // 86400000 = number of milliseconds in a day
        if (cacheClearFreq > 0 && System.currentTimeMillis() - lastCacheClearTime > cacheClearFreq * 86400000L) {
            clearCache();
        }

        initPos = new LatLong(lat, lon);

        mv.setCenter(initPos);
        mv.setZoomLevel((byte) zoom);

        createTileRendererLayer();

        httpCallback = new HTTPCallback(this);

        Shared.initSavedDataInstance();

        // do something about reconnecting asynctasks
        Shared.savedData.reconnect(this, httpCallback);


        annCacheMgr = new AnnotationCacheManager(opentrailDir + "/annotations");


        FreemapFileFormatter formatter = new FreemapFileFormatter(this.proj.getID());
        formatter.setScript("bsvr.php");
        formatter.selectPOIs("place,amenity,natural");
        formatter.selectAnnotations(true);
        WebDataSource wds = new WebDataSource("http://www.free-map.org.uk/fm/ws/", formatter);
        poiDeliverer = new CachedTileDeliverer("poi", wds, new XMLDataInterpreter
                (new FreemapDataHandler()), 5000, 5000, this.proj, cachedir);
        poiDeliverer.setCache(true);
        poiDeliverer.setReprojectCachedData(true);

        if (Shared.pois == null) {
            Shared.pois = new FreemapDataset();
            Shared.pois.setProjection(proj);
        }

        File wrDir = new File(opentrailDir + "/walkroutes/");
        if (!wrDir.exists()) {
            wrDir.mkdir();
        }

        alertDisplay = new AlertDisplay() {
            public void displayAnnotationInfo(String msg, int type, int alertId) {
                DialogUtils.showDialog(OpenTrail.this, msg);


                Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (ringtoneUri != null) {
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), ringtoneUri);
                    r.play();
                }
                String summary = (type == AlertDisplay.ANNOTATION) ? "New walk note" : "New walk route stage";
                NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                Notification.Builder nBuilder = new Notification.Builder(OpenTrail.this).setSmallIcon(R.drawable.marker).setContentTitle(summary).
                        setContentText(msg);
                Intent intent = new Intent(OpenTrail.this, OpenTrail.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent pIntent = PendingIntent.getActivity(OpenTrail.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                nBuilder.setContentIntent(pIntent);
                mgr.notify(alertId, nBuilder.getNotification()); // deprecated api level 16 - use build() instead

            }
        };

        alertDisplayMgr = new AlertDisplayManager(alertDisplay, 50);

        if (curDownloadedWalkroute != null && curDownloadedWalkroute.getPoints().size() > 0) {

            alertDisplayMgr.setWalkroute(curDownloadedWalkroute);
            overlayManager.addWalkroute(curDownloadedWalkroute);
        }


        ;
        filter = new IntentFilter();
        filter.addAction("freemap.opentrail.providerenabled");
        filter.addAction("freemap.opentrail.statuschanged");
        filter.addAction("freemap.opentrail.locationchanged");


        dataReceiver = new DataReceiver() {
            public void receivePOIs(FreemapDataset ds) {
                Log.d("OpenTrail", "POIs received");
                if (ds != null) {
                    Shared.pois = ds;
                    alertDisplayMgr.setPOIs(Shared.pois);
                    waitingForNewPOIData = false;
                    loadAnnotationOverlay();
                }
            }

            public void receiveWalkroutes(ArrayList<WalkrouteSummary> walkroutes) {
                Shared.walkroutes = walkroutes;
            }

            public void receiveWalkroute(int idx, Walkroute walkroute) {


                setWalkroute(walkroute);


                try {
                    wrCacheMgr.addWalkrouteToCache(walkroute);
                } catch (IOException e) {
                    DialogUtils.showDialog(OpenTrail.this, "Downloaded walk route, but unable to save to cache: " +
                            e.getMessage());
                }
            }
        };




        alertDisplayMgr.setPOIs(Shared.pois);

        gpsServiceConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName n, IBinder binder) {
                gpsService = ((GPSService.Binder) binder).getService();

            }

            public void onServiceDisconnected(ComponentName n) {
                //	gpsService = null; does this cause garbage collector to clean up service?
            }
        };

        Intent bindServiceIntent = new Intent(this, GPSService.class);
        bindService(bindServiceIntent, gpsServiceConn, Context.BIND_AUTO_CREATE);

        registerReceiver(mapLocationProcessor, filter);

        if (loadedRenderTheme=loadAssetsRenderTheme("freemap_v4.xml")) {
            addOverlays();
        }


    }

    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        prefGPSTracking = prefs.getBoolean("prefGPSTracking", true);
        prefAnnotations = prefs.getBoolean("prefAnnotations", true);
        prefAutoDownload = prefs.getBoolean("prefAutoDownload", false);
        prefNoUpload = prefs.getBoolean("prefNoUpload", false);

        overlayManager.setAnnotationsShowing(prefAnnotations);
        overlayManager.requestRedraw();

    }

    public void onResume() {
        super.onResume();
        if(loadedRenderTheme) {

            startGPS();
        }

        mv.requestFocus();
    }

    public void onStop() {
        super.onStop();
    }

    public void onPause() {
        super.onPause();


    }

    protected void onDestroy() {


        // this was in onStop() in former version of opentrail
        // 300517 now in onDestroy() as we want the service to continue going if the activity
        // is running but not visible (i.e to show notifications of directions etc)

        if(loadedRenderTheme) {
            Intent stopIfNotLoggingBroadcast = new Intent("freemap.opentrail.stopifnotlogging");
            sendBroadcast(stopIfNotLoggingBroadcast);
        }

        // from minimal Mapsforge example - this should (hopefully...) destroy everything
        mv.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();

        unregisterReceiver(mapLocationProcessor);
        unbindService(gpsServiceConn);

        Shared.savedData.disconnect();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (location != null) {
            editor.putFloat("lat", (float) location.latitude);
            editor.putFloat("lon", (float) location.longitude);
        }
        editor.putInt("zoom", mv.getModel().mapViewPosition.getZoomLevel());
        editor.putLong("lastCacheClearTime", lastCacheClearTime);
        editor.putBoolean("isRecordingWalkroute", isRecordingWalkroute);
        editor.commit();

    }

    // according to the docs, if it's a single top activity and the activity is already on the
    // top of the stack, this gets called if a new intent is received, not onCreate()
    // so we can use this to handle a search intent
    // https://developer.android.com/guide/topics/manifest/activity-element.html
    // https://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)

    public void onNewIntent(Intent intent) {
        // was the intent a search intent? if so launch the search activity
        // blog.dpdearing.com/2011/05/getting-android-to-call-onactivityresult-after-onsearchrequested

        // called before onResume()
        if (intent != null && intent.ACTION_SEARCH.equals(intent.getAction())) {
            Intent launchSearchResultsIntent = new Intent(this, SearchResultsActivity.class);
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (location != null) {
                Point p = new OSGBProjection().project(new Point(location.longitude,
                        location.latitude));
                launchSearchResultsIntent.putExtra("projectedX", p.x);
                launchSearchResultsIntent.putExtra("projectedY", p.y);
            }
            launchSearchResultsIntent.putExtra("query", query);
            startActivityForResult(launchSearchResultsIntent, 1);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem recordWalkrouteMenuItem = menu.findItem(R.id.recordWalkrouteMenuItem);
        recordWalkrouteMenuItem.setTitle(isRecordingWalkroute ? "Stop recording" : "Record walk route");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        LatLong loc = this.location != null ? this.location :
            mv.getModel().mapViewPosition.getCenter();

        boolean retcode = true;
        if (item.getItemId() == R.id.aboutMenuItem) {
            about();
        } else {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.myLocationMenuItem:
                    if (this.location != null) {
                        gotoMyLocation();
                    } else {
                        DialogUtils.showDialog(this, "Location not known yet");
                    }
                    break;

                case R.id.inputAnnotationMenuItem:
                    launchInputAnnotationActivity(loc.latitude, loc.longitude);
                    break;


                case R.id.settingsMenuItem:

                    intent = new Intent(this, OpenTrailPreferences.class);
                    startActivity(intent);
                    break;

                case R.id.poisMenuItem:
                    SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    startPOIDownload(true, sprefs.getBoolean("prefForcePOIDownload", false), loc);
                    break;

                case R.id.findPoisMenuItem:

                    intent = new Intent(this, POITypesListActivity.class);
                    Point p = this.proj.project(new Point(loc.longitude, loc.latitude));
                    intent.putExtra("projectedX", p.x);
                    intent.putExtra("projectedY", p.y);
                    startActivityForResult(intent, 1);

                    break;

                case R.id.walkroutesMenuItem:
                    if (loc == null) {
                        DialogUtils.showDialog(this, "Location not known");
                    } else {

                        Shared.savedData.setDataCallbackTask(new DownloadWalkroutesTask(this, dataReceiver, loc));
                        ((DownloadWalkroutesTask) Shared.savedData.getDataCallbackTask()).execute();
                    }
                    break;

                case R.id.findWalkroutesMenuItem:
                    if (Shared.walkroutes != null) {
                        intent = new Intent(this, WalkrouteListActivity.class);
                        startActivityForResult(intent, 2);
                    } else {
                        DialogUtils.showDialog(this, "No walk routes downloaded yet");
                    }
                    break;

                case R.id.uploadAnnotationsMenuItem:
                    uploadCachedAnnotations();
                    break;

                case R.id.recordWalkrouteMenuItem:
                    isRecordingWalkroute = !isRecordingWalkroute;
                    item.setTitle(isRecordingWalkroute ? "Stop recording" : "Record walk route");


                    overlayManager.removeWalkroute(true);

                    if (isRecordingWalkroute) {

                        mv.invalidate();


                        Intent startLoggingBroadcast = new Intent("freemap.opentrail.startlogging");
                        sendBroadcast(startLoggingBroadcast);

                    } else {

                        Intent stopLoggingBroadcast = new Intent("freemap.opentrail.stoplogging");
                        sendBroadcast(stopLoggingBroadcast);
                        showWalkrouteDetailsActivity();
                    }
                    break;
                case R.id.uploadWalkrouteMenuItem:
                    showRecordedWalkroutesActivity();
                    break;

                case R.id.clearCacheMenuItem:
                    clearCache();
                    break;


                default:
                    retcode = false;
                    break;
            }
        }
        return retcode;

    }


    public void onActivityResult(int request, int result, Intent i) {
        Bundle extras;
        if (result == RESULT_OK) {
            switch (request) {

                case 0:
                    extras = i.getExtras();
                    if (extras.getBoolean("success")) {
                        boolean isWalkrouteAnnotation = extras.getBoolean("walkrouteAnnotation");

                        String id = extras.getString("ID"), description = extras.getString("description");
                        LatLong gp = new LatLong(extras.getDouble("lat"), extras.getDouble("lon"));
                        Point p = new Point(extras.getDouble("lon"), extras.getDouble("lat"));
                        String annotationType = extras.getString("annotationType");
                        Marker item = MapsforgeUtil.makeTappableMarker(this, isWalkrouteAnnotation ?
                                getResources().getDrawable(R.mipmap.flag) :
                                (annotationType.equals("1")?getResources().getDrawable(R.mipmap.caution):
                                getResources().getDrawable(R.mipmap.interest)), gp, description);





                        mv.invalidate();

                        Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();

                        if (isWalkrouteAnnotation && isRecordingWalkroute && recordingWalkroute != null) {
                            recordingWalkroute.addStage(p, description);
                            ArrayList<Walkroute.Stage> stages = recordingWalkroute.getStages();
                            overlayManager.addStageToWalkroute(stages.get(stages.size()-1));
                            overlayManager.requestRedraw();
                  //      } else if (idInt < 0) { // 240516 why???
                        } else {
                            int idInt = id.equals("0") ? -(annCacheMgr.size() + 1) : Integer.parseInt(id);
                            try {
                                Annotation an = new Annotation(idInt, p.x, p.y, description, annotationType);
                                if(prefNoUpload) {
                                    annCacheMgr.addAnnotation(an); // adding in wgs84 latlon
                                }

                                // 290116 add the annotation to the layer
                                // 240517 must add before the overlay manager!! Otherwise the next call
                                // reprojects the thing and then we're screwed
                                overlayManager.addAnnotation(an, false);

                                overlayManager.requestRedraw();

                                Shared.pois.add(an); // this reprojects it


                                Log.d("OpenTrail","annotation ought to have been added: ID=" + an.getId());
                            } catch (IOException e) {
                                DialogUtils.showDialog(this, "Could not save annotation, please enable upload");
                            }
                        }
                    }
                    break;
                case 1:

                    // 060517 POI activity now returns the projected x and y of the selected POI
                    // so get this, unproject and set the mapview's centre point to this point
                    // Note: no marker added at the POI now
                    if(i!=null) {
                        extras = i.getExtras();
                        if (extras.containsKey("foundX") && extras.containsKey("foundY")) {
                            Point llPoint = this.proj.unproject(new Point(extras.getDouble("foundX"), extras.getDouble("foundY")));
                            mv.setCenter(new LatLong(llPoint.y, llPoint.x));
                        }
                    }
                    break;

                case 2:
                    extras = i.getExtras();
                    int idx = extras.getInt("selectedRoute"), wrId = Shared.walkroutes.get(idx).getId();
                    boolean loadSuccess = false;

                    if (wrCacheMgr.isInCache(wrId)) {
                        try {
                            Walkroute wr = wrCacheMgr.getWalkrouteFromCache(wrId);
                            loadSuccess = true;
                            setWalkroute(wr);

                        } catch (Exception e) {
                            DialogUtils.showDialog(this, "Unable to retrieve route from cache: " +
                                    e.getMessage() + ". Loading from network");
                        }
                    }
                    if (!loadSuccess) {

                        Shared.savedData.setDataCallbackTask(new DownloadWalkrouteTask(this, dataReceiver));
                        ((DownloadWalkrouteTask) Shared.savedData.getDataCallbackTask()).execute(wrId, idx);
                    }
                    break;


                case 3:
                    extras = i.getExtras();
                    String title = extras.getString("freemap.opentrail.wrtitle"),
                            description = extras.getString("freemap.opentrail.wrdescription"),
                            fname = extras.getString("freemap.opentrail.wrfilename");


                    Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();

                    if (recordingWalkroute != null) {
                        recordingWalkroute.setTitle(title);
                        recordingWalkroute.setDescription(description);


                        AsyncTask<String, Void, Boolean> addToCacheTask = new AsyncTask<String, Void, Boolean>() {

                            Walkroute recWR;
                            String errMsg;

                            private AsyncTask<String, Void, Boolean> init(Walkroute recWR) {
                                this.recWR = recWR;
                                return this;
                            }

                            public Boolean doInBackground(String... fname) {


                                try {

                                    wrCacheMgr.addRecordingWalkroute(recWR);
                                    wrCacheMgr.renameRecordingWalkroute(fname[0]);
                                    gpsService.clearRecordingWalkroute();

                                } catch (IOException e) {
                                    errMsg = e.toString();
                                    return false;
                                }
                                return true;
                            }

                            protected void onPostExecute(Boolean result) {
                                if (!result) {
                                    DialogUtils.showDialog(OpenTrail.this, "Unable to save walk route: error=" + errMsg);
                                } else {
                                    overlayManager.removeWalkroute(true);

                                    DialogUtils.showDialog(OpenTrail.this, "Successfully saved walk route.");
                                }
                            }
                        }.init(recordingWalkroute);
                        addToCacheTask.execute(fname);

                    } else {
                        DialogUtils.showDialog(this, "No recorded walk route");
                    }

                    break;

                case 4:
                    extras = i.getExtras();
                    String filename = extras.getString("freemap.opentrail.gpxfile");
                    uploadRecordedWalkroute(filename);
                    break;
            }
        }
    }

    private void startGPS() {
        // note service stuff below copied from old opentrail
        // in that, onResume() called "refreshDisplay(true)" (only) before doing this

        //services can be both started and bound
        //http://developer.android.com/guide/components/bound-services.html
        //we need this as the activity requires data from the service, but we
        //also need the service to keep going once the activity finishes
        Intent startServiceIntent = new Intent(this, GPSService.class);
        startServiceIntent.putExtra("wrCacheLoc", opentrailDir + "/walkroutes/");
        startServiceIntent.putExtra("isRecordingWalkroute", isRecordingWalkroute);
        startService(startServiceIntent);
    }

    private void createTileRendererLayer() {
        tileRendererLayer = new TileRendererLayer(tileCache, ds,
                mv.getModel().mapViewPosition,
                AndroidGraphicFactory.INSTANCE);
    }

    private boolean loadAssetsRenderTheme(String filename) {
        try {
            theme = new AssetsRenderTheme(this, "", filename);
            tileRendererLayer.setXmlRenderTheme(theme);
            return true;
        } catch (IOException e) {
            DialogUtils.showDialog(this, e.toString());
            return false;
        }
    }

    private void addOverlays() {


        // This is a bit messy, we are forced to give the location marker an arbitrary location
        // when we first create it.


        mv.addLayer(tileRendererLayer);
        // 270517 remove addAllOverlays(): no longer necessary as overlays always added straight away

        if(prefGPSTracking==true && !overlayManager.isLocationMarker()) {
            Point markerPos = (location == null ? new Point(-0.72, 51.05) :
                    new Point(location.longitude, location.latitude));
            overlayManager.addLocationMarker(markerPos);
        }

        if(gpsService!=null) {
            Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();
            if(recordingWalkroute!=null && recordingWalkroute.getPoints().size()>0) {
                overlayManager.addWalkroute(recordingWalkroute, false);
            }
        }
    }

    public void launchInputAnnotationActivity(double lat, double lon) {

        Intent intent = new Intent(this,InputAnnotationActivity.class);
        Bundle extras = new Bundle();
        extras.putDouble("lat", lat);
        extras.putDouble("lon", lon);
        extras.putBoolean("isRecordingWalkroute", isRecordingWalkroute);
        intent.putExtras(extras);
        startActivityForResult(intent, 0);
    }

    private void startPOIDownload(boolean showDialog, boolean forceWebDownload, LatLong loc)
    {
        if(Shared.savedData.getDataCallbackTask()==null || Shared.savedData.getDataCallbackTask().getStatus()!= AsyncTask.Status.RUNNING) {
                Shared.savedData.setDataCallbackTask(new DownloadPOIsTask(this, poiDeliverer, dataReceiver, showDialog, forceWebDownload, loc));
                ((DownloadPOIsTask) Shared.savedData.getDataCallbackTask()).execute();
        }
    }

    public void uploadCachedAnnotations() {
        if(annCacheMgr.isEmpty()) {
            DialogUtils.showDialog(this, "No annotations to upload");
        } else {
            try {
                String xml = annCacheMgr.getAllAnnotationsXML();
                ArrayList<NameValuePair> postData = new ArrayList<NameValuePair>();
                postData.add(new BasicNameValuePair("action","createMulti"));
                postData.add(new BasicNameValuePair("inProj","4326"));
                postData.add(new BasicNameValuePair("data",xml));


                Shared.savedData.executeHTTPCommunicationTask(new HTTPUploadTask
                        (this, "http://www.free-map.org.uk/fm/ws/annotation.php",
                                postData,
                                "Upload annotations?", httpCallback, 2), "Uploading...", "Uploading annotations...");
            } catch(IOException e) {
                DialogUtils.showDialog(this,"Error retrieving cached annotations: " + e.getMessage());
            }
        }
    }

    public void showWalkrouteDetailsActivity() {
        Intent intent = new Intent (this, WalkrouteDetailsActivity.class);

        Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();

        if(recordingWalkroute!=null) {
            intent.putExtra("distance", recordingWalkroute.getDistance());
        }

        startActivityForResult(intent, 3);
    }

    public void showRecordedWalkroutesActivity() {
        Intent intent = new Intent(this, RecordedWalkroutesListActivity.class);
        startActivityForResult(intent, 4);
    }

    public void uploadRecordedWalkroute(String wrFile) {
        try {
            final Walkroute walkroute = this.wrCacheMgr.getRecordedWalkroute(wrFile);
            if(walkroute!=null && walkroute.getPoints().size()>0) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if(prefs.getString("prefUsername", "").equals("") || prefs.getString("prefPassword","").equals("")) {
                    new AlertDialog.Builder(this).setMessage("WARNING: Username and password not specified in the preferences." +
                            " Walk route will still be uploaded but will need to be authorised before becoming "+
                            "visible.").setPositiveButton("OK",

                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface i, int which) {
                                    doUploadRecordedWalkroute(walkroute);
                                }
                            }

                    ).setNegativeButton("Cancel",null).show();
                } else {
                    doUploadRecordedWalkroute(walkroute);
                }
            }
        } catch(Exception e) {
            DialogUtils.showDialog(this,"Error obtaining walk route: " + e.getMessage());
        }
    }

    private void doUploadRecordedWalkroute(Walkroute walkroute) {

        if(walkroute!=null) {

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

    private void setWalkroute(Walkroute walkroute) {

        // 100316 changed this so that we only store the current walkroute in full
        // others are stored as WalkrouteSummary objects as we need only store the summary
        curDownloadedWalkroute = walkroute;
        alertDisplayMgr.setWalkroute(walkroute);
        overlayManager.addWalkroute(walkroute);
    }

    public void gotoMyLocation() {

        mv.setCenter(location);

    }

    private void clearCache() {
        ConfigChangeSafeTask<Void,Void> clearCacheTask = new ConfigChangeSafeTask<Void,Void>(this) {

            public String doInBackground(Void... unused) {
                boolean result = downloadCache.clear();
                if (result) {
                    lastCacheClearTime = System.currentTimeMillis();
                    return "Cache cleared successfully";
                } else {
                    return "Unable to clear cache";
                }
            }
        };
        clearCacheTask.setDialogDetails("Clearing", "Clearing cache...");
        clearCacheTask.execute();
    }

    private String makeCacheDir(String projID) {
        String cachedir = opentrailDir +"/cache/" + projID.toLowerCase().replace("epsg:", "")+"/";
        File dir = new File(cachedir);
        if(!dir.exists())
            dir.mkdirs();
        return cachedir;
    }

    public void loadAnnotationOverlay() {
        if (overlayManager!=null) {
            Shared.pois.operateOnAnnotations(overlayManager);
        //    overlayManager.showAnnotations(); // no need now - above call does this
            tileRendererLayer.requestRedraw();
            overlayManager.requestRedraw();
        }
    }

    public void about() {

        DialogUtils.showDialog(this, "OpenTrail 0.3 (beta), using Mapsforge. Uses OpenStreetMap data, copyright 2004-17 " +
                "OpenStreetMap contributors, Open Database Licence. Uses " +
                "Ordnance Survey OpenData LandForm Panorama contours, Crown Copyright." +
                "Person icon taken from the osmdroid project. Annotation icon based on " +
                "OpenStreetMap viewpoint icon.");
    }

    public void onSaveInstanceState(Bundle state)  {
        super.onSaveInstanceState(state);
        if (this.location!=null) {

            state.putFloat("lat", (float)this.location.latitude);
            state.putFloat("lon", (float)this.location.longitude);

        }
        state.putInt("zoom", mv.getModel().mapViewPosition.getZoomLevel());
        state.putBoolean("isRecordingWalkroute", isRecordingWalkroute);
        state.putBoolean("waitingForNewPOIData",waitingForNewPOIData);
        state.putLong("lastCacheClearTime", lastCacheClearTime);
        if(curDownloadedWalkroute != null) {
            state.putInt("curWalkrouteId", curDownloadedWalkroute.getId());
        }
    }
}
