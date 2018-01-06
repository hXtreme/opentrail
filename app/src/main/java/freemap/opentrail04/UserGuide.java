/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by nick on 03/06/17.
 */

public class UserGuide extends Activity {

    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView wv= new WebView(this);
        setContentView(wv);
        wv.loadUrl("file:///android_asset/userguide.html");
    }
}
