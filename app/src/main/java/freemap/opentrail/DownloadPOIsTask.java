/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;

import java.util.Map;
import java.util.Set;

import freemap.data.Point;
import freemap.datasource.FreemapDataset;
import freemap.datasource.TiledData;
import android.content.Context;

import org.oscim.core.GeoPoint;

import freemap.datasource.CachedTileDeliverer;
import freemap.andromaps.DataCallbackTask;



public class DownloadPOIsTask extends DataCallbackTask<Void,Void>  {




    CachedTileDeliverer td;
    boolean forceWebDownload;
    GeoPoint location;

    public DownloadPOIsTask(Context ctx, CachedTileDeliverer td, DataReceiver receiver, boolean showDialog,
                            boolean forceWebDownload, GeoPoint location)
    {
        super(ctx,receiver);
        setShowProgressDialog(showDialog);
        setShowDialogOnFinish(showDialog);
        setDialogDetails("Downloading...","Downloading POIs...");
        this.td=td;
        this.forceWebDownload=forceWebDownload;
        this.location=location;
    }


    public String doInBackground(Void... unused)
    {


        try
        {

            td.setForceReload(forceWebDownload);

            Point p = new Point(location.getLongitude(),location.getLatitude());
    //        Log.d("OpenTrail","Updating data with point: " + p);
     //       Log.d("OpenTrail","getSurroundingTiles()retuend:" +td.updateSurroundingTiles(p));
            td.updateSurroundingTiles(p);
            //setData((FreemapDataset)td.getAllData());
      //      Log.d("OpenTrail","done");



			/* old TileDeliverer getAllData() code */
            FreemapDataset allData = new FreemapDataset();
            allData.setProjection(td.getProjection());
            Set<Map.Entry<String, TiledData>> entries = td.getAllTiles();
            for(Map.Entry<String, TiledData> e: entries)
            {
           //     android.util.Log.d("OpenTrail", "RETURNED: " + e.getValue());
                allData.merge(e.getValue());
            }
            setData(allData);
            return "Successfully downloaded";
        }
        catch(Exception e)
        {
            return e.toString() + " " + e.getMessage();
        }
    }

    public void receive(Object data)
    {
        if(receiver!=null)
            ((DataReceiver)receiver).receivePOIs((FreemapDataset)data);
    }
}
