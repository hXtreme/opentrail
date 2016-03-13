package freemap.opentrail03;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.Location;
import android.os.Bundle;
import android.content.Context;
import android.content.BroadcastReceiver;

import android.os.IBinder;
import android.content.IntentFilter;
import android.widget.Toast;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import freemap.data.Walkroute;
import freemap.data.TrackPoint;
import freemap.datasource.WalkrouteCacheManager;

import java.io.PrintWriter;

public class GPSService extends Service implements LocationListener {

    boolean isLogging, listeningForUpdates, firstStart;
    LocationManager mgr;
    long lastUpdateTime = 0L;
    GPSServiceReceiver receiver;
    static final long LOGGING_INTERVAL = 5000L, NO_LOGGING_INTERVAL = 10000L;
    WalkrouteCacheManager wrCacheMgr;
    Walkroute recordingWalkroute;





    String date;
    java.text.SimpleDateFormat sdf;

    class SaveWalkrouteTask extends AsyncTask<Void,Void,Boolean>
    {
        public Boolean  doInBackground(Void... data)
        {
            boolean retcode=true;
            try
            {

                if(wrCacheMgr!=null)
                    wrCacheMgr.addRecordingWalkroute(recordingWalkroute);
                Log.d("OpenTrail", "Saving walkroute: no. of stages=" + recordingWalkroute.getStages().size());
            }
            catch(java.io.IOException e)
            {
                retcode=false;
            }
            return retcode;
        }
    }

    class Binder extends android.os.Binder
    {
        public GPSService getService()
        {
            return GPSService.this;
        }
    }

    public class GPSServiceReceiver extends BroadcastReceiver
    {
        GPSService service;

        public GPSServiceReceiver(GPSService service)
        {
            this.service=service;
        }

        public void onReceive(Context ctx, Intent intent)
        {
            String action = intent.getAction();
            if(service!=null)
            {
                if( action.equals("freemap.opentrail.startlogging"))
                    service.startLogging();
                else if (action.equals("freemap.opentrail.stoplogging"))
                    service.stopLogging();
                else if(action.equals("freemap.opentrail.stopifnotlogging"))
                    service.stopIfNotLogging();
            }
        }
    }
    Binder binder;

    public void onCreate()
    {
        super.onCreate();
        binder = new GPSService.Binder();
        firstStart=true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        isLogging = prefs.getBoolean("isRecordingWalkroute", false);

        sdf = new java.text.SimpleDateFormat("yyMMdd.HHmmss");
        date = sdf.format(new java.util.Date());

    }

    // called typically by an Activity's onStart()
    // intent will be null if the service dies and is restarted
    public int onStartCommand(Intent intent, int startFlags, int id)
    {
        isLogging = (intent==null) ? isLogging:intent.getExtras().getBoolean("isRecordingWalkroute", false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        //String wrCacheLoc = prefs.getString("wrCacheLoc",null);
        String wrCacheLoc = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()+"/opentrail/walkroutes/";
        if(receiver==null)
        {
            receiver=new GPSServiceReceiver(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction("freemap.opentrail.startlogging");
            filter.addAction("freemap.opentrail.stoplogging");
            filter.addAction("freemap.opentrail.stopifnotlogging");
            registerReceiver(receiver, filter);
        }

        if(wrCacheMgr==null && wrCacheLoc!=null)
            wrCacheMgr = new WalkrouteCacheManager(wrCacheLoc);

        // start updates if not already started
        mgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        //Log.d("OpenTrail","trying to start gps updates");

        if(true)//firstStart) // 020314 is firstStart appropriate? not sure why this was done
        {
            // To recover from the service possibly being killed - try to reload a
            // recording walkroute if there is one.

            firstStart=false;
            try
            {
                if(wrCacheMgr!=null && recordingWalkroute==null) // 040314 we do not want to overwrite a currently recording walkroute
                {
                    recordingWalkroute=wrCacheMgr.getRecordingWalkroute();

                    if(recordingWalkroute!=null)
                        Log.d("OpenTrail", "Got isRecordingWalkroute from wrCacheMgr: nstages=" + recordingWalkroute.getStages().size());
                    else
                        Log.d("OpenTrail", "GPSService: recording walkroute is null");
                }

            }
            catch(Exception e)
            {
                Toast.makeText(getApplicationContext(), "Previous walk route corrupted, starting new one, renaming crashed walkroute with date." +
                                "Details of error: " + e,
                        Toast.LENGTH_LONG).show();
                wrCacheMgr.timestampRecordingWalkroute(); // save crashed route for possible recovery
            }

            if(recordingWalkroute==null)
            {
                recordingWalkroute = new Walkroute();
                Log.d("OpenTrail", "Creating new recording walkroute");
            }
        }

        Location lastKnown = null;

        if(!listeningForUpdates)
        {
            //Log.d("OpenTrail","not listening so starting");
            listeningForUpdates=true;
            mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, isLogging?
                    LOGGING_INTERVAL : NO_LOGGING_INTERVAL, 10, this);
            Intent broadcast = new Intent("freemap.opentrail.startupdates");
            this.sendBroadcast(broadcast);
        }
        // 090314 if we are listening for updates, immediately send a locationchanged broadcast so we can e.g. update display.
        else  if ((lastKnown=mgr.getLastKnownLocation(LocationManager.GPS_PROVIDER)) != null)
        {

            Intent broadcast = new Intent("freemap.opentrail.locationchanged");

            broadcast.putExtra("lat", lastKnown.getLatitude());
            broadcast.putExtra("lon", lastKnown.getLongitude());
            broadcast.putExtra("refresh", true);

            sendBroadcast (broadcast);
        }
        return START_STICKY;
    }

    public void onDestroy()
    {
        super.onDestroy();
        mgr.removeUpdates(this);
        unregisterReceiver(receiver);



		/* 230514 surely this should be in as if the service is killed when recording and the activity was never killed, we need to make sure
		 * we save to the preferences so the service will pick them up again next time?  so uncomment */

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor ed = prefs.edit();
        ed.putBoolean("gpsServiceIsLogging", isLogging);
        ed.commit();

        receiver=null;
    }
    public void clearRecordingWalkroute()
    {
        recordingWalkroute.clear();
    }

    // called if user starts a recording session
    public void startLogging()
    {
        isLogging=true;

        // adjust interval to 5 secs as we are logging
        LocationManager mgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mgr.removeUpdates(this);
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOGGING_INTERVAL, 10, this);
    }

    // called if user ends a recording session
    public void stopLogging()
    {
        isLogging=false;

        // adjust interval to 10 secs as we are not logging
        LocationManager mgr = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mgr.removeUpdates(this);
        mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, NO_LOGGING_INTERVAL, 10, this);

        // save walkroute before stopping
        new SaveWalkrouteTask().execute();

    }

    // called by activity onStop() typically - stop updates if no logging
    public void stopIfNotLogging()
    {
        if(!isLogging)
        {
            listeningForUpdates=false;
            mgr.removeUpdates(this);
            stopSelf();

        }

    }
    public void onLocationChanged(Location loc)
    {
        boolean refresh=false;

        long time = System.currentTimeMillis();
        TrackPoint tp = new TrackPoint(loc.getLongitude(), loc.getLatitude(), time);
        if(isLogging)
        {
            recordingWalkroute.addPoint(tp);

            if(time - lastUpdateTime > 10000 &&  isLogging)
            {
                new SaveWalkrouteTask().execute();
                lastUpdateTime = time;
            }

            refresh=true;
        }




        // send msg to any receivers informing them of location
        Intent broadcast = new Intent("freemap.opentrail.locationchanged");
        broadcast.putExtra("lat", loc.getLatitude());
        broadcast.putExtra("lon", loc.getLongitude());
        broadcast.putExtra("refresh", refresh);
        this.sendBroadcast(broadcast);
    }

    public void onProviderEnabled(String provider)
    {
        // pass on msg
        Intent broadcast = new Intent("freemap.opentrail.providerenabled");
        broadcast.putExtra("provider", provider);
        broadcast.putExtra("enabled", true);
        this.sendBroadcast(broadcast);
    }

    public void onProviderDisabled(String provider)
    {
        // pass on msg
        Intent broadcast = new Intent("freemap.opentrail.providerenabled");
        broadcast.putExtra("provider", provider);
        broadcast.putExtra("enabled", false);
        this.sendBroadcast(broadcast);
    }

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        // pass on msg
        Intent broadcast = new Intent("freemap.opentrail.statuschanged");
        broadcast.putExtra("provider", provider);
        broadcast.putExtra("status", status);
        this.sendBroadcast(broadcast);
    }

    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    public Walkroute getRecordingWalkroute()
    {
        return recordingWalkroute;
    }
}
