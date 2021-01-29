package it.pgp.xfiles.items;

import android.support.annotation.NonNull;

public class BulkRenameItem {
    public final String filename;
    public boolean duplicate;

    public BulkRenameItem(String filename) {
        this.filename = filename;
        this.duplicate = false;
    }

    @NonNull
    @Override
    public String toString() {
        return filename;
    }

    // equals implementation is needed, for hashCode to work as well
    @Override
    public boolean equals(Object obj) {
        if(obj instanceof BulkRenameItem)
            return filename.equals(((BulkRenameItem) obj).filename);
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return filename.hashCode();
    }
}

