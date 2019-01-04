package it.pgp.xfiles.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 26/10/16 (converted inner class to standalone one)
 */
public class PropertiesDialog extends Dialog {

    private TextView childrenFiles,childrenFolders,totalFiles,totalFolders,totalSize;
    private SingleStatsItem stats;
    private folderStats_resp statsFolderOrMultipleItems;

    private LinearLayout singleItemLayout;
    private LinearLayout aggregatesLayout;

    // Constructor for showing properties of a single file or a single folder
    // TODO maybe it's better to inherit a FolderPropertiesDialog from this
    public PropertiesDialog(final Context context, FileMode fileMode, List<BasePathContent> pathname_) {
        super(context);
        setContentView(R.layout.properties_file_dialog);
        singleItemLayout = findViewById(R.id.propertiesSingleItemLayout);
        aggregatesLayout = findViewById(R.id.propertiesAggregatesLayout);

        if (pathname_.size() != 1) {
            setTitle("Multiple items properties");
            singleItemLayout.setVisibility(View.GONE);
        }
        else {
            if (fileMode==FileMode.DIRECTORY) {
                setTitle("Directory properties"); // do not hide any sub-layout
            }
            else {
                setTitle("File properties");
                aggregatesLayout.setVisibility(View.GONE); // hide aggregates layout
            }
        }

        TextView pathname;
        TextView size;
        TextView created;
        TextView modified;
        TextView lastAccessed;
        TextView permissions;
        TextView owner;
        TextView group;

        FileMode fm_ = fileMode == null ? FileMode.DIRECTORY : fileMode;
        switch (fm_) {
            case DIRECTORY:
                childrenFiles = findViewById(R.id.propertiesChildrenFilesTextView);
                childrenFolders = findViewById(R.id.propertiesChildrenFoldersTextView);
                totalFiles = findViewById(R.id.propertiesTotalFilesTextView);
                totalFolders = findViewById(R.id.propertiesTotalFoldersTextView);
                totalSize = findViewById(R.id.propertiesTotalSizeTextView);
                // no break here, file layout elements are in common
            case FILE:
                pathname = findViewById(R.id.propertiesFileNameTextView);
//                TextView type = findViewById(R.id.propertiesFileTypeTextView);
                size = findViewById(R.id.propertiesFileSizeTextView);
                created = findViewById(R.id.propertiesFileDateCreatedTextView);
                modified = findViewById(R.id.propertiesFileDateLastModifiedTextView);
                lastAccessed = findViewById(R.id.propertiesFileDateLastAccessTextView);
                permissions = findViewById(R.id.propertiesPermissionsTextView);
                owner = findViewById(R.id.propertiesOwnerTextView);
                group = findViewById(R.id.propertiesGroupTextView);
                Button closeButton = findViewById(R.id.propertiesDismissButton);
                closeButton.setOnClickListener(view -> dismiss());
                break;
            default:
                throw new RuntimeException("Unknown operation mode, only dir and file allowed");
        }

        try {
            if (pathname_.size() != 1) { // ignore filemode, stats multiple items
                statsFolderOrMultipleItems = MainActivity.currentHelper.statFiles(pathname_);
            }
            else {
                stats = MainActivity.currentHelper.statFile(pathname_.get(0));
                if (fileMode == FileMode.DIRECTORY) {
                    statsFolderOrMultipleItems = MainActivity.currentHelper.statFolder(pathname_.get(0));
                }

                pathname.setText(pathname_.get(0).toString());
                // TODO type setText MIME type
                size.setText(""+stats.size);
                created.setText(""+stats.creationTime);
                modified.setText(""+stats.modificationTime);
                lastAccessed.setText(""+stats.lastAccessTime);
                permissions.setText(stats.permissions);
                owner.setText(stats.owner);
                group.setText(stats.group);
            }

            if (pathname_.size() != 1 || fileMode == FileMode.DIRECTORY) {
                childrenFiles.setText(""+ statsFolderOrMultipleItems.childrenFiles);
                childrenFolders.setText(""+ statsFolderOrMultipleItems.childrenDirs);
                totalFiles.setText(""+ statsFolderOrMultipleItems.totalFiles);
                totalFolders.setText(""+ statsFolderOrMultipleItems.totalDirs);
                totalSize.setText(""+ statsFolderOrMultipleItems.totalSize);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Generic stats error", Toast.LENGTH_SHORT).show();
        } // TODO refine error msgs from commented code below

        /*// @@@
        try {
            stats = MainActivity.currentHelper.statFile(pathname_);
        } catch (IOException e) {
            if (MainActivity.currentHelper instanceof XFilesUtilsUsingPathContent) {
                Toast.makeText(context,"It seems that Java implementation of file stats is only in Java NIO Files (not included in Android); however, reverting to roothelper failed (cannot start roothelper or connection error)",Toast.LENGTH_LONG).show();
            }

            Toast.makeText(context,"Unable to stat file",Toast.LENGTH_SHORT).show();
            cancel(); // TODO dismiss/cancel not working inside constructor, need to try catch from activity
            return;
        }

        // @@@
        if (fileMode == FileMode.DIRECTORY) {
            try {
                statsFolderOrMultipleItems = MainActivity.currentHelper.statFolder(pathname_);
            } catch (IOException e) {
                Toast.makeText(context, "Unable to stat folder", Toast.LENGTH_SHORT).show();
                cancel();
                return;
            }
        }

        if (stats == null) {
            Toast.makeText(context, "Unable to stat file", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fileMode == FileMode.DIRECTORY) {
            if (statsFolderOrMultipleItems == null) {
                Toast.makeText(context, "Unable to stat folder", Toast.LENGTH_SHORT).show();
                return;
            }
        }*/
    }
}
