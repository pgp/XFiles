package it.pgp.xfiles.sftpclient;

/**
 * Created by pgp on 21/11/17
 * Adapted from {@link net.schmizz.sshj.xfer.FileSystemFile}
 * SSHJ-compatible wrapper to InputStream and OutputStream using roothelper for read and write
 * (custom Inputstream and Outputstream classes connecting to roothelper only if needed are provided as well)
 */

import android.support.annotation.Nullable;
import android.util.Log;

import net.schmizz.sshj.xfer.LocalDestFile;
import net.schmizz.sshj.xfer.LocalFileFilter;
import net.schmizz.sshj.xfer.LocalSourceFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Stack;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.enums.FileMode;
import it.pgp.xfiles.io.RobustLocalFileInputStream;
import it.pgp.xfiles.io.RobustLocalFileOutputStream;
import it.pgp.xfiles.roothelperclient.RootHelperClientUsingPathContent;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

public class XFileSystemFile implements LocalSourceFile, LocalDestFile {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final RootHelperClientUsingPathContent rhc = new RootHelperClientUsingPathContent();
    private RobustLocalFileInputStream fis;
    private RobustLocalFileOutputStream fos;

    private String file;

    public XFileSystemFile(String path) {
        this.file = path;
    }

    public XFileSystemFile(File file) {
        this.file = file.getAbsolutePath();
    }

    @Override
    public String getName() {
        String[] file_ = file.split("/");
        return file_[file_.length-1];
    }

    @Override
    public boolean isFile() {
        Log.e(this.getClass().getName(),"isFile");
        BitSet x = rhc.existsIsFileIsDir(new LocalPathContent(file),true,true,true);
        Log.e(this.getClass().getName(),"isFile completed");
        return x.get(1);
    }

    @Override
    public boolean isDirectory() {
        Log.e(this.getClass().getName(),"isDirectory");
        BitSet x = rhc.existsIsFileIsDir(new LocalPathContent(file),true,true,true);
        Log.e(this.getClass().getName(),"isDirectory completed");
        return x.get(2);
    }

    @Override
    public long getLength() {
        try {
            return rhc.statFile(new LocalPathContent(file)).size;
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (fis==null) fis = new RobustLocalFileInputStream(file);
        return fis;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (fos==null) fos = new RobustLocalFileOutputStream(file);
        return fos;
    }

    @Override
    public Iterable<XFileSystemFile> getChildren(@Nullable final LocalFileFilter filter) throws IOException {
        // TODO keep into account filter
        GenericDirWithContent gdwc = rhc.listDirectory(new LocalPathContent(file));
        if (gdwc.errorCode != null)
            throw new IOException("Error listing files in directory: " + this);

        List<XFileSystemFile> children = new ArrayList<>();
        for (BrowserItem b : gdwc.content)
            children.add(new XFileSystemFile(gdwc.dir+"/"+b.getFilename()));
        return children;
    }

    @Override
    public boolean providesAtimeMtime() {
        return true;
    }

    @Override
    public long getLastAccessTime() throws IOException {
        return System.currentTimeMillis() / 1000;
    }

    @Override
    public long getLastModifiedTime() throws IOException {
        return rhc.statFile(new LocalPathContent(file)).modificationTime.getTime() / 1000;
    }

    @Override
    public int getPermissions() throws IOException {
        BitSet x = rhc.existsIsFileIsDir(new LocalPathContent(file),true,true,true);
        if (x.get(2)) return 0755;
        else if (x.get(1)) return 0644;
        else throw new IOException("Unsupported file type or non-existing file");
    }

    @Override
    public void setLastAccessedTime(long t) throws IOException {
        if (rhc.setDates(new LocalPathContent(file),new Date(t),null) != 0) {
//            throw new IOException("Unable to set last access time");
            Log.e(getClass().getName(),"Unable to set last access time");
        }
    }

    @Override
    public void setLastModifiedTime(long t) throws IOException {
        if (rhc.setDates(new LocalPathContent(file),null,new Date(t)) != 0) {
//            throw new IOException("Unable to set last modified time");
            Log.e(getClass().getName(),"Unable to set last modified time");
        }
    }

    @Override
    public void setPermissions(int perms) throws IOException {
        if (rhc.setPermissions(new LocalPathContent(file),perms) != 0) {
//            throw new IOException("Unable to set permissions"); // just treat as warning or avoid to call completely, since perms are very limited by default on Android filesystems
            Log.e(getClass().getName(),"Unable to set permissions");
        }
    }

    @Override
    public XFileSystemFile getChild(String name) {
        validateIsChildPath(name);
        return new XFileSystemFile(file+"/"+name);
    }

    private void validateIsChildPath(String name) {
        String[] split = name.split("/");
        Stack<String> s = new Stack<>();
        for (String component : split) {
            if (component == null || component.isEmpty() || ".".equals(component)) {
                continue;
            } else if ("..".equals(component) && !s.isEmpty()) {
                s.pop();
                continue;
            } else if ("..".equals(component)) {
                throw new IllegalArgumentException("Cannot traverse higher than " + file + " to get child " + name);
            }
            s.push(component);
        }
    }

    @Override
    public XFileSystemFile getTargetFile(String filename) throws IOException {
        XFileSystemFile f = this;

        if (f.isDirectory()) {
            f = f.getChild(filename);
        }

        LocalPathContent ffile = new LocalPathContent(f.file);
        BitSet x = rhc.existsIsFileIsDir(ffile,true,true,true);
        if (!x.get(0)) { // not exists
            rhc.createFileOrDirectory(ffile, FileMode.FILE);
        }
        else if (x.get(2))
            throw new IOException("A directory by the same name already exists: " + f);
        return f;
    }

    @Override
    public XFileSystemFile getTargetDirectory(String dirname) throws IOException {
        XFileSystemFile f = this;
        LocalPathContent fdir = new LocalPathContent(f.file);
        BitSet x = rhc.existsIsFileIsDir(fdir,true,true,true);
        if (x.get(0)) { // exists
            if (x.get(2)) // is directory
                if (!f.getName().equals(dirname)) {
                    f = f.getChild(dirname);
                    fdir = new LocalPathContent(f.file);
                }
            else throw new IOException(f + " - already exists as a file; directory required");
        }
        x = rhc.existsIsFileIsDir(fdir,true,true,true);
        if (!x.get(0))
            rhc.createFileOrDirectory(fdir,FileMode.DIRECTORY);
        return f;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof XFileSystemFile)
                && file.equals(((XFileSystemFile) other).file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public String toString() {
        return file;
    }

}

