package it.pgp.xfiles.sftpclient;

import net.schmizz.sshj.sftp.*;
import net.schmizz.sshj.xfer.FilePermission;
import net.schmizz.sshj.xfer.LocalDestFile;
import net.schmizz.sshj.xfer.LocalSourceFile;

import java.io.IOException;
import java.util.*;

public class XSFTPClient
        implements AutoCloseable {

    public final SFTPEngine engine;
    public final XSFTPFileTransfer xfer;

    public XSFTPClient(SFTPEngine engine) {
        this.engine = engine;
        this.xfer = new XSFTPFileTransfer(engine);
    }

    public void setProgressIndicator(XProgress progressIndicator) {
        ((XTransferListener)xfer.transferListener).progressIndicator = progressIndicator;
    }

    public SFTPEngine getSFTPEngine() {
        return engine;
    }

    public SFTPFileTransfer getFileTransfer() {
        return xfer;
    }

    public List<RemoteResourceInfo> ls(String path)
            throws IOException {
        return ls(path, null);
    }

    public List<RemoteResourceInfo> ls(String path, RemoteResourceFilter filter)
            throws IOException {
        try (RemoteDirectory dir = engine.openDir(path)) {
            return dir.scan(filter);
        }
    }

    public RemoteFile open(String filename, Set<OpenMode> mode, FileAttributes attrs)
            throws IOException {
        return engine.open(filename, mode, attrs);
    }

    public RemoteFile open(String filename, Set<OpenMode> mode)
            throws IOException {
        return open(filename, mode, FileAttributes.EMPTY);
    }

    public RemoteFile open(String filename)
            throws IOException {
        return open(filename, EnumSet.of(OpenMode.READ));
    }

    public void mkdir(String dirname)
            throws IOException {
        engine.makeDir(dirname);
    }

    public void mkdirs(String path)
            throws IOException {
        final Deque<String> dirsToMake = new LinkedList<>();
        for (PathComponents current = engine.getPathHelper().getComponents(path); ;
             current = engine.getPathHelper().getComponents(current.getParent())) {
            final FileAttributes attrs = statExistence(current.getPath());
            if (attrs == null) {
                dirsToMake.push(current.getPath());
            } else if (attrs.getType() != FileMode.Type.DIRECTORY) {
                throw new SFTPException(current.getPath() + " exists but is not a directory");
            } else {
                break;
            }
        }
        while (!dirsToMake.isEmpty()) {
            mkdir(dirsToMake.pop());
        }
    }

    public FileAttributes statExistence(String path)
            throws IOException {
        try {
            return engine.stat(path);
        } catch (SFTPException sftpe) {
            if (sftpe.getStatusCode() == Response.StatusCode.NO_SUCH_FILE) {
                return null;
            } else {
                throw sftpe;
            }
        }
    }

    public void rename(String oldpath, String newpath)
            throws IOException {
        engine.rename(oldpath, newpath);
    }

    public void rm(String filename)
            throws IOException {
        engine.remove(filename);
    }

    public void rmdir(String dirname)
            throws IOException {
        engine.removeDir(dirname);
    }

    public void symlink(String linkpath, String targetpath)
            throws IOException {
        engine.symlink(linkpath, targetpath);
    }

    public int version() {
        return engine.getOperativeProtocolVersion();
    }

    public void setattr(String path, FileAttributes attrs)
            throws IOException {
        engine.setAttributes(path, attrs);
    }

    public int uid(String path)
            throws IOException {
        return stat(path).getUID();
    }

    public int gid(String path)
            throws IOException {
        return stat(path).getGID();
    }

    public long atime(String path)
            throws IOException {
        return stat(path).getAtime();
    }

    public long mtime(String path)
            throws IOException {
        return stat(path).getMtime();
    }

    public Set<FilePermission> perms(String path)
            throws IOException {
        return stat(path).getPermissions();
    }

    public FileMode mode(String path)
            throws IOException {
        return stat(path).getMode();
    }

    public FileMode.Type type(String path)
            throws IOException {
        return stat(path).getType();
    }

    public String readlink(String path)
            throws IOException {
        return engine.readLink(path);
    }

    public FileAttributes stat(String path)
            throws IOException {
        return engine.stat(path);
    }

    public FileAttributes lstat(String path)
            throws IOException {
        return engine.lstat(path);
    }

    public void chown(String path, int uid)
            throws IOException {
        setattr(path, new FileAttributes.Builder().withUIDGID(uid, gid(path)).build());
    }

    public void chmod(String path, int perms)
            throws IOException {
        setattr(path, new FileAttributes.Builder().withPermissions(perms).build());
    }

    public void chgrp(String path, int gid)
            throws IOException {
        setattr(path, new FileAttributes.Builder().withUIDGID(uid(path), gid).build());
    }

    public void truncate(String path, long size)
            throws IOException {
        setattr(path, new FileAttributes.Builder().withSize(size).build());
    }

    public String canonicalize(String path)
            throws IOException {
        return engine.canonicalize(path);
    }

    public long size(String path)
            throws IOException {
        return stat(path).getSize();
    }

    public void get(String source, String dest)
            throws IOException {
        xfer.download(source, dest);
    }

    public void put(String source, String dest)
            throws IOException {
        xfer.upload(source, dest);
    }

    public void get(String source, LocalDestFile dest)
            throws IOException {
        xfer.download(source, dest);
    }

    public void put(LocalSourceFile source, String dest)
            throws IOException {
        xfer.upload(source, dest);
    }

    @Override
    public void close()
            throws IOException {
        engine.close();
    }

}
