package it.pgp.xfiles.sftpclient;

import android.util.Log;

import net.schmizz.sshj.Config;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPEngine;
import net.schmizz.sshj.transport.TransportException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;
import it.pgp.xfiles.utils.Misc;

public class XSSHClient extends SSHClient implements AutoCloseable {

    private void checkConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected");
        }
    }

    private void checkAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("Not authenticated");
        }
    }

    public XSSHClient(Config config) {
        super(config);
    }

    public XSFTPClient newXSFTPClient()
            throws IOException {
        checkConnected();
        checkAuthenticated();
        return new XSFTPClient(new SFTPEngine(this).init());
    }

    /**
     * Accumulates the count of regular files in a list of items (that can contain both files and directories)
     * by recursing on subdirectories if necessary
     */
    public long countTotalRegularFilesInItems(Iterable<Map.Entry<String,Boolean>> paths) {
        long totalFirstLevelRegularFiles = 0;
        long total = -1;
        try (Session helperSession = startSession()) {
            StringBuilder builder = new StringBuilder("(");
            for (Map.Entry<String,Boolean> path : paths) {
                if (path.getValue()) // launch find command only if is directory
                    builder.append("find ").append(path.getKey()).append(" -type f;"); // seek recursively for all files in the directory
                else totalFirstLevelRegularFiles++;
            }
            builder.append(") | wc -l");
            try (Session.Command cmd = helperSession.exec(builder.toString());
                 InputStream is = cmd.getInputStream()) {
                String output = IOUtils.readFully(is).toString().trim();
                long exitStatus = cmd.getExitStatus();
                if (exitStatus==0) {
                    total = Long.valueOf(output);
                    total += totalFirstLevelRegularFiles;
                    Log.d("TOTALTOTAL","Current total is "+total);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return total;
        }
        catch (ConnectionException | TransportException e) {return total;}
    }

    protected long countTotalSizeInItems_duMethod(Iterable<Map.Entry<String,Boolean>> filenames, String parentDir) {
        long total = 0;
        if (!filenames.iterator().hasNext()) return total;
        try (Session helperSession = startSession()) {
            // try with linux du (all filenames scenarios should be already covered, provided the target shell has enabled UTF-8 support)
            StringBuilder builder = new StringBuilder("cd '"+parentDir.replace("'","'\"'\"'")+"' && du -s -0 --apparent-size -B1 -l ");
            for (Map.Entry<String,Boolean> path : filenames) {
                builder.append("'").append(path.getKey().replace("'","'\"'\"'")).append("' ");
            }

            try (Session.Command cmd = helperSession.exec(builder.toString());
                 InputStream is = cmd.getInputStream()) {
                byte[] commandOutput = IOUtils.readFully(is).toByteArray();
                long exitStatus = cmd.getExitStatus();
                if (exitStatus!=0) {
                    return -1;
                }
                else {
                    List<String> outputLines = Misc.splitByteArrayOverByteAndEncode(commandOutput, (byte)0); // -0 param of du separates lines with \0
                    for (String outputLine : outputLines) {
                        String[] sLine = outputLine.split("[ \t]");
                        if (sLine.length > 1) total += Long.valueOf(sLine[0]); // take only first cell, contains size
                        else Log.w("TOTALSIZE","du returned success, but parsed line with less than 2 cells, ignoring it. Line is:\n"+outputLine);
                    }
                    return total;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (ConnectionException | TransportException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // should work fine for all filename scenario at least with Python 3 on Unix OSes (python 2 may fail on utf-8 chars filenames)
    protected long countTotalSizeInItems_pythonMethod(Iterable<Map.Entry<String,Boolean>> filenames, String parentDir) {
        long total = 0;
        if (!filenames.iterator().hasNext()) return total;
        try (Session helperSession = startSession()) {
            StringBuilder builder = new StringBuilder("python -c 'from __future__ import print_function;import os;os.chdir(r\"");
            // add parentDir for chdir
            builder.append(parentDir.replace("\n","\\n").replace("\"","\\\"").replace("'","'\"'\"'"))
                    .append("\");print(sum([sum([sum(map(lambda fname: os.path.getsize(os.path.join(directory, fname)), files)) for directory, folders, files in os.walk(singlePath)]) for singlePath in [");

            for (Map.Entry<String,Boolean> path : filenames) {
                builder.append("r\"").append(path.getKey().replace("\n","\\n").replace("\"","\\\"").replace("'","'\"'\"'")).append("\",");
            }
            builder.append("]]));'"); // non-empty lists with ending comma characters are allowed in python (emptiness checked at the beginning)

            try (Session.Command cmd = helperSession.exec(builder.toString());
                 InputStream is = cmd.getInputStream()) {
                String commandOutput = IOUtils.readFully(is).toString().trim();
                long exitStatus = cmd.getExitStatus();
                return exitStatus!=0?-1:Long.valueOf(commandOutput);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (ConnectionException | TransportException ignored) {}
        return -1;
    }

    protected long countTotalSizeInItems_dirMethod(Iterable<Map.Entry<String,Boolean>> filenames, String parentDir) {
        long total = 0;
        long totalFiles = 0;
        if (!filenames.iterator().hasNext()) return total;
        try {
            String pdwf = parentDir.substring((parentDir.startsWith("/")?1:0),parentDir.length()-((parentDir.endsWith("/")?1:0)));
            pdwf = pdwf.replace("/","\\");
            if (pdwf.length()<=1 || pdwf.charAt(1) != ':') pdwf = ""+pdwf.charAt(0)+":"+pdwf.substring(1); // actually the operator should be == for length, <= only to avoid app crash in case of malformed paths
            //   C:\example\path or C\example\path
            //   C:\                C               C\

            String changeUnitCommand = pdwf.substring(0,2);

            Pattern pattern = Pattern.compile("([0-9]+)([^0-9]+)([0-9]+)"); // dir output format: nnnnnnnn files, MMMMMMMM bytes

            for (Map.Entry<String,Boolean> path : filenames) {
                try (Session helperSession = startSession()) {
                    final String dircmd = "cmd /V:ON /c \"@echo off & " +
                            changeUnitCommand + " & " +
                            "@cd \"" + pdwf + "\\\" & " + // additional final slash for avoiding cases like |cd "c:"| that don't work (while |cd "c:\"| does work )
                            "@set pline=na & @set cline=na & " +
                            "(@for /F \"delims=\" %i in ( ' dir /s /a /-c \"" + path.getKey() + "\" ' ) do " +
                            "@( @set pline=!cline! & @set cline=%i)) & " +
                            "@echo !pline!\"";
                    Log.e(getClass().getName(),"dir cmd is: "+dircmd);

                    try (Session.Command cmd = helperSession.exec(dircmd);
                         InputStream is = cmd.getInputStream()) {
                        String commandOutput = IOUtils.readFully(is).toString().trim();
                        Log.e(getClass().getName(),"dir cmd output is: "+commandOutput);
                        long exitStatus = cmd.getExitStatus();
                        if (exitStatus != 0) return -1;
                        else {
                            Matcher matcher = pattern.matcher(commandOutput);
                            if (matcher.find()) {
                                totalFiles += Long.valueOf(matcher.group(1));
                                total += Long.valueOf(matcher.group(3));
                            }
                            else Log.e(getClass().getName(),"No match found for dir cmd output: "+commandOutput);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                catch (ConnectionException | TransportException e) {
                    e.printStackTrace();
                    return -1;
                }
            }
            return total;
        }
        catch (Exception e) {
            Log.e(getClass().getName(),"Unhandled exception, leaving size count", e);
            return -1;
        }
    }

    /**
     * Tries to invoke external commands du, python with os.walk script, and dir (in case remote host is a Windows one)
     */
    public long countTotalSizeInItems(Iterable<Map.Entry<String,Boolean>> filenames, String parentDir) throws IOException {
        try {
            long total = countTotalSizeInItems_duMethod(filenames,parentDir);
            if (total >= 0) return total;
            Log.e("TOTALSIZE","du command failed, trying with python command...");

            total = countTotalSizeInItems_pythonMethod(filenames, parentDir);
            if (total >= 0) return total;
            Log.e("TOTALSIZE","python command failed, trying with dir command...");

            // try with dir command (for windows remote hosts running a SSH server, e.g. Windows 10 >=1803 built-in OpenSSH, or Bitvise SSH Server)
            total = countTotalSizeInItems_dirMethod(filenames, parentDir);
            if (total >= 0) return total;
            Log.e("TOTALSIZE","dir command failed, external progress or total size for stats won't be available");

            return -1;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public folderStats_resp statFoldersInPaths(Map.Entry<String,Boolean>... paths) {
        folderStats_resp resp = new folderStats_resp(0,0,0,0,0);

        long totalFirstLevelRegularFiles = 0;
        final StringBuilder builderD = new StringBuilder("(");
        final StringBuilder builderF = new StringBuilder("(");
        for (Map.Entry<String,Boolean> path : paths) {
            if (path.getValue()) { // launch find command only if is directory
                builderD.append("find ").append(path.getKey()).append(" -type d;"); // all dir nodes in dir
                builderF.append("find ").append(path.getKey()).append(" -type f;"); // all files nodes in dir
            }
            else totalFirstLevelRegularFiles++;
        }
        builderD.append(") | wc -l");
        builderF.append(") | wc -l");

        // TODO refactor duplicated code

        try (Session helperSession = startSession();
             Session.Command cmd = helperSession.exec(builderD.toString());
             InputStream is = cmd.getInputStream()) {
            String output = IOUtils.readFully(is).toString().replace("\n","");
            long exitStatus = cmd.getExitStatus();
            if (exitStatus==0) {
                resp.totalDirs = Long.valueOf(output);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        try (Session helperSession = startSession();
             Session.Command cmd = helperSession.exec(builderF.toString());
             InputStream is = cmd.getInputStream()) {
            String output = IOUtils.readFully(is).toString().replace("\n","");
            long exitStatus = cmd.getExitStatus();
            if (exitStatus==0) {
                resp.totalFiles = Long.valueOf(output);
                resp.totalFiles += totalFirstLevelRegularFiles;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return resp;

        // TODO childrenDirs, childrenFiles and totalSize
    }
}
