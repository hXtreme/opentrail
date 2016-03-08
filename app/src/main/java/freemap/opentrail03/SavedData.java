package freemap.opentrail03;

/**
 * Created by nick on 09/01/16.
 */


import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

import freemap.andromaps.DataCallbackTask;
import freemap.andromaps.HTTPCommunicationTask;
import freemap.data.Walkroute;
import freemap.datasource.FreemapDataset;
import android.app.Activity;

public class SavedData
{

    private DataCallbackTask<?,?> dataTask;
    private HTTPCommunicationTask dfTask;
  //  private FreemapDataset pois;
   // private ArrayList<Walkroute> walkroutes;

    private static SavedData instance;

    private SavedData() { }

    public static SavedData getInstance()
    {
        if(instance==null)
            instance = new SavedData();
        return instance;
    }

    public void reconnect(Context ctx, HTTPCommunicationTask.Callback callback)
    {


        Log.d("OpenTrail", "onActivityCreated(): dataTask = " + dataTask + " dfTask = " + dfTask);


        // any tasks running, connect to activity
        if(dataTask!=null)
        {
            Log.d("OpenTrail", "dataTask not null so reconnecting");

            dataTask.reconnect(ctx, callback);
        }
        if(dfTask!=null)
        {
            Log.d("OpenTrail", "dfTask not null so reconnecting");

            dfTask.reconnect(ctx, callback);
        }
        /* 250116 why is this necessary?
        if(Shared.pois==null && pois!=null)

            Shared.pois = pois;
        if(Shared.walkroutes==null && walkroutes!=null)
            Shared.walkroutes = walkroutes;
            */
    }

    public void setHTTPCommunicationTask (HTTPCommunicationTask dfTask)
    {
        this.dfTask = dfTask;
    }

    public void setHTTPCommunicationTask (HTTPCommunicationTask dfTask, String dialogTitle, String dialogText)
    {
        this.dfTask = dfTask;
        this.dfTask.setDialogDetails(dialogTitle, dialogText);
    }
    public void executeHTTPCommunicationTask (HTTPCommunicationTask dfTask, String dialogTitle, String dialogText)
    {
        Log.d("OpenTrail", "executeHTTPCommunciatonTask()");
        setHTTPCommunicationTask (dfTask, dialogTitle, dialogText);
        Log.d("OpenTrail", "confirmAndExccute()");
        dfTask.confirmAndExecute();
    }

    public HTTPCommunicationTask getHTTPCommunicationTask()
    {
        return dfTask;
    }

    public void setDataCallbackTask (DataCallbackTask<?,?> dataTask)
    {
        this.dataTask = dataTask;
    }

    public DataCallbackTask<?,?> getDataCallbackTask()
    {
        return dataTask;
    }

    public void disconnect()
    {


        Log.d("OpenTrail", "onDetach()");
        // any tasks running, disconnect from activity
        if(dataTask!=null && dataTask.getStatus()==AsyncTask.Status.RUNNING)
        {

            Log.d("OpenTrail", "disconnecting data task");

            dataTask.disconnect();
        }
        else
            dataTask = null;
        if(dfTask!=null && dfTask.getStatus()==AsyncTask.Status.RUNNING)
        {
            Log.d("OpenTrail", "disconnecting dfTask");

            dfTask.disconnect();
        }
        else
            dfTask = null;

 //       pois = Shared.pois;
  //      walkroutes = Shared.walkroutes;
    }

}
