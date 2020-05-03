package it.pgp.xfiles.roothelperclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Binder;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.locks.LockSupport;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.service.SocketNames;
import it.pgp.xfiles.utils.Misc;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

/**
 * Created by pgp on 28/09/16
 * Last modified on 07/01/18
 */

public class RootHandler {

    public static synchronized long getPidOfProcess(Process p) {
        long pid;

        try {
            // on Android: java.lang.ProcessManager$ProcessImpl
//            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
                return pid;
//            }
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isRootAvailableAndGranted = false;

    public static boolean isRooted() {
        return findBinary("su");
    }

    private static boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if ( new File( where + binaryName ).exists() ) {
                found = true;
                Log.d(RootHandler.class.getName(),"su binary found at "+where);
                break;
            }
        }
        return found;
    }

    private static Process executeCommandSimple(String command, File workingDir, boolean runAsSuperUser, String... args) throws IOException {
        String s = "";
        s += command;
        if (args != null) for (String arg : args) s += " " + arg;

        Process p;
        if (runAsSuperUser) {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream dos = new DataOutputStream(p.getOutputStream());
            if (workingDir != null) {
                dos.writeBytes("cd " + workingDir +"\n");
            }
            dos.writeBytes(s + "\n");
            dos.writeBytes("exit\n");
            dos.flush();
            dos.close();
        } else {
            p = (workingDir==null)?
                    Runtime.getRuntime().exec(s):
                    Runtime.getRuntime().exec(s,null,workingDir);
        }

//        lastStartedPid = getPidOfProcess(p);

        return p; // p started, not joined

        // exitValue to be called later
//        int exitValue = 0;
//        try {
//            exitValue = p.waitFor();
//        }
//        catch (InterruptedException ignored) {}

//        StringBuilder output = new StringBuilder();
//        // no console output expected from process
//        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
//
//        String line;
//        while ((line = reader.readLine()) != null) {
//            output.append(line).append("\n");
//        }
//
//        Log.d(RootHandler.class.getName(), "***BEGIN Parent process output:***\n" + output.toString() + "\n***END Parent process output***\nExit value: " + exitValue);
    }

    public static Process runRootHelper(Context context, boolean runAsSu, SocketNames socketName) throws IOException {
//        File workingDir = context.getFilesDir(); // old, taken from app private dir (files)
        File workingDir = new File(context.getApplicationInfo().nativeLibraryDir);

        File rootHelperExecutable = new File(workingDir,FirstRunAssetsExtract.rootHelperInstallName);
        if (socketName == null)
            return executeCommandSimple(rootHelperExecutable.getAbsolutePath(),workingDir,runAsSu,Binder.getCallingUid()+"");
        else
            return executeCommandSimple(rootHelperExecutable.getAbsolutePath(),workingDir,runAsSu,Binder.getCallingUid()+"",socketName.name());
    }

    public static Process runRootHelper(SocketNames socketName) throws IOException {
        return runRootHelper(MainActivity.mainActivityContext,isRooted(),socketName);
    }

    /**
     * Tries to start a new RH process on the given socket name,
     * then creates a new RHClient instance connected to that process
     * and checks connection by the two by sending a ping message.
     * If everything is ok, the RHClient instance is returned, otherwise null.
     */
    public static RootHelperClientUsingPathContent startAndGetRH(Context... context) {
        Context c = context.length > 0 ? context[0]:MainActivity.mainActivityContext;

        SocketNames socketName = SocketNames.theroothelper;
        RootHelperClientUsingPathContent rh = null;
        long pid; // communicated by RH server itself once successfully started

        for(boolean asRoot : new Boolean[]{true,false}) {
            try {
                Process p = runRootHelper(c,asRoot,socketName);

                rh = new RootHelperClientUsingPathContent(socketName);
                pid = rh.checkConnection();
                while (pid < 0) {
                    try {
                        p.exitValue();
                        // process exited prematurely, failed to start roothelper process in the given mode (when asRoot is false, should never happen)
                        rh = null;
                        break;
                    }
                    catch (IllegalThreadStateException e) {
                        Log.d(RootHandler.class.getName(),"Waiting for roothelper process to start...");
                        LockSupport.parkNanos(250000000); // sleep 250 ms
                    }
                    pid = rh.checkConnection();
                }

                if (rh != null) {
                    Log.d(RootHandler.class.getName(),"Started roothelper in "+(asRoot?"root":"normal")+" mode");
                    if(asRoot) {
                        isRootAvailableAndGranted = asRoot;
                        String rootModeStoragePath = getInternalStoragePathInRootMode(rh,c);
                        if(rootModeStoragePath != null) Misc.internalStorageDir = new File(rootModeStoragePath);
                        else MainActivity.showToastOnUIWithHandler("Unable to get a common internal storage path for both normal and root mode, RH integration may not work properly");
                    }

                    if (pid <= 0) Log.e(RootHandler.class.getName(),"Failed to get roothelper pid: "+pid);
                    break;
                }
            }
            catch (IOException e) {
                if(!asRoot) e.printStackTrace();
            }
        }

        return rh;
    }

    public static String getInternalStoragePathInRootMode(RootHelperClientUsingPathContent rh, Context c) {
        SharedPreferences sp = c.getSharedPreferences(c.getPackageName(), Context.MODE_PRIVATE);
        String rootModeStoragePath = sp.getString("rootModeStoragePath",null);
        if(rootModeStoragePath != null) return rootModeStoragePath; // a working path was already set

        GenericDirWithContent dwc = rh.listDirectory(new LocalPathContent(Misc.internalStorageDir.getAbsolutePath()));
        if(dwc.errorCode != null) {
            // internal storage dir exposed by Environment.getExternalStorageDir is not mounted for root user (some Kitkat devices)
            try {
                rootModeStoragePath = System.getenv("EXTERNAL_STORAGE");
                if(rootModeStoragePath == null) return null;
                dwc = rh.listDirectory(new LocalPathContent(rootModeStoragePath));
                if(dwc.errorCode != null) return null;
                if(new File(rootModeStoragePath).listFiles()==null) return null; // the found path for root mode must be accessible in normal mode as well
            }
            catch(Exception e) {
                return null;
            }
        }

        SharedPreferences.Editor editor = sp.edit();
        editor.putString("rootModeStoragePath",rootModeStoragePath);
        editor.commit();

        return rootModeStoragePath;
    }
}
