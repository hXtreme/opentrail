
package freemap.opentrail03;

import android.graphics.drawable.Drawable;
import android.content.Context;
import android.util.Log;

import freemap.data.Annotation;
import freemap.data.Point;
import freemap.data.TrackPoint;
import freemap.data.Walkroute;
import freemap.data.Projection;

import freemap.andromaps.MapLocationProcessor;


import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.map.layer.overlay.Marker;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

// 240116 is walkrouteShowing necessary? Removed it
// is it necessary to test existence of tile renderer layer before adding things?
// can we not just assume it will be there?

// 140517 remove reference to tile renderer layer, as tile renderer layer/overlay management
// is much simpler with a single tile renderer layer rather than the old 0.1/0.2 style of
// having multiple mapfiles each with their own tile renderer layer.

public class OverlayManager  implements
        freemap.datasource.FreemapDataset.AnnotationVisitor,
        MapLocationProcessor.LocationDisplayer {


    protected Context ctx;
    protected Drawable locationIcon;
    protected Marker myLocOverlayItem;
    protected MapView mv;
    protected boolean markerShowing; // to prevent exceptions when marker added twice

    Paint outline;
    Drawable markerIcon;
    Drawable[] annotationIcons;

    HashMap<Integer,Marker> indexedAnnotations;
    Projection proj;
    Polyline renderedWalkroute;
    ArrayList<Marker> walkrouteStages;




    boolean annotationsShowing;


    public OverlayManager(Context ctx, MapView mapView, Drawable locationIcon,  Drawable markerIcon, Drawable[] annotationIcons,
                          Projection proj) {



        this.mv = mapView;
        this.ctx = ctx;
        this.locationIcon = locationIcon;

        this.markerIcon = markerIcon;
        this.annotationIcons = annotationIcons;
        this.proj = proj;

        outline = MapsforgeUtil.makePaint(Color.BLUE, 5, Style.STROKE); // also alpha 128

        walkrouteStages = new ArrayList<>();
        indexedAnnotations = new HashMap<>();
    }



    public void addLocationMarker(Point p) {
        myLocOverlayItem = MapsforgeUtil.makeMarker(locationIcon, new LatLong(p.y, p.x));
        showLocationMarker();
    }

    public void showLocationMarker() {
        if(mv!=null && myLocOverlayItem!=null && !markerShowing)
        {
            mv.addLayer(myLocOverlayItem);
            markerShowing=true;
        }
    }

    public void hideLocationMarker() {
        if(mv!=null && myLocOverlayItem!=null && markerShowing) {
            mv.getLayerManager().getLayers().remove(myLocOverlayItem);
            markerShowing=false;
        }
    }

    public void moveLocationMarker(Point p) {
        if(myLocOverlayItem!=null)
            myLocOverlayItem.setLatLong(new LatLong(p.y, p.x));
    }

    public void removeLocationMarker() {
        hideLocationMarker();
        myLocOverlayItem = null;
    }

    public boolean isLocationMarker() {
        return myLocOverlayItem != null;
    }

    public void addWalkroute(Walkroute walkroute) {
        addWalkroute(walkroute,true);
    }

    public void addWalkroute(Walkroute walkroute, boolean doCentreMap) {
        Log.d("OpenTrail", "addWalkroute(): walkroute details: " + walkroute.getId() +
                " length=" + walkroute.getPoints().size());
        // remove any existing walk route
        removeWalkroute(true);
        renderedWalkroute = new Polyline (MapsforgeUtil.makePaint(Color.BLUE, 5, Style.STROKE), AndroidGraphicFactory.INSTANCE);

        if(doCentreMap) {
            Point p = walkroute.getStart();
            if(p!=null) {
                LatLong gp = new LatLong(p.y, p.x);
                mv.setCenter(gp);
            }
        }
        ArrayList<TrackPoint> points = walkroute.getPoints();
        LatLong p[] = new LatLong[points.size()];
        for(int i=0; i<points.size(); i++)
            p[i] = new LatLong(points.get(i).y, points.get(i).x);


        for(int i=0; i<points.size(); i++)
            renderedWalkroute.getLatLongs().add(new LatLong(points.get(i).y, points.get(i).x));

        ArrayList<Walkroute.Stage> stages = walkroute.getStages();
        LatLong curStagePoint = null;

        for(int i=0; i<stages.size(); i++)
        {
            curStagePoint = new LatLong(stages.get(i).start.y, stages.get(i).start.x);
            Marker item = MapsforgeUtil.makeTappableMarker(ctx, markerIcon, curStagePoint, stages.get(i).description);
            walkrouteStages.add(item);

        }

        showWalkroute();

    }

    // 120316 do this to avoid having to redraw the *whole* walkroute every time we add a point...
    public void addPointToWalkroute(Point p) {
        renderedWalkroute.getLatLongs().add(new LatLong(p.y, p.x));
        renderedWalkroute.requestRedraw();
    }

    public boolean hasRenderedWalkroute() {
        return renderedWalkroute != null && renderedWalkroute.getLatLongs().size() > 0;
    }

    public void showWalkroute() {
        if(renderedWalkroute==null) return;
        Log.d("OpenTrail", "showWalkroute(): " +renderedWalkroute.getLatLongs().size() + " "+
                walkrouteStages);
        // only add the walkroute as a layer if not added already
        if(hasRenderedWalkroute() && walkrouteStages!=null)
        {
            Log.d("OpenTrail", "addWalkroute(): adding walk route");
            mv.getLayerManager().getLayers().add(renderedWalkroute);

            for(int i=0; i<walkrouteStages.size(); i++)
                mv.getLayerManager().getLayers().add(walkrouteStages.get(i));

        }
    }

    public void removeWalkroute(boolean removeData) {
        Log.d("OpenTrail", "removeWalkroute(): removeData=" + removeData);

        if(renderedWalkroute != null) {
            Log.d("newmapsforge", "removeWalkroute(): removing rendered walkroute");

            for(int i=0; i<walkrouteStages.size(); i++)
                mv.getLayerManager().getLayers().remove(walkrouteStages.get(i));

            mv.getLayerManager().getLayers().remove(renderedWalkroute);


        }

        if(removeData) {

            while(walkrouteStages.size() > 0)
                walkrouteStages.remove(0);
            renderedWalkroute = null;
        }
    }



    // to be called after adding the TileRendererLayer
    public void addAllOverlays() {
        showLocationMarker();
        showAnnotations();
        showWalkroute();
    }


    public void removeAnnotations() {
        if(annotationsShowing)
        {
            for(HashMap.Entry<Integer,Marker> entry: indexedAnnotations.entrySet())
                mv.getLayerManager().getLayers().remove(entry.getValue());
            annotationsShowing = false;
        }
    }

    public void showAnnotations() {

        Log.d("OpenTrail", "showAnnotations(): annotationsShowing=" + annotationsShowing +
            " indexedAnnotations=" + (indexedAnnotations!=null));
        if(!annotationsShowing && indexedAnnotations!=null)
        {
            Set<HashMap.Entry<Integer,Marker>> markersEntrySet = indexedAnnotations.entrySet();
            for(HashMap.Entry<Integer,Marker> entry: markersEntrySet) {

                Log.d("OpenTrail", "ADDING ANNOTATIONS (showAnnotations() loop - actually showing them)");
                mv.getLayerManager().getLayers().add(entry.getValue());
            }
            annotationsShowing = markersEntrySet.size()>0;
        }
    }


    // 290116 projection stuff - messy
    public void addAnnotation(Annotation ann, boolean unproject) {
        Point unproj = unproject ? proj.unproject(ann.getPoint()) : ann.getPoint();
        int type = ann.getType().equals("") ? 2:  Integer.parseInt(ann.getType());
        Marker item = MapsforgeUtil.makeTappableMarker(ctx, annotationIcons[type-1],
                new LatLong(unproj.y,unproj.x), ann.getDescription());

        indexedAnnotations.put(ann.getId(), item);
    }

    public void visit(Annotation ann) {

        if(indexedAnnotations.get(ann.getId()) == null)
        {
            Log.d("OpenTrail", "OverlayManager: adding annotation (visit method) - not showing them");
            addAnnotation(ann, true);
        }
    }


    public void requestRedraw() {
        if(renderedWalkroute!=null)
            renderedWalkroute.requestRedraw();
        for(int i=0; i<walkrouteStages.size(); i++)
            walkrouteStages.get(i).requestRedraw();
        for(HashMap.Entry<Integer,Marker> entry: indexedAnnotations.entrySet())
            entry.getValue().requestRedraw();

    }
}