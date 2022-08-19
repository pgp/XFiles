package it.pgp.xfiles.service;

import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ForegroundServiceType;

/**
 * Foreground service with notification bar for find operation
 */

public class FindService extends BaseBackgroundService {
    public static final int FOREGROUND_SERVICE_NOTIFICATION_ID = 0x777ABC;

//    public static final String FIND_ACTION = "Find";
//    public static final String PAUSE_ACTION = "Pause"; // pause, on next activity open, show results found so far
//    public static final String CANCEL_ACTION = "Cancel"; // cancel, on next activity open, show results found so far
//    public static final String TARGETFOLDERPATH_TAG = "targetfolder";
//    public static final String SEARCH_NAME_PATTERN_TAG = "namepattern";
//    public static final String SEARCH_CONTENT_PATTERN_TAG = "contentpattern";
//    public static final String SEARCH_IN_SUBFOLDERS_OPTION_TAG = "searchinsubfolders";
//    public static final String SEARCH_IN_ARCHIVES_OPTION_TAG = "searchinarchives";

    @Override
    public int getServiceIconRes() {
        return R.drawable.xfiles_find;
    }

    @Override
    public int getForegroundServiceNotificationId() {
        return FOREGROUND_SERVICE_NOTIFICATION_ID;
    }

    @Override
    public final ForegroundServiceType getFgServiceType() {
        return ForegroundServiceType.FIND;
    }

    protected void prepareLabels() {
        foreground_ticker="XFiles find";
        foreground_content_text="Search in progress...";
        foreground_pause_action_label="";
        foreground_stop_action_label="Stop search";
    }

    @Override
    protected BaseBackgroundTask getTask() {
        return new FindTask(params);
    }
}
