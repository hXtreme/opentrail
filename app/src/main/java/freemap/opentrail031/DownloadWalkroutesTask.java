package freemap.opentrail031;

import java.util.ArrayList;


import freemap.data.WalkrouteSummary;
import freemap.datasource.WalkroutesHandler;
import freemap.datasource.WebXMLSource;


import android.util.Log;
import android.content.Context;

import freemap.andromaps.DataCallbackTask;
import org.mapsforge.core.model.LatLong;


public class DownloadWalkroutesTask extends DataCallbackTask<Void,Void> {


    LatLong location;


    public DownloadWalkroutesTask(Context ctx, DataReceiver receiver, LatLong location)
    {
        super(ctx,receiver);
        setDialogDetails("Downloading...","Downloading walk routes...");
        this.location=location;
    }

    public String doInBackground(Void... unused)
    {


        try
        {

            String url = "http://www.free-map.org.uk/fm/ws/wr.php?action=getByRadius&format=gpx&radius=20&lat="
                    + location.latitude
                    + "&lon="
                    + location.longitude;
            Log.d("OpenTrail","URL=" + url);
            WebXMLSource xmlsource = new WebXMLSource(url,new WalkroutesHandler());
            setData((ArrayList<WalkrouteSummary>) xmlsource.getData());


            return "Successfully downloaded walk routes";
        }
        catch (org.xml.sax.SAXException e)
        {
            return "saxexception:" +e.getMessage();
        }
        catch(java.io.IOException e)
        {
            return "ioexception:"  + e.getMessage();
        }

    }

    public void receive(Object data)
    {
        if(receiver!=null)
            ((DataReceiver)receiver).receiveWalkroutes((ArrayList<WalkrouteSummary>)data);
    }
}