/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;


import freemap.andromaps.DataCallbackTask;
import android.content.Context;
import freemap.andromaps.Base64;


public class InputAnnotationTask extends DataCallbackTask<String, Void> {

    public interface Receiver {
        public void receiveResponse(String response);
    }

    boolean success;
    String username, password;

    public InputAnnotationTask(Context ctx, InputAnnotationTask.Receiver receiver) {
        super(ctx,receiver);
    }

    public String doInBackground(String... postData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://www.free-map.org.uk/fm/ws/annotation.php");
            conn = (HttpURLConnection)url.openConnection();
            if(username!=null && password!=null) {
                String details=username+":"+password;
                conn.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(details.getBytes()));
            }
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(postData[0].length());
            OutputStream out = conn.getOutputStream();
            out.write(postData[0].getBytes());
            int status = conn.getResponseCode();
            if(status==200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String resp = "", line;
                while((line=reader.readLine())!=null) {
                    resp += line;
                }
                setData(resp);
                success=true;
             //   return "Annotation added with ID " + resp;
                // 160517 i think above will ballsup the ID to beadded to the annotations
                return resp;
            } else if (status==401) {
                return "Login incorrect - unable to add annotation";
            } else {
                return "Server Error, HTTP code=" + status;
            }
        } catch(IOException e) {
            return e.getMessage();
        } finally {
            if(conn!=null) {
                conn.disconnect();
            }
        }
    }

    public void receive(Object obj) {
        ((InputAnnotationTask.Receiver)receiver).receiveResponse((String)obj);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setLoginDetails(String username, String password) {
        this.username=username;
        this.password=password;
    }
}
