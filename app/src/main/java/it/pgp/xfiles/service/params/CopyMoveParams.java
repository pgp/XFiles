package it.pgp.xfiles.service.params;

import android.support.annotation.NonNull;

import java.io.Serializable;

import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.utils.pathcontent.BasePathContent;

/**
 * Created by pgp on 23/06/17
 */

public class CopyMoveParams implements Serializable {
    public CopyMoveListPathContent list;
    public BasePathContent destPath;

    public CopyMoveParams(@NonNull CopyMoveListPathContent list, @NonNull BasePathContent destPath) {
        this.list = list;
        this.destPath = destPath;
    }
}
