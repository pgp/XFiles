package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Created by pgp on 05/06/17
 */

public class CompressService extends BaseBackgroundService {
    private static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0x123AB;

    @Override
    public int getServiceIconRes() {
        return R.drawable.xfiles_archive;
    }

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getFgServiceType() {
        return ForegroundServiceType.FILE_ARCHIVING;
    }

    @Override
    protected void prepareLabels() {
        foreground_ticker="XFiles compress";
        foreground_content_text="Compress in progress...";
        foreground_pause_action_label="Pause compress";
        foreground_stop_action_label="Stop compress";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new CompressTask(params);
    }
}
