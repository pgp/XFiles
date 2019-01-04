package it.pgp.xfiles.utils;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by pgp on 28/09/16
 */

public class Checksums {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // TODO byte[] or hexstring output
    public static long crc32(Object filepath) throws IOException {
        InputStream inputStream;
        if (filepath instanceof File) {
            inputStream = new BufferedInputStream(new FileInputStream((File)filepath));
        }
        else if (filepath instanceof String) {
            inputStream = new BufferedInputStream(new FileInputStream((String)filepath));
        }
        else {
            throw new IOException("Neither file object nor filepath string");
        }

        Checksum crc = new CRC32();

        int cnt;
        while ((cnt = inputStream.read()) != -1) {
            crc.update(cnt);
        }

        return crc.getValue();
    }

    public static byte[] sha1(String filepath) throws NoSuchAlgorithmException,IOException {
        return sha1(new File(filepath));
    }
    public static byte[] sha1(File file) throws NoSuchAlgorithmException,IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }

    public static byte[] sha256(File file) throws NoSuchAlgorithmException,IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }

    public static byte[] sha512(File file) throws NoSuchAlgorithmException,IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }

    public static byte[] md5(File file) throws NoSuchAlgorithmException,IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }

    public static byte[] stdDigest(File file, String algorithm) throws NoSuchAlgorithmException,IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        InputStream fis = new FileInputStream(file);
        int n = 0;
        byte[] buffer = new byte[8192];
        while (n != -1) {
            n = fis.read(buffer);
            if (n > 0) {
                digest.update(buffer, 0, n);
            }
        }
        return digest.digest();
    }
}
