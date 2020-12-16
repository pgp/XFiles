package it.pgp.xfiles.service;

import it.pgp.xfiles.R;

public class TestService extends ExtractService {
    static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0x123AD;

    @Override
    public int getServiceIconRes() {
        return R.drawable.xfiles_test;
    }

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    protected void prepareLabels() {
        foreground_ticker="XFiles test";
        foreground_content_text="Test in progress...";
        foreground_pause_action_label="Pause test";
        foreground_stop_action_label="Stop test";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new ExtractTask(params);
    }
}
