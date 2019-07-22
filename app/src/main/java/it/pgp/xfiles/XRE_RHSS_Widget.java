package it.pgp.xfiles;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import it.pgp.xfiles.dialogs.RemoteRHServerManagementDialog;
import it.pgp.xfiles.dialogs.XFilesRemoteSessionsManagementActivity;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;

public class XRE_RHSS_Widget extends AppWidgetProvider {

    private static final String standard = "it.pgp.xfiles.appwidget.action.STANDARD_UPDATE";
    private static final String onDemand = "it.pgp.xfiles.appwidget.action.ON_DEMAND_UPDATE";

    public static void updateAllDirect(Context context) {
        Log.d(XRE_RHSS_Widget.class.getName(),"updateAllDirect");
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, XRE_RHSS_Widget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.xre_rhss_widget);

        // check rhss manager thread status
        if (RemoteServerManager.rhssManagerThreadRef.get() == null) {
            remoteViews.setImageViewResource(R.id.rhss_toggle_rhss_button,R.drawable.xf_xre_server_down);
            remoteViews.setTextViewText(R.id.rhssIPAddresses,"");
        }
        else {
            remoteViews.setImageViewResource(R.id.rhss_toggle_rhss_button,R.drawable.xf_xre_server_up);
            remoteViews.setTextViewText(R.id.rhssIPAddresses,RemoteRHServerManagementDialog.getInterfaceAddressesAsString());
        }

        for (int appWidgetId : ids) {
            Intent forToggleIntentUpdate = new Intent(context, XRE_RHSS_Widget.class);
            forToggleIntentUpdate.setAction(onDemand);
            forToggleIntentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});
            PendingIntent forToggleUpdate = PendingIntent.getBroadcast(
                    context, appWidgetId, forToggleIntentUpdate,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.rhss_toggle_rhss_button, forToggleUpdate);

            Intent XREActiveClientsActivityLaunchIntent = new Intent(context, XFilesRemoteSessionsManagementActivity.class);
            PendingIntent XREActiveClientsActivityLaunchPendingIntent = PendingIntent.getActivity(context, 0, XREActiveClientsActivityLaunchIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.rhss_show_xre_connections, XREActiveClientsActivityLaunchPendingIntent);

            widgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String a = intent.getAction();
        Log.d("XRE_RHSS","onReceive action: "+intent.getAction());
        if (a == null) return;
        if (MainActivity.mainActivityContext == null) {
            MainActivity.mainActivityContext = context; // hack, to let RHSSUpdateThread be able to refresh widgets without an alive activity
            MainActivity.refreshToastHandler(context);
        }
        try {
            switch (a) {
                case standard:
                    Log.d("XRE_RHSS","standard");
                    break;
                case onDemand:
                    Log.d("XRE_RHSS","onDemand: toggle server status");
                    // FIXME TODO
                    if (RemoteServerManager.rhssManagerThreadRef.get() == null) {
                        MainActivity.getRootHelperClient(context);
                        int result = RemoteServerManager.rhss_action(RemoteServerManager.RHSS_ACTION.START_ANNOUNCE);
                        Log.d("XRE_RHSS", "onDemand toggle result (->ON): "+result);
                    }
                    else {
                        if (MainActivity.mainActivity == null)
                            MainActivity.killRHWrapper();
                        else {
                            int result = RemoteServerManager.rhss_action(RemoteServerManager.RHSS_ACTION.STOP);
                            Log.d("XRE_RHSS", "onDemand toggle result (->OFF): "+result);
                        }

                    }
                    // RHSSUpdateThread will do the direct widget update once started/stopped
                    return;
                default:
                    break;
            }
            updateAllDirect(context);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

