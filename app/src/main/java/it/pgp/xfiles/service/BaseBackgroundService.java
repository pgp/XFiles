package it.pgp.xfiles.service;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.Serializable;

import it.pgp.xfiles.EffectActivity;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.service.visualization.ViewType;

/**
onStartCommand's intent must contain at least:
 FOREGROUND_SERVICE_NOTIFICATION_ID (assigned from corresponding static field of subclasses)
 BROADCAST_ACTION (assigned from subclasses as well)
 sub-class dependent params
 */

public abstract class BaseBackgroundService extends Service {

    public static final String START_ACTION = "Start";
    public static final String PAUSE_ACTION = "Pause"; // pause, on next activity open, show results found so far
    public static final String CANCEL_ACTION = "Cancel"; // cancel, on next activity open, show results found so far
	String currentAction;

	NotificationManager notificationManager;
    PowerManager mgr;
    PowerManager.WakeLock wakeLock;
	
	public BaseBackgroundTask task;
    public Serializable params;

    public static final String BROADCAST_ACTION = "base_service_broadcast_action";

    public String foreground_content_text;
    public String foreground_ticker;
    public String foreground_pause_action_label;
    public String foreground_stop_action_label;

    public abstract int getServiceIconRes();

    public abstract int getForegroundServiceNotificationId();

    public abstract ForegroundServiceType getFgServiceType();
	
	@Override
    public IBinder onBind(Intent intent) {
        return null;
    }
	
	private void abortServiceWithConfirmation() {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setTitle("Cancel "+getClass().getName()+"?");
        bld.setNegativeButton(android.R.string.cancel, null);
        bld.setPositiveButton(android.R.string.ok, (dialog, which) -> task.cancelTask());
        AlertDialog alertDialog = bld.create();
        alertDialog.getWindow().setType(ViewType.OVERLAY_WINDOW_TYPE);
        alertDialog.show();
    }
	
	protected abstract void prepareLabels();

    public NotificationCompat.Builder getForegroundNotificationBuilder() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(BROADCAST_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent pauseIntent = new Intent(this, getClass());
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent ppauseIntent = PendingIntent.getService(this, 0,
                pauseIntent, 0);

        Intent stopIntent = new Intent(this, getClass());
        stopIntent.setAction(CANCEL_ACTION);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);

        Bitmap icon = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(
                        getResources(),
                        getServiceIconRes()),
                128, 128, false);

        return new NotificationCompat.Builder(this, getPackageName())
                .setContentTitle("XFiles")
                .setTicker(foreground_ticker)
                .setContentText(foreground_content_text)
                .setSmallIcon(R.drawable.xfiles_new_app_icon)
                .setLargeIcon(icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_pause, foreground_pause_action_label,
                        ppauseIntent)
                .addAction(R.drawable.ic_media_stop, foreground_stop_action_label,
                        pstopIntent);
    }


    protected static NotificationChannel notificationChannel;
    protected void createNotificationChannelForService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(getPackageName(), "nch", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }
	
	@Override
    public void onDestroy() {
	    if (wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
	
	@Override
    public void onCreate()
    {
        super.onCreate();
        mgr  = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName()+":theWakeLock");
        notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
    }
	
	public void startAndShowNotificationBar() {
        switch (currentAction) {
            case START_ACTION:
                task = getTask();
                if (!task.init(this)) {
                    Toast.makeText(getApplicationContext(), "Cannot start service, overlay is busy", Toast.LENGTH_SHORT).show();
                    stopSelf();
                    return;
                }
                wakeLock.acquire();
                break;
            // Forbidden zone
            case CANCEL_ACTION:
            case PAUSE_ACTION:
                Toast.makeText(getApplicationContext(),
                        "Service not running, pause/cancel command should not arrive here",
                        Toast.LENGTH_SHORT).show();
                return;
            default:
                // DEBUG Forbidden zone
                Toast.makeText(getApplicationContext(),
                        "Unknown action in onStartCommand",
                        Toast.LENGTH_SHORT).show();
                return;
        }

        /************************** build notification **************************/

        Notification notification = getForegroundNotificationBuilder().build();
        createNotificationChannelForService();
        startForeground(getForegroundServiceNotificationId(),notification);
        task.execute((Void[])null);
    }

    protected abstract BaseBackgroundTask getTask();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(currentAction == null) {
            currentAction = intent.getAction();

            if (!START_ACTION.equals(currentAction)) {
                Toast.makeText(this, "Service not yet started, expected start action", Toast.LENGTH_SHORT).show();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            params = EffectActivity.serviceParams;
            if (params == null) {
                throw new RuntimeException("Null params not allowed in start action");
            }

            prepareLabels();
            startAndShowNotificationBar();
        }
        else {
            // trying to abort?
            if (intent.getAction().equals(CANCEL_ACTION)) {
                abortServiceWithConfirmation();
            }
            else if (intent.getAction().equals(PAUSE_ACTION)) {
                task.pauseTask();
                Toast.makeText(getApplicationContext(),"Service paused",Toast.LENGTH_LONG).show();
            }
            // trying to start another concurrent task?
            else {
                Toast.makeText(getApplicationContext(),
                        "Service already running!",
                        Toast.LENGTH_SHORT).show();
            }
        }
        return START_NOT_STICKY;
    }
	
}