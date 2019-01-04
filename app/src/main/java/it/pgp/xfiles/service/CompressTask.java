package it.pgp.xfiles.service;

import android.content.Intent;
import android.widget.Toast;

import java.io.IOException;
import java.io.Serializable;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.params.CompressParams;
import it.pgp.xfiles.service.visualization.MovingRibbon;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 05/06/17
 */

public class CompressTask extends RootHelperClientTask {

//    public static final SocketNames compressSocketName = SocketNames.thecompressor;
    public static final SocketNames compressSocketName = SocketNames.theroothelper;

    // direct input to compressArchive
    public CompressParams params;

    private static final FileOpsErrorCodes defaultErrorResult = FileOpsErrorCodes.TRANSFER_ERROR;

    private BasePathContent currentDir;

    CompressTask(Serializable params_) {
        super(params_);
        this.params = (CompressParams) params_;
    }

    @Override
    public boolean init(BaseBackgroundService service) {
        if (!super.init(service)) return false;

        mr = new MovingRibbon(service,windowManager);
        return true;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        try {
            currentDir = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname();
        }
        catch (Exception e) {
            // standalone CompressActivity, no path view to update after end of task
        }
    }

    @Override
    protected Object doInBackground(Object[] params) {
        if (rh == null) {
            result = FileOpsErrorCodes.ROOTHELPER_INIT_ERROR;
            return null;
        }
        try {

//            Log.e(this.getClass().getName(),"Sleeping before executing...");
//            try {
//                for (int i=5;i>=0;i--) {
//                    Log.e(this.getClass().getName(),""+i);
//                    Thread.currentThread().sleep(1000);
//                }
//            } catch (InterruptedException ignored) {}

            rh.initProgressSupport(this);

            int ret = rh.compressToArchive(
                    this.params.srcDirectory,
                    this.params.destArchive,
                    this.params.compressionLevel,
                    this.params.encryptHeaders,
                    this.params.solidMode,
                    this.params.password,
                    this.params.filenames);
            if (ret != 0) result = FileOpsErrorCodes.COMPRESS_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            result = defaultErrorResult;
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        try {
            if (result == null) {
                // refresh dir only if it's the same of when the task started
                Toast.makeText(service,"Compress completed",Toast.LENGTH_LONG).show();
                MainActivity activity = MainActivity.mainActivity;
                if (activity == null) {
                    if (params.standaloneMode) { // start MainActivity in order to show created archive
                        Intent intent = new Intent(service.getApplicationContext(),MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("STARTDIR",params.destArchive.getParent().dir);
                        service.startActivity(intent);
                    }
                    // activity closed while service active and not in standalone mode, nothing to refresh
                }
                else {
                    BasePathContent cd = activity.getCurrentDirCommander().getCurrentDirectoryPathname();
                    if (cd.equals(currentDir))
                        activity.browserPagerAdapter.showDirContent(activity.getCurrentDirCommander().refresh(),activity.browserPager.getCurrentItem(),params.destArchive.dir);
                }
            }
            else {
                // show error message only if task was not interrupted by user
                if (status != ServiceStatus.CANCELLED)
                    Toast.makeText(service,"Compress error: "+result.getValue(),Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(service,"Compress cancelled",Toast.LENGTH_LONG).show();
            }
        }
        finally {
            super.onPostExecute(o);
        }
    }
}
