package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

public class CreateFileService extends BaseBackgroundService {
    private static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0xB01;

    @Override
    public int getServiceIconRes() {
        return R.drawable.xfiles_file_icon;
    }

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getFgServiceType() {
        return ForegroundServiceType.CREATE_FILE;
    }

    @Override
    protected void prepareLabels() {
        foreground_ticker="XFiles create file";
        foreground_content_text="File creation in progress...";
        foreground_pause_action_label="Pause create file";
        foreground_stop_action_label="Stop create file";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new CreateFileTask(params);
    }
}
