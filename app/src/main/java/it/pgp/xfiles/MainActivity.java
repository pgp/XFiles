package it.pgp.xfiles;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import net.alhazmy13.mediagallery.library.activity.MediaGallery;
import net.alhazmy13.mediagallery.library.activity.MediaGalleryActivity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Future;

import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.adapters.BrowserPagerAdapter;
import it.pgp.xfiles.adapters.OperationalPagerAdapter;
import it.pgp.xfiles.dialogs.AboutDialog;
import it.pgp.xfiles.dialogs.AdvancedSortingDialog;
import it.pgp.xfiles.dialogs.BulkRenameDialog;
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
import it.pgp.xfiles.dialogs.SSHAlreadyInKnownHostsDialog;
import it.pgp.xfiles.dialogs.SSHNotInKnownHostsDialog;
import it.pgp.xfiles.dialogs.XFilesRemoteSessionsManagementActivity;
import it.pgp.xfiles.dialogs.compress.AskPasswordDialogOnListing;
import it.pgp.xfiles.dialogs.compress.CompressActivity;
import it.pgp.xfiles.dialogs.compress.ExtractActivity;
import it.pgp.xfiles.enums.ArchiveType;
import it.pgp.xfiles.enums.BrowserViewMode;
import it.pgp.xfiles.enums.ComparatorField;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ForegroundServiceType;
import it.pgp.xfiles.enums.Permissions;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.fileservers.FileServer;
import it.pgp.xfiles.roothelperclient.RemoteClientManager;
import it.pgp.xfiles.roothelperclient.RemoteServerManager;
import it.pgp.xfiles.roothelperclient.RootHandler;
import it.pgp.xfiles.roothelperclient.RootHelperClient;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.CopyMoveService;
import it.pgp.xfiles.service.ExtractService;
import it.pgp.xfiles.service.HTTPUploadService;
import it.pgp.xfiles.service.NonInteractiveSftpService;
import it.pgp.xfiles.service.NonInteractiveSmbService;
import it.pgp.xfiles.service.NonInteractiveXFilesRemoteTransferService;
import it.pgp.xfiles.service.TestService;
import it.pgp.xfiles.service.params.CopyMoveParams;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.service.params.ExtractParams;
import it.pgp.xfiles.service.params.TestParams;
import it.pgp.xfiles.service.visualization.ProgressIndicator;
import it.pgp.xfiles.sftpclient.InteractiveHostKeyVerifier;
import it.pgp.xfiles.sftpclient.SFTPProvider;
import it.pgp.xfiles.sftpclient.VaultActivity;
import it.pgp.xfiles.smbclient.SmbProvider;
import it.pgp.xfiles.smbclient.SmbVaultActivity;
import it.pgp.xfiles.utils.ContentProviderUtils;
import it.pgp.xfiles.utils.DirCommander;
import it.pgp.xfiles.utils.FileOperationHelper;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.SelectImageButtonListener;
import it.pgp.xfiles.utils.XFilesUtils;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.dircontent.SftpDirWithContent;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.SFTPPathContent;

public class MainActivity extends EffectActivity {

    /************** JNI part **************/
    static {
        System.loadLibrary("r"); // libr.so
        // avoid messing up with content URIs
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);
    }
    /************** end JNI part **************/

    public static MainActivity mainActivity;
    public static Context context;
    public static final Handler handler = new Handler(Looper.getMainLooper());

    public ActivityManager activityManager;

    public BrowserViewPager browserPager;

    private CopyMoveListPathContent copyMoveList = null; // only one for the entire ViewPager (you may want to copy files from one browser view to the other one)

    private LayoutInflater layoutInflater;

    public BrowserPagerAdapter browserPagerAdapter;

    public static final int fullScreenVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    public static final int horizontalVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    public int defaultUIVisibility;

    public static void makeImageButtonsStateful(ViewGroup layout, SelectImageButtonListener l) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View vv = layout.getChildAt(i);
            if (vv instanceof ImageButton)
                vv.setOnTouchListener(l);
        }
    }

    // File Operations Helpers
    public static SmbProvider smbProvider;
    public static SFTPProvider sftpProvider;

    public static XFilesUtils xFilesUtils;
    private static RootHelperClient rootHelperClient;

    public FileOperationHelper getFileOpsHelper(ProviderType providerType) {
        switch(providerType) {
            case LOCAL:
                return usingRootHelperForLocal?getRootHelperClient():xFilesUtils;
            case LOCAL_WITHIN_ARCHIVE:
            case XFILES_REMOTE:
            case URL_DOWNLOAD:
                return getRootHelperClient();
            case SFTP:
                return sftpProvider;
            case SMB:
                return smbProvider;
            default:
                return null;
        }
    }

    public static RootHelperClient getRootHelperClient(Context... context) {
        if (rootHelperClient == null) {
            rootHelperClient = RootHandler.startAndGetRH(context);
            if (rootHelperClient != null)
                showToast(RootHandler.isRootAvailableAndGranted?
                        "Started roothelper in root mode":
                        "Root privileges not available, started roothelper in normal mode");
            else showToast("Unable to start roothelper");
        }
        return rootHelperClient;
    }

    public static void killRHWrapper() {
        try {
            rootHelperClient.killServer();
        }
        catch (NullPointerException n) {
            Log.e("RH","Unable to kill roothelper server, reference already null");
        }
        catch (Exception e) {
            Log.e("RH","Unable to kill roothelper server",e);
        }
        rootHelperClient = null;
    }

    public static final RemoteClientManager rootHelperRemoteClientManager = new RemoteClientManager();
    public static boolean usingRootHelperForLocal = false;

    public BrowserAdapter getCurrentBrowserAdapter() {
        return browserPagerAdapter.browserAdapters[browserPager.getCurrentItem()];
    }

    public DirCommander getCurrentDirCommander() {
        return browserPagerAdapter.dirCommanders[browserPager.getCurrentItem()];
    }

    public AbsListView getCurrentMainBrowserView() {
        return browserPagerAdapter.mainBrowserViews[browserPager.getCurrentItem()];
    }

    public static void refreshAppContext(Context context) {
        MainActivity.context = context;
    }

    public static void showToast(String s) {
        handler.post(()-> Toast.makeText(context, s, Toast.LENGTH_SHORT).show());
    }

    public ProgressBar progressCircleForGoDirOps;
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
                goDir_async(currentFile,null);
                return;
            }

            // from now on, exclude any open operation on non-local path contents
            if (currentFile.providerType != ProviderType.LOCAL) return;

            // open local archive
            if (browserItem.hasExt()) {
                String arcExt = browserItem.getFileExt();
                ArchivePathContent apc = new ArchivePathContent(currentFile.dir,"/");
                if(ArchiveType.APK.name().equals(arcExt.toUpperCase())) {
                    AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
                    bld.setTitle("Choose APK action");
                    bld.setNegativeButton("Install", (dialog, which) -> installApk(new File(currentFile.dir)));
                    bld.setPositiveButton("Open as archive", (dialog, which) -> goDir_async(apc,null));
                    bld.create().show();
                    return;
                }
                if(ArchiveType.formats.contains(arcExt)) {
                    goDir_async(apc,null);
                    return;
                }
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
        new PropertiesDialog(MainActivity.this,
                b.isDirectory?FileMode.DIRECTORY :FileMode.FILE,
                Collections.singletonList(getCurrentDirCommander().getCurrentDirectoryPathname().concat(b.filename))).show();
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
        int size = selection.size();
        String name = size==1?selection.get(0).getName():""+size+" items";
        bld.setTitle("Delete items");
        bld.setIcon(R.drawable.xf_recycle_bin);
        bld.setNegativeButton(android.R.string.cancel, null);
        bld.setPositiveButton(android.R.string.ok, null);

        TextView a = new TextView(this);
        a.setText("Confirm delete "+name);
        a.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        bld.setView(a);

        AlertDialog alertDialog = bld.create();
        alertDialog.show();

        Button ok = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button no = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        ok.setOnClickListener(view -> {
            int posToRestore = browserPagerAdapter.mainBrowserViews[browserPager.getCurrentItem()].getFirstVisiblePosition();
            a.setText("Deleting...");
            ok.setEnabled(false);
            no.setEnabled(false);
            new Thread(()->{
                try {
                    mainActivity.getFileOpsHelper(selection.get(0).providerType).deleteFilesOrDirectories(selection);
                    runOnUiThread(()->{
                        Toast.makeText(MainActivity.this,((size==1)?name:(size+" items"))+" deleted", Toast.LENGTH_SHORT).show();
                        browserPagerAdapter.showDirContent(getCurrentDirCommander().refresh(),browserPager.getCurrentItem(),posToRestore);
                    });
                }
                catch (IOException e) {
                    e.printStackTrace();
                    showToast("Unable to delete some items");
                }
                alertDialog.dismiss();
            }).start();
        });
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
        else if (v.getId()==R.id.currentDirectoryTextView) {
            inflater.inflate(R.menu.menu_fast_changedir,menu);
        }
        else {
            Toast.makeText(this, "Switch not allowed anymore here, check showPopup", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch(itemId) {

            // check showPopup for additional items moved from here

            // sorting
            // TODO need to add directory priority switch some way (used priority on as default)
            case R.id.sortByFilename:
            case R.id.sortByFilenameDesc:
            case R.id.sortByDate:
            case R.id.sortByDateDesc:
            case R.id.sortBySize:
            case R.id.sortBySizeDesc:
            case R.id.sortByType:
            case R.id.sortByTypeDesc:
                browserPagerAdapter.showSortedDirContent(getCurrentDirCommander().refresh(),
                        ComparatorField.fromResMap.get(itemId),browserPager.getCurrentItem());
                return true;
            // browser view
            case R.id.listBrowserViewMode:
                return true;
            case R.id.gridBrowserViewMode:
                return true;

            // sftp credentials or favorites
            case R.id.openSftpCredManager:
            case R.id.openSmbCredManager:
            case R.id.openFavsManager:
                openCredOrFavsManager(itemId);
                return true;

            case R.id.openAboutDialog:
                openAboutDialog();
                return true;

            // fast menu for change directory
            case R.id.localFolder:
            case R.id.localArchive:
            case R.id.sftpRemoteFolder:
            case R.id.xfilesRemoteFolder:
            case R.id.smbRemoteFolder:
            case R.id.httpUrlDownload:
                cdd = new GenericChangeDirectoryDialog(
                        MainActivity.this,
                        getCurrentDirCommander().getCurrentDirectoryPathname()
                );
                cdd.show();
                // using same resIds (with different parent) for both menu items and radiobuttons
                ((RadioButton)cdd.findViewById(itemId)).setChecked(true);
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

    public boolean checkDangerousPermissions() {
        EnumSet<Permissions> nonGrantedPerms = EnumSet.noneOf(Permissions.class);
        for (Permissions p : Permissions.values()) {
            if (ActivityCompat.checkSelfPermission(context,p.value()) != PackageManager.PERMISSION_GRANTED) {
                nonGrantedPerms.add(p);
            }
        }

        return nonGrantedPerms.isEmpty();
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
                goDirOrArchive(new LocalPathContent(startDir));
            }
            catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Unable to access directory: "+startDir, Toast.LENGTH_SHORT).show();
            }
        }
        else if (intent.getData() !=null) {
            if((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) return; // avoid spurious download intents when re-opening from Recent Apps menu
            try {
                Uri data = intent.getData();
                String path = ContentProviderUtils.getPathFromUri(this, data);
//                Toast.makeText(this, path, Toast.LENGTH_SHORT).show();
                // treat path as archive, since one cannot do open as on folders
                // (even sending intent from other apps, it doesn't make sense to "open as" a folder)
                // obviously, this doesn't work if the path is a in-archive (with relative-to-root subpath not empty) or remote one
                if (path != null) goDirOrArchive(new LocalPathContent(path));
                else if ("https".equalsIgnoreCase(data.getScheme()) || "http".equalsIgnoreCase(data.getScheme())) {
                    // launch URL download dialog, populating it with the received URL
                    showChangeDirectoryDialog_(data.toString());
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
        sharedPrefs = getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        boolean copied = sharedPrefs.getBoolean("FR",false);
        SharedPreferences.Editor editor = null;
        if (!copied) {
//            FirstRunAssetsExtract.copyInstallNamesToRuntimeNames(mainActivityContext);
            editor = sharedPrefs.edit();
            editor.putBoolean("FR",true);
            editor.putBoolean("SOFTKEYS",hasSoftKeys());
            editor.apply();
        }

        int isTablet_ = sharedPrefs.getInt("ISTABLET",-1);
        if(isTablet_ < 0) {
            if(editor == null) editor = sharedPrefs.edit();
            isTablet_ = getDisplayDiagonalSizeInches()>=6.5?1:0;
            editor.putInt("ISTABLET",isTablet_);
            editor.apply();
        }
        isTablet = isTablet_==1;
        hasPermanentMenuKey = !(sharedPrefs.getBoolean("SOFTKEYS",true));
    }

    // 2 bits: LSB for dang, MSB for sign
    static int permMask = 0; // 0: nothing enabled, 1: dang enabled, 2: sign enabled, 3: both

    void startPermissionManagementActivity() {
        Intent i = new Intent(this,PermissionManagementActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    boolean hasPermanentMenuKey;

    public double getDisplayDiagonalSizeInches() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float yInches = metrics.heightPixels/metrics.ydpi;
        float xInches = metrics.widthPixels/metrics.xdpi;
        return Math.sqrt(xInches*xInches + yInches*yInches);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean isHorizontal = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        getWindow().getDecorView().setSystemUiVisibility(horizontalVisibility);
        setOperationButtonsLayout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        setContentView(R.layout.activity_main_with_pager);

        refreshAppContext(getApplicationContext());
        mainActivity = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // ensure at least storage permissions are granted, it's useless to proceed otherwise
            if (!checkDangerousPermissions()) {
                Toast.makeText(this, "Storage permissions not granted, please enable them",
                        Toast.LENGTH_SHORT).show();
                startPermissionManagementActivity();
                return;
            }
        }

        xFilesUtils = new XFilesUtils();

        smbProvider = new SmbProvider(context,this);
        sftpProvider = new SFTPProvider(this);


        layoutInflater = LayoutInflater.from(MainActivity.this);

        defaultUIVisibility = getWindow().getDecorView().getSystemUiVisibility();
        getWindow().getDecorView().setSystemUiVisibility(horizontalVisibility);

        firstRunCheck();

        progressCircleForGoDirOps = findViewById(R.id.progressCircleForGoDirOps);
        fileOperationHelperSwitcher = findViewById(R.id.toggleRootHelperButton);

        // conditional inflating
        setOperationButtonsLayout();

        sortButton = findViewById(R.id.sortButton);
        sortButton.setOnClickListener(this::showAdvancedSortingDialogOrMenu);

        credsFavsButton = findViewById(R.id.openCredsFavsMenu);

        chooseBrowserViewButton = findViewById(R.id.chooseBrowserViewButton);

        itemSelectionButton = findViewById(R.id.itemSelectionButton);

        quickFindButton = findViewById(R.id.quickFindButton);

        itemSelectionButton.setOnClickListener(v -> browserPagerAdapter.switchMultiSelectMode(browserPager.getCurrentItem()));

        quickFindButton.setOnClickListener(v -> browserPagerAdapter.switchQuickFindMode(browserPager.getCurrentItem()));

        quickFindButton.setOnLongClickListener(v -> {
            findInFolders(getCurrentDirCommander().getCurrentDirectoryPathname(), true);
            return true;
        });

        makeImageButtonsStateful(findViewById(R.id.pathViewLayout),
                new SelectImageButtonListener(this, R.color.imagebuttonselect));

        registerForContextMenu(sortButton);
        registerForContextMenu(credsFavsButton);
        credsFavsButton.setOnClickListener(this::openContextMenu);
        registerForContextMenu(chooseBrowserViewButton);

//        getRootHelperClient();

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

    public void openCredOrFavsManager(int resId) {
        Class targetActivity;
        switch(resId) {
            case R.id.openSftpCredManager:
                targetActivity = VaultActivity.class;
                break;
            case R.id.openSmbCredManager:
                targetActivity = SmbVaultActivity.class;
                break;
            case R.id.openFavsManager:
                targetActivity = FavoritesActivity.class;
                break;
            default:
                throw new RuntimeException("Guard block");
        }
        startActivity(new Intent(MainActivity.this,targetActivity));
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
    public void showChangeDirectoryDialog_(String... downloadUrlToPopulate) {
        cdd = new GenericChangeDirectoryDialog(
                MainActivity.this,
                getCurrentDirCommander().getCurrentDirectoryPathname()
        );
        cdd.show();
        if(downloadUrlToPopulate.length > 0) {
            cdd.findViewById(R.id.httpUrlDownload).performClick();
            ((EditText)cdd.findViewById(R.id.httpUrlEditText)).setText(downloadUrlToPopulate[0]);
        }
    }

    public void showChangeDirectoryDialog(View v) {
        showChangeDirectoryDialog_();
    }

    public void toggleRootHelper(View v) {
        String h = usingRootHelperForLocal ? "standard" : "roothelper-enabled";
        AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
        bld.setTitle("Switch dir commander to "+h+" one?");
        bld.setIcon(usingRootHelperForLocal?R.drawable.xfiles_root_off:R.drawable.xfiles_root_on);
        bld.setNegativeButton(android.R.string.cancel, null);
        bld.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            // disabled, better user experience, to be tested
            // browserPagerAdapter.createStandardCommanders();
            if (usingRootHelperForLocal) { // switch to normal dircommander
                usingRootHelperForLocal = false;
                fileOperationHelperSwitcher.setImageResource(R.drawable.xfiles_root_off);
            }
            else { // switch to roothelper-based dircommander
                getRootHelperClient();
                usingRootHelperForLocal = true;
                fileOperationHelperSwitcher.setImageResource(R.drawable.xfiles_root_on);
            }
        });
        AlertDialog alertDialog = bld.create();
        alertDialog.show();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        getWindow().getDecorView().setSystemUiVisibility(horizontalVisibility);
        if(isTablet || hasPermanentMenuKey || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return;
        }
        /**
         * Web source:
         * https://stackoverflow.com/questions/54140793/how-to-fix-navigation-bar-icons-still-showing-when-pop-up-menu-is-opened-ful
         * Still ugly as hack (navbar appears and disappears rapidly) but at least it works
         */
        else if(EffectActivity.currentlyOnFocus instanceof MainActivity) {
            // When PopupMenu appears, the current Activity looses the focus;
            // hijack to the current peek view, apply the Flags on it
            try {
                Class wmgClass = Class.forName("android.view.WindowManagerGlobal");
                Object wmgInstance = wmgClass.getMethod("getInstance").invoke(null);
                Field viewsField = wmgClass.getDeclaredField("mViews");
                viewsField.setAccessible(true);

                List<View> views = (List<View>) viewsField.get(wmgInstance);
                views.get(views.size()-1).setSystemUiVisibility(horizontalVisibility);
//                v.setOnSystemUiVisibilityChangeListener(i->{v.setSystemUiVisibility(i);});
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void simulateHomePress(Activity activity) {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(startMain);
    }

    public void installApk(File file) {
        if(file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(ContentProviderUtils.getUriFromFile(getApplicationContext(), file), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error in opening the file as APK for installation", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "APK for installation not existing or not accessible", Toast.LENGTH_SHORT).show();
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

    public void testItems(ProviderType providerType) {
        Intent startIntent = new Intent(this, TestService.class);
        startIntent.setAction(BaseBackgroundService.START_ACTION);
        startIntent.putExtra("params",
                providerType == ProviderType.LOCAL_WITHIN_ARCHIVE ?
                        new TestParams(Collections.singletonList(getCurrentDirCommander().getCurrentDirectoryPathname()),
                                null,getCurrentBrowserAdapter().getSelectedItemsAsNameOnlyStrings()) :
                        new TestParams(getCurrentBrowserAdapter().getSelectedItemsAsPathContents(), null, null));
        startService(startIntent);
    }

    public void shareItems(boolean unattended) {
        List<BasePathContent> selection = getCurrentBrowserAdapter().getSelectedItemsAsPathContents();
        if (selection.size()==0) {
            Toast.makeText(this,"No items selected for sharing",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent sharingIntent = unattended?
                new Intent(this,XREDirectShareActivity.class):new Intent();
        sharingIntent.setAction(Intent.ACTION_SEND_MULTIPLE);

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
        sharingIntent.putExtra("unattended",unattended);
        startActivity(unattended?sharingIntent:Intent.createChooser(sharingIntent, "Share files using"));
    }

    // with singleSelection = true, basePath is the only folder in which to perform search
    // otherwise, basePath is the parent dir, and we have to retrieve current selection (and concat the folder names to basePath)
    public void findInFolders(BasePathContent basePath, boolean singleSelection) {
        ArrayList<BasePathContent> localFolders = new ArrayList<>();
        if(singleSelection) {
            switch(basePath.providerType) {
                case LOCAL:
                case LOCAL_WITHIN_ARCHIVE:
                    localFolders.add(basePath);
                    break;
                default:
                    localFolders.add(new LocalPathContent(Misc.internalStorageDir.getPath()));
                    break;
            }
        }
        else {
            List<BrowserItem> selection = getCurrentBrowserAdapter().getSelectedItems();
            if(selection.size() == 0) {
                Toast.makeText(this, "No selected folders to search in", Toast.LENGTH_SHORT).show();
                return;
            }
            for(BrowserItem item: selection) {
                if(!(item.isDirectory)) {
                    Toast.makeText(this, "Selection to search in contains files", Toast.LENGTH_SHORT).show();
                    return;
                }
                localFolders.add(basePath.concat(item.filename));
            }
        }
        Intent intent = new Intent(MainActivity.this, FindActivity.class);
        intent.putExtra("paths", localFolders);
        startActivity(intent);
    }

    public void paste(BasePathContent bpc) {
        final BasePathContent destPath = bpc==null?getCurrentDirCommander().getCurrentDirectoryPathname():bpc;

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
            if (copyMoveList.parentDir.concat(fn.filename).isParentOrSameOf(destPath)) {
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
////                            Log.d("COPYTASK","sleeping... "+k);
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
        }
        // SFTP to SFTP file transfer
        else if (copyMoveList.parentDir.providerType == ProviderType.SFTP &&
                destPath.providerType == ProviderType.SFTP) {
            // remote transfer on the same remote host
            if (((SFTPPathContent)copyMoveList.parentDir).authData.equals(((SFTPPathContent)destPath).authData)) {
                // move (rename) on the remote host
                if (copyMoveList.copyOrMove == CopyMoveMode.MOVE) {
                    try {
                        sftpProvider.copyMoveFilesToDirectory(copyMoveList,destPath);
                        copyMoveList = null;
                        browserPagerAdapter.showDirContent(getCurrentDirCommander().refresh(),browserPager.getCurrentItem(),null);
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
        }
        else if (copyMoveList.parentDir.providerType == ProviderType.LOCAL_WITHIN_ARCHIVE &&
                destPath.providerType == ProviderType.LOCAL) {
            if(copyMoveList.copyOrMove==CopyMoveMode.MOVE)
                Toast.makeText(MainActivity.this,
                        "Warning: extracting files from archive in response to cut/paste command", Toast.LENGTH_SHORT).show();
            Intent startIntent = new Intent(MainActivity.this, ExtractService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra(
                    "params",
                    new ExtractParams(
                            Collections.singletonList(copyMoveList.parentDir),
                            destPath,
                            null,
                            copyMoveList.asNameOnlyStrings(),
                            false
                    ));
            startService(startIntent);
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

    void renameSelection() {
        List<BrowserItem> selection = getCurrentBrowserAdapter().getSelectedItems();
        if (selection.size()==0) {
            Toast.makeText(this,"No items selected for rename",Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> a = new ArrayList<>();
        for(BrowserItem item: selection)
            a.add(item.filename);
        BulkRenameDialog.createAndShow(this, getCurrentDirCommander().getCurrentDirectoryPathname(),a);
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
        startActivity(new Intent(MainActivity.this,ChecksumActivity.class));
    }

    private final int[] goDirOpsStatuses = new int[]{View.VISIBLE,View.GONE};

    // false when starting, true after end
    public void toggleGoDirOpsIndeterminateProgress(boolean status) {
        progressCircleForGoDirOps.setVisibility(status?goDirOpsStatuses[1]:goDirOpsStatuses[0]);
    }

    void upOneLevel() {
        BasePathContent parentFile = getCurrentDirCommander().getCurrentDirectoryPathname().getParent();
        if (parentFile == null) {
            Toast.makeText(this,"Already on root path for the current filesystem",Toast.LENGTH_SHORT).show();
            return;
        }
        goDir_async(parentFile,null);
    }

    public void goDirOrArchive(LocalPathContent path) {
        final int targetViewPagerPosition = browserPager.getCurrentItem();
        BasePathContent path_ = (getRootHelperClient().isDir(path))? path: new ArchivePathContent(path.dir,"");
        GenericDirWithContent gdwc = goDir_inner(path_);
        completeGoDir(gdwc,path_,targetViewPagerPosition,null);
    }

    /**
     * @param targetFilenameToHighlight Target filename to be highlighted and centered in the listview (in case of Locate command from {@link FindActivity})
     */
    public FileOpsErrorCodes goDir(Object dirOrDirection, int targetViewPagerPosition, @Nullable String targetFilenameToHighlight, Runnable... onCompletion) {
        GenericDirWithContent gdwc = goDir_inner(dirOrDirection);
        completeGoDir(gdwc,dirOrDirection,targetViewPagerPosition,targetFilenameToHighlight,onCompletion);
        return gdwc.errorCode;
    }

    public void goDir_async(Object dirOrDirection, @Nullable String targetFilenameToHighlight) {
        Future<FileOpsErrorCodes> ff = browserPagerAdapter.goDirExecutors[browserPager.getCurrentItem()].submit(() -> goDir(
                dirOrDirection,
                browserPager.getCurrentItem(),
                targetFilenameToHighlight,
                () -> toggleGoDirOpsIndeterminateProgress(true)));
        if(ff == null) return;
        handler.postDelayed(() -> {
            if (!ff.isDone())
                toggleGoDirOpsIndeterminateProgress(false);
        },250);
    }

    /**
     * @param dirOrDirection Target path to be loaded, or direction as boolean (back or ahead)
     */
    public GenericDirWithContent goDir_inner(Object dirOrDirection) {
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
        else return new GenericDirWithContent(FileOpsErrorCodes.ILLEGAL_ARGUMENT);

        return dwc;
    }

    // this part can be submitted to UI
    public void completeGoDir(GenericDirWithContent dwc, Object dirOrDirection, int position, @Nullable String targetFilenameToHighlight, Runnable... onCompletion) {
        runOnUiThread(()->{
            if(dwc.errorCode != null && dwc.errorCode != FileOpsErrorCodes.OK) {
                switch(dwc.errorCode) {
                    case ILLEGAL_ARGUMENT:
                        showToast("Invalid object type for dir browsing");
                        break;
                    case NULL_OR_WRONG_PASSWORD:
                        new AskPasswordDialogOnListing(
                                MainActivity.this,
                                (BasePathContent) dirOrDirection // tested, no classCastException on go back/ahead into an archive
                        ).show();
                        break;
                    case HOST_KEY_INEXISTENT_ERROR:
                        new SSHNotInKnownHostsDialog(
                                MainActivity.this,
                                ((SftpDirWithContent)dwc).authData,
                                InteractiveHostKeyVerifier.currentHostKey,
                                MainActivity.sftpProvider,
                                new SFTPPathContent(
                                        ((SftpDirWithContent)dwc).authData,
                                        ((SftpDirWithContent)dwc).pendingLsPath)).show();
                        break;
                    case HOST_KEY_CHANGED_ERROR:
                        new SSHAlreadyInKnownHostsDialog(
                                MainActivity.this,
                                ((SftpDirWithContent)dwc).authData,
                                InteractiveHostKeyVerifier.oldHostEntry,
                                InteractiveHostKeyVerifier.currentHostKey,
                                MainActivity.sftpProvider,
                                new SFTPPathContent(
                                        ((SftpDirWithContent)dwc).authData,
                                        ((SftpDirWithContent)dwc).pendingLsPath)).show();
                        break;
                    default:
                        Toast.makeText(MainActivity.this, dwc.errorCode.getValue(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
            else browserPagerAdapter.showDirContent(dwc,position,targetFilenameToHighlight);

            if(onCompletion.length > 0) onCompletion[0].run();
        });
    }

    public void showPopup(AdapterView<?> parent, View v, int position1, long id) {
        Context wrapper = new ContextThemeWrapper(this, R.style.popupMenuStyle);
        PopupMenu mypopupmenu = new PopupMenu(wrapper, v);
        setForceShowIcon(mypopupmenu);

        MenuInflater inflater = mypopupmenu.getMenuInflater();
        Menu menu = mypopupmenu.getMenu();

        if(v.getId()==R.id.newFileButton) {
            inflater.inflate(R.menu.menu_new, menu);
        }
        else if (getCurrentBrowserAdapter().getSelectedCount() == 0) { // long-click on single file, without active selection
            BrowserItem b = getCurrentBrowserAdapter().getItem(position1);
            switch(getCurrentDirCommander().getCurrentDirectoryPathname().providerType) {
                case LOCAL:
                    inflater.inflate(R.menu.menu_single, menu);
                    if(b.isDirectory) inflater.inflate(R.menu.menu_single_local_folder,menu);
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
                    if(b.isDirectory) inflater.inflate(R.menu.menu_single_remote_folder,menu);
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

////        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
////                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
////
////        getWindow().getDecorView().setSystemUiVisibility(horizontalVisibility);

        mypopupmenu.show();

////        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
////
////        //Update the WindowManager with the new attributes (no nicer way I know of to do this)..
////        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
////        wm.updateViewLayout(getWindow().getDecorView(), getWindow().getAttributes());

//        mypopupmenu.getMenu().getItem(0).setIcon(getResources().getDrawable(R.mipmap.ic_launcher));
        mypopupmenu.setOnMenuItemClickListener(item -> {
            BrowserItem b;
            File currentFile;
            List<BasePathContent> selection;
            BasePathContent path = getCurrentDirCommander().getCurrentDirectoryPathname();
            int itemId = item.getItemId();
            switch (itemId) {
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
                case R.id.itemsTest:
                    if (path.providerType != ProviderType.LOCAL_WITHIN_ARCHIVE && path.providerType != ProviderType.LOCAL) {
                        Toast.makeText(MainActivity.this,"Cannot extract/test multiple items if they are on remote filesystems",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if(itemId == R.id.itemsExtract) extractItems();
                    else testItems(path.providerType);
                    return true;
                case R.id.itemsDelete:
                    deleteSelection();
                    return true;
                case R.id.itemsRename:
                    renameSelection();
                    return true;
                case R.id.itemsFind:
                    findInFolders(path, false);
                    return true;
                case R.id.itemsShare:
                    shareItems(false);
                    return true;
                case R.id.itemsXreShareUnattended:
                    shareItems(true);
                    return true;
                case R.id.itemsShowInGallery:
                    // TODO consider also the case when image viewer is invoked by third party app - use MainActivity.mainActivity
                    BasePathContent currentDir = getCurrentDirCommander().getCurrentDirectoryPathname();
                    ArrayList<String> imageList = MediaGalleryActivity.filterByImageExtensionsOnSelection(currentDir,getCurrentBrowserAdapter().getSelectedItems());
                    MediaGallery.Builder(MainActivity.this,imageList)
                            .title("Media Gallery")
                            .backgroundColor(R.color.white)
                            .placeHolder(R.drawable.media_gallery_placeholder)
                            .selectedImagePosition(MediaGalleryActivity.targetIdx)
                            .show();
                    return true;
                case R.id.itemsProperties:
                    getStats();
                    return true;

                // single-selection menu
                case R.id.itemOpenAs:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    if (path.providerType != ProviderType.LOCAL) {
                        Toast.makeText(MainActivity.this,"Open not implemented for non-local or within-archive paths",Toast.LENGTH_LONG).show();
                        return true;
                    }

                    currentFile = new File(path.dir, b.filename);
                    showOpenAsList(currentFile);
                    return true;
                case R.id.itemCopy:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    copyMoveList = new CopyMoveListPathContent(b, CopyMoveMode.COPY, path);
                    Toast.makeText(MainActivity.this, "Copy item " + b.filename, Toast.LENGTH_LONG).show();
                    return true;
                case R.id.itemMove:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    copyMoveList = new CopyMoveListPathContent(b, CopyMoveMode.MOVE, path);
                    Toast.makeText(MainActivity.this, "Move item " + b.filename, Toast.LENGTH_LONG).show();
                    return true;
                case R.id.itemPasteIntoFolder:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    paste(path.concat(b.filename));
                    return true;
                case R.id.itemFind:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    findInFolders(path.concat(b.filename), true);
                    return true;
                case R.id.itemCreateLink:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    new CreateLinkDialog(MainActivity.this, path.concat(b.filename), b.isDirectory?FileMode.DIRECTORY:FileMode.FILE).show();
                    return true;
                case R.id.itemCompress:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    Intent i = new Intent(MainActivity.this,CompressActivity.class);
                    i.putExtra("filename", b);
                    startActivity(i);
                    return true;
                case R.id.itemExtract:
                case R.id.itemTest:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    if(b.isDirectory && path.providerType != ProviderType.LOCAL_WITHIN_ARCHIVE) {
                        Toast.makeText(MainActivity.this, "Cannot extract/test files from a directory, please select a compressed archive", Toast.LENGTH_LONG).show();
                        return true;
                    }
                    if(itemId == R.id.itemTest) {
                        Intent startIntent = new Intent(this, TestService.class);
                        startIntent.setAction(BaseBackgroundService.START_ACTION);
                        startIntent.putExtra(
                                "params",
                                new TestParams(Collections.singletonList(path.concat(b.filename)),null,null));
                        startService(startIntent);
                    }
                    else extractItem(b);
                    return true;
                case R.id.itemDelete:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    selection = Collections.singletonList(path.concat(b.filename));
                    showDeleteDialog(selection);
                    return true;
                case R.id.itemRename:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    AbsListView lv = getCurrentMainBrowserView();
                    if(lv instanceof ListView)
                        RenameDialog.toggleFastRename(this, position1,
                            path.concat(b.filename),true);
                    else
                        new RenameDialog(MainActivity.this, path.concat(b.filename)).show();
                    return true;
                case R.id.itemChecksum:
                    if (path.providerType != ProviderType.LOCAL &&
                            path.providerType != ProviderType.XFILES_REMOTE) {
                        Toast.makeText(MainActivity.this,"Checksum implemented only for local and XFiles remote files",Toast.LENGTH_LONG).show();
                        return true;
                    }
                    Intent intent = new Intent(MainActivity.this, ChecksumActivity.class);
                    intent.putExtra("browseritem", getCurrentBrowserAdapter().getItem(position1));

                    startActivity(intent);
                    return true;
                case R.id.itemShare:
                case R.id.itemXreShareUnattended:
                    boolean unattended = itemId == R.id.itemXreShareUnattended;
                    b = getCurrentBrowserAdapter().getItem(position1);
                    Intent sharingIntent = unattended?new Intent(this,XREDirectShareActivity.class):new Intent();
                    sharingIntent.setAction(Intent.ACTION_SEND);
                    Uri sharingUri = Uri.fromFile(new File(path.dir, b.filename));
//                Uri sharingUri = FileProvider.getUriForFile(MainActivity.this,
//                        BuildConfig.APPLICATION_ID + ".provider",
//                        new File(path.dir, b.filename));
                    sharingIntent.setType("*/*");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, sharingUri);
                    sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sharingIntent.putExtra("unattended",unattended);
                    startActivity(unattended?sharingIntent:Intent.createChooser(sharingIntent, "Share file using"));
                    return true;
                case R.id.itemShareX0at:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    if(b.isDirectory) {
                        Toast.makeText(MainActivity.this, "Selected file is a directory", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    String srcPath = (path.concat(b.filename)).dir;
                    // build alert dialog, three options: x0.at, 0x0.st, dismiss
                    AlertDialog.Builder bld = new AlertDialog.Builder(MainActivity.this);
                    bld.setMessage("You are about to upload this file:\n"+b.filename+"\nto a public internet service");
                    DialogInterface.OnClickListener l = (d,w) -> {
                        String domain = w==Dialog.BUTTON_POSITIVE ? "x0.at" : "0x0.st";
                        Intent uploadIntent = new Intent(MainActivity.this, HTTPUploadService.class);
                        uploadIntent.setAction(BaseBackgroundService.START_ACTION);
                        uploadIntent.putExtra("params",new DownloadParams(domain, srcPath, null));
                        startService(uploadIntent);
                    };
                    bld.setPositiveButton("Upload to x0.at", l);
                    bld.setNegativeButton("Upload to 0x0.st", l);
                    bld.setNeutralButton("Cancel", null);
                    bld.create().show();
                    return true;
                case R.id.itemShowInGallery:
                    // TODO consider also the case when image viewer is invoked by third party app - use MainActivity.mainActivity
                    currentDir = getCurrentDirCommander().getCurrentDirectoryPathname();
                    if(!(currentDir instanceof LocalPathContent)) {
                        Toast.makeText(MainActivity.this, "Cannot preview images in a non-local directory", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    b = getCurrentBrowserAdapter().getItem(position1);
                    if(b.filename.length()>=4 &&
                            MediaGalleryActivity.allowedImageExtensions.contains(b.filename.substring(b.filename.length()-4).toLowerCase())) {
                        imageList = MediaGalleryActivity.filterByImageExtensionsAndSaveTargetIdx(currentDir,b.filename);
                        MediaGallery.Builder(MainActivity.this,imageList)
                                .title("Media Gallery")
                                .backgroundColor(R.color.white)
                                .placeHolder(R.drawable.media_gallery_placeholder)
                                .selectedImagePosition(Math.max(MediaGalleryActivity.targetIdx, 0))
                                .show();
                    }
                    else Toast.makeText(MainActivity.this, "This file doesn't seem to be an image", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.itemShareOverHTTP:
                case R.id.itemShareOverFTP:
                case R.id.itemShareOverXRE:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    path = path.concat(b.filename);
                    new RemoteRHServerManagementDialog(MainActivity.this);

                    if(itemId==R.id.itemShareOverXRE) {
                        ((EditText)RemoteRHServerManagementDialog.instance.findViewById(R.id.xreHomePath)).setText(path.dir);
                        RemoteRHServerManagementDialog.instance.show();
                        if(RemoteServerManager.rhssManagerRef.get() != null) {
                            Toast.makeText(MainActivity.this, "XRE server is already active, please stop it before sharing a new directory", Toast.LENGTH_LONG).show();
                            return true;
                        }
                        RemoteRHServerManagementDialog.instance.findViewById(R.id.rhss_toggle_rhss_button).performClick();
                    }
                    else {
                        ((EditText)RemoteRHServerManagementDialog.instance.findViewById(R.id.ftpHttpRootPath)).setText(path.dir);
                        RemoteRHServerManagementDialog.instance.show();
                        // autostart HTTP/FTP server
                        FileServer fileServer = FileServer.fromMenuRes(itemId);
                        if(fileServer.server.isAlive()) {
                            Toast.makeText(MainActivity.this, fileServer.name()+" server is already running, please stop it before sharing a new directory", Toast.LENGTH_LONG).show();
                            return true;
                        }
                        RemoteRHServerManagementDialog.instance.findViewById(fileServer.buttonId).performClick();
                    }
                    return true;
                case R.id.itemProperties:
                    b = getCurrentBrowserAdapter().getItem(position1);
                    getStats(b);
                    return true;
                case R.id.createNewFile:
                case R.id.createNewFileAdvanced:
                case R.id.createNewDirectory:
                    FileMode fileMode = itemId == R.id.createNewDirectory ? FileMode.DIRECTORY : FileMode.FILE;
                    if(browserPagerAdapter.browserViewModes[browserPager.getCurrentItem()]==BrowserViewMode.GRID ||
                            itemId == R.id.createNewFileAdvanced)
                        new CreateFileOrDirectoryDialog(MainActivity.this,fileMode,itemId == R.id.createNewFileAdvanced,"").show();
                    else
                        CreateFileOrDirectoryDialog.toggleFastCreateMode(MainActivity.this, fileMode, true);
                    return true;
                default:
                    return true;
            }
        });
    }

    private void setForceShowIcon(PopupMenu popupMenu) {
        try {
            Field[] mFields = popupMenu.getClass().getDeclaredFields();
            for (Field field : mFields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> popupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method mMethods = popupHelper.getMethod("setForceShowIcon", boolean.class);
                    mMethods.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
                if (RemoteServerManager.rhssManagerRef.get() == null)
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
                        if (RemoteServerManager.rhssManagerRef.get() == null)
                            killRHWrapper();
                        break;
                    case SMB_TRANSFER:
                        if (sftpProvider != null) sftpProvider.closeAllSessions();
                        break;
                }
            }
        }

        usingRootHelperForLocal = false;
//        context = null; // FIXME may cause NPE? better to leave it non-null and check null-check usages
        mainActivity = null;
    }

    boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        // TODO maybe should handle mode exit differently (not both at once)
        // TODO decide if it is needed to restore original adapter content on quick find mode exit
        int pos = browserPager.getCurrentItem();

        if(browserPagerAdapter.fastRenameModeViews[pos] != null) {
            RenameDialog.resetRenameMode(pos,browserPagerAdapter.fastRenameModeViews);
            return;
        }

        if(browserPagerAdapter.browserAdapters[pos].fastCreateModeHeaderView != null) {
            CreateFileOrDirectoryDialog.resetCreateMode(
                    browserPagerAdapter.browserAdapters[pos],
                    browserPagerAdapter.mainBrowserViews[pos]);
            return;
        }

        if (browserPagerAdapter.multiSelectModes[pos] ||
                browserPagerAdapter.quickFindModes[pos])
        {
            if (browserPagerAdapter.multiSelectModes[pos]) browserPagerAdapter.switchMultiSelectMode(pos);
            if (browserPagerAdapter.quickFindModes[pos]) browserPagerAdapter.switchQuickFindMode(pos);
            return;
        }

        if (doubleBackToExitPressedOnce) {

            // check if there is any remote server active and, in case, show dialog
            if((RemoteServerManager.rhssManagerRef.get() != null) ||
                    FileServer.FTP.server.isAlive() ||
                    FileServer.HTTP.server.isAlive()) {
                new CloseActiveServersDialog(this).show();
            }
            else {
                super.onBackPressed();
            }
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        handler.postDelayed(() -> doubleBackToExitPressedOnce=false, 2000);
    }

    boolean isTablet;
    public void setOperationButtonsLayout() {
        LinearLayout operationButtonsLayout = findViewById(R.id.operationButtonsLayout);
        operationButtonsLayout.removeAllViews();

        ViewPager vp = new ViewPager(this);
        int[] l1 = new int[]{R.layout.overriding_home_buttons_operational_layout, R.layout.standard_operational_layout};
        int[] l2 = new int[]{R.layout.standard_operational_layout};
        int[] l3 = new int[]{R.layout.horizontal_operational_layout};

        int[] targetLayout;

        boolean isHorizontal = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        if(isHorizontal || (isTablet && !hasPermanentMenuKey)) {
            // no need for switching layouts when one has all the possible buttons available
            targetLayout = l3;
        }
        else { // vertical mode AND (smartphone OR (tablet with physical buttons))
            if(isTablet) {
                // (tablet with physical buttons) in vertical mode, show all buttons except Back and Home
                targetLayout = l2;
            }
            else {
                if (hasPermanentMenuKey) targetLayout = l2;
                else targetLayout = l1;
            }
        }

        if(targetLayout == l1) { // more than one layout, needs ViewPager
            vp.setAdapter(new OperationalPagerAdapter(this, targetLayout));
            operationButtonsLayout.addView(vp);
            // MainActivity.makeImageButtonsStateful called in instantiateItem, calling it here would produce an NPE
        }
        else {
            ViewGroup layout = (ViewGroup) layoutInflater.inflate(targetLayout[0], operationButtonsLayout, false);
            operationButtonsLayout.addView(layout);
            MainActivity.makeImageButtonsStateful(layout,
                    new SelectImageButtonListener(context, R.color.imagebuttonselect));
        }
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

    public void operationBarOnClick(View v) {
        switch (v.getId()) {
            case R.id.androidGoBackButton:
                onBackPressed();break;
            case R.id.androidGoHomeButton:
                simulateHomePress(this);break;

            case R.id.upOneLevelButton:
                upOneLevel();break;
            case R.id.pasteButton:
                paste(null);break;

            case R.id.goBackButton:
                goDir_async(Boolean.TRUE,null);break;
            case R.id.goAheadButton:
                goDir_async(Boolean.FALSE,null);break;

            case R.id.newFileButton: // context menu with both file and dir options
                showPopup(null,v,0,v.getId());
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
                shareItems(false);break;

            default: break;
        }
    }
}
