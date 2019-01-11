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
import java.util.Map;

import it.pgp.xfiles.roothelperclient.resps.folderStats_resp;

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
                    Log.e("TOTALTOTAL","Current total is "+total);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            return total;
        }
        catch (ConnectionException | TransportException e) {return total;}
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
