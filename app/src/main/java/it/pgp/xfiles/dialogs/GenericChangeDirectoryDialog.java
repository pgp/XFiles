package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

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
import it.pgp.xfiles.utils.legacy.ChangeDirectoryDialog;
import it.pgp.xfiles.utils.pathcontent.ArchivePathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;
import it.pgp.xfiles.utils.pathcontent.RemotePathContent;
import it.pgp.xfiles.utils.pathcontent.SmbRemotePathContent;
import it.pgp.xfiles.utils.pathcontent.XFilesRemotePathContent;
import it.pgp.xfiles.utils.wifi.WifiButtonsLayout;

/**
 * Created by pgp on 14/05/17 (adapted from {@link ChangeDirectoryDialog})
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
    Spinner localStoredData;

    // sftp remote dir
    Spinner storedUsers;
    EditText user;
    EditText domain;
    EditText port;
    EditText password;
    AutoCompleteTextView remotePath;

    FavoritesList<AuthData>[] credsWithFavs;

    // xfiles remote dir
    Spinner xreStoredData;
    EditText xreServerHost;
//    EditText xreServerPort;
    EditText xreRemotePath;
    Map.Entry<String,String>[] xreItems;

    // smb remote dir
    Spinner smbStoredUsers;
    EditText smbUser;
    EditText smbDomain;
    EditText smbHost;
    EditText smbPort;
    EditText smbPassword;
    AutoCompleteTextView smbRemotePath;

    FavoritesList<SmbAuthData>[] smbCredsWithFavs;

    // http download params
    EditText httpUrlToDownload;
    EditText httpDestPath; // if empty, take currently shown path in browser view
    EditText httpTargetFilename;

    private boolean currentDirAutofillOverride = true;

    private void ok(View unused) {
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
                 accordingly. Otherwise, a RemotePathContent object needs explicitly a home directory
                 */
                    if (!basicNonEmptyValidation(user,domain,port)) return; // password can be empty
                    path = new RemotePathContent(
                            new AuthData(
                                    user.getText().toString(),
                                    domain.getText().toString(),
                                    Integer.valueOf(port.getText().toString()),
                                    password.getText().toString()
                            ),
                            remotePath.getText().toString()
                    );
                    break;
                case 3: // XFILES_REMOTE
                    if (!basicNonEmptyValidation(xreServerHost)) return;
                    path = new XFilesRemotePathContent(
                            xreServerHost.getText().toString(),
//                        Integer.valueOf(xreServerPort.getText().toString()),
                            xreRemotePath.getText().toString()
                    );
                    break;
                case 4: // SMB
                    if (!basicNonEmptyValidation(smbUser,smbDomain,smbHost,smbPort,smbPassword)) return; // password cannot be empty (no pubkey authentication for SMB)
                    path = new SmbRemotePathContent(
                            new SmbAuthData(
                                    smbUser.getText().toString(),
                                    smbDomain.getText().toString(),
                                    smbHost.getText().toString(),
                                    Integer.valueOf(smbPort.getText().toString()),
                                    smbPassword.getText().toString()
                            ),
                            smbRemotePath.getText().toString()
                    );
                    break;

            }
            FileOpsErrorCodes ret = mainActivity.goDir(path);
            if (ret == FileOpsErrorCodes.OK ||
                    ret == FileOpsErrorCodes.NULL_OR_WRONG_PASSWORD) dismiss();
        }
        else if (idx == 5) {
            // start download service
            DownloadParams params = new DownloadParams(
                    httpUrlToDownload.getText().toString(),
                    httpDestPath.getText().toString(),
                    httpTargetFilename.getText().toString());

            Intent startIntent = new Intent(mainActivity,HTTPDownloadService.class);
            startIntent.setAction(BaseBackgroundService.START_ACTION);
            startIntent.putExtra("params",params);
            mainActivity.startService(startIntent);
            dismiss();
        }
        else Toast.makeText(mainActivity, "Unexpected selector index", Toast.LENGTH_SHORT).show();
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
                localStoredData = findViewById(R.id.localStoredDataSpinner);
                break;
            case LOCAL_WITHIN_ARCHIVE:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_archive, null);
                containerLayout.addView(targetLayout);
                localPath = findViewById(R.id.archivePathnameEditText);
                localStoredData = findViewById(R.id.archivePathnameStoredDataSpinner);
                inArchivePath = findViewById(R.id.archiveSubDirEditText);
                break;
            case SFTP:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_sftp, null);
                containerLayout.addView(targetLayout);
                storedUsers = findViewById(R.id.connectionStoredUsersSpinner);
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
                xreStoredData = findViewById(R.id.xreConnectionStoredDataSpinner);
                xreServerHost = findViewById(R.id.xreConnectionDomainEditText);
//                xreServerPort = findViewById(R.id.xreConnectionPortEditText);
                xreRemotePath = findViewById(R.id.xreRemoteDirEditText);
                break;
            case SMB:
                targetLayout = layoutInflater.inflate(R.layout.change_directory_dialog_frame_smb, null);
                containerLayout.addView(targetLayout);
                smbStoredUsers = findViewById(R.id.connectionStoredUsersSpinner);
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
                httpUrlToDownload.setText("http://");
                break;
            default:
                throw new RuntimeException("Unknown subtype layout");
        }



        switch(providerType) {
            case LOCAL:
            case LOCAL_WITHIN_ARCHIVE:
                // autocomplete stuff
                ArrayList<String> lItems = new ArrayList<>();
                lItems.add(""); // empty item for no selection
                lItems.add("/sdcard"); // empty item for no selection
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
                        if (currentDirAutofillOverride) {
                            currentDirAutofillOverride = false;
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
                credsWithFavs = new FavoritesList[cwf.size()];
                cwf.toArray(credsWithFavs);

                // add empty spinner for no selection
                items.add("");
                for (FavoritesList<AuthData> awf : credsWithFavs) items.add(awf.a.toString());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_dropdown_item, items);
                storedUsers.setAdapter(adapter);
                //////////////////////////////
                storedUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDirAutofillOverride) {
                            currentDirAutofillOverride = false;
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
                        String[] autoCompleteSupport;
                        if (paths != null) {
                            autoCompleteSupport = new String[paths.size()];
                            paths.toArray(autoCompleteSupport);
                        }
                        else {
                            autoCompleteSupport = new String[0];
                        }

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
                ArrayList<Map.Entry<String,String>> xreitems_ = new ArrayList<>();
                xreitems_.add(new AbstractMap.SimpleEntry<>("","")); // empty item for no selection
                xreitems_.add(new AbstractMap.SimpleEntry<>("192.168.43.1","/sdcard")); // default remote server and path when server provides network access with its WiFi hotspot
                xreitems_.addAll(dbh.getAllRowsOfXreFavoritesTable().values());
                xreItems = new Map.Entry[xreitems_.size()];
                xreitems_.toArray(xreItems);

                ArrayAdapter<Map.Entry<String,String>> xreAdapter = new ArrayAdapter<>(
                        mainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        xreItems);
                xreStoredData.setAdapter(xreAdapter);
                xreStoredData.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDirAutofillOverride) {
                            currentDirAutofillOverride = false;
                            return;
                        }

                        Map.Entry<String,String> item = (Map.Entry<String, String>) parent.getItemAtPosition(position);
                        xreServerHost.setText(item.getKey());
                        xreRemotePath.setText(item.getValue());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                break;
            case SMB:
                items = new ArrayList<>();

//                cwf = dbh.getAllSmbCredsWithFavs().values();
                cwf = dbh.getAllCredsWithFavs(SmbAuthData.ref).values();
                smbCredsWithFavs = new FavoritesList[cwf.size()]; // TODO replace with return value from toArray of size 0
                cwf.toArray(smbCredsWithFavs);

                // add empty spinner for no selection
                items.add("");
                for (FavoritesList<SmbAuthData> awf : smbCredsWithFavs) items.add(awf.a.toString());
                adapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_dropdown_item, items);
                smbStoredUsers.setAdapter(adapter);
                //////////////////////////////
                smbStoredUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (currentDirAutofillOverride) {
                            currentDirAutofillOverride = false;
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
                        String[] autoCompleteSupport;
                        if (paths != null) {
                            autoCompleteSupport = new String[paths.size()];
                            paths.toArray(autoCompleteSupport);
                        }
                        else {
                            autoCompleteSupport = new String[0];
                        }

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
                    RemotePathContent rpc = (RemotePathContent)currentDir[0];
                    if (rpc.authData != null) {
                        user.setText(rpc.authData.username==null?"":rpc.authData.username);
                        // ignore setText of password, which may not be present, AuthData hashcode ignores it, so on login attempt, if any credential is present, it will work anyway
                        domain.setText(rpc.authData.domain==null?"":rpc.authData.domain);
                        port.setText(rpc.authData.port==0?"":rpc.authData.port+"");
                    }
                    remotePath.setText(rpc.dir);
                    break;
                case XFILES_REMOTE:
                    XFilesRemotePathContent xrpc = (XFilesRemotePathContent)currentDir[0];
                    xreServerHost.setText(xrpc.serverHost);
                    xreRemotePath.setText(xrpc.dir);
                    break;
                case SMB:
                    SmbRemotePathContent srpc = (SmbRemotePathContent) currentDir[0];
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

    }

    public GenericChangeDirectoryDialog(MainActivity mainActivity, BasePathContent curDirPath) {
        super(mainActivity);
        MainActivity.cdd = this;
        this.mainActivity = mainActivity;
        this.curDirPath = curDirPath;
        dbh = new GenericDBHelper(mainActivity);
        layoutInflater = (LayoutInflater) mainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        setTitle("Change directory");
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
        });
    }

    private boolean basicNonEmptyValidation(EditText... fields) {
        boolean valid = true;
        for (EditText field : fields) {
            valid &= (field != null) && !(field.getText().toString().equals(""));
        }
        if (!valid) Toast.makeText(mainActivity, "Invalid parameters", Toast.LENGTH_SHORT).show();
        return valid;
    }

}
