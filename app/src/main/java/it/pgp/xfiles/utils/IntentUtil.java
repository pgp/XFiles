package it.pgp.xfiles.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import it.pgp.Native;
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
            String tmp = ContentProviderUtils.getPathFromUri(context,uri);
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

    public static List<String> retrievePathsFromUrisUsingProcFS(ContentResolver resolver, List<Uri> uris) throws IOException {
        List<String> paths = new ArrayList<>();

        for(Uri uri : uris) {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri,"r");
            String fdpath = Native.getPathFromFd(""+pfd.getFd());
            pfd.close();
            if (!fdpath.isEmpty()) paths.add(fdpath);
        }

        return paths;
    }

    public static Map.Entry<BasePathContent,List<String>> getCommonAncestorAndItems_mode2(Context context, List<Uri> uris) throws IOException {
        List<String> selectedItems = retrievePathsFromUrisUsingProcFS(context.getContentResolver(),uris);

        BasePathContent dirPath = new LocalPathContent(Misc.getLongestCommonPathFromPrefix(
                Misc.getLongestCommonPrefix(selectedItems)
        ));
        // trim prefixes from selectedItems once extracted dirPath
        for (int i=0;i<selectedItems.size();i++)
            selectedItems.set(i,selectedItems.get(i).substring(dirPath.dir.length()+1));

        return new AbstractMap.SimpleEntry<>(dirPath,selectedItems);
    }

}
