package it.pgp.xfiles;

import android.widget.Checkable;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.pgp.xfiles.roothelperclient.resps.ls_resp;

/**
 * Created by pgp on 26/09/16
 */

public class BrowserItem implements Serializable, Checkable, Comparable<BrowserItem> {
    public Boolean isDirectory;
    public Boolean isLink;
    protected String filename;
    public Long size;
    public Date date;
    private boolean checked = false;

    public BrowserItem(String filename, long size, Date date, Boolean isDirectory, Boolean isLink) {
        this.filename = filename;
        this.size = size;
        this.date = date;
        this.isDirectory = isDirectory;
        this.isLink = isLink;
    }

    // build from roothelper ls response
    public BrowserItem(ls_resp resp) {
        filename = new String(resp.filename); // new String(resp.filename,"UTF-8")
        size = resp.size;
        date = new Date(resp.date*1000);
        isDirectory = resp.permissions[0]=='d' || resp.permissions[0]=='L'; // new String(resp.permissions, "UTF-8").charAt(0) == 'd')
        isLink = resp.permissions[0]=='l' || resp.permissions[0]=='L';
    }

    // build item from vMap node properties
    // a directory entry may not be stored explicitly in an archive, so attributes may not exists in the vmap
    // for simplicity, then, assume isDir true when isDir not present, and set default values for other attributes
    public BrowserItem(String name, Map<String,?> nodeProperties) {
        this.filename = name;
        if (nodeProperties == null) {
            this.isDirectory = true;
            this.size = 0L;
            this.date = new Date(0);
            this.isLink = false; // assume no directory softlink in archive
            this.checked = false;
        }
        else {
            this.size = nodeProperties.containsKey("size")?(Long)nodeProperties.get("size"):0;
            this.date = nodeProperties.containsKey("date")?(Date)nodeProperties.get("date"):new Date(0);
            this.isDirectory = nodeProperties.containsKey("isDir")?(Boolean)nodeProperties.get("isDir"):true;
            this.isLink = nodeProperties.containsKey("isLink")?(Boolean)nodeProperties.get("isLink"):false;
            this.checked = false;
        }
    }

    public String getFileExt() {
        String extension = "";
        int i = filename.lastIndexOf('.');
        if (i > 0) extension = filename.substring(i+1);
        return extension;
    }

    public boolean hasExt() {
        int i = filename.lastIndexOf('.');
        return (i>0);
    }

    @Override
    public boolean isChecked() {
        return checked;
    }

    @Override
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public void toggle() {
        checked = !checked;
    }

    @Override
    public int compareTo(BrowserItem o) {
        return filename.compareTo(o.filename); // sort by filename attribute
    }

    @Override
    public String toString() {
        return filename+"\t"+size+"\t"+date+"\t"+isDirectory+"\t"+isLink;
    }

    public String getFilename() {
        return filename;
    }
}
