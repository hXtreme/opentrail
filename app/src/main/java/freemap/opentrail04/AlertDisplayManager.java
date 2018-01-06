/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

import freemap.data.Annotation;
import freemap.datasource.FreemapDataset;
import freemap.data.Point;
import freemap.data.Walkroute;

public class AlertDisplayManager {


    private Annotation prevAnnotation;
    double distLimit;
    AlertDisplay display;
    FreemapDataset pois;
    Walkroute walkroute;
    Walkroute.Stage prevStage;
    int alertId;



    public AlertDisplayManager( AlertDisplay display,double distLimit)
    {
        this.distLimit = distLimit;
        this.display = display;
    }

    public void setWalkroute(Walkroute walkroute)
    {
        this.walkroute = walkroute;
    }

    public void setPOIs(FreemapDataset pois)
    {
        this.pois = pois;
    }

    public void update(Point lonLat)
    {
        if(pois!=null)
        {
            Annotation newAnnotation = pois.findNearestAnnotation(lonLat, distLimit, null);
            if(newAnnotation != null)
            {
                if(prevAnnotation==null || newAnnotation.getId() != prevAnnotation.getId())
                {
                    display.displayAnnotationInfo(newAnnotation.getDescription(), AlertDisplay.ANNOTATION, ++alertId);
                    prevAnnotation = newAnnotation;
                    alertId++;
                }
            }
        }
        if(walkroute!=null)
        {
            Walkroute.Stage newStage = walkroute.findNearestStage(lonLat, distLimit);
            if(newStage != null)
            {
                if (prevStage==null || newStage.id != prevStage.id)
                {
                    display.displayAnnotationInfo(newStage.description, AlertDisplay.WALKROUTE_STAGE, ++alertId);
                    prevStage = newStage;
                }

            }
        }
    }
}