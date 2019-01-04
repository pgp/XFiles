package it.pgp.xfiles.roothelperclient.resps;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class folderStats_resp {

    public long childrenDirs;
    public long childrenFiles;
    public long totalDirs;
    public long totalFiles;
    public long totalSize;

    public static long castBytesToUnsignedNumber(byte[] b) {
        if (b.length > 8)
            throw new RuntimeException("Max byte array size is 8 (long type), found array length greater!");
        long value = 0;
        int cut = b.length;
        for (int i = cut-1; i >= 0; i--) // replace with decrement for for little endianness compliance
            value = (value << 8) + (b[i] & 0xFF);
        return value;
    }

    public static byte[] castUnsignedNumberToBytes(long l, Integer cut_) {
        byte[] o = (cut_ == null)?new byte[8]:new byte[cut_];
        long mask = 0xFF;
        for (int i=0;i<cut_;i++) {
            o[i] = (byte) (l & mask);
            l >>= 8;
        }
        return o;
    }
    public folderStats_resp(DataInputStream inputStream) throws IOException {
        byte[] tmp;
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.childrenDirs = castBytesToUnsignedNumber(tmp);
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.childrenFiles = castBytesToUnsignedNumber(tmp);
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.totalDirs = castBytesToUnsignedNumber(tmp);
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.totalFiles = castBytesToUnsignedNumber(tmp);
        tmp = new byte[8];
        inputStream.readFully(tmp);
        this.totalSize = castBytesToUnsignedNumber(tmp);
    }

    public folderStats_resp(long childrenDirs, long childrenFiles, long totalDirs, long totalFiles, long totalSize)  {
        this.childrenDirs = childrenDirs;
        this.childrenFiles = childrenFiles;
        this.totalDirs = totalDirs;
        this.totalFiles = totalFiles;
        this.totalSize = totalSize;
    }

    public void writefolderStats_resp(OutputStream outputStream) throws IOException {
        byte[] tmp;tmp = castUnsignedNumberToBytes(this.childrenDirs,8);
        outputStream.write(tmp);
        tmp = castUnsignedNumberToBytes(this.childrenFiles,8);
        outputStream.write(tmp);
        tmp = castUnsignedNumberToBytes(this.totalDirs,8);
        outputStream.write(tmp);
        tmp = castUnsignedNumberToBytes(this.totalFiles,8);
        outputStream.write(tmp);
        tmp = castUnsignedNumberToBytes(this.totalSize,8);
        outputStream.write(tmp);
    }



}