package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Created by pgp on 05/06/17
 */

public class ExtractService extends BaseBackgroundService {
    static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0x123AC;

    @Override
    public int getServiceIconRes() {
        return R.drawable.xfiles_extract;
    }

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
    protected BaseBackgroundTask getTask() {
        return new ExtractTask(params);
    }
}
