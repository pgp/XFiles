package it.pgp;

/**
 * Created by pgp on 07/01/18
 */

public class Native {

    // just ignore the IDE's symbol resolution errors here, we don't need to put C/CPP headers here,
    // just the exported libr.so (which is both executable - for su - and shared object - for jni)

    public static native int isSymLink(String path);
    public static native String stringFromJNI();
    public static native int nHashCode(byte[] input);
    public static native byte[] spongeForHashViewShake(byte[] input, int inputLen, int outputLen);
//    public static native void c20StreamGen(byte[] key, byte[] output);
//    public static native void spongeForHashView(byte[] input, byte[] output);
//    public static native void spongeForHashViewInPlace(byte[] input, int inputLen, byte[] output, int outputLen);
    public static native int sendDetachedFD(int udsToSendFdOver, int fdToSend);

    /***/
    public static native String nonExisting();
}
