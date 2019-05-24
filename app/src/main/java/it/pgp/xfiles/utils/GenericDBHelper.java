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
import it.pgp.xfiles.smbclient.SmbAuthData;
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
    private static final String smbCredentialsVaultTableName = "smbCredentialsAndFavorites";


    // fields of credentials & favorites table for sftp
    public static final String username_field = "username";
    public static final String domain_field = "domain";
    public static final String host_field = "host"; // valid only for SMB, equivalent of domain in SFTP
    public static final String port_field = "port";
    public static final String password_field = "password";
    public static final String listOfFavorites_field = "favoritePaths"; // serialized list of remote path strings

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

    private static final String sqlCreateStatement_smbCredentialsAndFavorites =
            "CREATE TABLE IF NOT EXISTS "+ smbCredentialsVaultTableName +"(" +
                    username_field+" TEXT," +
                    domain_field+" TEXT," +
                    host_field+" TEXT," +
                    port_field+" INTEGER," +
                    password_field +" TEXT," +
                    listOfFavorites_field+" BLOB," +
                    "PRIMARY KEY ("+username_field+","+domain_field+","+host_field+","+port_field+")" +
                    ");";

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

    public Map<Long,Map.Entry<String, String>> getAllRowsOfXreFavoritesTable() {
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

//    public Map<Long,AuthData> getAllSftpCreds() {
//        String[] cols = new String[] {"oid",username_field,domain_field,port_field,password_field};
//        Map<Long,AuthData> m = new HashMap<>();
//        Cursor mCursor = db.query(true, credentialsVaultTableName,cols,null,null, null, null, null, null);
//        if (mCursor!=null) {
//            while (mCursor.moveToNext()) {
//                // uid = username@domain:port
//                AuthData a = new AuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getInt(3),mCursor.getString(4));
//                m.put(mCursor.getLong(0),a);
//            }
//            mCursor.close();
//        }
//        return m;
//    }

    public <T> Map<Long,T> getAllCreds(T ref) {
        String[] cols = (ref instanceof AuthData)?
                new String[] {"oid",username_field,domain_field,port_field,password_field}:
                new String[] {"oid",username_field,domain_field,host_field,port_field,password_field};
        String srcTableName = (ref instanceof AuthData)?credentialsVaultTableName:smbCredentialsVaultTableName;
        Map<Long,T> m = new HashMap<>();
        Cursor mCursor = db.query(true, srcTableName,cols,null,null, null, null, null, null);
        if (mCursor!=null) {
            while (mCursor.moveToNext()) {
                // uid = username@domain:port
                T a = (ref instanceof AuthData)?
                        (T)new AuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getInt(3),mCursor.getString(4)):
                        (T)new SmbAuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getString(3),mCursor.getInt(4),mCursor.getString(5));
                m.put(mCursor.getLong(0), a);
            }
            mCursor.close();
        }
        return m;
    }

    public <T> Map<Long,FavoritesList<T>> getAllCredsWithFavs(T ref) {
        String[] cols = (ref instanceof AuthData)?
                new String[] {"oid",username_field,domain_field,port_field,password_field,listOfFavorites_field}:
                new String[] {"oid",username_field,domain_field,host_field,port_field,password_field,listOfFavorites_field};
        String srcTableName = (ref instanceof AuthData)?credentialsVaultTableName:smbCredentialsVaultTableName;
        Map<Long,FavoritesList<T>> m = new HashMap<>();
        Cursor mCursor = db.query(true, srcTableName,cols,null,null, null, null, null, null);
        if (mCursor!=null) {
            while (mCursor.moveToNext()) {
                T a = (ref instanceof AuthData)?
                        (T)new AuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getInt(3),mCursor.getString(4)):
                        (T)new SmbAuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getString(3),mCursor.getInt(4),mCursor.getString(5));
                byte[] b = mCursor.getBlob(5 + ((ref instanceof SmbAuthData)?1:0));
                if (b == null || b.length == 0) {
                    m.put(mCursor.getLong(0),new FavoritesList<>(a));
                }
                else {
                    ObjectInputStream ois;
                    Set<String> favs;
                    try {
                        ois = new ObjectInputStream(new ByteArrayInputStream(b));
                        favs = (Set) ois.readObject();
                        m.put(mCursor.getLong(0),new FavoritesList<>(a,favs));
                    }
                    catch (IOException | ClassNotFoundException e) {
                        Log.e(this.getClass().getName(),"Exception: "+e.getMessage());
                        e.printStackTrace();
                        m.put(mCursor.getLong(0),new FavoritesList<>(a));
                    }
                }
            }
            mCursor.close();
        }
        return m;
    }

    public <T> Map.Entry<Long,T> insertCred(T ref, String username, String domain, int port, String password, String... host) throws InsertFailedException {
        // password can be null
        ContentValues cv = new ContentValues();
        cv.put(username_field,username);
        cv.put(domain_field,domain);
        cv.put(port_field,port);
        cv.put(password_field,password);
        if(ref instanceof SmbAuthData) {
            cv.put(host_field,host[0]);
            long oid = db.insert(smbCredentialsVaultTableName,null,cv);
            if (oid == -1) throw new InsertFailedException();
            return new AbstractMap.SimpleEntry(oid, new SmbAuthData(username,domain,host[0],port,password)); // for updating list adapter in SmbVaultActivity
        }
        else {
            long oid = db.insert(credentialsVaultTableName,null,cv);
            if (oid == -1) throw new InsertFailedException();
            return new AbstractMap.SimpleEntry(oid, new AuthData(username,domain,port,password)); // for updating list adapter in SmbVaultActivity
        }
    }

    public <T> boolean updateCred(long oldOid, T newAuthData_) {
        ContentValues cv = new ContentValues();
        if (newAuthData_ instanceof SmbAuthData) { // TODO make AuthData and SmbAuthData implement common interface or SmbAuthData subclass of AuthData
            SmbAuthData newAuthData = (SmbAuthData) newAuthData_;
            cv.put(username_field, newAuthData.username);
            cv.put(domain_field, newAuthData.domain);
            cv.put(host_field, newAuthData.host);
            cv.put(port_field, newAuthData.port);
            cv.put(password_field, newAuthData.password == null?"": newAuthData.password);
            return db.update(smbCredentialsVaultTableName,cv,"oid="+oldOid,null)>0;

        }
        else if (newAuthData_ instanceof AuthData){
            AuthData newAuthData = (AuthData) newAuthData_;
            cv.put(username_field, newAuthData.username);
            cv.put(domain_field, newAuthData.domain);
            cv.put(port_field, newAuthData.port);
            cv.put(password_field, newAuthData.password == null?"": newAuthData.password);
            return db.update(credentialsVaultTableName,cv,"oid="+oldOid,null)>0;
        }
        else throw new RuntimeException("invalid authdata type");
//        else {
//            try {
//                Map<String,Object> newAuthData = (Map)newAuthData_;
//                Constructor<ContentValues> ctor = ContentValues.class.getDeclaredConstructor(HashMap.class);
//                ctor.setAccessible(true);
//                cv = ctor.newInstance(new HashMap(newAuthData));
//                // ASSUMPTION: must provide all the kv pairs even if not all are updated
//                return db.update((newAuthData.containsKey("host")?smbCredentialsVaultTableName:credentialsVaultTableName),cv,"oid="+oldOid,null)>0;
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
    }

    /**
     * delete all favorites: along with credentials (deleteRowFromSftpTable(long))
     * or with empty list as input
     * @param oldOid
     * @param newFavs
     * @return true on update success
     */

    public <T> boolean updateFavs(T ref, long oldOid, Set<String> newFavs) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(newFavs);
            out.flush();
            byte[] bytes = bos.toByteArray();
            ContentValues cv = new ContentValues();
            cv.put(listOfFavorites_field,bytes);
            return db.update(ref instanceof AuthData?credentialsVaultTableName:smbCredentialsVaultTableName,cv,"oid="+oldOid,null)>0;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public <T> boolean deleteRowFromTable(T ref, long oid) {
        return db.delete(ref instanceof AuthData?credentialsVaultTableName:smbCredentialsVaultTableName,"oid="+oid,null)>0;
    }

    // input: (Smb)AuthData without password
    // output: (Smb)AuthData (possibly) with password

    public <T> T find(T a_) {
        if(a_ instanceof AuthData) {
            AuthData a = (AuthData) a_;
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
                return (T) o;
            }
        }
        else {
            SmbAuthData a = (SmbAuthData) a_;
            String[] cols = new String[] {"oid",username_field,domain_field,host_field,port_field,password_field};
            String selection = username_field+"= ? and "+domain_field+"= ? and "+host_field+" = ? and "+port_field+" = ?";
            String[] selectionArgs = new String[]{a.username,a.domain,a.host,a.port+""};
            Cursor mCursor = db.query(true, smbCredentialsVaultTableName,cols,selection,selectionArgs, null, null, null, null);
            if (mCursor!=null) {
                SmbAuthData o = null;
                if (mCursor.moveToNext()) {
                    o = new SmbAuthData(mCursor.getString(1),mCursor.getString(2),mCursor.getString(3),mCursor.getInt(4),mCursor.getString(5));
                }
                mCursor.close();
                return (T) o;
            }
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
        db.execSQL(sqlCreateStatement_smbCredentialsAndFavorites);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
