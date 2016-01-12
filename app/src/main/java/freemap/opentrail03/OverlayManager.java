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
//import org.mapsforge.map.android.layer.MyLocationOverlay;
import org.mapsforge.map.model.MapViewPosition;


import java.util.HashMap;
import java.util.ArrayList;

public class OverlayManager extends LocationDisplayer implements
        freemap.datasource.FreemapDataset.AnnotationVisitor,
        MapLocationProcessor.LocationDisplayer {



    Paint outline;
    Drawable markerIcon, annotationIcon;

    HashMap<Integer,Marker> indexedAnnotations;
    Projection proj;
    Polyline renderedWalkroute;
    ArrayList<Marker> walkrouteStages;
    Marker lastAddedPOI;
    TileRendererLayer tileLayer;


    boolean includeAnnotations;
    boolean poiShowing, walkrouteShowing, annotationsShowing, renderLayerAdded;

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
        if(!renderLayerAdded)
        {
            tileLayer=layer;
            mv.getLayerManager().getLayers().add(layer);
            renderLayerAdded = true;
        }
    }

    public void removeTileRendererLayer() {
        if(renderLayerAdded)
        {
            mv.getLayerManager().getLayers().remove(tileLayer);
            renderLayerAdded=false;
        }
    }

    // Note the difference between "setting" an overlay and adding it.
    // "Setting" it merely creates the overlay layer but will only actually add it
    // as an overlay if the tile renderer layer has been added first.
    // "setting" also removes any existing object from the map

    public void setWalkroute(Walkroute walkroute) {
        setWalkroute(walkroute,true);
    }

    public void setWalkroute(Walkroute walkroute, boolean doCentreMap) {
        Log.d("newmapsforge", "setting walkroute to: " + walkroute.getId());
        // remove any existing walk route
        removeWalkroute(true);
        renderedWalkroute = new Polyline (MapsforgeUtil.makePaint(Color.BLUE, 5, Style.STROKE), AndroidGraphicFactory.INSTANCE);
        LatLong gp = new LatLong(walkroute.getStart().y,walkroute.getStart().x);

        if(doCentreMap)
            mv.getModel().mapViewPosition.setCenter(gp);
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

        if(renderLayerAdded)
            addWalkroute();
    }

    public void addWalkroute() {
        // only add the walkroute as a layer if not added already
        if(!walkrouteShowing && renderedWalkroute!=null && walkrouteStages!=null && renderedWalkroute.getLatLongs().size()>0)
        {
            Log.d("newmapsforge", "addWalkroute(): adding walk route");
            mv.getLayerManager().getLayers().add(renderedWalkroute);

            for(int i=0; i<walkrouteStages.size(); i++)
                mv.getLayerManager().getLayers().add(walkrouteStages.get(i));

            walkrouteShowing=true;
        }
    }

    public void removeWalkroute(boolean removeData) {
        Log.d("newmapsforge", "removeWalkroute(): removeData=" + removeData);
        if(walkrouteShowing)
        {
            Log.d("newmapsforge", "removeWalkroute(): removing rendered walkroute");

            for(int i=0; i<walkrouteStages.size(); i++)
                mv.getLayerManager().getLayers().remove(walkrouteStages.get(i));

            mv.getLayerManager().getLayers().remove(renderedWalkroute);
            walkrouteShowing = false;

        }

        if(removeData)
        {
            Log.d("newmapsforge", "removeWalkroute(): removing data");
            while(walkrouteStages.size() > 0)
                walkrouteStages.remove(0);
        }
    }


    public void setPOI(POI poi) {
        if(lastAddedPOI!=null)
            removePOI();


        Point unproj = proj.unproject(poi.getPoint());
        LatLong gp = new LatLong(unproj.y,unproj.x);


        mv.getModel().mapViewPosition.setCenter(gp);

        String name=poi.getValue("name");
        name=(name==null)? "unnamed":name;
        Marker item = MapsforgeUtil.makeTappableMarker(ctx, markerIcon, gp, name);


        lastAddedPOI = item;
        if(renderLayerAdded)
            addPOI();
    }

    // to be called in onStart() after adding the TileRendererLayer
    public void addAllOverlays() {
        addLocationMarker();
        addPOI();
        addAnnotations();
        addWalkroute();
    }

    // called in onStop()
    public void removeAllOverlays(boolean removeData) {
        removeWalkroute(removeData);
        removeAnnotations();
        removePOI();
        removeLocationMarker();
        removeTileRendererLayer();
    }

    public void setLocationMarker(Point p) {
        super.setLocationMarker(p);
        if(renderLayerAdded)
            super.addLocationMarker();
    }

    public void addPOI() {
        if(!poiShowing && lastAddedPOI!=null)
        {
            mv.getLayerManager().getLayers().add(lastAddedPOI);
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

    public void addAnnotations() {

        if(!annotationsShowing && indexedAnnotations!=null)
        {
            for(HashMap.Entry<Integer,Marker> entry: indexedAnnotations.entrySet())
                mv.getLayerManager().getLayers().add(entry.getValue());
            annotationsShowing = true;

        }
    }




    public void visit(Annotation ann) {

        if(indexedAnnotations.get(ann.getId()) == null)
        {
            Point unproj = proj.unproject(ann.getPoint());
            Marker item = MapsforgeUtil.makeTappableMarker(ctx, annotationIcon, new LatLong(unproj.y,unproj.x), ann.getDescription());

            indexedAnnotations.put(ann.getId(), item);
        }

        if(renderLayerAdded)
            addAnnotations();
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