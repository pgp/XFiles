package it.pgp.xfiles.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.enums.ArchiveType;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 26/09/16
 */

public abstract class BrowserAdapter extends ArrayAdapter<BrowserItem> {

    public static Bitmap dirIV,fileIV,linkIV;
    public static final Map<String,Bitmap> archiveIcons = new HashMap<>();

    public static Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    private static void loadArchiveIcons(Context context) {
        if(archiveIcons.isEmpty()) {
            for(ArchiveType a : ArchiveType.values()) {
                if (a == ArchiveType.RAR5 || a == ArchiveType.UNKNOWN) continue;
                archiveIcons.put(a.s,BitmapFactory.decodeResource(context.getResources(), a.resId));
            }
        }
    }

    public static Bitmap getBitmapByExtension(BrowserItem item) {
        Bitmap target;
        if (item.isDirectory)
            target = dirIV;
        else {
            String ext = item.getFileExt().toLowerCase();
            if (ArchiveType.formats.contains(ext)) target = archiveIcons.get(ext);
            else target = fileIV;
        }
        if(item.isLink)
            target = overlay(target,linkIV);

        return target;
    }

    protected final MainActivity mainActivity;
    protected LayoutInflater inflater;
    protected int containerLayout; // to be assigned in subclasses constructors
    public List<BrowserItem> objects,currentObjects;
    // "objects" (full objects) as reference list, and currentObjects for quick find currently shown results

    BrowserAdapter(MainActivity mainActivity, List<BrowserItem> objects) {
        super(mainActivity, android.R.layout.simple_list_item_1, objects);
        this.mainActivity = mainActivity;
        this.objects = objects;
        this.currentObjects = objects;
        inflater = LayoutInflater.from(mainActivity);

        if (dirIV == null) dirIV = BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.xf_dir_blu);
        if (fileIV == null) fileIV = BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.xfiles_file_icon);
        if (linkIV == null) linkIV = BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.xfiles_link_icon);
        loadArchiveIcons(mainActivity);
    }

    @Override
    public long getItemId(int position) {
        return position; //return position here
    }

    @Override
    public BrowserItem getItem(int position) {
        return currentObjects.get(position);
    }

    @Override
    public int getCount() {
        return currentObjects.size();
    }

    @Override
    public boolean areAllItemsEnabled() {
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            if (!b.isChecked()) return false;
        }
        return true;
    }

    public int getSelectedCount() {
        int selectedCount=0;
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            if (b.isChecked()) selectedCount++;
        }
        return selectedCount;
    }

    // remove return value when checksum directory protocol will be implemented
    public boolean ensureOnlyFilesSelection() {
        for (BrowserItem b : objects) {
            if (b.isChecked()) {
                if (b.isDirectory) return false;
            }
        }
        return true;
    }

//    public List<String> getSelectedItemsAsStrings() {
//        List<String> selectedItems = new ArrayList<>();
//        for(int i=0; i<getCount() ; i++) {
//            BrowserItem b = getItem(i);
//            if (b.isChecked()) {
//                String f = activity.getCurrentDirCommander().getCurrentDirectoryPathname().dir+"/"+b.filename;
//                selectedItems.add(f);
//            }
//        }
//        return selectedItems;
//    }

    public List<BrowserItem> getSelectedItems() {
        List<BrowserItem> selectedItems = new ArrayList<>();
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            if (b.isChecked()) {
                selectedItems.add(b);
            }
        }
        return selectedItems;
    }

    public List<String> getSelectedItemsAsNameOnlyStrings() {
        List<String> selectedItems = new ArrayList<>();
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            if (b.isChecked()) {
                selectedItems.add(b.getFilename());
            }
        }
        return selectedItems;
    }

    public List<BasePathContent> getSelectedItemsAsPathContents() {
        List<BasePathContent> selectedFiles = new ArrayList<>();
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            if (b.isChecked()) {
                BasePathContent f = MainActivity.mainActivity.getCurrentDirCommander().getCurrentDirectoryPathname().concat(b.getFilename());
                selectedFiles.add(f);
            }
        }
        return selectedFiles;
    }


    public void toggleSelectOne(BrowserItem b) {
        b.toggle();
        notifyDataSetChanged();
    }

    // method used for one-click select all / deselect all switching
//    public void selectAllDeselectAllSwitch() {
//        if (areAllItemsEnabled()) selectNone();
//        else selectAll();
//    }

    public void selectAll() {
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            b.setChecked(true);
        }
        notifyDataSetChanged();
    }

    public void selectNone() {
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            b.setChecked(false);
        }
        notifyDataSetChanged();
    }

    public void invertSelection() {
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            b.toggle();
        }
        notifyDataSetChanged();
    }

    // from RAR UI
    public void filterSelection(String content, boolean selectOrDeselect) {
        for(int i=0; i<getCount() ; i++) {
            BrowserItem b = getItem(i);
            if (b.getFilename().contains(content)) {
                b.setChecked(selectOrDeselect);
            }
        }
        notifyDataSetChanged();
    }

    public void filterObjects(CharSequence content, boolean ignoreCase) {
        if (content.equals("")) {
            // no filter, revert to full list of objects
            currentObjects = objects;
        }
        else {
            currentObjects = new ArrayList<>();
            // TODO to be replaced with recursive filtering (on currentObjects)
            if (ignoreCase) {
                for (BrowserItem b : objects) {
                    if (b.getFilename().toLowerCase().contains(content.toString().toLowerCase())) currentObjects.add(b);
                }
            }
            else {
                for (BrowserItem b : objects) {
                    if (b.getFilename().contains(content)) currentObjects.add(b);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * @param filename filename to get the position
     * @return position in the adapter of the located file
     * @apiNote To be used only from {@link it.pgp.xfiles.FindActivity}
     */
    public int findPositionByFilename(String filename) {
        int locatedPosition = 0;
        String[] splitted = filename.split("/"); // from find, contains whole path
        if (splitted.length == 0) return -1;
        String filenameOnly = splitted[splitted.length-1];
        for (BrowserItem b : objects) { // from main browser adapter, they contain filename only
            if (filenameOnly.equals(b.getFilename())) return locatedPosition;
            locatedPosition++;
        }
        return -1;
    }

}
