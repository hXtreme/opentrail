/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

import java.util.Map;


public class LayerFinder {


    static class FeatureTests {
        public static boolean isWaterFeature(String k, String v) {
            return k.equals("natural") && v.equals("water") ||
                    k.equals("waterway");
        }
    
        public static boolean isLandscapeFeature(String k, String v) {
            return k.equals("natural") && v.equals("wood") ||
                    k.equals("landuse") && v.equals("forest") ||
                    k.equals("natural") && v.equals("heath");
        }

        public static boolean isLand(String k, String v) {
            return k.equals("natural") && v.equals("nosea");
        }

        public static boolean isSea(String k, String v) {
            return k.equals("natural") && v.equals("sea");
        }
    }    


    public static int findLayer(Map<String, Object> properties) {

        int layer =6;

        for (Map.Entry<String, Object> property : properties.entrySet()) {
            String k = property.getKey(), v = (String)property.getValue();
            if(k.equals("contour")) {
                layer = 4; // contours under roads/paths
            } else if (k.equals("power")) {

              layer = 7; // power lines above all else
            } else if (FeatureTests.isLandscapeFeature(k,v)) {
               layer = 3; // woods etc below contours

            } else if (FeatureTests.isWaterFeature(k,v)) {
                layer = 5; // lakes above contours, below rds
            } else if (FeatureTests.isLand(k,v)) {

                layer = 2; // land below everything else

            } else if (FeatureTests.isSea(k,v)) {
                layer = 1; // sea below land

            }
        }

        return layer;
    }
}
