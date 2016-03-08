package freemap.opentrail03;

/**
 * Created by nick on 25/01/16.
 */

import android.content.Context;

import freemap.datasource.AnnotationCacheManager;
import freemap.andromaps.HTTPCommunicationTask;
import freemap.andromaps.DialogUtils;

// 260116 removed codes 0 and 1 as these relate to mapfile downloads

public class HTTPCallback implements HTTPCommunicationTask.Callback {

    Context ctx;
    AnnotationCacheManager annCacheMgr;

    public HTTPCallback(Context ctx) {
        this.ctx = ctx;
    }

    public void downloadFinished(int id, Object addData) {

        switch(id) {


            case 2:
                annCacheMgr.deleteCache();
                break;

            case 3:
                // walkroute uploaded

                break;
        }
    }

    public void downloadCancelled(int id) {

    }

    public void downloadError(int id) {
        DialogUtils.showDialog(ctx,"Upload/download task failed");
    }
}
