package freemap.opentrail03;

import freemap.datasource.FreemapDataset;
import freemap.data.Walkroute;
import java.util.ArrayList;

public interface DataReceiver {
    public void receivePOIs(FreemapDataset dataset);
    public void receiveWalkroute(int id, Walkroute walkroute);
    public void receiveWalkroutes(ArrayList<Walkroute> walkroutes);
}