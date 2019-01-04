package it.pgp.xfiles.utils;

import android.support.annotation.NonNull;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;

/**
 * Created by pgp on 02/11/16
 * Last modified on 11/11/2016
 *
 * TODO should become part of FileOperationHelper interface
 */
public class DirTreeWalker implements Iterator<File>{
    private Stack<File> stack;
    public DirTreeWalker(File rootDir) {
        if (!rootDir.isDirectory())
            throw new RuntimeException("Not a directory");
        stack = new Stack<>();
        for (File x : rootDir.listFiles()) stack.push(x);
    }

    public DirTreeWalker() {
        stack = new Stack<>();
    }

    public DirTreeWalker(@NonNull Collection<File> collection) {
        this();
        this.append(collection);
    }

    public DirTreeWalker(@NonNull File... args) {
        this();
        this.append(args);
    }

    public void append(@NonNull File file) {
        stack.push(file);
    }

    public void append(@NonNull Collection<File> collection) {
        for (File x : collection)
            if (x!=null)
                stack.push(x);
    }

    public void append(@NonNull File... args) {
        for (File x : args)
            stack.push(x);
    }

    @Override
    public boolean hasNext() {
        return !stack.isEmpty();
    }

    // only adds files, and directories in which subtree there is at least a file as leaf
//    @Override
//    public File next() {
//        // get next regular file from stack (if file is dir, expand it and go on)
//        File f = stack.pop();
//        while (f.isDirectory()) {
//            for (File x : f.listFiles()) stack.push(x);
//            f = stack.pop();
//        }
//
//        return f;
//    }

    @Override
    public File next() {
        // get next regular file from stack (if file is dir, expand it and go on)
        File f = stack.pop();
        if (f.isDirectory()) {
            for (File x : f.listFiles()) stack.push(x);
        }

        return f;
    }
}
