package it.pgp.xfiles.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.pgp.xfiles.sftpclient.AuthData;
import it.pgp.xfiles.sftpclient.AuthDataWithFavorites;
import it.pgp.xfiles.sftpclient.InsertFailedException;

/**
 * Created by pgp on 01/07/17
 * For storing both sftp credentials and favorite PathContents
 */

public class GenericDBHelper extends SQLiteOpenHelper {

    private static final String DbName = "XFilesDB";
    private static final int DbVersion = 2;

    private static final String credentialsVaultTableName = "sftpCredentialsAndFavorites";
    private static final String localFavoritesTableName = "localFavorites";
    private static final String xreFavoritesTableName = "xreFavorites";


    // fields of credentials & favorites table for sftp
    private static final String username_field = "username";
    private static final String domain_field = "domain";
    private static final String port_field = "port";
    private static final String password_field = "password";
    private static final String listOfFavorites_field = "favoritePaths"; // serialized list of remote path strings

    // 1 field for local favorites table (path string)
    private static final String local_favorites_content_field = "path";

    private static final String xre_favorites_server_field = "server";
    private static final String xre_favorites_path_field = "path";

    private static SQLiteDatabase db;

    /*
    username, domain, port: strings (port is integer)
    for sftp access (domain being a domain name or a IP)
    type: password string (can be null, in which case private keys are tried upon authentication)
    */

    private static final String sqlCreateStatement_sftpCredentialsAndFavorites =
            "CREATE TABLE IF NOT EXISTS "+ credentialsVaultTableName +"(" +
                    username_field+" TEXT," +
                    domain_field+" TEXT," +
                    port_field+" INTEGER," +
                    password_field +" TEXT," +
                    listOfFavorites_field+" BLOB," +
                    "PRIMARY KEY ("+username_field+","+domain_field+","+port_field+")" +
                    ");";

    // do not enforce primary key constraints, read all rows into hashset, on add favorite, truncate table and rewrite all rows
    private static final String sqlCreateStatement_localFavorites =
            "CREATE TABLE IF NOT EXISTS "+ localFavoritesTableName +"(" +
                    local_favorites_content_field +" TEXT," +
                    "PRIMARY KEY ("+local_favorites_content_field+"));";

    private static final String sqlCreateStatement_xreFavorites =
            "CREATE TABLE IF NOT EXISTS "+ xreFavoritesTableName +"(" +
                    xre_favorites_server_field +" TEXT," +
                    xre_favorites_path_field +" TEXT," +
                    "PRIMARY KEY ("+xre_favorites_server_field+","+xre_favorites_path_field+"));";

    /**********************************
     Operations on local favorites table
     **********************************/

    public Map<Long,String> getAllRowsOfLocalFavoritesTable() {
        String[] cols = new String[] {"oid",local_favorites_content_field};
        Cursor mCursor = db.query(false, localFavoritesTableName,cols,null,null, null, null, null, null);
        Map<Long,String> map = new HashMap<>();
        if (mCursor!=null) {
            while (mCursor.moveToNext()) {
                // 64 bit ints clearly not needed for a few favorites
                map.put(mCursor.getLong(0),mCursor.getString(1));
            }
            mCursor.close();
        }
        return map;
    }

    public Map<Long,Map.Entry<String,String>> getAllRowsOfXreFavoritesTable() {
        String[] cols = new String[] {"oid",xre_favorites_server_field,xre_favorites_path_field};
        Cursor mCursor = db.query(false, xreFavoritesTableName,cols,null,null, null, null, null, null);
        Map<Long,Map.Entry<String,String>> map = new HashMap<>();
        if (mCursor!=null) {
            while (mCursor.moveToNext()) {
                // 64 bit ints clearly not needed for a few favorites
                map.put(mCursor.getLong(0),
                        new AbstractMap.SimpleEntry<>(
                                mCursor.getString(1),
                                mCursor.getString(2))
                );
            }
            mCursor.close();
        }
        return map;
    }

    // get oid by pathname, for update without changing oid (delete & insert changes oid)
    // returns null if value not found in table
//    public Integer getRowOfLocalFavoritesTable(String path) {
//        Cursor mCursor = db.query(true,
//                localFavoritesTableName,
//                new String[] {"oid",local_favorites_content_field},
//                local_favorites_content_field+"=?",
//                new String[]{path},
//                null, null, null, null);
//        Integer ret = null; // remaining to null == not found
//        if (mCursor!=null) {
//            while (mCursor.moveToNext()) {
//                ret = (int)mCursor.getLong(0);
//                break; // only first result needed (anyway there cannot be more than one since there is primary key constraint over the only field in this table
//            }
//            mCursor.close();
//        }
//        return ret;
//    }

//    public void insertAllRowsOfFavoritesTable(Set<BasePathContent> s) throws IOException {
//        // truncate table before insert
//        db.delete(localFavoritesTableName,null,null);
//        // perform bulk insert
//        db.beginTransaction();
//        for (BasePathContent b : s) {
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();
//            ObjectOutput out = new ObjectOutputStream(bos);
//            out.writeObject(b);
//            out.flush();
//            byte[] bytes = bos.toByteArray();
//            ContentValues cv = new ContentValues();
//            cv.put(local_favorites_content_field,bytes);
//            db.insert(localFavoritesTableName,null,cv);
//            db.setTransactionSuccessful();
//        }
//        db.endTransaction();
//    }

    /**
     * @param path to be added to local favorites
     * @return map entry with inserted oid and path string
     * @throws InsertFailedException if that path already exists
     */
    public Map.Entry<Long,String> addLocalFavorite(String path) throws InsertFailedException {
        ContentValues cv = new ContentValues();
        cv.put(local_favorites_content_field,path);
        long oid = db.insert(localFavoritesTableName,null,cv);
        if (oid == -1) throw new InsertFailedException(); // row already exists
        return new AbstractMap.SimpleEntry<>(oid,path);
    }

    /**
     * @param path to be added to XFiles remote favorites
     * @return map entry with: inserted oid and [map entry of server and path string]
     * @throws InsertFailedException if that server-path pair already exists
     */
    public Map.Entry<Long,Map.Entry<String,String>> addXreFavorite(String server, String path) throws InsertFailedException {
        ContentValues cv = new ContentValues();
        cv.put(xre_favorites_server_field,server);
        cv.put(xre_favorites_path_field,path);
        long oid = db.insert(xreFavoritesTableName,null,cv);
        if (oid == -1) throw new InsertFailedException(); // row already exists
        Map.Entry<String,String> me = new AbstractMap.SimpleEntry<>(server,path);
        return new AbstractMap.SimpleEntry<>(oid,me);
    }


    /**
     * @param oldPath old local path favorite
     * @param newPath new local path favorite (to replace old in the same oid)
     * @return true on update ok
     */
    public boolean updateLocalFavorite(String oldPath, String newPath) {
        ContentValues cv = new ContentValues();
        cv.put(local_favorites_content_field,newPath);
        return db.update(localFavoritesTableName,cv,local_favorites_content_field+"=?",new String[]{oldPath}) > 0;
    }

    public boolean updateXreFavorite(String oldServer,
                                     String newServer,
                                     String oldPath,
                                     String newPath) {
        ContentValues cv = new ContentValues();
        cv.put(xre_favorites_server_field,newServer);
        cv.put(xre_favorites_path_field,newPath);
        String whereString = xre_favorites_server_field+"=? AND "+xre_favorites_path_field+"=?";
        String[] whereArgs = new String[]{oldServer,oldPath};
        return db.update(xreFavoritesTableName,cv,whereString,whereArgs) > 0;
    }

    public boolean deleteRowFromLocalFavoritesTable(long oid) {
        return db.delete(localFavoritesTableName,"oid="+oid,null)>0;
    }

    public boolean deleteRowFromXreFavoritesTable(long oid) {
        return db.delete(xreFavoritesTableName,"oid="+oid,null)>0;
    }

    /********************************************************
     Operations on sftp table
     ********************************************************/

    public Map<Long,AuthData> getAllSftpCreds() {
        String[] cols = new String[] {"oid",username_field,domain_field,port_field,password_field};
        HashMap<Long,AuthData> m = new HashMap<>();
        Cursor mCursor = db.query(true, credentialsVaultTableName,cols,null,null, null, null, null, null);
        if (mCursor!=null) {
            while (mCursor.moveToNext()) {
                // uid = username@domain:port
                AuthData a = new AuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getInt(3),mCursor.getString(4));
                m.put(mCursor.getLong(0),a);
            }
            mCursor.close();
        }
        return m;
    }

    public Map<Long,AuthDataWithFavorites> getAllSftpCredsWithFavs() {
        String[] cols = new String[] {"oid",username_field,domain_field,port_field,password_field,listOfFavorites_field};
        HashMap<Long,AuthDataWithFavorites> m = new HashMap<>();
        Cursor mCursor = db.query(true, credentialsVaultTableName,cols,null,null, null, null, null, null);
        if (mCursor!=null) {
            while (mCursor.moveToNext()) {
                AuthData a = new AuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getInt(3),mCursor.getString(4));
                byte[] b = mCursor.getBlob(5);
                if (b == null || b.length == 0) {
                    m.put(mCursor.getLong(0),new AuthDataWithFavorites(a));
                }
                else {
                    ObjectInputStream ois;
                    Set favs;
                    try {
                        ois = new ObjectInputStream(new ByteArrayInputStream(b));
                        favs = (Set) ois.readObject();
                        m.put(mCursor.getLong(0),new AuthDataWithFavorites(a,favs));
                    }
                    catch (IOException | ClassNotFoundException e) {
                        Log.e(this.getClass().getName(),"Exception: "+e.getMessage());
                        e.printStackTrace();
                        m.put(mCursor.getLong(0),new AuthDataWithFavorites(a));
                    }
                }
            }
            mCursor.close();
        }
        return m;
    }

    public Map.Entry<Long,AuthData> insertSftpCred(String username, String domain, int port, String password) throws InsertFailedException {
        // password can be null
        ContentValues cv = new ContentValues();
        cv.put(username_field,username);
        cv.put(domain_field,domain);
        cv.put(port_field,port);
        cv.put(password_field,password);
        long oid = db.insert(credentialsVaultTableName,null,cv);
        if (oid == -1) throw new InsertFailedException();
        return new AbstractMap.SimpleEntry<>(oid, new AuthData(username,domain,port,password)); // for updating list adapter in VaultActivity
    }

    public boolean updateSftpCred(long oldOid, AuthData newAuthData) {
        ContentValues cv = new ContentValues();
        cv.put(username_field,newAuthData.username);
        cv.put(domain_field,newAuthData.domain);
        cv.put(port_field,newAuthData.port);
        cv.put(password_field,newAuthData.password == null?"":newAuthData.password);
        return db.update(credentialsVaultTableName,cv,"oid="+oldOid,null)>0;
    }

    /**
     * delete all favorites: along with credentials {@link #deleteRowFromSftpTable(long)}
     * or with empty list as input
     * @param oldOid
     * @param newFavs
     * @return true on update success
     */
    public boolean updateSftpFavs(long oldOid, Set<String> newFavs) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(newFavs);
            out.flush();
            byte[] bytes = bos.toByteArray();
            ContentValues cv = new ContentValues();
            cv.put(listOfFavorites_field,bytes);
            return db.update(credentialsVaultTableName,cv,"oid="+oldOid,null)>0;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteRowFromSftpTable(long oid) {
        return db.delete(credentialsVaultTableName,"oid="+oid,null)>0;
    }

    // input: AuthData without password
    // output: AuthData (possibly) with password
    public AuthData find(AuthData a) {
        String[] cols = new String[] {"oid",username_field,domain_field,port_field,password_field};
        String selection = username_field+"= ? and "+domain_field+" = ? and "+port_field+" = ?";
        String[] selectionArgs = new String[]{a.username,a.domain,a.port+""};
        Cursor mCursor = db.query(true, credentialsVaultTableName,cols,selection,selectionArgs, null, null, null, null);
        if (mCursor!=null) {
            AuthData o = null;
            if (mCursor.moveToNext()) {
                o = new AuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getInt(3),mCursor.getString(4));
            }
            mCursor.close();
            return o;
        }
        return null;
    }

    public GenericDBHelper(Context context) {
        super(context, DbName, null, DbVersion);
        if (db == null) db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(sqlCreateStatement_localFavorites);
        db.execSQL(sqlCreateStatement_sftpCredentialsAndFavorites);
        db.execSQL(sqlCreateStatement_xreFavorites);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
