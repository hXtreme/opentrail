/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import freemap.data.Walkroute;

import freemap.datasource.WalkrouteHandler;

import freemap.datasource.WebXMLSource;


import android.util.Log;
import android.content.Context;

import freemap.andromaps.DataCallbackTask;


public class DownloadWalkrouteTask extends DataCallbackTask<Integer,Void> {



    int returnedIdx;


    public DownloadWalkrouteTask(Context ctx, DataReceiver receiver)
    {
        super(ctx,receiver);
        setDialogDetails("Downloading...","Downloading walk route...");
    }

    // two parameters, one the ID then other the index in the walkroutes arraylist
    public String doInBackground(Integer... input)
    {


        try
        {
            WebXMLSource source = new WebXMLSource(
                    "http://www.free-map.org.uk/fm/ws/wr.php?action=get&id="
                            + input[0] // Shared.walkroutes.get(idx[0]).getId()
                            + "&format=gpx", new WalkrouteHandler());
            Log.d("OpenTrail","URL="+"http://www.free-map.org.uk/fm/ws/wr.php?action=get&id="
                    + input[0] // Shared.walkroutes.get(idx[0]).getId()
                    + "&format=gpx");
            Log.d("OpenTrail","hello1");
            Walkroute wr = (Walkroute)source.getData();
            Log.d("OpenTrail","hello2");
            Log.d("OpenTrail","Sent back: data");
            setData(wr);
            returnedIdx = input[1];



            return "Successfully downloaded walk route";
        }
        catch (Exception e)
        {
            return e.getMessage();
        }

    }

    public void receive(Object obj)
    {
        Log.d("OpenTrail","Received a walkroute: ");

        if(receiver!=null)
            ((DataReceiver)receiver).receiveWalkroute(returnedIdx,(Walkroute)obj);

    }
}

