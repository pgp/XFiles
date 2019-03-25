package it.pgp.xfiles;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class CopyListUris extends CopyMoveListPathContent {

    public List<String> contentUris;

    public CopyListUris(List<String> contentUris) {
        super();
        this.contentUris = contentUris;
    }

    public static CopyListUris getFromUriList(List<Uri> uris) {
        List<String> urisAsStrings = new ArrayList<>();
        for (Uri uri : uris) urisAsStrings.add(uri.toString());
        return new CopyListUris(urisAsStrings);
    }
}
