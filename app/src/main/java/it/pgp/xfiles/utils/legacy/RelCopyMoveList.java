package it.pgp.xfiles.utils.legacy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.adapters.BrowserAdapter;
import it.pgp.xfiles.enums.CopyMoveMode;

/**
 * Created by pgp on 18/11/16
 * CopyMoveList storing parent dir file and relative file paths
 */

@Deprecated
public class RelCopyMoveList {
    public List<String> files;
    public CopyMoveMode copyOrMove;
    File parentDir;

    // multiple selection
    public RelCopyMoveList(BrowserAdapter ba, CopyMoveMode copyOrMove, File parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = new ArrayList<>();

        for(int i=0 ; i<ba.getCount() ; i++) {
            BrowserItem b = ba.getItem(i);
            if (b.isChecked()) {
                this.files.add(b.getFilename()); // on first level, only one file name without slashes
            }
        }
    }

    // single-file
    public RelCopyMoveList(BrowserItem b, CopyMoveMode copyOrMove, File parentDir) {
        this.copyOrMove = copyOrMove;
        this.parentDir = parentDir;
        this.files = new ArrayList<>();
        this.files.add(b.getFilename());
    }

}
