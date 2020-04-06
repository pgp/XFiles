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
 * Foreground service with notification bar for find operation
 */

public class FindService extends BaseBackgroundService {
    public static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0x777ABC;
    static final String BROADCAST_ACTION = "find_service_broadcast_action";

//    public static final String FIND_ACTION = "Find";
//    public static final String PAUSE_ACTION = "Pause"; // pause, on next activity open, show results found so far
//    public static final String CANCEL_ACTION = "Cancel"; // cancel, on next activity open, show results found so far
//    public static final String TARGETFOLDERPATH_TAG = "targetfolder";
//    public static final String SEARCH_NAME_PATTERN_TAG = "namepattern";
//    public static final String SEARCH_CONTENT_PATTERN_TAG = "contentpattern";
//    public static final String SEARCH_IN_SUBFOLDERS_OPTION_TAG = "searchinsubfolders";
//    public static final String SEARCH_IN_ARCHIVES_OPTION_TAG = "searchinarchives";

    private String foreground_content_text;
    private String foreground_ticker;
    private String foreground_stop_action_label;

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getForegroundServiceType() {
        return ForegroundServiceType.FIND;
    }

    protected void prepareLabels() {
        foreground_ticker="XFiles find";
        foreground_content_text="Search in progress...";
        foreground_stop_action_label="Stop search";
    }

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
                        R.drawable.xfiles_find),
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
                .addAction(android.R.drawable.ic_media_pause, foreground_stop_action_label,
                        ppauseIntent)
                .addAction(R.drawable.ic_media_stop, foreground_stop_action_label,
                        pstopIntent);
    }

    @Override
    protected boolean onStartAction() {
        task = new FindTask(params);
        if (!task.init(this)) return false;
        task.execute((Void[])null);
        return true;
    }
}
