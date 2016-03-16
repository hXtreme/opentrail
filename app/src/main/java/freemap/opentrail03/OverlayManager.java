
package freemap.opentrail03;

import org.mapsforge.map.android.view.MapView;


import android.graphics.drawable.Drawable;
import android.content.Context;
import android.util.Log;

import freemap.data.Annotation;
import freemap.data.POI;
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
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

// 240116 is walkrouteShowing necessary? Removed it
// is it necessary to test existence of tile renderer layer before adding things?
// can we not just assume it will be there?

public class OverlayManager extends LocationDisplayer implements
        freemap.datasource.FreemapDataset.AnnotationVisitor,
        MapLocationProcessor.LocationDisplayer {



    Paint outline;
    Drawable markerIcon, annotationIcon;

    HashMap<Integer,Marker> indexedAnnotations;
    Projection proj;
    Polyline renderedWalkroute;
    ArrayList<Polyline> renderedWalkroutes;
    ArrayList<Marker> walkrouteStages;
    Marker lastAddedPOI;
    TileRendererLayer tileLayer;


    boolean includeAnnotations;
    boolean poiShowing,  annotationsShowing;
    // boolean renderLayerAdded; // can replace with tileLayer = null?
    // boolean walkrouteShowing; // can replace with renderedWalkroute = null?

    public OverlayManager(Context ctx, MapView mapView, Drawable locationIcon,  Drawable markerIcon, Drawable annotationIcon,
                          Projection proj) {

        super(ctx,mapView,locationIcon);

        this.markerIcon = markerIcon;
        this.annotationIcon = annotationIcon;
        this.proj = proj;

        outline = MapsforgeUtil.makePaint(Color.BLUE, 5, Style.STROKE); // also alpha 128

        walkrouteStages = new ArrayList<Marker>();
        indexedAnnotations = new HashMap<Integer,Marker>();
    }

    public void addTileRendererLayer(TileRendererLayer layer) {
        if(tileLayer==null) // !renderLayerAdded)
        {
            tileLayer=layer;
            mv.addLayer(layer);
      //      renderLayerAdded = true;
        }
    }

    public void removeTileRendererLayer() {
        if(tileLayer!=null)//renderLayerAdded)
        {
            mv.getLayerManager().getLayers().remove(tileLayer);
            tileLayer = null;
           // renderLayerAdded=false;
        }
    }

    // Note the difference between "setting" an overlay and adding it.
    // "Setting" it merely creates the overlay layer but will only actually add it
    // as an overlay if the tile renderer layer has been added first.
    // "setting" also removes any existing object from the map

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

        // Only add the walk route if the tile render layer has been added already

        Log.d("OpenTrail", "NOW SHOWING WALKROUTE: tileLayer=" + (tileLayer!=null));
       // if(renderLayerAdded) // strictly necessary? wont render layer always be there?
        if(tileLayer!=null) {
            Log.d("OpenTrail", "calling showWalkroute() from addWalkroute()");
            showWalkroute();
        }
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
                walkrouteStages + " " + tileLayer);
        // only add the walkroute as a layer if not added already
        if(hasRenderedWalkroute() && walkrouteStages!=null && tileLayer!=null)
        {
            Log.d("newmapsforge", "addWalkroute(): adding walk route");
            mv.getLayerManager().getLayers().add(renderedWalkroute);

            for(int i=0; i<walkrouteStages.size(); i++)
                mv.getLayerManager().getLayers().add(walkrouteStages.get(i));

          //  walkrouteShowing=true;
        }
    }

    public void removeWalkroute(boolean removeData) {
        Log.d("newmapsforge", "removeWalkroute(): removeData=" + removeData);
       // if(walkrouteShowing)
        if(renderedWalkroute != null) {
            Log.d("newmapsforge", "removeWalkroute(): removing rendered walkroute");

            for(int i=0; i<walkrouteStages.size(); i++)
                mv.getLayerManager().getLayers().remove(walkrouteStages.get(i));

            mv.getLayerManager().getLayers().remove(renderedWalkroute);
         //   walkrouteShowing = false;

        }

        if(removeData) {
            Log.d("newmapsforge", "removeWalkroute(): removing data");
            while(walkrouteStages.size() > 0)
                walkrouteStages.remove(0);
            renderedWalkroute = null;
        }
    }


    public void addPOI(POI poi) {
        if(lastAddedPOI!=null)
            removePOI();


        Point unproj = proj.unproject(poi.getPoint());
        LatLong gp = new LatLong(unproj.y,unproj.x);


        mv.setCenter(gp);

        String name=poi.getValue("name");
        name=(name==null)? "unnamed":name;
        lastAddedPOI = MapsforgeUtil.makeTappableMarker(ctx, markerIcon, gp, name);

       // if(renderLayerAdded) again is this necessary?
        if(tileLayer!=null)
            showPOI();
    }

    // to be called in onStart() after adding the TileRendererLayer
    public void addAllOverlays() {
        showLocationMarker();
        showPOI();
        showAnnotations();
        Log.d("OpenTrail", "calling showWalkroute() from addAllOverlays()");
        showWalkroute();
    }

    // called in onStop()
    public void removeAllOverlays(boolean removeData) {
        removeWalkroute(removeData);
        removeAnnotations();
        removePOI();
        hideLocationMarker();
    }

    /*
    public void setLocationMarker(Point p) {
        super.setLocationMarker(p);
      //  if(renderLayerAdded) again necessary?

        // 250116 dont like this its inconsistent
        //if(tileLayer!=null) super.addLocationMarker();

    }
    */

    public void showPOI() {
        if(!poiShowing && lastAddedPOI!=null && tileLayer!=null)
        {
            mv.addLayer(lastAddedPOI);
            poiShowing = true;
        }
    }

    public void removePOI() {
        if(poiShowing)
        {
            mv.getLayerManager().getLayers().remove(lastAddedPOI);
            poiShowing = false;
        }
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
            " indexedAnnotations=" + (indexedAnnotations!=null) + " tileLayer=" + tileLayer);
        if(!annotationsShowing && indexedAnnotations!=null && tileLayer!=null)
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
        Marker item = MapsforgeUtil.makeTappableMarker(ctx, annotationIcon, new LatLong(unproj.y,unproj.x), ann.getDescription());

        indexedAnnotations.put(ann.getId(), item);
    }

    public void visit(Annotation ann) {

        if(indexedAnnotations.get(ann.getId()) == null)
        {
            Log.d("OpenTrail", "OverlayManager: adding annotation (visit method) - not showing them");
            addAnnotation(ann, true);
        }
    }

    public void redrawLocation() {
        if(tileLayer!=null)
            tileLayer.requestRedraw();
        if(myLocOverlayItem!=null)
            myLocOverlayItem.requestRedraw();
    }

    public void requestRedraw() {
        redrawLocation();
        if(renderedWalkroute!=null)
            renderedWalkroute.requestRedraw();
        for(int i=0; i<walkrouteStages.size(); i++)
            walkrouteStages.get(i).requestRedraw();
        for(HashMap.Entry<Integer,Marker> entry: indexedAnnotations.entrySet())
            entry.getValue().requestRedraw();

    }
}