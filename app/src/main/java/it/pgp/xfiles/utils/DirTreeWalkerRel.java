package it.pgp.xfiles.utils;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;

/**
 * Created by pgp on 22/11/16
 */
public class DirTreeWalkerRel implements Iterator<DirTreeWalkerRel.BaseAndTargetFiles>{
    private Stack<BaseAndTargetFiles> stack;
    public DirTreeWalkerRel(File baseDir, File targetDir, Collection<String> filenames) {
        this.stack = new Stack<>();
        for (String fn : filenames) {
            stack.push(new BaseAndTargetFiles(new File(baseDir, fn), new File(targetDir, fn)));
        }
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    @Override
    public BaseAndTargetFiles next() {
        // get next regular file from baseStack (if file is dir, expand it and go on)
        BaseAndTargetFiles bt = stack.pop();

        if (bt.startFile.isDirectory()) {
            for (File x : bt.startFile.listFiles()) {
                String fn = x.getName();
                File y = new File(bt.targetFile,fn);
                stack.push(new BaseAndTargetFiles(x, y));
            }
        }

        return bt;
    }

    public static class BaseAndTargetFiles {
        public File startFile;
        public File targetFile;

        public BaseAndTargetFiles(File startFile, File targetFile) {
            this.startFile = startFile;
            this.targetFile = targetFile;
        }

        public String getSrc() {
            return startFile.getAbsolutePath();
        }

        public String getDest() {
            return targetFile.getAbsolutePath();
        }
    }
}
