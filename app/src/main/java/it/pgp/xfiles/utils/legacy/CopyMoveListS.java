package it.pgp.xfiles.utils.legacy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.enums.CopyMoveMode;
import it.pgp.xfiles.enums.ProviderType;

/**
 * Created by pgp on 2/2/17
 * Adapted from CopyMoveList, stores path strings instead of File objects
 * Last modified on 17/02/17
 */
@Deprecated
public class CopyMoveListS implements Iterable<String> {
    public List<String> files; // only filenames, to be concatenated with parentDir (full path of parent folder)
    public CopyMoveMode copyOrMove;
    public String parentDir;
    public ProviderType providerType;

    // multiple selection
    public CopyMoveListS(BrowserAdapter ba, CopyMoveMode copyOrMove, String parentDir, ProviderType providerType) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.providerType = providerType;
        this.files = new ArrayList<>();

        for(int i=0 ; i<ba.getCount() ; i++) {
            BrowserItem b = ba.getItem(i);
            if (b.isChecked()) {
                this.files.add(b.getFilename());
            }
        }
    }

    // single-file
    public CopyMoveListS(BrowserItem b, CopyMoveMode copyOrMove, String parentDir, ProviderType providerType) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.providerType = providerType;
        this.files = new ArrayList<>();
        this.files.add(b.getFilename());
    }

    private class CopyMoveListIterator implements Iterator<String> {

        Iterator<String> i;
        String pa;
        public CopyMoveListIterator() {
            pa = parentDir;
            i = files.iterator();
        }

        @Override
        public boolean hasNext() {
            return i.hasNext();
        }

        @Override
        public String next() {
            return parentDir+"/"+i.next();
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new CopyMoveListIterator();
    }
}
