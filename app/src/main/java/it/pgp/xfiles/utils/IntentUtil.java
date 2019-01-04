package it.pgp.xfiles.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import it.pgp.xfiles.utils.pathcontent.BasePathContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

public class IntentUtil {

    public static List<Uri> getShareSelectionFromIntent(Intent intent) {
        List<Uri> imageUris;
        Uri singleUri;

        singleUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (singleUri == null)
            imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        else imageUris = Collections.singletonList(singleUri);

        return imageUris;
    }

    public static Map.Entry<BasePathContent,List<String>> getCommonAncestorAndItems(Context context, List<Uri> uris) {
        List<String> selectedItems = new ArrayList<>();
        for (Uri uri : uris) {
            String tmp = PathUtil.getPath(context,uri);
            selectedItems.add(tmp);
            Log.e("getPath",uri.toString()+"\t"+tmp);
        }

        BasePathContent dirPath = new LocalPathContent(Misc.getLongestCommonPathFromPrefix(
                Misc.getLongestCommonPrefix(selectedItems)
        ));
        // trim prefixes from selectedItems once extracted dirPath
        for (int i=0;i<selectedItems.size();i++)
            selectedItems.set(i,selectedItems.get(i).substring(dirPath.dir.length()+1));

        return new AbstractMap.SimpleEntry<>(dirPath,selectedItems);
    }

}
