package it.pgp.xfiles.service;

/**
 * Created by pgp on 27/09/17
 */

// e.g. thecompressor is parallel-compatible with thetransferrer in upload mode (no concurrent write load)
public enum SocketNames {
    theroothelper, // Main, persistent, for short-lived operations
//        thecompressor, // READ & WRITE
//        theextractor, // READ & WRITE
//        thetransferrer, // READ & WRITE (READ ONLY IF UPLOAD, WRITE ONLY IF DOWNLOAD)
//        thefinder // READ ONLY
}
