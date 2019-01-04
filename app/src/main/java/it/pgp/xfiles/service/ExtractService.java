package it.pgp.xfiles.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Created by pgp on 05/06/17
 */

public class ExtractService extends BaseBackgroundService {
    static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0x123AC;
    static final String BROADCAST_ACTION = "extract_service_broadcast_action";

    private String foreground_content_text;
    private String foreground_ticker;
    private String foreground_pause_action_label;
    private String foreground_stop_action_label;

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getForegroundServiceType() {
        return ForegroundServiceType.FILE_ARCHIVING;
    }

    @Override
    protected void prepareLabels() {
        foreground_ticker="XFiles extract";
        foreground_content_text="Extract in progress...";
        foreground_pause_action_label="Pause extract";
        foreground_stop_action_label="Stop extract";
    }

    @Override
    protected NotificationCompat.Builder getForegroundNotificationBuilder() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(BROADCAST_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent pauseIntent = new Intent(this, this.getClass());
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent ppauseIntent = PendingIntent.getService(this, 0,
                pauseIntent, 0);

        Intent stopIntent = new Intent(this, this.getClass());
        stopIntent.setAction(CANCEL_ACTION);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                stopIntent, 0);

        Bitmap icon = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(
                        getResources(),
                        R.drawable.xfiles_archive),
                128, 128, false);

        return new NotificationCompat.Builder(this)
                .setContentTitle("XFiles")
                .setTicker(foreground_ticker)
                .setContentText(foreground_content_text)
                .setSmallIcon(R.drawable.xfiles_new_app_icon)
                .setLargeIcon(icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setChannelId(getPackageName())
                .addAction(android.R.drawable.ic_media_pause, foreground_pause_action_label,
                        ppauseIntent)
                .addAction(R.drawable.ic_media_stop, foreground_stop_action_label,
                        pstopIntent);
    }

    @Override
    protected boolean onStartAction() {
        task = new ExtractTask(params);
        if (!task.init(this)) return false;
        task.execute((Void[])null);
        return true;
    }
}
