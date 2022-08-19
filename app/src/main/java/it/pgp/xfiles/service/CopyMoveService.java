package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Created by pgp on 23/06/17
 */

public class CopyMoveService extends BaseBackgroundService {
    private static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0xC01;

    @Override
    public int getServiceIconRes() {
        return R.drawable.xf_copy;
    }

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getFgServiceType() {
        return ForegroundServiceType.FILE_TRANSFER;
    }

    @Override
    protected void prepareLabels() {
        foreground_ticker="XFiles file transfer";
        foreground_content_text="Transfer in progress...";
        foreground_pause_action_label="Pause transfer";
        foreground_stop_action_label="Stop transfer";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new CopyMoveTask(params);
    }
}
