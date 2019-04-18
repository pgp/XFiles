package it.pgp.xfiles;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.adapters.BrowserPagerAdapter;
import it.pgp.xfiles.dialogs.AboutDialog;
import it.pgp.xfiles.dialogs.AdvancedSortingDialog;
import it.pgp.xfiles.dialogs.ChecksumActivity;
import it.pgp.xfiles.dialogs.CloseActiveServersDialog;
import it.pgp.xfiles.dialogs.CreateFileOrDirectoryDialog;
import it.pgp.xfiles.dialogs.CreateLinkDialog;
import it.pgp.xfiles.dialogs.FilterSelectionDialog;
import it.pgp.xfiles.dialogs.GenericChangeDirectoryDialog;
import it.pgp.xfiles.dialogs.OpenAsDialog;
import it.pgp.xfiles.dialogs.PropertiesDialog;
import it.pgp.xfiles.dialogs.RemoteRHServerManagementDialog;
import it.pgp.xfiles.dialogs.RenameDialog;
import it.pgp.xfiles.dialogs.XFilesRemoteSessionsManagementActivity;
import it.pgp.xfiles.dialogs.compress.AskPasswordDialogOnListing;
import it.pgp.xfiles.dialogs.compress.CompressActivity;
import it.pgp.xfiles.dialogs.compress.ExtractActivity;
import it.pgp.xfiles.enums.ArchiveType;
import it.pgp.xfiles.enums.ComparatorField;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.enums.Permissions;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.fileservers.FileServer;
import it.pgp.xfiles.roothelperclient.FirstRunAssetsExtract;
import it.pgp.xfiles.roothelperclient.RemoteClientManager;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;
import it.pgp.xfiles.roothelperclient.RootHandler;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.HTTPDownloadService;
import it.pgp.xfiles.service.CopyMoveService;
import it.pgp.xfiles.service.NonInteractiveSftpService;
import it.pgp.xfiles.service.NonInteractiveSmbService;
import it.pgp.xfiles.service.NonInteractiveXFilesRemoteTransferService;
import it.pgp.xfiles.service.params.CopyMoveParams;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.visualization.ProgressIndicator;
import it.pgp.xfiles.sftpclient.SFTPProviderUsingPathContent;
import it.pgp.xfiles.sftpclient.SftpRetryLsListener;
import it.pgp.xfiles.sftpclient.VaultActivity;
import it.pgp.xfiles.smbclient.SmbProviderUsingPathContent;
import it.pgp.xfiles.smbclient.SmbVaultActivity;
import it.pgp.xfiles.utils.ContentProviderUtils;
import it.pgp.xfiles.utils.DirCommanderCUsingBrowserItemsAndPathContent;
import it.pgp.xfiles.utils.FileOperationHelperUsingPathContent;
import it.pgp.xfiles.utils.XFilesUtilsUsingPathContent;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;

public class MainActivity extends EffectActivity {

    /************** JNI part **************/
    static {
        System.loadLibrary("r"); // libr.so
        // avoid messing up with content URIs
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);
    }
    /************** end JNI part **************/

    public ActivityManager activityManager;

    public static Context mainActivityContext;
    public static MainActivity mainActivity;

    public BrowserViewPager browserPager;

    private CopyMoveListPathContent copyMoveList = null; // only one for the entire ViewPager (you may want to copy files from one browser view to the other one)

    private LayoutInflater layoutInflater;
    private LinearLayout operationButtonsLayout; // target container for conditional inflating

    public BrowserPagerAdapter browserPagerAdapter;

    // File Operations Helpers
    public static SmbProviderUsingPathContent smbProvider; // TODO add entry points in MainActivity
    public static SFTPProviderUsingPathContent sftpProvider;
    public static SftpRetryLsListener sftpRetryLsListener;

    public static XFilesUtilsUsingPathContent xFilesUtils;
    private static RootHelperClientUsingPathContent rootHelperClient;

    public static RootHelperClientUsingPathContent getRootHelperClient(Context... context) {
        if (rootHelperClient == null) {
            rootHelperClient = RootHandler.startAndGetRH(context);
        }
        return rootHelperClient;
    }

    public static void killRHWrapper() {
        try {
            rootHelperClient.killServer();
            rootHelperClient = null;
        }
        catch (NullPointerException n) {
            Log.e("RH","Unable to kill roothelper server, reference already null");
        }
        catch (Exception e) {
            Log.e("RH","Unable to kill roothelper server",e);
        }
    }

    public static final RemoteClientManager rootHelperRemoteClientManager = new RemoteClientManager();
    public static boolean usingRootHelperForLocal = false;

    public static FileOperationHelperUsingPathContent currentHelper;

    public BrowserAdapter getCurrentBrowserAdapter() {
        return browserPagerAdapter.browserAdapters[browserPager.getCurrentItem()];
    }

    public DirCommanderCUsingBrowserItemsAndPathContent getCurrentDirCommander() {
        return browserPagerAdapter.dirCommanders[browserPager.getCurrentItem()];
    }

    public AbsListView getCurrentMainBrowserView() {
        return browserPagerAdapter.mainBrowserViews[browserPager.getCurrentItem()];
    }

    public static void showToastOnUI(String msg) {
        if (mainActivity != null)
            mainActivity.runOnUiThread(()-> Toast.makeText(mainActivity, msg, Toast.LENGTH_SHORT).show());
        else Log.e(MainActivity.class.getName(), "showToastOnUI failed, no active activity, msg is: "+msg);
    }

    public static final int toastHandlerTag = 123571141;
    public static Handler toastHandler;
    public static void refreshToastHandler(Context context) {
        if (toastHandler == null) toastHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == toastHandlerTag) {
                    Log.e("handleMessage", "Received toastmessage");
                    Toast.makeText(context,""+msg.obj,Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public static void showToastOnUIWithHandler(String s) {
        Message m = new Message();
        m.obj = s;
        m.what = toastHandlerTag;
        toastHandler.sendMessage(m);
    }

    public ImageButton operationButtonsLayoutSwitcher;
    public ImageButton fileOperationHelperSwitcher;

    ImageButton quickFindButton,
            itemSelectionButton,
            sortButton,
            credsFavsButton,
            chooseBrowserViewButton;

    public AdapterView.OnItemClickListener listViewLevelOICL = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            BrowserAdapter ba = getCurrentBrowserAdapter();
            BrowserItem browserItem = ba.getItem(position);

            if (browserPagerAdapter.multiSelectModes[browserPager.getCurrentItem()]) {
                // if in multi select mode, simply select item
                ba.toggleSelectOne(browserItem);
                return;
            }
//            String currentFile = getCurrentDirCommander().getCurrentDirectoryPathname() + "/" + browserItem.filename;
            BasePathContent currentFile = getCurrentDirCommander().getCurrentDirectoryPathname().concat(browserItem.filename);

            // file or dir may have been deleted meanwhile, anyway don't do this check here, since it couples fileopshelper responsibilities with browseradapter ones
//            if (!currentHelper.exists(currentFile)) {
//                Toast.makeText(parent.getContext(),"File should exist: "+currentFile,Toast.LENGTH_LONG).show();
//                return;
//            }

            // open local directory
            if (browserItem.isDirectory) {
                goDir(currentFile);
                return;
            }

            // from now on, exclude any open operation on non-local path contents
            if (currentFile.providerType != ProviderType.LOCAL) return;

            // open local archive
            if (browserItem.hasExt() && ArchiveType.formats.contains(browserItem.getFileExt())) {
                ArchivePathContent a = new ArchivePathContent(currentFile.dir,"/");
                goDir(a);
                return;
            }

            // TODO take BasePathContent as input
            openWithDefaultApp(new File(currentFile.dir));
        }
    };

    public Point currentScreenDimensions; // TODO should be updated on screen rotation
    public void updateScreenDimensions() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        currentScreenDimensions = size;
    }

    public void getStats(BrowserItem b) {
        BasePathContent pathname = getCurrentDirCommander().getCurrentDirectoryPathname().concat(b.filename);
        PropertiesDialog propertiesDialog = new PropertiesDialog(
                MainActivity.this,
                b.isDirectory?FileMode.DIRECTORY :FileMode.FILE,
                Collections.singletonList(pathname));
        propertiesDialog.show();
    }

    // for current browserAdapter selection
    public void getStats() {
        PropertiesDialog propertiesDialog = new PropertiesDialog(
                MainActivity.this,
                null,
                getCurrentBrowserAdapter().getSelectedItemsAsPathContents());
        propertiesDialog.show();
    }

    private void showDeleteDialog(final List<BasePathContent> selection) {
        AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
        String name = selection.size()==1?selection.get(0).getName():"selection";
        bld.setTitle("Confirm delete "+name);
        bld.setIcon(R.drawable.xf_recycle_bin);
        bld.setNegativeButton("No", (dialog, which) -> {/*no action*/});
        bld.setPositiveButton("Yes", (dialog, which) -> {
            try {
                int posToRestore = browserPagerAdapter.mainBrowserViews[browserPager.getCurrentItem()].getFirstVisiblePosition();

                currentHelper.deleteFilesOrDirectories(selection);
                Toast.makeText(MainActivity.this,selection.size()+" files deleted",Toast.LENGTH_SHORT).show();
                browserPagerAdapter.showDirContent(getCurrentDirCommander().refresh(),browserPager.getCurrentItem(),posToRestore);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this,"Unable to delete some files",Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alertDialog = bld.create();
        alertDialog.show();
    }

    private byte[] reverseByteArray(byte[] b) {
        byte[] c = new byte[b.length];
        for (int i=0;i<b.length;i++)
            c[b.length-1-i] = b[i];
        return c;
    }

    public String getMyIP() {
        WifiManager manager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiinfo = manager.getConnectionInfo();
        byte[] myIPAddress = BigInteger.valueOf(wifiinfo.getIpAddress()).toByteArray();
        // you must reverse the byte array before conversion. Use Apache's commons library
        myIPAddress = reverseByteArray(myIPAddress);
        InetAddress myInetIP;
        try {
            myInetIP = InetAddress.getByAddress(myIPAddress);
            return myInetIP.getHostAddress();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return "UNKNOWN";
        }
    }

//    public static final AtomicReference<RemoteServerManager.RHSSUpdateThread> rhssManagerThreadRef = new AtomicReference<>(null);

    public void showXREConnections(View unused) {
        Intent i = new Intent(MainActivity.this, XFilesRemoteSessionsManagementActivity.class);
        startActivity(i);
    }

    public void showStartRHRemoteServerDialog(View unused) {
        new RemoteRHServerManagementDialog(MainActivity.this).show();
    }

    private boolean wasShortClick = false;
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();

        if (v instanceof ImageButton) {
            switch (v.getId()) {
                case R.id.sortButton:
                    if (wasShortClick)
                        inflater.inflate(R.menu.menu_sort, menu);
                    else
                        new AdvancedSortingDialog(MainActivity.this,getCurrentBrowserAdapter()).show();
                    wasShortClick = false;
                    break;
                case R.id.itemSelectionButton:
                    inflater.inflate(R.menu.menu_checkbox, menu);
                    break;
                case R.id.openCredsFavsMenu:
                    inflater.inflate(R.menu.menu_credentials_favorites, menu);
                    break;
                default: // chooseBrowserViewButton
                    inflater.inflate(R.menu.menu_browserview, menu);
            }
        }
        else {
            if (getCurrentBrowserAdapter().getSelectedCount() == 0) { // long-click on single file
                switch(getCurrentDirCommander().getCurrentDirectoryPathname().providerType) {
                    case LOCAL:
                        inflater.inflate(R.menu.menu_single, menu);
                        break;
                    case LOCAL_WITHIN_ARCHIVE:
                        // allowed operations: extract, properties (click only if folder, extract on click)
                        inflater.inflate(R.menu.menu_single_within_archive, menu);
                        break;
                    case SFTP:
                    case XFILES_REMOTE:
                    case SMB:
                        // allowed operations: copy, move, delete, rename, properties
                        inflater.inflate(R.menu.menu_single_remote, menu);
                        break;
                }
            }
            else {
                switch(getCurrentDirCommander().getCurrentDirectoryPathname().providerType) {
                    case LOCAL:
                        inflater.inflate(R.menu.menu_multi, menu);
                        break;
                    case LOCAL_WITHIN_ARCHIVE:
                        inflater.inflate(R.menu.menu_multi_within_archive, menu);
                        break;
                    case SFTP:
                    case XFILES_REMOTE:
                    case SMB:
                        inflater.inflate(R.menu.menu_multi_remote, menu);
                        break;
                }
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        BrowserItem b;
        File currentFile;
        List<BasePathContent> selection;
        BasePathContent path = getCurrentDirCommander().getCurrentDirectoryPathname();
        switch (item.getItemId()) {

            // multi-selection menu
            case R.id.itemsCopy:
                prepareForCopyOrMove(CopyMoveMode.COPY);
                return true;
            case R.id.itemsMove:
                prepareForCopyOrMove(CopyMoveMode.MOVE);
                return true;
            case R.id.itemsChecksum:
                checksumSelection();
                return true;
            case R.id.itemsCompress:
                compressSelection();
                return true;
            case R.id.itemsExtract:
                if (path.providerType != ProviderType.LOCAL_WITHIN_ARCHIVE) {
                    Toast.makeText(this,"Cannot extract items if they are not in an archive",Toast.LENGTH_SHORT).show();
                    return true;
                }
                extractItems();
                return true;
            case R.id.itemsDelete:
                deleteSelection();
                return true;
            case R.id.itemsShare:
                shareItems();
                return true;
            case R.id.itemsProperties:
                getStats();
                return true;

            // single-selection menu
            case R.id.itemOpenAs:
                b = getCurrentBrowserAdapter().getItem(info.position);
                if (path.providerType != ProviderType.LOCAL) {
                    Toast.makeText(this,"Open not implemented for non-local or within-archive paths",Toast.LENGTH_LONG).show();
                    return true;
                }

                currentFile = new File(path.dir, b.filename);
                showOpenAsList(currentFile);
                return true;
            case R.id.itemCopy:
                b = getCurrentBrowserAdapter().getItem(info.position);
                copyMoveList = new CopyMoveListPathContent(b, CopyMoveMode.COPY, path);
                Toast.makeText(this, "Copy item " + b.filename, Toast.LENGTH_LONG).show();
                return true;
            case R.id.itemMove:
                b = getCurrentBrowserAdapter().getItem(info.position);
                copyMoveList = new CopyMoveListPathContent(b, CopyMoveMode.MOVE, path);
                Toast.makeText(this, "Move item " + b.filename, Toast.LENGTH_LONG).show();
                return true;
            case R.id.itemCreateLink:
                b = getCurrentBrowserAdapter().getItem(info.position);
                new CreateLinkDialog(this, path.concat(b.filename), b.isDirectory?FileMode.DIRECTORY:FileMode.FILE).show();
                return true;
            case R.id.itemCompress:
                b = getCurrentBrowserAdapter().getItem(info.position);
//                CompressDialog compressDialog = new CompressDialog(this,b.filename);
//                compressDialog.show();
                // with CompressActivity
                Intent i = new Intent(MainActivity.this,CompressActivity.class);
                i.putExtra("filename", b);
                startActivity(i);
                return true;
            case R.id.itemExtract:
                b = getCurrentBrowserAdapter().getItem(info.position);
                extractItem(b);
                return true;
            case R.id.itemDelete:
                b = getCurrentBrowserAdapter().getItem(info.position);
                selection = Collections.singletonList(path.concat(b.filename));
                showDeleteDialog(selection);
                return true;
            case R.id.itemRename:
                b = getCurrentBrowserAdapter().getItem(info.position);
                new RenameDialog(
                        MainActivity.this,
                        path.concat(b.filename)
                ).show();
                return true;
            case R.id.itemChecksum:
                if (path.providerType != ProviderType.LOCAL &&
                        path.providerType != ProviderType.XFILES_REMOTE) {
                    Toast.makeText(this,"Checksum implemented only for local and XFiles remote files",Toast.LENGTH_LONG).show();
                    return true;
                }
                b = getCurrentBrowserAdapter().getItem(info.position);
                path = path.concat(b.filename);
                if (currentHelper.isDir(path)) { // TODO to be tested
                    Toast.makeText(this, "File is a directory", Toast.LENGTH_SHORT).show();
                    return true;
                }
                Intent intent = new Intent(MainActivity.this, ChecksumActivity.class);
                // LEGACY, only local paths
//                intent.putExtra("file", path.dir);

                // NEW, also XRE paths
                intent.putExtra("pathcontent", path);

                startActivity(intent);
                return true;
            case R.id.itemShare:
                b = getCurrentBrowserAdapter().getItem(info.position);
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                Uri sharingUri = Uri.fromFile(new File(path.dir, b.filename));
//                Uri sharingUri = FileProvider.getUriForFile(MainActivity.this,
//                        BuildConfig.APPLICATION_ID + ".provider",
//                        new File(path.dir, b.filename));
                sharingIntent.setType("*/*");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, sharingUri);
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(sharingIntent, "Share file using"));
                return true;
            case R.id.itemProperties:
                b = getCurrentBrowserAdapter().getItem(info.position);
                getStats(b);
                return true;

            // TODO collapse cases using indexed enum
            // sorting
            // TODO need to add directory priority switch some way (used priority on as default)
            case R.id.sortByFilename:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.FILENAME, false,browserPager.getCurrentItem());
                return true;
            case R.id.sortByFilenameDesc:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.FILENAME, true,browserPager.getCurrentItem());
                return true;
            case R.id.sortByDate:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.DATE, false,browserPager.getCurrentItem());
                return true;
            case R.id.sortByDateDesc:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.DATE, true,browserPager.getCurrentItem());
                return true;
            case R.id.sortBySize:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.SIZE, false,browserPager.getCurrentItem());
                return true;
            case R.id.sortBySizeDesc:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.SIZE, true,browserPager.getCurrentItem());
                return true;
            case R.id.sortByType:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.TYPE, false,browserPager.getCurrentItem());
                return true;
            case R.id.sortByTypeDesc:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(), ComparatorField.TYPE, true,browserPager.getCurrentItem());
                return true;

            // browser view
            case R.id.listBrowserViewMode:
                return true;
            case R.id.gridBrowserViewMode:
                return true;

            // sftp credentials or favorites
            case R.id.openSftpCredManager:
                openCredManager(VaultActivity.class);
                return true;
            case R.id.openSmbCredManager:
                openCredManager(SmbVaultActivity.class);
                return true;
            case R.id.openFavsManager:
                openFavsManager();
                return true;
            case R.id.openAboutDialog:
                openAboutDialog();
                return true;
            default:
                return true; // No action
        }
    }

    /**************************************************
     * Runtime permission management for Android >= 6 *
     **************************************************/

    public void exitOnPermissionsDenied() {
        Toast.makeText(this,"Some permissions were denied, exiting...",Toast.LENGTH_LONG).show();
        finishAffinity();
    }

    public void restartActivityOnPermissionOK() {
        // with kill process
        Intent i = new Intent(MainActivity.this,RestarterActivity.class);
        i.putExtra("",android.os.Process.myPid());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkSignaturePermissions() {
        return Settings.canDrawOverlays(this) &&
                Settings.System.canWrite(this);
    }

    public void requestSignaturePermissions() {
        Toast.makeText(this, "Signature permissions not granted", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(MainActivity.this,SettingsLauncherActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(i,0);
    }

    public boolean checkDangerousPermissions() {
        EnumSet<Permissions> nonGrantedPerms = EnumSet.noneOf(Permissions.class);
        for (Permissions p : Permissions.values()) {
            if (ActivityCompat.checkSelfPermission(mainActivityContext,p.value()) != PackageManager.PERMISSION_GRANTED) {
                nonGrantedPerms.add(p);
            }
        }

        return nonGrantedPerms.isEmpty();
    }

    public void requestDangerousPermissions() {
        ActivityCompat.requestPermissions(
                mainActivity,
                new String[]{Permissions.WRITE_EXTERNAL_STORAGE.value()},
                0x11111);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == 0) { // request cancelled
            exitOnPermissionsDenied();
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                exitOnPermissionsDenied();
                return;
            }
        }

        restartActivityOnPermissionOK();
    }

    // for handling result of alert permission request, once other permission have already been granted
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Settings.canDrawOverlays(this) && Settings.System.canWrite(this))
            restartActivityOnPermissionOK();
        else exitOnPermissionsDenied();
    }

    /**************************************************
     **************************************************
     **************************************************/

    // for receiving intents in open as archive from same app
    // (for archives with unknown extension)

    public void updateFromSelfIntent(Intent intent) {
        String startDir = intent.getStringExtra("STARTDIR");
        // start with custom dir, used at the end of CompressTask if CompressActivity was started by share intent, in order to show the compressed archive in its destination folder
        if(startDir != null) {
            try {
                goDir(new LocalPathContent(startDir));
            }
            catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to access directory: "+startDir, Toast.LENGTH_SHORT).show();
            }
        }
        else if (intent.getData() !=null) {
            boolean launchedFromHistory = (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
            if(launchedFromHistory) return; // avoid spurious download intents when re-opening from Recent Apps menu
            try {
                Uri data = intent.getData();
                String path = ContentProviderUtils.getPathFromUri(this, data);
//                Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                // treat path as archive, since one cannot do open as on folders
                // (even sending intent from other apps, it doesn't make sense to "open as" a folder)
                // obviously, this doesn't work if the path is a in-archive (with relative-to-root subpath not empty) or remote one
                if (path != null) goDir(new ArchivePathContent(path,""));
                else if ("https".equalsIgnoreCase(data.getScheme()) || "http".equalsIgnoreCase(data.getScheme())) {
                    // start download service
                    DownloadParams params = new DownloadParams(
                            data.toString(), null,null);

                    Intent startIntent = new Intent(this,HTTPDownloadService.class);
                    startIntent.setAction(BaseBackgroundService.START_ACTION);
                    startIntent.putExtra("params",params);
                    mainActivity.startService(startIntent);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to convert URI to path", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        updateFromSelfIntent(intent);
    }

    public SharedPreferences sharedPrefs;
    private void firstRunCheck() {
        sharedPrefs = getSharedPreferences(
                mainActivityContext.getPackageName(), Context.MODE_PRIVATE);
        boolean copied = sharedPrefs.getBoolean("FR",false);
        if (!copied) {
            FirstRunAssetsExtract.copyInstallNamesToRuntimeNames(mainActivityContext);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean("FR",true);
            editor.putBoolean("SOFTKEYS",hasSoftKeys());
            editor.commit();
        }
    }

    // 2 bits: LSB for dang, MSB for sign
    static int permMask = 0; // 0: nothing enabled, 1: dang enabled, 2: sign enabled, 3: both

    void startPermissionManagementActivity() {
        Intent i = new Intent(this,PermissionManagementActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    boolean isFirstRun() {
        SharedPreferences sp = getSharedPreferences(getPackageName(),MODE_PRIVATE);
        return sp.getBoolean("1stRun",true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        setContentView(R.layout.activity_main_with_pager);

        mainActivityContext = getApplicationContext();
        mainActivity = this;
        refreshToastHandler(mainActivityContext);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isFirstRun()) {
                startPermissionManagementActivity();
                return;
            }

            // ensure at least storage permissions are granted, it's useless to proceed otherwise
            if (!checkDangerousPermissions()) {
                Toast.makeText(this, "Storage permissions not granted, please enable them",
                        Toast.LENGTH_SHORT).show();
                startPermissionManagementActivity();
                return;
            }
        }

        // LEGACY, no explicit first-run activity
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // not already granted, stop loading UI and wait the permission callback to restart or finish activity

            permMask =
                    (checkDangerousPermissions()?1:0)+
                    (checkSignaturePermissions()?2:0);

            switch (permMask) {
                case 0:
                case 2:
                    requestDangerousPermissions();
                    return;
                case 1:
                    requestSignaturePermissions();
                    return;
                case 3: // both OK, continue loading
                    break;
            }
        }*/

        xFilesUtils = new XFilesUtilsUsingPathContent();
        currentHelper = xFilesUtils; // start with non-root (Java) file ops helper

        smbProvider = new SmbProviderUsingPathContent(mainActivityContext,this);
        sftpProvider = new SFTPProviderUsingPathContent(mainActivityContext,this);
        sftpRetryLsListener = new SftpRetryLsListener(this);


        layoutInflater = LayoutInflater.from(MainActivity.this);

        firstRunCheck();

        // if(!sharedPrefs.contains("SOFTKEYS")) throw new RuntimeException("Softkeys item not set");
        boolean hasPermanentMenuKey = !(sharedPrefs.getBoolean("SOFTKEYS",true));

        operationButtonsLayoutSwitcher = findViewById(R.id.operationButtonsLayoutSwitcher);
        fileOperationHelperSwitcher = findViewById(R.id.toggleRootHelperButton);

        if (hasPermanentMenuKey)
            operationButtonsLayoutSwitcher.setVisibility(View.GONE); // home-buttons embedding layout not needed in devices with hardware button

        // conditional inflating
        setOperationButtonsLayout(hasPermanentMenuKey);

        sortButton = findViewById(R.id.sortButton);
        sortButton.setOnClickListener(this::showAdvancedSortingDialogOrMenu);

        credsFavsButton = findViewById(R.id.openCredsFavsMenu);

        chooseBrowserViewButton = findViewById(R.id.chooseBrowserViewButton);

        itemSelectionButton = findViewById(R.id.itemSelectionButton);

        quickFindButton = findViewById(R.id.quickFindButton);

        itemSelectionButton.setOnClickListener(v -> browserPagerAdapter.switchMultiSelectMode(browserPager.getCurrentItem()));

        quickFindButton.setOnClickListener(v -> browserPagerAdapter.switchQuickFindMode(browserPager.getCurrentItem()));

        quickFindButton.setOnLongClickListener(v -> {
            Intent intent = new Intent(MainActivity.this,FindActivity.class);
            startActivity(intent);
            return true;
        });

        registerForContextMenu(sortButton);
        registerForContextMenu(credsFavsButton);
        credsFavsButton.setOnClickListener(this::openContextMenu);
        registerForContextMenu(chooseBrowserViewButton);

        rootHelperClient = RootHandler.startAndGetRH();

        if (rootHelperClient != null) {
            if (RootHandler.isRootAvailableAndGranted) {
                Toast.makeText(mainActivityContext, "Started roothelper in root mode", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mainActivityContext, "Root privileges not available, started roothelper in normal mode", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(mainActivityContext, "Unable to start roothelper", Toast.LENGTH_SHORT).show();
        }

        browserPagerAdapter = new BrowserPagerAdapter(this,this);

        browserPager = findViewById(R.id.browserpager);
        browserPager.setAdapter(browserPagerAdapter);

        // XFiles being run by external application for opening file
        browserPagerAdapter.checkUpdateIntent = true;
        // updateFromSelfIntent(getIntent()); // MOVED INTO BrowserPagerAdapter
    }

    // called only on first start, then saved and retrieved from SharedPreferences
    public boolean hasSoftKeys() {
        boolean hasSoftwareKeys;

        Display d = getWindowManager().getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        hasSoftwareKeys =  (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;
        return hasSoftwareKeys;
    }

    public void changeBrowserViewMode(View v) {
        browserPagerAdapter.changeBrowserViewMode(browserPager.getCurrentItem());
    }

    public void openCredManager(Class activity) {
        Intent intent = new Intent(MainActivity.this,activity);
        startActivity(intent);
    }

    public void openFavsManager() {
        Intent intent = new Intent(MainActivity.this,FavoritesActivity.class);
        startActivity(intent);
    }

    public void openAboutDialog() {
        new AboutDialog(this).show();
    }

    public void showAdvancedSortingDialogOrMenu(View v) {
        wasShortClick = true;
        openContextMenu(v);
        // dialog open moved in onCreateContextMenu
        // new AdvancedSortingDialog(MainActivity.this,getCurrentBrowserAdapter()).show();
    }

    public static GenericChangeDirectoryDialog cdd;
    public void showChangeDirectoryDialog(View v) {
        cdd = new GenericChangeDirectoryDialog(
                MainActivity.this,
                getCurrentDirCommander().getCurrentDirectoryPathname()
        );
        cdd.show();
    }

    public void toggleRootHelper(View v) {
        boolean isRootHelperUsed = currentHelper instanceof RootHelperClientUsingPathContent;
        String h = isRootHelperUsed ? "standard" : "roothelper-enabled";
        AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
        bld.setTitle("Switch dir commander to "+h+" one?");
        bld.setIcon(isRootHelperUsed?R.drawable.xfiles_root_off:R.drawable.xfiles_root_on);
        bld.setNegativeButton("No", (dialog, which) -> {/* no action */});
        bld.setPositiveButton("Yes", (dialog, which) -> {
            // disabled, better user experience, to be tested
            // browserPagerAdapter.createStandardCommanders();
            if (isRootHelperUsed) { // switch to normal dircommander
                currentHelper = xFilesUtils;
                usingRootHelperForLocal = false;
                fileOperationHelperSwitcher.setImageResource(R.drawable.xfiles_root_off);
            }
            else { // switch to roothelper-based dircommander
                currentHelper = rootHelperClient;
                usingRootHelperForLocal = true;
                fileOperationHelperSwitcher.setImageResource(R.drawable.xfiles_root_on);
            }
        });
        AlertDialog alertDialog = bld.create();
        alertDialog.show();
    }

    /**
     * To disable fullscreen and hide title, comment this method and add:
     * requestWindowFeature(Window.FEATURE_NO_TITLE);
     * in onCreate, before super.onCreate();
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public static void simulateHomePress(Activity activity) {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(startMain);
    }

    public void simulateRecentPress() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            Intent intent = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
            intent.setComponent(new ComponentName("com.android.systemui", "com.android.systemui.recent.RecentsActivity"));
            startActivity(intent);
        }
        else {
            Toast.makeText(this,"Recents button not implemented on android != Kitkat",Toast.LENGTH_SHORT).show();
        }
    }

    public void openWithDefaultApp(File file) {
        // get extension
        String extension = "";
        String fullName = file.getName();
        int i = fullName.lastIndexOf('.');
        if (i > 0) extension = fullName.substring(i+1);

        MimeTypeMap myMime = MimeTypeMap.getSingleton();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mimeType = myMime.getMimeTypeFromExtension(extension);
        intent.setDataAndType(Uri.fromFile(file),mimeType);
//        intent.setDataAndType(
//                FileProvider.getUriForFile(MainActivity.this,
//                        BuildConfig.APPLICATION_ID + ".provider",
//                        file),
//                mimeType
//        );

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) { // if no default app found, show open as menu
            Toast.makeText(this, "No handler for this type of file.", Toast.LENGTH_LONG).show();
            showOpenAsList(file);
        }
    }

    public void showOpenAsList(File file) {
        if (file.isDirectory()) {
            Toast.makeText(this,"File is a directory",Toast.LENGTH_SHORT).show();
            return;
        }
        OpenAsDialog oad = new OpenAsDialog(MainActivity.this,file);
        oad.show();
    }

    public void prepareForCopyOrMove(CopyMoveMode mode) {
        BasePathContent path = getCurrentDirCommander().getCurrentDirectoryPathname();
        copyMoveList = new CopyMoveListPathContent(
                getCurrentBrowserAdapter(),mode,path);
        String zeroWarning = "No items selected for ";
        String success = " items ready to be ";
        String successS = " item ready to be ";
        if (copyMoveList.files.size()==0) {
            Toast.makeText(this,
                    (mode==CopyMoveMode.COPY)?zeroWarning+"copy":zeroWarning+"move",
                    Toast.LENGTH_SHORT).show();
            copyMoveList=null;
            return;
        }
        String prefix = ""+copyMoveList.files.size();
        prefix+=copyMoveList.files.size()==1?successS:success;
        Toast.makeText(this,
                (mode==CopyMoveMode.COPY)?prefix+"copied":prefix+"moved",
                Toast.LENGTH_SHORT).show();
    }

    public void extractItem(BrowserItem b) {
//        new ExtractDialog(this,null,b.filename).show();

        Intent i = new Intent(MainActivity.this, ExtractActivity.class);
        i.putExtra("filename",b.filename);
        startActivity(i);
    }

    public void extractItems() {
        // extract dialog will take selected items directly from browser adapter
//        new ExtractDialog(this,null,null).show();
        Intent i = new Intent(MainActivity.this, ExtractActivity.class);
        startActivity(i);
    }

    public void shareItems() {
        List<BasePathContent> selection = getCurrentBrowserAdapter().getSelectedItemsAsPathContents();
        if (selection.size()==0) {
            Toast.makeText(this,"No items selected for sharing",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent sharingIntent;
        sharingIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        sharingIntent.setType("*/*");
        ArrayList<Uri> uris = new ArrayList<>();
        for (BasePathContent f : selection) {
            if (f.providerType != ProviderType.LOCAL) {
                Toast.makeText(this,"Sharing not implemented for non-local or within-archive files",Toast.LENGTH_LONG).show();
                return;
            }
            // commented in order to allow also directories sharing with XRE or XFiles compress
//            if (new File(f.dir).isDirectory()) {
//                Toast.makeText(this,"Only files can be shared, not directories",Toast.LENGTH_SHORT).show();
//                return;
//            }
            uris.add(Uri.fromFile(new File(f.dir)));
//            uris.add(FileProvider.getUriForFile(MainActivity.this,
//                    BuildConfig.APPLICATION_ID + ".provider",
//                    new File(f.dir)));
        }
        sharingIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sharingIntent, "Share files using"));
    }

    public void paste() {
        final BasePathContent destPath = getCurrentDirCommander().getCurrentDirectoryPathname();

        if (copyMoveList==null || copyMoveList.files.size()==0) {
            Toast.makeText(this,"No items to be pasted",Toast.LENGTH_SHORT).show();
            return;
        }

        // useless with conflict handling enabled
//        if (copyMoveList.parentDir.equals(destPath)) {
//            Toast.makeText(this,"Source and destination are the same!",Toast.LENGTH_SHORT).show();
//            return;
//        }

        // if destPath is a sub-folder of some item in the copy/move selection, cancel file transfer
        for (BrowserItem fn : copyMoveList.files) {
            if (copyMoveList.parentDir.concat(fn.filename).isParentOf(destPath)) {
                Toast.makeText(this, "Cannot copy or move a directory into one of its descendants", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 4 cases local x remote, origin x destination

        // local to local file transfer
        if (copyMoveList.parentDir.providerType == ProviderType.LOCAL &&
                destPath.providerType == ProviderType.LOCAL) {

            //*/*/*/*/*/*/*/*/ 1 - with service and task
            Intent startIntent = new Intent(MainActivity.this,CopyMoveService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra("params",new CopyMoveParams(copyMoveList,destPath));
            startService(startIntent);
            //*/*/*/*/*/*/*/*/

            //*/*/*/*/*/*/*/*/ 2 - with IndeterminateAsyncTask
//            IndeterminateAsyncTask t = new IndeterminateAsyncTask(
//                    MainActivity.this,
//                    copyMoveList.copyOrMove==CopyMoveMode.COPY?"Copying...":"Moving...",
//                    copyMoveList.copyOrMove.name().toLowerCase()+" completed",
//                    copyMoveList.copyOrMove.name().toLowerCase()+" error")
//            {
//                @Override
//                protected Integer doInBackground(Void... params) {
//                    try {
////                        for(int k=5;k>=0;k--) {
////                            Thread.sleep(1000);
////                            Log.e("COPYTASK","sleeping... "+k);
////                        }
//                        currentHelper.copyMoveFilesToDirectory(copyMoveList, destPath);
//                        return 0;
//                    }
//                    catch (Exception e) {
//                        e.printStackTrace();
//                        return -1;
//                    }
//                }
//
//                @Override
//                protected void onPostExecute(Integer integer) {
//                    super.onPostExecute(integer);
//                    copyMoveList = null;
//                    showDirContent(getCurrentDirCommander().refresh());
//                }
//            };
//            t.execute((Void[])null);
            //*/*/*/*/*/*/*/*/

            //*/*/*/*/*/*/*/*/ 3 - plain, on UI thread, without dialogs or progress indication
//            try {
//                currentHelper.copyMoveFilesToDirectory(copyMoveList, destPath);
//            } catch (IOException e) {
//                Toast.makeText(this,"File transfer error",Toast.LENGTH_LONG).show();
//            }
            //*/*/*/*/*/*/*/*/
        }

        // SFTP upload or download
        else if ((copyMoveList.parentDir.providerType == ProviderType.LOCAL &&
                destPath.providerType == ProviderType.SFTP) ||
                ((copyMoveList.parentDir.providerType == ProviderType.SFTP &&
                        destPath.providerType == ProviderType.LOCAL))) {
            //*/*/*/*/*/*/*/*/ 1 - with service and task
            Intent startIntent = new Intent(MainActivity.this,NonInteractiveSftpService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra("params",new CopyMoveParams(copyMoveList,destPath));
            startService(startIntent);
            //*/*/*/*/*/*/*/*/
            return;
        }
        // SFTP to SFTP file transfer
        else if (copyMoveList.parentDir.providerType == ProviderType.SFTP &&
                destPath.providerType == ProviderType.SFTP) {
            // remote transfer on the same remote host
            if (((RemotePathContent)copyMoveList.parentDir).authData.equals(((RemotePathContent)destPath).authData)) {
                // move (rename) on the remote host
                if (copyMoveList.copyOrMove == CopyMoveMode.MOVE) {
                    try {
                        currentHelper.copyMoveFilesToDirectory(copyMoveList,destPath);
                        copyMoveList = null;
                        browserPagerAdapter.showDirContent(getCurrentDirCommander().refresh(),browserPager.getCurrentItem());
                        Toast.makeText(this,"Remote-to-remote move completed",Toast.LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(this,"Remote-to-remote error: "+e.getMessage(),Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this,"Only remote to remote move on same host supported",Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this,"Unpractical to be implemented",Toast.LENGTH_SHORT).show();
                // not implementable without downloading to device & uploading from it (unpractical), or
                // without already key-exchanged remote end-points
            }
            return;
        }
        // XFiles remote transfer
        else if ((copyMoveList.parentDir.providerType == ProviderType.XFILES_REMOTE &&
                        destPath.providerType == ProviderType.LOCAL) ||
                (copyMoveList.parentDir.providerType == ProviderType.LOCAL &&
                        destPath.providerType == ProviderType.XFILES_REMOTE)) {
            Intent startIntent = new Intent(MainActivity.this,NonInteractiveXFilesRemoteTransferService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra("params",new CopyMoveParams(copyMoveList,destPath));
            startService(startIntent);
        }
        // SMB upload or download
        else if ((copyMoveList.parentDir.providerType == ProviderType.LOCAL &&
                destPath.providerType == ProviderType.SMB) ||
                ((copyMoveList.parentDir.providerType == ProviderType.SMB &&
                        destPath.providerType == ProviderType.LOCAL))) {
            //*/*/*/*/*/*/*/*/ 1 - with service and task
            Intent startIntent = new Intent(MainActivity.this, NonInteractiveSmbService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra("params",new CopyMoveParams(copyMoveList,destPath));
            startService(startIntent);
            //*/*/*/*/*/*/*/*/
            return;
        }
        else {
            Toast.makeText(mainActivity, "Unknown data provider pair", Toast.LENGTH_LONG).show();
        }

        // With asynctask used in copy/move, the following commented lines are enabled in the onPostExecute method of the IndeterminateAsyncTask
//        Toast.makeText(this,(copyMoveList.copyOrMove==CopyMoveMode.COPY?"copy":"move")+" completed",Toast.LENGTH_SHORT).show();
//        copyMoveList = null;
//
//        showDirContent(getCurrentDirCommander().refresh());

    }

    void deleteSelection() {
        List<BasePathContent> selection = getCurrentBrowserAdapter().getSelectedItemsAsPathContents();
        if (selection.size()==0) {
            Toast.makeText(this,"No items selected for deletion",Toast.LENGTH_SHORT).show();
            return;
        }
        showDeleteDialog(selection);
    }

    void compressSelection() {
        if (getCurrentBrowserAdapter().getSelectedCount() == 0) {
            Toast.makeText(this,"No items selected for compression",Toast.LENGTH_SHORT).show();
            return;
        }
        // with CompressActivity
        startActivity(new Intent(MainActivity.this,CompressActivity.class));
    }

    void checksumSelection() {
        if (getCurrentBrowserAdapter().getSelectedCount() == 0) {
            Toast.makeText(this,"No items selected for checksum",Toast.LENGTH_SHORT).show();
            return;
        }
        if (!getCurrentBrowserAdapter().ensureOnlyFilesSelection()) {
            Toast.makeText(this, "Selection contains directories, please choose only files for checksum", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(new Intent(MainActivity.this,ChecksumActivity.class));
    }

    void upOneLevel() {
        BasePathContent parentFile = getCurrentDirCommander().getCurrentDirectoryPathname().getParent();
        if (parentFile == null) {
            Toast.makeText(this,"Already on root path for the current filesystem",Toast.LENGTH_SHORT).show();
            return;
        }
        goDir(parentFile);
    }

    /**
     * @param dirOrDirection Target path to be loaded, or direction as boolean (back or ahead)
     * @param targetFilenameToHighlight Target filename to be highlighted and centered in the listview (in case of Locate command from {@link FindActivity})
     */
    public FileOpsErrorCodes goDir(Object dirOrDirection, String... targetFilenameToHighlight) {
        GenericDirWithContent dwc;
        int prevPos = getCurrentMainBrowserView().getFirstVisiblePosition();
        if (dirOrDirection instanceof Boolean) {
            Boolean b = (Boolean)dirOrDirection;
            if (b) dwc = getCurrentDirCommander().goBack(prevPos);
            else dwc = getCurrentDirCommander().goAhead(prevPos);
        }
        else if (dirOrDirection instanceof BasePathContent) {
            dwc = getCurrentDirCommander().setDir((BasePathContent) dirOrDirection, prevPos);
        }
        else {
            Toast.makeText(this,"Invalid object type for dir browsing",Toast.LENGTH_SHORT).show();
            return FileOpsErrorCodes.ILLEGAL_ARGUMENT;
        }

        // check for errors here
        if (dwc.errorCode != null) {
            if (dwc.errorCode == FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD) {
                AskPasswordDialogOnListing askPasswordDialogOnListing = new AskPasswordDialogOnListing(
                        MainActivity.this,
                        (BasePathContent) dirOrDirection // tested, no classCastException on go back/ahead into an archive
                );
                askPasswordDialogOnListing.show();
                return dwc.errorCode;
            }

            Toast.makeText(this,dwc.errorCode.getValue(),Toast.LENGTH_SHORT).show();
            // TODO switch error codes to dialogs or toast popups
            return dwc.errorCode;
        }

        browserPagerAdapter.showDirContent(dwc,browserPager.getCurrentItem(),targetFilenameToHighlight);
        return FileOpsErrorCodes.OK;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*
         * ensure the are no other active long term tasks; if so,
         * the main roothelper server instance could be needed
         * to cancel those tasks (via kill signal from one RH to the other)
         */

        // kill RH server only if there aren't foreground services using it, and if XRE remote server is not active

        // if busy, terminate all the rest, if not, terminate everything
        synchronized (ProgressIndicator.busy) {
            ForegroundServiceType f = ProgressIndicator.busy.get();
            if (f == null) {
                // close all
                if (sftpProvider != null) sftpProvider.closeAllSessions();
                if (smbProvider != null) smbProvider.closeAllSessions();
                rootHelperRemoteClientManager.closeAllSessions();
                if (RemoteServerManager.rhssManagerThreadRef.get() == null)
                    killRHWrapper();
            }
            else {
                // TODO apply a better construct for set exclusion
                switch (f) {
                    case FILE_TRANSFER:
                    case FILE_ARCHIVING:
                    case XRE_TRANSFER:
                    case URL_DOWNLOAD:
                        if (sftpProvider != null) sftpProvider.closeAllSessions();
                        if (smbProvider != null) smbProvider.closeAllSessions();
                        break;
                    case SFTP_TRANSFER:
                        if (smbProvider != null) smbProvider.closeAllSessions();
                        rootHelperRemoteClientManager.closeAllSessions(); // FIXME this shouldn't be done anymore since the use of RobustLocal file streams, to be checked
                        if (RemoteServerManager.rhssManagerThreadRef.get() == null)
                            killRHWrapper();
                        break;
                    case SMB_TRANSFER:
                        if (sftpProvider != null) sftpProvider.closeAllSessions();
                        break;
                }
            }
        }

        usingRootHelperForLocal = false;
        mainActivityContext = null;
        mainActivity = null;
        toastHandler = null;
    }

    boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        // TODO maybe should handle mode exit differently (not both at once)
        // TODO decide if it is needed to restore original adapter content on quick find mode exit
        int pos = browserPager.getCurrentItem();
        if (browserPagerAdapter.multiSelectModes[pos] ||
                browserPagerAdapter.quickFindModes[pos])
        {
            if (browserPagerAdapter.multiSelectModes[pos]) browserPagerAdapter.switchMultiSelectMode(pos);
            if (browserPagerAdapter.quickFindModes[pos]) browserPagerAdapter.switchQuickFindMode(pos);
            return;
        }

        if (doubleBackToExitPressedOnce) {

            // check if there is any remote server active and, in case, show dialog
            if((RemoteServerManager.rhssManagerThreadRef.get() != null) ||
                    FileServer.FTP.isAlive() ||
                    FileServer.HTTP.isAlive()) {
                new CloseActiveServersDialog(this).show();
            }
            else {
                super.onBackPressed();
            }
            return;
        }

        // Test case for path content hashcode (should be same hashcode three times)
//        Toast.makeText(this,
//                new RemotePathContent(new AuthData("user","domain",22,"notempty"),"/remotedir").hashCode()+"\n"+new RemotePathContent(new AuthData("user","domain",22,"empty"),"/remotedir").hashCode()+"\n"+new RemotePathContent(new AuthData("user","domain",22,null),"/remotedir").hashCode()
//                ,Toast.LENGTH_LONG
//                ).show();
        //

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce=false, 2000);
    }

    boolean currentMode;
    public void setOperationButtonsLayout(boolean standardMode) {
        this.currentMode = standardMode;
        operationButtonsLayoutSwitcher.setImageResource(standardMode?R.drawable.xfiles_switch_operation_buttons_blue:R.drawable.xfiles_switch_operation_buttons_green);

        operationButtonsLayout = findViewById(R.id.operationButtonsLayout);
        operationButtonsLayout.removeAllViews();
        View targetLayout;
        if (standardMode) {
            targetLayout = layoutInflater.inflate(R.layout.standard_operational_layout,null);
        }
        else {
            targetLayout = layoutInflater.inflate(R.layout.overriding_home_buttons_operational_layout,null);
        }
        operationButtonsLayout.addView(targetLayout);
    }

    public void multiSelectAction(View v) {
        FilterSelectionDialog fsd;
        switch (v.getId()) {
            case R.id.itemsSelectAll:
                getCurrentBrowserAdapter().selectAll();
                return;
            case R.id.itemsSelectNone:
                getCurrentBrowserAdapter().selectNone();
                return;
            case R.id.itemsInvertSelection:
                getCurrentBrowserAdapter().invertSelection();
                return;
            case R.id.itemsFilterSelection:
                fsd = new FilterSelectionDialog(MainActivity.this, getCurrentBrowserAdapter(), true);
                fsd.show();
                return;
            case R.id.itemsFilterDeselection:
                fsd = new FilterSelectionDialog(MainActivity.this, getCurrentBrowserAdapter(), false);
                fsd.show();
                return;
        }
    }

    public void switchOperationButtonsLayout(View v) {
        setOperationButtonsLayout(!currentMode);
    }

    public void operationBarOnClick(View v) {
        switch (v.getId()) {
            case R.id.androidGoBackButton:
                onBackPressed();break;
            case R.id.androidGoHomeButton:
                simulateHomePress(this);break;
            case R.id.androidGoRecentButton:
                simulateRecentPress();break;

            case R.id.upOneLevelButton:
                upOneLevel();break;
            case R.id.pasteButton:
                paste();break;

            case R.id.goBackButton:
                goDir(Boolean.TRUE);break;
            case R.id.goAheadButton:
                goDir(Boolean.FALSE);break;

            case R.id.newFileButton:
                CreateFileOrDirectoryDialog createFileDialog = new CreateFileOrDirectoryDialog(this, FileMode.FILE);
                createFileDialog.show();
                break;
            case R.id.newDirectoryButton:
                CreateFileOrDirectoryDialog createDirectoryDialog = new CreateFileOrDirectoryDialog(this, FileMode.DIRECTORY);
                createDirectoryDialog.show();
                break;

            case R.id.cutButton:
                prepareForCopyOrMove(CopyMoveMode.MOVE);break;
            case R.id.copyButton:
                prepareForCopyOrMove(CopyMoveMode.COPY);break;

            case R.id.deleteButton:
                deleteSelection();break;
            case R.id.compressButton:
                compressSelection();break;
            case R.id.shareButton:
                shareItems();break;

            default: break;
        }
    }
}
