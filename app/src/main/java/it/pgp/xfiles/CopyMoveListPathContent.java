package it.pgp.xfiles;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 15/05/17
 * Stores parent dir as BasePathContent, and items as BrowserItem (size and attributes known in advance)
 */

public class CopyMoveListPathContent implements Serializable,Iterable<String> {
    public List<BrowserItem> files; // only filenames, to be concatenated with parentDir (full path of parent folder)
    public CopyMoveMode copyOrMove;
    public BasePathContent parentDir;

    // multiple selection
    public CopyMoveListPathContent(BrowserAdapter ba, CopyMoveMode copyOrMove, BasePathContent parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = new ArrayList<>();

        for(int i=0; i<ba.getCount(); i++) {
            BrowserItem b = ba.getItem(i);
            if (b != null && b.isChecked()) this.files.add(b);
        }
    }

    // single-file
    public CopyMoveListPathContent(BrowserItem b, CopyMoveMode copyOrMove, BasePathContent parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = Collections.singletonList(b);
    }

    // multiple selection, for XREDirectShareActivity
    public CopyMoveListPathContent(List<BrowserItem> files, CopyMoveMode copyOrMove, BasePathContent parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = files;
    }

    @NonNull
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            final Iterator<BrowserItem> i = files.iterator();

            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public String next() {
                return parentDir+"/"+i.next().filename;
            }
        };
    }

    // iterator for SFTP download preliminary progress building: returns full remote paths and file type (file or dir)
    public Iterable<Map.Entry<String,Boolean>> getSFTPProgressHelperIterable() {
        return () -> new Iterator<Map.Entry<String, Boolean>>() {
            final Iterator<BrowserItem> i = files.iterator();
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }
            @Override
            public Map.Entry<String, Boolean> next() {
                BrowserItem b = i.next();
                return new AbstractMap.SimpleEntry<>(
                        parentDir.dir + "/" + b.filename, b.isDirectory);
            }
        };
    }
}
