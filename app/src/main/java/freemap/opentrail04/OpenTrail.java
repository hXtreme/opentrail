/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;



import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.NetworkOnMainThreadException;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.util.Log;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.Toast;



import org.oscim.android.MapView;
import org.oscim.android.theme.AssetsRenderTheme;
import org.oscim.core.GeoPoint;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;


import freemap.andromaps.HTTPCommunicationTask;
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

import freemap.andromaps.DialogUtils;
import freemap.andromaps.MapLocationProcessor;

import freemap.datasource.AnnotationCacheManager;
import freemap.proj.OSGBProjection;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


public class OpenTrail extends AppCompatActivity {

    MapView mv;
    Map map;

    String opentrailDir;

    MapLocationProcessorBR mapLocationProcessor;

    HTTPCallback httpCallback;
    OverlayManager overlayManager;
    OSGBProjection proj;
    GeoPoint location, initPos;
    AnnotationCacheManager annCacheMgr;
    CachedTileDeliverer poiDeliverer;
    WalkrouteCacheManager wrCacheMgr;
    DataReceiver dataReceiver;
    AlertDisplay alertDisplay;
    AlertDisplayManager alertDisplayMgr;
    ServiceConnection gpsServiceConn;
    GPSService gpsService;
    Walkroute curDownloadedWalkroute;
    TileCache tileCache;


    long lastCacheClearTime;

    IntentFilter filter;


    boolean isRecordingWalkroute, waitingForNewPOIData;
    boolean prefGPSTracking, prefAutoDownload, prefAnnotations, prefNoUpload;
    String cachedir;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mv = (MapView) findViewById(R.id.mapView);
        map = mv.map();

        mv.setClickable(true);

        opentrailDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/opentrail/";

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        String renderThemeFile = metrics.densityDpi>=DisplayMetrics.DENSITY_XXHIGH ?
                "freemap_v4_xxhdpi.xml" :
                (metrics.densityDpi>= DisplayMetrics.DENSITY_XHIGH ? "freemap_v4_xhdpi.xml" :
                        (metrics.densityDpi>=DisplayMetrics.DENSITY_HIGH ? "freemap_v4_hdpi.xml":"freemap_v4_mdpi.xml"));
        UrlTileSource source = new FreemapGeojsonTileSource();
        source.setHttpEngine(new OkHttpEngine.OkHttpFactory());
        tileCache = new TileCache (this, null, "opentrail_tiles.db");
        tileCache.setCacheSize (512 * (1<<10));
        source.setCache(tileCache);
        VectorTileLayer l = map.setBaseMap(source);
        map.setTheme(new AssetsRenderTheme(getAssets(), "", renderThemeFile));
        map.layers().add(new BuildingLayer(map, l));
        map.layers().add(new LabelLayer(map, l));


        proj = new OSGBProjection();
        cachedir = makeCacheDir(proj.getID());

        float lat = 50.9f, lon = -1.4f;
        int zoom = 14;

        int cacheClearFreq = 0;
        lastCacheClearTime = System.currentTimeMillis();

        wrCacheMgr = new WalkrouteCacheManager(opentrailDir + "/walkroutes/");

        overlayManager = new OverlayManager(this, mv, getResources().getDrawable(R.mipmap.person),
                getResources().getDrawable(R.mipmap.flag),
                getResources().getDrawable(R.mipmap.marker),
                new Drawable[] {getResources().getDrawable(R.mipmap.caution),
                        getResources().getDrawable(R.mipmap.directions),
                        getResources().getDrawable(R.mipmap.interest)},
                proj);

        mapLocationProcessor = new MapLocationProcessorBR(new MapLocationProcessor.LocationReceiver() {
            public void noGPS() {

            }

            public void receiveLocation(double lon, double lat, boolean refresh) {
                OpenTrail.this.location = new GeoPoint(lat, lon);
                Point pt = new Point(lon, lat);

                Log.d("opentrail", "Lon/lat=" + lon +" " + lat);
                try {
                    Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();

                    if (recordingWalkroute != null && recordingWalkroute.getPoints().size() != 0) {

                        if (overlayManager.hasRenderedRecordingWalkroute()) {
                            overlayManager.addPointToRecordingWalkroute(pt);
                        } else {
                            overlayManager.addRecordingWalkroute(recordingWalkroute, false);
                        }
                    }
                } catch (Exception e) {
                    DialogUtils.showDialog(OpenTrail.this, "Unable to read GPS track for drawing");
                }

                alertDisplayMgr.update(pt);

                downloadNewPOIs(location);

                if (prefGPSTracking) {
                    gotoMyLocation();
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
            prefAutoDownload = prefs.getBoolean("prefAutoDownload", false);
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

        initPos = new GeoPoint(lat, lon);

        map.setMapPosition(initPos.getLatitude(), initPos.getLongitude(), 1 << zoom);

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
                String summary = (type == ANNOTATION) ? "New walk note" : "New walk route stage";
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
            overlayManager.addDownloadedWalkroute(curDownloadedWalkroute);
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

        addOverlays();


        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                GeoPoint loc = OpenTrail.this.getGPSPositionOrMapCentre();
                launchInputAnnotationActivity(loc.getLatitude(), loc.getLongitude());
            }
        });

    }

    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        prefGPSTracking = prefs.getBoolean("prefGPSTracking", true);
        prefAnnotations = prefs.getBoolean("prefAnnotations", true);
        prefAutoDownload = prefs.getBoolean("prefAutoDownload", false);
        prefNoUpload = prefs.getBoolean("prefNoUpload", false);
        overlayManager.setAnnotationsShowing(prefAnnotations);
       // overlayManager.requestRedraw();
        downloadNewPOIs(map.getMapPosition().getGeoPoint());
    }

    public void onResume() {
        super.onResume();
        mv.onResume();
        startGPS();
        mv.requestFocus();
    }

    public void onStop() {
        super.onStop();
    }

    public void onPause() {
        mv.onPause();
        super.onPause();
    }

    protected void onDestroy() {

        try {
            mv.onDestroy();
        }catch(NetworkOnMainThreadException e) {
            // silently catch this exception - issue in OkHttp in which data might be being
            // fetched on main thread. Seeing as the rest of onDestroy() does not depend on
            // new data being fetched, there is no harm in 'squashing' this one.
        }
        // this was in onStop() in former version of opentrail
        // 300517 now in onDestroy() as we want the service to continue going if the activity
        // is running but not visible (i.e to show notifications of directions etc)


        Intent stopIfNotLoggingBroadcast = new Intent("freemap.opentrail.stopifnotlogging");
        sendBroadcast(stopIfNotLoggingBroadcast);

        super.onDestroy();

        unregisterReceiver(mapLocationProcessor);
        unbindService(gpsServiceConn);

        Shared.savedData.disconnect();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        GeoPoint loc = this.getGPSPositionOrMapCentre();

        editor.putFloat("lat", (float)loc.getLatitude());
        editor.putFloat("lon", (float)loc.getLongitude());

        editor.putInt("zoom", map.getMapPosition().getZoomLevel());
        editor.putLong("lastCacheClearTime", lastCacheClearTime);
        editor.putBoolean("isRecordingWalkroute", isRecordingWalkroute);
        editor.commit();
    }

    private void doSearch(Intent launchSearchResultsIntent, String query) {
        if (location != null) {
            Point p = new OSGBProjection().project(new Point(location.getLongitude(),
                    location.getLatitude()));
            launchSearchResultsIntent.putExtra("projectedX", p.x);
            launchSearchResultsIntent.putExtra("projectedY", p.y);
        }
        launchSearchResultsIntent.putExtra("query", query);
        startActivityForResult(launchSearchResultsIntent, 1);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item  = menu.findItem(R.id.action_search);
        SearchView sv = (SearchView) item.getActionView();
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String txt) {
                return true;
            }
            public boolean onQueryTextSubmit(String txt) {
                Intent intent = new Intent(OpenTrail.this, SearchResultsActivity.class);
                doSearch(intent, txt);
                return true;
            }
         });

        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem recordWalkrouteMenuItem = menu.findItem(R.id.recordWalkrouteMenuItem);
        recordWalkrouteMenuItem.setTitle(isRecordingWalkroute ? "Stop recording" : "Record walk route");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        GeoPoint loc = this.getGPSPositionOrMapCentre();

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
                    launchInputAnnotationActivity(loc.getLatitude(), loc.getLongitude());
                    break;


                case R.id.settingsMenuItem:
                    intent = new Intent(this, OpenTrailPreferences.class);
                    startActivity(intent);
                    break;

                case R.id.poisMenuItem:
                    SharedPreferences sprefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    startPOIDownload(true, false, loc);
                    break;

                case R.id.findPoisMenuItem:

                    intent = new Intent(this, POITypesListActivity.class);
                    Point p = this.proj.project(new Point(loc.getLongitude(), loc.getLatitude()));
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


                    overlayManager.removeRecordingWalkroute(true);

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

                case R.id.clearPOICacheMenuItem:
                    clearPOICache();
                    break;

                case R.id.userGuideMenuItem:
                    intent = new Intent(this, UserGuide.class);
                    startActivity(intent);
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
                        GeoPoint gp = new GeoPoint(extras.getDouble("lat"), extras.getDouble("lon"));
                        Point p = new Point(extras.getDouble("lon"), extras.getDouble("lat"));
                        String annotationType = extras.getString("annotationType");
                        Log.d("OpenTrail", "Annotation type=" + annotationType);
                        /* 311217 this appears to have never been used!
                        Marker item = MapsforgeUtil.makeTappableMarker(this, isWalkrouteAnnotation ?
                                getResources().getDrawable(R.mipmap.flag) :
                                (annotationType.equals("1")?getResources().getDrawable(R.mipmap.caution):
                                getResources().getDrawable(R.mipmap.interest)), gp, description);
                        */
                        mv.invalidate();

                        Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();

                        if (isWalkrouteAnnotation && isRecordingWalkroute && recordingWalkroute != null) {
                            recordingWalkroute.addStage(p, description);
                            ArrayList<Walkroute.Stage> stages = recordingWalkroute.getStages();
                            overlayManager.addStageToWalkroute(stages.get(stages.size()-1), false);
                        //    overlayManager.requestRedraw();
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

                          //      overlayManager.requestRedraw();

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
                            map.setMapPosition(llPoint.y, llPoint.x, map.getMapPosition().getScale());
                            overlayManager.addPOIMarker(new GeoPoint(llPoint.y, llPoint.x),
                                    extras.containsKey("name") ? extras.getString("name") : "");
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

                        AddToWalkrouteCacheTask addToCacheTask = new AddToWalkrouteCacheTask (this, recordingWalkroute);
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

    private void addOverlays() {

        // 270517 remove addAllOverlays(): no longer necessary as overlays always added straight away

        if(prefGPSTracking==true && !overlayManager.isLocationMarker()) {
            Point markerPos = (location == null ? new Point(-0.72, 51.05) :
                    new Point(location.getLongitude(), location.getLatitude()));
            overlayManager.addLocationMarker(markerPos);
        }

        if(gpsService!=null) {
            Walkroute recordingWalkroute = gpsService.getRecordingWalkroute();
            if(recordingWalkroute!=null && recordingWalkroute.getPoints().size()>0) {
                overlayManager.addRecordingWalkroute(recordingWalkroute, false);
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




    private void downloadNewPOIs(GeoPoint gp) {

        Point pt = new Point(gp.getLongitude(), gp.getLatitude());
        if (prefAutoDownload && poiDeliverer.needNewData(pt)) {
            if (poiDeliverer.isCache(pt)) {
                Toast.makeText(OpenTrail.this, "Loading POI data from cache", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OpenTrail.this, "Loading POI data from web", Toast.LENGTH_SHORT).show();
            }
        }
        startPOIDownload(false, false, gp);
    }

    private void startPOIDownload(boolean showDialog, boolean forceWebDownload, GeoPoint loc)
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
                String postData = "action=createMulti&inProj=4326&data=" + xml;
                Log.d("OpenTrail", "XML to send=" + xml);

                HTTPUploadTask task  = new HTTPUploadTask
                        (this, "http://www.free-map.org.uk/fm/ws/annotation.php",
                               postData,
                                "Upload annotations?", httpCallback, 2);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String username=prefs.getString("prefUsername",""), password=prefs.getString("prefPassword","");
                task.setLoginDetails(username, password);
                Shared.savedData.executeHTTPCommunicationTask(task, "Uploading...", "Uploading annotations...");
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
        overlayManager.addDownloadedWalkroute(walkroute);
    }

    public void gotoMyLocation() {
        map.setMapPosition(location.getLatitude(), location.getLongitude(), map.getMapPosition().getScale());
    }

    private void clearCache() {

        ClearTileCacheTask task = new ClearTileCacheTask(this);
        task.execute();
    }

    private void clearPOICache() {

        ClearPOICacheTask clearCacheTask = new ClearPOICacheTask(this);
        clearCacheTask.execute();
    }

    private void doDeletePOICache(File dir) {

        for (File f: dir.listFiles()) {
            if(f.isDirectory()) {
                doDeletePOICache(f);
            }
            f.delete();
        }
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
        }
    }

    public void about() {

        DialogUtils.showDialog(this, "OpenTrail 0.4 (beta), using VTM. Uses OpenStreetMap data, copyright 2004-17 " +
                "OpenStreetMap contributors, Open Database Licence. Uses " +
                "Ordnance Survey OpenData LandForm Panorama contours, Crown Copyright." +
                "Person icon taken from the osmdroid project. Annotation icon based on " +
                "OpenStreetMap viewpoint icon.");
    }

    private GeoPoint getGPSPositionOrMapCentre() {
        return this.location != null ? this.location :
                map.getMapPosition().getGeoPoint();
    }

    public void onSaveInstanceState(Bundle state)  {
        super.onSaveInstanceState(state);
        GeoPoint loc = this.getGPSPositionOrMapCentre();

        state.putFloat("lat", (float)loc.getLatitude());
        state.putFloat("lon", (float)loc.getLongitude());

        state.putInt("zoom", map.getMapPosition().getZoomLevel());
        state.putBoolean("isRecordingWalkroute", isRecordingWalkroute);
        state.putBoolean("waitingForNewPOIData",waitingForNewPOIData);
        state.putLong("lastCacheClearTime", lastCacheClearTime);
        if(curDownloadedWalkroute != null) {
            state.putInt("curWalkrouteId", curDownloadedWalkroute.getId());
        }
    }

    public class HTTPCallback implements HTTPCommunicationTask.Callback {

        Context ctx;

        public HTTPCallback(Context ctx) {
            this.ctx = ctx;
        }

        public void downloadFinished(int id, Object addData) {

            switch(id) {


                case 2:
                    annCacheMgr.deleteCache();
                    break;

                case 3:
                    // walkroute uploaded

                    break;
            }
        }

        public void downloadCancelled(int id) {

        }

        public void downloadError(int id) {
            DialogUtils.showDialog(ctx,"Upload/download task failed");
        }
    }

    static class AddToWalkrouteCacheTask extends AsyncTask<String, Void, Boolean> {

        Walkroute recWR;
        String errMsg;
        WeakReference<OpenTrail> activityRef;

        public AddToWalkrouteCacheTask (OpenTrail activity, Walkroute recWR) {
            this.activityRef = new WeakReference<OpenTrail>(activity);
            this.recWR = recWR;
        }

        public Boolean doInBackground(String... fname) {

            OpenTrail activity = activityRef.get();
            if(activity!=null) {
                try {

                    activity.wrCacheMgr.addRecordingWalkroute(recWR);
                    activity.wrCacheMgr.renameRecordingWalkroute(fname[0]);
                    activity.gpsService.clearRecordingWalkroute();

                } catch (IOException e) {
                    errMsg = e.toString();
                    return false;
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            OpenTrail activity = activityRef.get();
            if(activity!=null) {
                if (!result) {
                    DialogUtils.showDialog(activity, "Unable to save walk route: error=" + errMsg);
                } else {
                    activity.overlayManager.removeRecordingWalkroute(true);

                    DialogUtils.showDialog(activity, "Successfully saved walk route.");
                }
            }
        }
    }

    abstract static class WeakRefTask extends AsyncTask<Void, Void, String> {

        WeakReference<OpenTrail> activityRef;

        public WeakRefTask(OpenTrail activity) {
            activityRef = new WeakReference<OpenTrail>(activity);
        }

        protected String doInBackground(Void... unused) {
            OpenTrail activity = activityRef.get();
            if(activity!=null) {
                return doBackgroundTask(activity);
            }
            return "";
        }

        public void onPostExecute(String msg) {
            OpenTrail activity = activityRef.get();
            if (activity != null) {
                DialogUtils.showDialog(activity, msg);
            }
        }

        abstract public String doBackgroundTask(OpenTrail activity);
    }

    static class ClearPOICacheTask extends WeakRefTask  {

        public ClearPOICacheTask (OpenTrail activity) {
           super(activity);
        }
        @Override
        public String doBackgroundTask(OpenTrail activity) {

            activity.doDeletePOICache(new File(activity.cachedir));
            return "POI cache cleared successfully";
        }

    };

    static class ClearTileCacheTask extends WeakRefTask  {


        public ClearTileCacheTask (OpenTrail activity) {
            super(activity);
        }

        @Override
        public String doBackgroundTask(OpenTrail activity) {
            activity.tileCache.deleteTiles();
            return "Tile cache cleared successfully";
        }
    };
}
