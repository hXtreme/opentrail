package freemap.opentrail031;



import java.util.ArrayList;
import freemap.data.WalkrouteSummary;
import freemap.datasource.FreemapDataset;

// Data which must be shared between activities.

public class Shared {

    public static FreemapDataset pois;
    public static ArrayList<WalkrouteSummary> walkroutes;
    public static SavedData savedData;

    // the currently selected walk route, we need this rather than just using a walk route ID
    // so we can deal with existing walkroutes (with an ID) *and* new, recording, walkroutes
    public static void initSavedDataInstance() {
        savedData = SavedData.getInstance();
    }

}
