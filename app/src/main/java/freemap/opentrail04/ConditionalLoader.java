/* OpenTrail is licensed under the GNU General Public License v2.
(c) Nick Whitelegg, 2012-18 */

package freemap.opentrail04;

/**
 * Created by nick on 10/01/16.
 */

import android.content.Context;

import java.io.File;

import freemap.andromaps.DialogUtils;
import freemap.andromaps.DownloadTextFilesTask;
import freemap.andromaps.HTTPCommunicationTask;

public class ConditionalLoader implements HTTPCommunicationTask.Callback {

    File downloadingFile;
    Context ctx;
    String webDir;
    ConditionalLoader.Callback callback;
    int id;

    public interface Callback {
        public void receiveLoadedData(int id, Object data);
    }

    public ConditionalLoader(Context ctx, int id, String webDir, File downloadingFile, ConditionalLoader.Callback callback) {
        this.ctx = ctx;
        this.webDir = webDir;
        this.downloadingFile = downloadingFile;
        this.callback = callback;
        this.id=id;
    }

    public void downloadOrLoad() {
         if(!downloadingFile.exists()) {
            downloadFile();
        }else{
             callback.receiveLoadedData(id, downloadingFile);
        }
    }


    public void downloadFile() {

        String url = webDir + "/" + downloadingFile.getName();
        /*
        frag.executeHTTPCommunicationTask
                (new DownloadTextFilesTask(this,
                        new String[] { url },
                        new String[] { filename },
                        "No Freemap style file found. Download?",
                        this,0), "Downloading...",
                        "Downloading style file...");
                        */
        DownloadTextFilesTask t = new DownloadTextFilesTask(ctx,
                new String[] { url },
                new String[] { downloadingFile.getAbsolutePath() },
                "No Freemap style file found. Download?",
                this,id);
        t.setAdditionalData(downloadingFile);
        t.setDialogDetails("downloading style file...", "downloading style file...");

        // something funny goes on when doing this through the SavedDataFragment, not sure why
        // download task seems to get going without the dialog appearing

        t.confirmAndExecute();

        // Literally, this line screws it up. No idea why right now
        // frag.setHTTPCommunicationTask(t);

    }
    public void downloadFinished (int id, Object data) {
    //    DialogUtils.showDialog(ctx,((File)data).getAbsolutePath());
        callback.receiveLoadedData(id, data);
    }

    public void downloadCancelled (int id) {
        DialogUtils.showDialog(ctx, "Download cancelled");
    }

    public void downloadError (int id) {
        DialogUtils.showDialog(ctx, "Error downloading file");
    }

}
