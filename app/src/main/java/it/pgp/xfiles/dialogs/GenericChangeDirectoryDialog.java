package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileOpsErrorCodes;
import it.pgp.xfiles.enums.ProviderType;
import it.pgp.xfiles.service.BaseBackgroundService;
import it.pgp.xfiles.service.HTTPDownloadService;
import it.pgp.xfiles.service.params.DownloadParams;
import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.smbclient.SmbAuthData;
import it.pgp.xfiles.utils.FavoritesList;
import it.pgp.xfiles.utils.GenericDBHelper;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.NetworkUtils;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.SFTPPathContent;
import it.pgp.xfiles.utils.pathcontent.SMBPathContent;
import it.pgp.xfiles.utils.pathcontent.XREPathContent;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;
import it.pgp.xfiles.viewmodels.PasteableEditText;
import it.pgp.xfiles.viewmodels.XREDirectoryViewModel;

/**
 * Created by pgp on 14/05/17
 */
public class GenericChangeDirectoryDialog extends Dialog {
    private MainActivity mainActivity;
    LayoutInflater layoutInflater;
    BasePathContent curDirPath;

    final GenericDBHelper dbh;

    // parent layout controls

    RadioGroup pathContentTypeSelector;
    Button okButton;
    LinearLayout containerLayout; // target container for inflating content for different pathContent types

    final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);

    // aggregated controls mapped to specialized PathContent classes

    // in-archive dir
    EditText inArchivePath;

    // local path common to local and in-archive modes
    AutoCompleteTextView localPath;
    Spinner localStoredData; // same resource label for all path types

    // sftp remote dir
    EditText user;
    EditText domain;
    EditText port;
    EditText password;
    AutoCompleteTextView remotePath;

    FavoritesList<AuthData>[] credsWithFavs;

    final XREDirectoryViewModel xreDirectoryViewModel;

    // smb remote dir
    EditText smbUser;
    EditText smbDomain;
    EditText smbHost;
    EditText smbPort;
    EditText smbPassword;
    AutoCompleteTextView smbRemotePath;

    FavoritesList<SmbAuthData>[] smbCredsWithFavs;

    // http download params
    PasteableEditText httpUrlToDownload;
    EditText httpDestPath; // if empty, take currently shown path in browser view
    EditText httpTargetFilename;
    CheckedTextView httpForceHttps; // -> httpsOnly

    private final AtomicBoolean currentDirAutofillOverride = new AtomicBoolean(true);

    // on Android 11, /data/data is not accessible even with root
    final String datadataPath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ? "/data_mirror/data_ce/null/0" : "/data/data";

    private void ok(View unused) {
        okButton.setEnabled(false);
        okButton.setText("Loading...");
        new Thread(this::ok_inner).start();
    }

    // dummy int return, just to avoid writing two lines of code for early returns in invocations of this method
    private int reenableOkButton(Object ret) { // ret is a String in all calls of this method
        mainActivity.runOnUiThread(()->{
            Toast.makeText(mainActivity, ret.toString(), Toast.LENGTH_SHORT).show();
            okButton.setEnabled(true);
            okButton.setText(android.R.string.ok);
        });
        return 0;
    }

    private int ok_inner() {
        BasePathContent path = null;

        // see which pathContent type is currently selected
        int idx = pathContentTypeSelector.indexOfChild(
                pathContentTypeSelector.findViewById(
                        pathContentTypeSelector.getCheckedRadioButtonId()));

        // empty base path means root path (/), so don't validate it
        if (idx < 5) {
            switch (idx) {
                case 0: // LOCAL
                    path = new LocalPathContent(localPath.getText().toString());
                    break;
                case 1: // ARCHIVE
                    path = new ArchivePathContent(
                            localPath.getText().toString(),
                            inArchivePath.getText().toString()
                    );
                    break;
                case 2: // SFTP
                /*
                 TODO call tryConnectAndGetPath, which returns the default remote home
                 (in order, on success, to perform listDir from SftpProvider and update dir commander)
                 and launches error dialogs (host key not found/not valid and auth error -> provide password)
                 accordingly. Otherwise, a SFTPPathContent object needs explicitly a home directory
                 */
                    String ret = XREDirectoryViewModel.basicNonEmptyValidation(user,domain,port);
                    if (!ret.isEmpty()) return reenableOkButton(ret); // password can be empty
                    path = new SFTPPathContent(
                            new AuthData(
                                    user.getText().toString(),
                                    domain.getText().toString(),
                                    Integer.parseInt(port.getText().toString()),
                                    password.getText().toString()
                            ),
                            remotePath.getText().toString()
                    );
                    break;
                case 3: // XFILES_REMOTE
                    ret = XREDirectoryViewModel.basicNonEmptyValidation(xreDirectoryViewModel.xreServerHost);
                    if (!ret.isEmpty()) return reenableOkButton(ret);
                    path = new XREPathContent(
                            xreDirectoryViewModel.xreServerHost.getText().toString(),
//                        Integer.valueOf(xreServerPort.getText().toString()),
                            xreDirectoryViewModel.xreRemotePath.getText().toString()
                    );
                    break;
                case 4: // SMB
                    ret = XREDirectoryViewModel.basicNonEmptyValidation(smbUser,smbDomain,smbHost,smbPort,smbPassword);
                    if (!ret.isEmpty()) return reenableOkButton(ret); // password cannot be empty (no pubkey authentication for SMB)
                    path = new SMBPathContent(
                            new SmbAuthData(
                                    smbUser.getText().toString(),
                                    smbDomain.getText().toString(),
                                    smbHost.getText().toString(),
                                    Integer.parseInt(smbPort.getText().toString()),
                                    smbPassword.getText().toString()
                            ),
                            smbRemotePath.getText().toString()
                    );
                    break;

            }
            final int targetViewPagerPosition = mainActivity.browserPager.getCurrentItem();
            GenericDirWithContent gdwc = mainActivity.goDir_inner(path);
            FileOpsErrorCodes fe = gdwc.errorCode;
            mainActivity.runOnUiThread(()-> {
                if (fe == null || fe == FileOpsErrorCodes.OK) dismiss();
                else reenableOkButton(fe);
            });
            mainActivity.completeGoDir(gdwc,path,targetViewPagerPosition,null);
        }
        else if (idx == 5) {
            // if we're not trying to download from a local url...
            boolean isLocalUrl = false;
            try {
                URI uri = new URI(httpUrlToDownload.getText().toString());
                String s = uri.getHost();
                isLocalUrl = s.startsWith("127.0.0.1") || s.startsWith("localhost");
            }
            catch (URISyntaxException e) {
                e.printStackTrace();
            }
            // ...check whether there's some connection enabled first
            if(!isLocalUrl && !(Misc.isDataConnectionEnabled(mainActivity) ||
                    Misc.isWifiEnabled(mainActivity) ||
                    Misc.isHotspotEnabled(mainActivity))) {
                mainActivity.runOnUiThread(() -> {
                    okButton.setEnabled(true);
                    okButton.setText(android.R.string.ok);
                    Toast.makeText(mainActivity, "Please enable some data connection in order to download", Toast.LENGTH_SHORT).show();
                });
                return 0;
            }

            // start download service
            Intent startIntent = new Intent(mainActivity,HTTPDownloadService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra("params",
                    new DownloadParams(
                            httpUrlToDownload.getText().toString(),
                            httpDestPath.getText().toString(),
                            httpTargetFilename.getText().toString(),
                            httpForceHttps.isChecked()
                            ));
            mainActivity.startService(startIntent);
            dismiss();
        }
        else throw new RuntimeException("Unexpected selector index");

        return 0;
    }

    public void setLayout(ProviderType providerType, BasePathContent... currentDir) {
        // inflate specialized layouts
        containerLayout = findViewById(R.id.pathContentTypeContainerLayout);
        containerLayout.removeAllViews();
        containerLayout.setLayoutParams(layoutParams);

        View targetLayout;

        switch(providerType) {
            case LOCAL:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_local, null);
                containerLayout.addView(targetLayout);
                localPath = findViewById(R.id.localDirEditText);
                localStoredData = findViewById(R.id.storedDataSpinner);
                break;
            case LOCAL_WITHIN_ARCHIVE:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_archive, null);
                containerLayout.addView(targetLayout);
                localPath = findViewById(R.id.archivePathnameEditText);
                localStoredData = findViewById(R.id.storedDataSpinner);
                inArchivePath = findViewById(R.id.archiveSubDirEditText);
                break;
            case SFTP:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_sftp, null);
                containerLayout.addView(targetLayout);
                localStoredData = findViewById(R.id.storedDataSpinner);
                user = findViewById(R.id.connectionUserEditText);
                password = findViewById(R.id.connectionPasswordEditText);
                domain = findViewById(R.id.connectionDomainEditText);
                port = findViewById(R.id.connectionPortEditText);
                port.setText(R.string.ssh_default_port);
                remotePath = findViewById(R.id.remoteDirEditText);
                break;
            case XFILES_REMOTE:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_xre, null);
                containerLayout.addView(targetLayout);
                xreDirectoryViewModel.initViews();
                break;
            case SMB:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_smb, null);
                containerLayout.addView(targetLayout);
                localStoredData = findViewById(R.id.storedDataSpinner);
                smbUser = findViewById(R.id.connectionUserEditText);
                smbPassword = findViewById(R.id.connectionPasswordEditText);
                smbDomain = findViewById(R.id.connectionDomainEditText);
                smbHost = findViewById(R.id.connectionHostEditText);
                smbPort = findViewById(R.id.connectionPortEditText);
                smbPort.setText(R.string.smb_default_port);
                smbRemotePath = findViewById(R.id.remoteDirEditText);
                break;
            case URL_DOWNLOAD:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_http, null);
                containerLayout.addView(targetLayout);
                httpUrlToDownload = findViewById(R.id.httpUrlEditText);
                httpDestPath = findViewById(R.id.httpDestDirEditText);
                httpTargetFilename = findViewById(R.id.httpTargetFilenameEditText);
                httpUrlToDownload.setText("https://");
                httpForceHttps = findViewById(R.id.httpForceHttpsCheckbox);
                httpForceHttps.setOnClickListener(Misc.ctvListener);
                break;
            default:
                throw new RuntimeException("Unknown subtype layout");
        }

        switch(providerType) {
            case LOCAL:
            case LOCAL_WITHIN_ARCHIVE:
                // autocomplete stuff
                ArrayList<String> lItems = new ArrayList<>(Arrays.asList(
                        "", // empty item for no selection
                        Misc.internalStorageDir.getAbsolutePath(),
                        mainActivity.getFilesDir().getAbsolutePath(), // xfiles app private dir
                        "/data/app",
                        datadataPath,
                        "/data/local/tmp"));
                lItems.addAll(dbh.getAllRowsOfLocalFavoritesTable().values());
                ArrayAdapter<String> lAdapter = new ArrayAdapter<>(
                        mainActivity,
                        android.R.layout.select_dialog_item,
                        lItems);
                localPath.setAdapter(lAdapter);
                localPath.setOnClickListener(v -> localPath.showDropDown());
                ArrayAdapter<String> lAdapterForSpinner = new ArrayAdapter<>(
                        mainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        lItems);
                localStoredData.setAdapter(lAdapterForSpinner);
                localStoredData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDirAutofillOverride.get()) {
                            currentDirAutofillOverride.set(false);
                            return;
                        }
                        localPath.setText((String)parent.getItemAtPosition(position));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                break;
            case SFTP:
                // fill spinner from database
                ArrayList<String> items = new ArrayList<>();

//                Collection cwf = dbh.getAllSftpCredsWithFavs().values();
                Collection cwf = dbh.getAllCredsWithFavs(AuthData.ref).values();
                credsWithFavs = (FavoritesList<AuthData>[]) cwf.toArray(new FavoritesList[0]);

                // add empty spinner for no selection
                items.add("");
                for (FavoritesList<AuthData> awf : credsWithFavs) items.add(awf.a.toString());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_dropdown_item, items);
                localStoredData.setAdapter(adapter);
                //////////////////////////////
                localStoredData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDirAutofillOverride.get()) {
                            currentDirAutofillOverride.set(false);
                            return;
                        }

                        // query database with that (user,domain,port) tuple, get password, and fill fields
                        String item = (String) parent.getItemAtPosition(position);
                        if (item.equals("")) {
                            user.setText("");
                            password.setText("");
                            domain.setText("");
                            port.setText(R.string.ssh_default_port);
                            return;
                        }

//                        AuthData a = dbh.find(new AuthData(item));
//                        if (a == null) return;

                        // no db update expected from this dialog, use adapter position to access data
                        AuthData a = credsWithFavs[position-1].a; // pos-1 cause first item is empty item
                        user.setText(a.username);
                        if (a.password != null) password.setText(a.password);
                        domain.setText(a.domain);
                        port.setText(a.port+"");

                        // populate auto-complete list for remote path's AutoCompleteTextView
                        Collection paths = credsWithFavs[position-1].paths; // pos-1: idem as before
                        String[] autoCompleteSupport = (paths!=null)?(String[])paths.toArray(new String[0]):new String[0];

                        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                                mainActivity,
                                android.R.layout.select_dialog_item,
                                autoCompleteSupport);
                        remotePath.setAdapter(autoCompleteAdapter);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                remotePath.setThreshold(1); // auto-completing from first character
                remotePath.setOnClickListener(v -> remotePath.showDropDown());
                break;
            case XFILES_REMOTE:
                // initViews() already called in viewmodel
                break;
            case SMB:
                items = new ArrayList<>();

                cwf = dbh.getAllCredsWithFavs(SmbAuthData.ref).values();
                smbCredsWithFavs = (FavoritesList<SmbAuthData>[]) cwf.toArray(new FavoritesList[0]);

                // add empty spinner for no selection
                items.add("");
                for (FavoritesList<SmbAuthData> awf : smbCredsWithFavs) items.add(awf.a.toString());
                adapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_dropdown_item, items);
                localStoredData.setAdapter(adapter);
                //////////////////////////////
                localStoredData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDirAutofillOverride.get()) {
                            currentDirAutofillOverride.set(false);
                            return;
                        }

                        // query database with that (user,domain,port) tuple, get password, and fill fields
                        String item = (String) parent.getItemAtPosition(position);
                        if (item.equals("")) {
                            smbUser.setText("");
                            smbPassword.setText("");
                            smbDomain.setText(R.string.smb_default_domain);
                            smbHost.setText("");
                            smbPort.setText(R.string.smb_default_port);
                            return;
                        }

                        // no db update expected from this dialog, use adapter position to access data
                        SmbAuthData a = smbCredsWithFavs[position-1].a; // pos-1 cause first item is empty item
                        smbUser.setText(a.username);
                        if (a.password != null) smbPassword.setText(a.password);
                        smbDomain.setText(a.domain);
                        smbHost.setText(a.host);
                        smbPort.setText(a.port+"");

                        // populate auto-complete list for remote path's AutoCompleteTextView
                        Collection paths = smbCredsWithFavs[position-1].paths; // pos-1: idem as before
                        String[] autoCompleteSupport = (paths!=null)?(String[])paths.toArray(new String[0]):new String[0];

                        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(
                                mainActivity,
                                android.R.layout.select_dialog_item,
                                autoCompleteSupport);
                        smbRemotePath.setAdapter(autoCompleteAdapter);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
                smbRemotePath.setThreshold(1); // auto-completing from first character
                smbRemotePath.setOnClickListener(v -> smbRemotePath.showDropDown());
                break;
            // useless to insert throw branch here, already thrown if necessary in previous switch construct
        }

        // populate fields with current path content on dialog open
        if (currentDir.length > 0) {
            switch (currentDir[0].providerType) {
                case LOCAL:
                    localPath.setText(currentDir[0].dir);
                    break;
                case LOCAL_WITHIN_ARCHIVE:
                    ArchivePathContent apc = (ArchivePathContent)currentDir[0];
                    localPath.setText(apc.archivePath);
                    inArchivePath.setText(apc.dir);
                    break;
                case SFTP:
                    SFTPPathContent rpc = (SFTPPathContent)currentDir[0];
                    if (rpc.authData != null) {
                        user.setText(rpc.authData.username==null?"":rpc.authData.username);
                        // ignore setText of password, which may not be present, AuthData hashcode ignores it, so on login attempt, if any credential is present, it will work anyway
                        domain.setText(rpc.authData.domain==null?"":rpc.authData.domain);
                        port.setText(rpc.authData.port==0?"":rpc.authData.port+"");
                    }
                    remotePath.setText(rpc.dir);
                    break;
                case XFILES_REMOTE:
                    XREPathContent xrpc = (XREPathContent)currentDir[0];
                    xreDirectoryViewModel.xreServerHost.setText(xrpc.serverHost);
                    xreDirectoryViewModel.xreRemotePath.setText(xrpc.dir);
                    break;
                case SMB:
                    SMBPathContent srpc = (SMBPathContent) currentDir[0];
                    if (srpc.smbAuthData != null) {
                        user.setText(srpc.smbAuthData.username==null?"":srpc.smbAuthData.username);
                        // ignore setText of password, which may not be present, SmbAuthData hashcode ignores it, so on login attempt, if any credential is present, it will work anyway
                        smbDomain.setText(srpc.smbAuthData.domain==null?"":srpc.smbAuthData.domain);
                        smbHost.setText(srpc.smbAuthData.host==null?"":srpc.smbAuthData.host);
                        smbPort.setText(srpc.smbAuthData.port==0?"":srpc.smbAuthData.port+"");
                    }
                    smbRemotePath.setText(srpc.dir);
                    break;
            }
        }

        ((RadioButton)pathContentTypeSelector.getChildAt(providerType.ordinal())).setChecked(true);

        if(providerType == ProviderType.XFILES_REMOTE)
            NetworkUtils.startXreAnnounceListenerThread(MainActivity.mainActivity,xreDirectoryViewModel.xreAnnouncesAdapter);
        else NetworkUtils.shutdownMulticastListening();
    }

    public GenericChangeDirectoryDialog(MainActivity mainActivity, BasePathContent curDirPath) {
        super(mainActivity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        MainActivity.cdd = this;
        this.mainActivity = mainActivity;
        this.curDirPath = curDirPath;
        dbh = new GenericDBHelper(mainActivity);
        layoutInflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        xreDirectoryViewModel = new XREDirectoryViewModel(mainActivity, this, dbh, currentDirAutofillOverride);

        setContentView(R.layout.change_directory_generic_dialog);
        pathContentTypeSelector = findViewById(R.id.pathContentRadioGroup);
        setLayout(curDirPath.providerType,curDirPath);

        pathContentTypeSelector.setOnCheckedChangeListener((group, checkedId) -> {
            int idx = pathContentTypeSelector.indexOfChild(
                    pathContentTypeSelector.findViewById(
                            pathContentTypeSelector.getCheckedRadioButtonId()));

            setLayout(ProviderType.values()[idx]); // reverse ordinal
        });

        okButton = findViewById(R.id.changeDirOkButton);
        okButton.setOnClickListener(this::ok);

        WifiButtonsLayout wbl = new WifiButtonsLayout(mainActivity);
        LinearLayout target = findViewById(R.id.targetWifiButtonsLayout);
        target.addView(wbl);

        wbl.registerListeners();
        setOnDismissListener(dialog->{
            wbl.unregisterListeners();
            MainActivity.cdd = null;
            NetworkUtils.shutdownMulticastListening();
        });
    }

}
