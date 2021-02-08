package it.pgp.xfiles.service;

import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.enums.ServiceStatus;
import it.pgp.xfiles.service.visualization.ProgressIndicator;
import it.pgp.xfiles.utils.Pair;

public abstract class BaseBackgroundTask extends AsyncTask<Object,Pair<Long,Long>,Object> {
	
	protected NotificationCompat.Builder builder;
    // for notifying progress on foreground service progress bar
    protected NotificationManager nm;

    public ProgressIndicator mr;

    protected BaseBackgroundService service;
	public ServiceStatus status;

    public FileOpsErrorCodes result;

    public Serializable params; // to be down-casted in subclasses

    public static final Deque<Runnable> nextAutoTasks = new ArrayDeque<>();

    public BaseBackgroundTask(Serializable params) {
        this.params = params;
    }

    // FIXME circular dependency (task <-> service), cannot set in constructor, since service constructor needs task instance as input param
    /* invocation order:
    t = new Task();
    s = new Service(t);
    t.init(s);
     */
	public boolean init(BaseBackgroundService service) {
        this.service = service;
        builder = service.getForegroundNotificationBuilder();
        nm = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
        // initialized in subclasses (MovingRibbon for Compress and extract tasks, MovingRibbonTwoBars for copy/move tasks
//        mr = new MovingRibbon(service,windowManager);

        // before initializing ribbon overlay, ensure to lock that resource (check if already locked)

        // progress interface lock to be done in subclasses
//        return ProgressIndicator.busy.compareAndSet(false,true); // compareAndSet true -> ok

        ForegroundServiceType f = service.getForegroundServiceType();
        return f != null && ProgressIndicator.acquire(f);
    }
	
	public void cancelTask() {
		status = ServiceStatus.CANCELLED;
		// to be explicitly overriden, task has to exit from doInBackground in order to stop foreground notification in onPostExecute
	}
	
	public void pauseTask() {
		status = ServiceStatus.PAUSED;
	}
	
	@Override
    protected void onPreExecute() {
        super.onPreExecute();
        builder.setProgress(100,0,false);
        nm.notify(service.getForegroundServiceNotificationId(), builder.build());
        status = ServiceStatus.ACTIVE;
    }

    /**
     * throttle UI progress update in order to make the buttons "easily" clickable
     * Web source:
     * https://stackoverflow.com/questions/6390016/android-notification-progressbar-freezing/28336857
     */

    protected long lastProgressUpdate = 0; // for overlay ribbon (fine)
    protected long lastProgressUpdateFgNotif = 0; // for foreground notification (coarse)

    public static final int OVERLAY_UPDATE_INTERVAL = 25;
    public static final int FGNOTIF_UPDATE_INTERVAL = 500;

    @Override
    protected void onProgressUpdate(Pair<Long,Long>... values) {
        // Update progress
        long current = System.currentTimeMillis();
        if(current - lastProgressUpdate > OVERLAY_UPDATE_INTERVAL) { // milliseconds
            mr.setProgress(values);
            lastProgressUpdate = current;
        }
        if(current - lastProgressUpdateFgNotif > FGNOTIF_UPDATE_INTERVAL) {
            builder.setProgress(100, (int) Math.round(values[0].i * 100.0 / values[0].j), false);
            nm.notify(service.getForegroundServiceNotificationId(), builder.build());
            lastProgressUpdateFgNotif = current;
        }

        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        mr.destroy();
        // in case of user cancelling task, cancel status is set before this
        if (status != ServiceStatus.CANCELLED)
            status = ServiceStatus.COMPLETED;
        service.stopForeground(true);
        service.stopSelf();

        // unlock ribbon overlay resource
        ProgressIndicator.release();

        if(!nextAutoTasks.isEmpty()) {
            if(result == null || result == FileOpsErrorCodes.OK) {
                Log.d(getClass().getName(),"Starting next auto task...");
                new Thread(nextAutoTasks.pop()).start();
            }
            else {
                Log.d(getClass().getName(),"Current task failed, clearing next auto tasks...");
                nextAutoTasks.clear();
            }
        }

        EffectActivity.serviceParams = null;
    }

    @Override
    protected abstract Object doInBackground(Object[] params);

    public void publishProgressWrapper(Pair<Long,Long>... values) {
        publishProgress(values);
    }
}