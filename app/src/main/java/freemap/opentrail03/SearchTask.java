package freemap.opentrail03;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import freemap.andromaps.DataCallbackTask;
import freemap.data.POI;



public class SearchTask extends DataCallbackTask<String,Void>  {

    public interface Receiver {
        public void receivePOIs(ArrayList<POI> pois);
    }

    public SearchTask(Context ctx, SearchTask.Receiver receiver) {
        super(ctx,receiver);
        setShowProgressDialog(true);
        setShowDialogOnFinish(true);
        setDialogDetails("Searching...","Searching for places...");
    }


    public String doInBackground(String... searchTerm) {


        try {

            ArrayList<POI> pois = new ArrayList<POI>();
            URL url = new URL("http://www.free-map.org.uk/fm/ws/search.php?q="+searchTerm[0] +
                        "&format=json&outProj=4326&poi=all");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String data="", line;
            while((line=reader.readLine()) != null) {
                data += line;
            }
            JSONObject object = new JSONObject(data);
            JSONArray features = object.getJSONArray("features");
            for(int i=0; i<features.length(); i++) {
                POI curPOI;
                JSONObject curFeature = features.getJSONObject(i);
                JSONObject geometry = curFeature.getJSONObject("geometry");
                if(geometry!=null) {
                    JSONArray coords = geometry.getJSONArray("coordinates");
                    if(coords!=null) {
                        curPOI = new POI (coords.getDouble(0), coords.getDouble(1));
                        JSONObject properties = curFeature.getJSONObject("properties");
                        if(properties!=null) {
                            Iterator<String> keys = properties.keys();
                            while(keys.hasNext()) {
                                String k = keys.next();
                                curPOI.addTag(k, properties.getString(k));
                            }
                        }
                        pois.add(curPOI);
                    }
                }
            }
            setData(pois);
            return "Successfully downloaded";
        } catch(Exception e) {
            return "ERROR with search:" + e.toString() + " " + e.getMessage();
        }
    }

    public void receive(Object data) {
        if(receiver!=null) {
            ((SearchTask.Receiver) receiver).receivePOIs((ArrayList<POI>) data);
        }
    }

    // show dialog only if there was an error (in which case data will be null)
    protected void showFinishDialog(String result) {
        if(data==null) {
            super.showFinishDialog(result);
        }
    }
}
