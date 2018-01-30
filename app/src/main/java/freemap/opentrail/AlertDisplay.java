/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */
package freemap.opentrail;

public interface AlertDisplay {
    public static final int ANNOTATION = 0, WALKROUTE_STAGE = 1;
    public void displayAnnotationInfo(String message, int type, int alertId);
}
