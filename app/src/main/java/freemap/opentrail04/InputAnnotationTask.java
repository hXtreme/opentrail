/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;


import java.io.IOException;

import java.util.ArrayList;


import org.apache.http.NameValuePair;


import freemap.andromaps.DataCallbackTask;
import freemap.datasource.HTTPCommunicator;
import android.content.Context;
import freemap.andromaps.Base64;


public class InputAnnotationTask extends DataCallbackTask<ArrayList<NameValuePair>, Void> {

    public interface Receiver
    {
        public void receiveResponse(String response);
    }

    boolean success;
    String username, password;

    public InputAnnotationTask(Context ctx, InputAnnotationTask.Receiver receiver)
    {
        super(ctx,receiver);
    }

    public String doInBackground(ArrayList<NameValuePair>... postData)
    {
        try
        {
            HTTPCommunicator comm=new HTTPCommunicator();
            if(username!=null && password!=null)
            {
                String details=username+":"+password;
                comm.setAuthentication(Base64.encodeBytes(details.getBytes()));
            }
            String resp = comm.postAndGetResponse
                    ("http://www.free-map.org.uk/fm/ws/annotation.php", postData[0]);
            if(comm.getStatusCode()==200)
            {
                setData(resp);
                success=true;
             //   return "Annotation added with ID " + resp;
                // 160517 i think above will ballsup the ID to beadded to the annotations
                return resp;
            }
            else if (comm.getStatusCode()==401)
            {
                return "Login incorrect - unable to add annotation";
            }
            else
            {
                return "Server Error, HTTP code=" + comm.getStatusCode();
            }
        }
        catch(IOException e)
        {
            return e.getMessage();
        }
    }

    public void receive(Object obj)
    {
        ((InputAnnotationTask.Receiver)receiver).receiveResponse((String)obj);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setLoginDetails(String username, String password)
    {
        this.username=username;
        this.password=password;
    }
}
