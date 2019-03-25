package it.pgp.xfiles.service.params;

import android.content.ContentResolver;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.Serializable;

import it.pgp.xfiles.CopyListUris;
import it.pgp.xfiles.CopyMoveListPathContent;
import it.pgp.xfiles.utils.ContentProviderUtils;
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

    public String getFirstFilename(ContentResolver resolver) {
        if (list instanceof CopyListUris)
            return ContentProviderUtils.getName(resolver,
                    Uri.parse(((CopyListUris)list).contentUris.get(0)));
        else
            return list.files.get(0).getFilename();
    }
}
