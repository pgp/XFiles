package it.pgp.xfiles.utils.legacy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.enums.CopyMoveMode;

/**
 * Created by pgp on 27/09/16
 * Last modified on 7/11/16
 */

@Deprecated
public class CopyMoveList {
    public List<File> files;
    public CopyMoveMode copyOrMove;
    public String parentDir;

    // multiple selection
    public CopyMoveList(BrowserAdapter ba, CopyMoveMode copyOrMove, String parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = new ArrayList<>();

        for(int i=0 ; i<ba.getCount() ; i++) {
            BrowserItem b = ba.getItem(i);
            if (b.isChecked()) {
                File f = new File(this.parentDir,b.getFilename());
                this.files.add(f);
            }
        }
    }

    // single-file
    public CopyMoveList(BrowserItem b, CopyMoveMode copyOrMove, String parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = new ArrayList<>();
        File f = new File(this.parentDir,b.getFilename());
        this.files.add(f);
    }
}
