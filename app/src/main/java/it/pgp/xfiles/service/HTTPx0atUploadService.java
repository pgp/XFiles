package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

public class HTTPx0atUploadService extends BaseBackgroundService {

    private static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0xF01;

    @Override
    public int getServiceIconRes() {
        return R.drawable.xf_share;
    }

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getForegroundServiceType() {
        return ForegroundServiceType.URL_DOWNLOAD;
    }

    @Override
    protected void prepareLabels() {
        foreground_ticker="HTTPS upload";
        foreground_content_text="Upload in progress...";
        foreground_pause_action_label="Pause upload";
        foreground_stop_action_label="Stop upload";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new HTTPx0atUploadTask(params);
    }
}
