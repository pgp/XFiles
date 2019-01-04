package it.pgp.xfiles.utils.legacy;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.pgp.xfiles.roothelperclient.FirstRunAssetsExtract;

/**
 * Created by pgp on 01/02/17
 */

@Deprecated
public class FirstRunAssetsExtractOld {
    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static final String rootHelperName = "r";
    public static final String _7zLibName = "7z.so";

    public static void copyFromAssetsToFilesDir(final Context context, String archSubfolder, String... filenames) {
        AssetManager assetManager = context.getAssets();
        File filesDir = context.getFilesDir();

        for (String filename : filenames) {

            File outFile = new File(filesDir, filename);
            try {
                InputStream inputStream;
                try {
                    inputStream = assetManager.open(archSubfolder + "/" + filename);
                } catch (IOException i) {
                    Log.e(FirstRunAssetsExtract.class.getName(), "*************file not found in assets*************");
                    return;
                }

                if (outFile.exists()) {
                    Log.e(FirstRunAssetsExtract.class.getName(), "*************output file already exists*************");
                    return;
                }

                OutputStream outputStream = new FileOutputStream(outFile);
                copyFile(inputStream, outputStream);
                outputStream.close();
                inputStream.close();
                Log.e(FirstRunAssetsExtract.class.getName(), "*************file copied from assets to dir*************");

                // make it executable
//            RootHandler.executeCommandSimple("/system/bin/chmod 744 "+outFile.getAbsolutePath(),false); // not working
                if (!outFile.setExecutable(true, true)) {
                    Log.e(FirstRunAssetsExtract.class.getName(), "Unable to set executable");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void copyFilesFromAssetsByArch(Context context) {
        String abi;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            abi = Build.CPU_ABI;
        } else {
            abi = Build.SUPPORTED_ABIS[0];
        }
//        Toast.makeText(context,abi,Toast.LENGTH_SHORT).show();

        switch (abi) {
//            case "armeabi":
            // map to existing subfolders in assets folder
            case "armeabi-v7a":
            case "arm64-v8a":
            case "mips":
            case "mips64":
            case "x86":
            case "x86_64":
                copyFromAssetsToFilesDir(context,abi,rootHelperName,_7zLibName);
                break;
            default:
                Toast.makeText(context,"Unsupported architecture "+abi,Toast.LENGTH_SHORT).show();
                throw new RuntimeException("Unsupported architecture "+abi);
        }
    }
}
