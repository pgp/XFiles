package com.tomclaw.imageloader.util.loader;

import android.content.ContentResolver;
import android.net.Uri;

import com.tomclaw.imageloader.core.Loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import it.pgp.xfiles.utils.Misc;

public class ContentLoader implements Loader {

    private final ContentResolver contentResolver;

    public ContentLoader(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    @Override
    public List<String> getSchemes() {
        return Collections.singletonList("content");
    }

    @Override
    public boolean load(String uriString, File file) {
        Uri uri = Uri.parse(uriString);
        try(InputStream input = contentResolver.openInputStream(uri);
            OutputStream output = new FileOutputStream(file)) {
            Misc.pipe(input,output);
            return true;
        }
        catch(IOException e) {
            return false;
        }
    }
}
