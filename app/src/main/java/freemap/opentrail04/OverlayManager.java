/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

import android.graphics.drawable.Drawable;
import android.content.Context;
import android.util.Log;

import org.oscim.android.MapView;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Map;

import freemap.andromaps.DialogUtils;
import freemap.data.Annotation;
import freemap.data.Point;
import freemap.data.TrackPoint;
import freemap.data.Walkroute;
import freemap.data.Projection;

import freemap.andromaps.MapLocationProcessor;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;

// 240116 is walkrouteShowing necessary? Removed it
// is it necessary to test existence of tile renderer layer before adding things?
// can we not just assume it will be there?

// 140517 remove reference to tile renderer layer, as tile renderer layer/overlay management
// is much simpler with a single tile renderer layer rather than the old 0.1/0.2 style of
// having multiple mapfiles each with their own tile renderer layer.

// 270517 remove addAllOverlays() as we now add items straight away after creating them

// 311217 convert to VTM

public class OverlayManager  implements
        freemap.datasource.FreemapDataset.AnnotationVisitor,
        MapLocationProcessor.LocationDisplayer {


    protected Context ctx;
    protected Drawable locationIcon;
    protected MarkerItem myLocOverlayItem;
    protected MapView mv;
    protected Map map;
    protected boolean markerShowing; // to prevent exceptions when marker added twice

    Drawable stageIcon, markerIcon;
    Drawable[] annotationIcons;

    HashMap<Integer,MarkerItem> indexedAnnotations;
    Projection proj;

    ArrayList<MarkerItem> walkrouteStages;

    ItemizedLayer<MarkerItem> myLocLayer, annotationLayer, recordingWalkrouteStageLayer, downloadedWalkrouteStageLayer, foundPOILayer;
    PathLayer recordingWalkrouteLayer, downloadedWalkrouteLayer;


    MarkerSymbol[] annotationSymbols;


    boolean annotationsShowing;

    static final int DEFAULT_SYMBOL_TYPE = 2;

    class GestureListener implements ItemizedLayer.OnItemGestureListener<MarkerItem> {
        public boolean onItemLongPress (int index, MarkerItem item) {
            onItemSingleTapUp(index, item);
            return true;
        }

        public boolean onItemSingleTapUp (int index, MarkerItem item) {
            DialogUtils.showDialog(ctx, item.getSnippet());
            return true;
        }
    }

    public OverlayManager(Context ctx, MapView mapView, Drawable locationIcon,  Drawable stageIcon,
                          Drawable markerIcon, Drawable[] annotationIcons, Projection proj) {


        this.mv = mapView;
        this.map = mapView.map();
        this.ctx = ctx;
        this.locationIcon = locationIcon;

        this.stageIcon = stageIcon;
        this.markerIcon = markerIcon;
        this.annotationIcons = annotationIcons;
        this.proj = proj;

        walkrouteStages = new ArrayList<>();
        indexedAnnotations = new HashMap<>();

        MarkerSymbol locationSymbol = makeMarkerSymbol(locationIcon),
                walkrouteStageSymbol = makeMarkerSymbol(stageIcon),
                markerSymbol = makeMarkerSymbol(markerIcon);
        annotationSymbols = new MarkerSymbol[annotationIcons.length];
        for (int i = 0; i < annotationSymbols.length; i++) {
            annotationSymbols[i] = makeMarkerSymbol(annotationIcons[i]);
        }

        GestureListener listener = new GestureListener();

        myLocLayer = new ItemizedLayer<MarkerItem>(map, new ArrayList<MarkerItem>(), locationSymbol, null);
        annotationLayer = new ItemizedLayer<MarkerItem>(map, new ArrayList<MarkerItem>(),
                annotationSymbols[DEFAULT_SYMBOL_TYPE], listener);

        foundPOILayer = new ItemizedLayer<MarkerItem>(map, new ArrayList<MarkerItem>(), markerSymbol, listener);

        downloadedWalkrouteStageLayer = new ItemizedLayer<MarkerItem>(map, new ArrayList<MarkerItem>(), walkrouteStageSymbol, listener);
        recordingWalkrouteStageLayer = new ItemizedLayer<MarkerItem>(map, new ArrayList<MarkerItem>(), walkrouteStageSymbol, listener);

        recordingWalkrouteLayer = new PathLayer(map, Color.BLUE,5);
        downloadedWalkrouteLayer = new PathLayer(map, Color.BLUE,5);

        map.layers().add(downloadedWalkrouteLayer);
        map.layers().add(recordingWalkrouteLayer);
        map.layers().add(annotationLayer);
        map.layers().add(foundPOILayer);
        map.layers().add(downloadedWalkrouteStageLayer);
        map.layers().add(recordingWalkrouteStageLayer);
        map.layers().add(myLocLayer);
    }


    public void addLocationMarker(Point p) {
        myLocOverlayItem = new MarkerItem("My location", "My location", new GeoPoint(p.y, p.x));
        showLocationMarker();
    }

    public void showLocationMarker() {
        if(mv!=null && myLocOverlayItem!=null && !markerShowing) {
            myLocLayer.addItem(myLocOverlayItem);
            markerShowing=true;
        }
    }

    public void hideLocationMarker() {
        if(mv!=null && myLocOverlayItem!=null && markerShowing) {
            myLocLayer.removeItem(myLocOverlayItem);
            markerShowing=false;
        }
    }

    public void moveLocationMarker(Point p) {
        // doesn't look like it's possible to alter the position of a MarkerItem
        removeLocationMarker();
        addLocationMarker(p);
    }

    public void removeLocationMarker() {
        hideLocationMarker();
        myLocOverlayItem = null;
    }

    public boolean isLocationMarker() {
        return myLocOverlayItem != null;
    }

    public void addPOIMarker(GeoPoint latLon, String name) {
        foundPOILayer.removeAllItems();
        foundPOILayer.addItem(new MarkerItem(name, name, latLon));
    }

    public void addRecordingWalkroute(Walkroute walkroute) {
        addWalkroute(walkroute,true, false);
    }
    public void addDownloadedWalkroute(Walkroute walkroute) {
        addWalkroute(walkroute,true, true);
    }

    public void addRecordingWalkroute(Walkroute walkroute, boolean doCentreMap) {
        addWalkroute(walkroute,doCentreMap, false);
    }
    public void addDownloadedWalkroute(Walkroute walkroute, boolean doCentreMap) {
        addWalkroute(walkroute,doCentreMap, true);
    }

    private void addWalkroute(Walkroute walkroute, boolean doCentreMap, boolean isDownloaded) {
        Log.d("OpenTrail", "addWalkroute(): walkroute details: " + walkroute.getId() +
                " length=" + walkroute.getPoints().size());

        PathLayer walkrouteLayer = isDownloaded ? downloadedWalkrouteLayer : recordingWalkrouteLayer;
        // remove any existing walk route
        removeWalkroute(true, isDownloaded);


        if(doCentreMap) {
            Point p = walkroute.getStart();
            if(p!=null) {
                map.setMapPosition(p.y, p.x, map.getMapPosition().getScale());
            }
        }

        ArrayList<TrackPoint> points = walkroute.getPoints();
        for(int i=0; i<points.size(); i++) {
            walkrouteLayer.addPoint(new GeoPoint(points.get(i).y, points.get(i).x));
        }


        ArrayList<Walkroute.Stage> stages = walkroute.getStages();

        for(int i=0; i<stages.size(); i++) {
            addStageToWalkroute(stages.get(i), isDownloaded);
        }

    }

    // 120316 do this to avoid having to redraw the *whole* walkroute every time we add a point...
    public void addPointToRecordingWalkroute(Point p) {
        recordingWalkrouteLayer.addPoint(new GeoPoint(p.y, p.x));
    }

    public void addStageToWalkroute(Walkroute.Stage s, boolean isDownloaded) {
        try {
            ItemizedLayer<MarkerItem> walkrouteStageLayer = isDownloaded ? downloadedWalkrouteStageLayer: recordingWalkrouteStageLayer;
            GeoPoint curStagePoint = new GeoPoint(s.start.y, s.start.x);
            MarkerItem item = new MarkerItem("Stage " + (s.id + 1), URLDecoder.decode(s.description, "UTF-8"), curStagePoint);
            walkrouteStageLayer.addItem(item);
            walkrouteStages.add(item);
        }catch(UnsupportedEncodingException e) {
            // do nothing, UTF-8 presumably always supported on Android
        }
    }

    public boolean hasRenderedRecordingWalkroute() {
        return recordingWalkrouteLayer.getPoints().size() > 0;
    }


    public void removeRecordingWalkroute(boolean removeData) {
        removeWalkroute (removeData, false);
    }

    public void removeDownloadedWalkroute(boolean removeData) {
        removeWalkroute (removeData, true);
    }

    private void removeWalkroute(boolean removeData, boolean isDownloaded) {
        Log.d("OpenTrail", "removeWalkroute(): removeData=" + removeData);


        PathLayer walkrouteLayer = isDownloaded ? downloadedWalkrouteLayer : recordingWalkrouteLayer;

        walkrouteLayer.clearPath();
        ItemizedLayer<MarkerItem> walkrouteStageLayer = isDownloaded ? downloadedWalkrouteStageLayer: recordingWalkrouteStageLayer;

        walkrouteStageLayer.removeAllItems();
        if(removeData) {
            walkrouteStages.clear();
            /* 311217 not sure why this was necessary?
            while(walkrouteStages.size() > 0) {
                walkrouteStages.remove(0);
            }
           */
        }
    }

    public void setAnnotationsShowing(boolean as) {
        if(as && !annotationsShowing) {
            showAnnotations();
        } else if (!as && annotationsShowing) {
            removeAnnotations();
        }
    }

    private void removeAnnotations() {
        annotationLayer.removeAllItems();
    }


    private void showAnnotations() {

        Log.d("OpenTrail", "showAnnotations(): annotationsShowing=" + annotationsShowing +
            " indexedAnnotations=" + (indexedAnnotations!=null));
        if(indexedAnnotations!=null) {
            Set<HashMap.Entry<Integer,MarkerItem>> markersEntrySet = indexedAnnotations.entrySet();
            for(HashMap.Entry<Integer,MarkerItem> entry: markersEntrySet) {

                Log.d("OpenTrail", "ADDING ANNOTATIONS (showAnnotations() loop - actually showing them)");
                annotationLayer.addItem(entry.getValue());
            }
            annotationsShowing = true;
        }

    }

    protected static MarkerSymbol makeMarkerSymbol (Drawable drawable) {
        Bitmap b = AndroidGraphics.drawableToBitmap(drawable);
        MarkerSymbol marker = new MarkerSymbol (b, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
        return marker;
    }



    // 290116 projection stuff - messy
    // 270517 now always actually adds the overlay
    public void addAnnotation(Annotation ann, boolean unproject) {
        try {
            Point unproj = unproject ? proj.unproject(ann.getPoint()) : ann.getPoint();
            int type = ann.getType().equals("") ? DEFAULT_SYMBOL_TYPE : Integer.parseInt(ann.getType());
            Log.d("opentrail", "Adding annotation: description=" + ann.getDescription() +
                    " lat/lon=" + unproj.y + " " + unproj.x);
            MarkerItem item = new MarkerItem("Annotation #" + ann.getId(), URLDecoder.decode(ann.getDescription(), "UTF-8"), new GeoPoint(unproj.y, unproj.x));
            item.setMarker(annotationSymbols[type - 1]);
            indexedAnnotations.put(ann.getId(), item);
            annotationLayer.addItem(item);
        }catch(UnsupportedEncodingException e) {
            // do nothing; UTF-8 will presumably always be supported on Android
        }
    }

    public void visit(Annotation ann) {
        if(indexedAnnotations.get(ann.getId()) == null) {
            addAnnotation(ann, true);
        }
    }
}