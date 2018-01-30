/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;

import freemap.data.WalkrouteSummary;
import freemap.datasource.FreemapDataset;
import freemap.data.Walkroute;
import java.util.ArrayList;

public interface DataReceiver {
    public void receivePOIs(FreemapDataset dataset);
    public void receiveWalkroute(int id, Walkroute walkroute);
    public void receiveWalkroutes(ArrayList<WalkrouteSummary> walkroutes);
}