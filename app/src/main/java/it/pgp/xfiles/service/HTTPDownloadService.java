package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Created by pgp on 05/11/17
 */

public class HTTPDownloadService extends BaseBackgroundService {

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
        foreground_ticker="XFiles HTTP download";
        foreground_content_text="Download in progress...";
        foreground_pause_action_label="Pause download";
        foreground_stop_action_label="Stop download";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new HTTPDownloadTask(params);
    }
}
