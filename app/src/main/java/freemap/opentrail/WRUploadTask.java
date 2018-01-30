/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;

import android.content.Context;

import freemap.andromaps.HTTPUploadTask;
import freemap.data.Walkroute;

import android.util.Log;

// need to subclass HTTPUploadTask as loading gpx from file is also a lengthy process
// so we need to put it in the AsyncTask

public class WRUploadTask extends HTTPUploadTask {

    Walkroute walkroute;
    double dpDist;


    public WRUploadTask(Context ctx,  Walkroute walkroute, String url,
                        String alertMsg, Callback callback, int taskId, double dpDist)
    {
        super(ctx, url, null, alertMsg, callback, taskId);
        this.walkroute=walkroute;
        this.dpDist=dpDist;
    }

    public String doInBackground (Void... unused) {

        Walkroute simplified = walkroute.simplifyDouglasPeucker(dpDist);
        String gpx = simplified.toXML();
        String postData="action=add&route="+gpx+"&format=gpx";
        setPostData(postData);
        String status = super.doInBackground(unused);
        Log.d("OpenTrail","HTTP task status=" + status);
        return status;
    }

    public void onPostExecute (String code)
    {
        super.onPostExecute(code);
        Log.d("OpenTrail", "additional data, i.e response from server: "  + addData);
    }
}