/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail;


import android.util.Log;

import org.oscim.tiling.source.geojson.GeojsonTileSource;

/**
 * Created by nick on 19/12/17.
 */


import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import java.util.Map;

public class FreemapGeojsonTileSource extends GeojsonTileSource {

  //  private final static String DEFAULT_URL = "http://www.free-map.org.uk/fm/ws/tsvr.php?way=highway,natural,waterway,railway,power,barrier,landuse&poi=all&ext=20&contour=1&coastline=1&outProj=4326";
    private final static String DEFAULT_URL =
          "http://www.free-map.org.uk/fm/ws/tsvr.php?way=highway,natural,waterway,railway,power,barrier&poi=all&contour=1&coastline=1&outProj=4326&ext=20";
    private final static String DEFAULT_PATH = "&x={X}&y={Y}&z={Z}";


    public FreemapGeojsonTileSource() {
        super (DEFAULT_URL, DEFAULT_PATH);
    }


    @Override
    public void decodeTags(MapElement mapElement, Map<String, Object> properties) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            mapElement.tags.add(new Tag (entry.getKey(), ""+entry.getValue()));
        }
    }
}
