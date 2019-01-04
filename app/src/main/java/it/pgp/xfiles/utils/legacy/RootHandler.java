package it.pgp.xfiles.utils.legacy;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by pgp on 28/09/16
 */

@Deprecated
public class RootHandler {

    private static String[] rootCommand = {"su", "-c", ""};
    private static String[] standardCommand = {"sh", "-c", ""};

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
                Log.e(RootHandler.class.getName(),"su binary found at "+where);
                break;
            }
        }
        return found;
    }

    public static void executeCommandSimple(String command, File workingDir, boolean runAsSuperUser) {
        String[] fullCommand = runAsSuperUser ? rootCommand.clone() : standardCommand.clone();
        fullCommand[2] = command;
        String s = fullCommand[0] + " " + fullCommand[1] + " " + fullCommand[2];

        try {
            Process process;
            if (workingDir == null) {
                process = Runtime.getRuntime().exec(s);
            }
            else {
                process = Runtime.getRuntime().exec(s,null,workingDir);
            }

            int exitValue = process.waitFor();

            StringBuilder output = new StringBuilder();
            // no console output expected from process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            Log.d(RootHandler.class.getName(), "***BEGIN Parent process output:***\n" + output.toString() + "\n***END Parent process output***\nExit value: " + exitValue);

        } catch (IOException i) {
            Log.e(RootHandler.class.getName(), "***IOException***");
            i.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
