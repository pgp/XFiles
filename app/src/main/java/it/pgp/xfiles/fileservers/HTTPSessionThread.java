package it.pgp.xfiles.fileservers;

import android.util.Log;

import org.apache.commons.lang3.CharUtils;

import java.io.*;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import it.pgp.xfiles.BrowserItem;
import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.io.RobustLocalFileInputStream;
import it.pgp.xfiles.items.SingleStatsItem;
import it.pgp.xfiles.utils.dircontent.GenericDirWithContent;
import it.pgp.xfiles.utils.pathcontent.LocalPathContent;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 * Adapted from:
 * http://cs.au.dk/~amoeller/WWW/examples/FileServer.java
 */

public class HTTPSessionThread extends Thread {

    private final Socket connection;
    private final String wwwhome;

    /**
     * Takes UTF-8 strings and encodes non-ASCII as
     * ampersand-octothorpe-digits-semicolon
     * HTML-encoded characters
     *
     * @param string
     * @return HTML-encoded String
     */
    private static String htmlEncode2(final String string) {
        final StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (CharUtils.isAscii(character)) {
                // Encode common HTML equivalent characters
                stringBuffer.append(
                        escapeHtml4(Character.toString(character)));
            } else {
                // Why isn't this done in escapeHtml4()?
                stringBuffer.append(
                        String.format("&#x%x;",
                                Character.codePointAt(string, i)));
            }
        }
        return stringBuffer.toString();
    }

    private static String getHeader(String path) throws UnsupportedEncodingException {
        return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n"+
                "<html>\n" +
                "<head>\n" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
                "<title>Directory listing for "+escapeHtml4(path)+"</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>Directory listing for "+escapeHtml4(path)+"</h1>\n" +
                "<hr>\n" +
                "<ul>\n";
    }

    private static final String footer = "</ul>\n" +
            "<hr>\n" +
            "</body>\n" +
            "</html>";

    // html dir listing output copied from python3 http.server module
    private static String assembleDirList(LocalPathContent path) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();

        // 1st attempt, Java
        try {
            List<File> dircontent = Arrays.asList(new File(path.dir).listFiles());
            Collections.sort(dircontent);

            for (File f: dircontent) {
                String dirTermination = f.isDirectory()?"/":"";
                sb.append("<li><a href=\"");
                sb.append(URLEncoder.encode(f.getName(), "UTF-8")).append(dirTermination);
                sb.append("\">");
                sb.append(htmlEncode2(f.getName())).append(dirTermination);
                sb.append("</a></li>\n");
            }

            return sb.toString();
        }
        catch (Exception e) {
            Log.e("assembleDirList","Exception during listdir, trying with roothelper...", e);
        }

        // 2nd attempt, via RH
        sb = new StringBuilder();
        GenericDirWithContent gdwc = MainActivity.getRootHelperClient().listDirectory(path);

        if (gdwc.errorCode == null)
            for (BrowserItem f: gdwc.content) {
                String dirTermination = (f.isDirectory?"/":"");
                sb.append("<li><a href=\"");
                sb.append(URLEncoder.encode(f.getFilename(), "UTF-8")).append(dirTermination);
                sb.append("\">");
                sb.append(htmlEncode2(f.getFilename())).append(dirTermination);
                sb.append("</a></li>\n");
            }

        return sb.toString();
    }

    private static String guessContentType(String path)
    {
        if (path.endsWith(".html") || path.endsWith(".htm"))
            return "text/html";
        else if (path.endsWith(".txt") || path.endsWith(".java"))
            return "text/plain";
        else if (path.endsWith(".gif"))
            return "image/gif";
        else if (path.endsWith(".class"))
            return "application/octet-stream";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        else
            return "text/plain";
    }

    private static void sendFile(InputStream in, OutputStream out)
    {
        try {
            byte[] buffer = new byte[1000];
            for(;;) {
                int readBytes = in.read(buffer);
                if (readBytes <= 0) {
                    Log.e("sendFile", "EOF or read error");
                    break;
                }
                out.write(buffer,0,readBytes);
            }
        }
        catch (IOException e) {
            Log.e("sendFile", "IOEXCEPTION or EOF", e);
        }
    }

    private static void log(Socket connection, String msg)
    {
        Log.e(HTTPSessionThread.class.getName(), new Date() + " [" + connection.getInetAddress().getHostAddress() + ":" + connection.getPort() + "] " + msg);
    }

    private static void errorReport(PrintStream pout, Socket connection,
                                    String code, String title, String msg)
    {
        pout.print("HTTP/1.0 " + code + " " + title + "\r\n" +
                "\r\n" +
                "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
                "<TITLE>" + code + " " + title + "</TITLE>\r\n" +
                "</HEAD><BODY>\r\n" +
                "<H1>" + title + "</H1>\r\n" + msg + "<P>\r\n" +
                "<HR><ADDRESS>SimpleHTTPServer 1.0 at " +
                connection.getLocalAddress().getHostName() +
                " Port " + connection.getLocalPort() + "</ADDRESS>\r\n" +
                "</BODY></HTML>\r\n");
        log(connection, code + " " + title);
    }

    public HTTPSessionThread(Socket connection, String wwwhome) {
        this.connection = connection;
        this.wwwhome = wwwhome;
    }

    @Override
    public void run() {
        Log.i(getClass().getName(),"Client session thread started");
        try {
            // wait for request
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            OutputStream out = new BufferedOutputStream(connection.getOutputStream());
            PrintStream pout = new PrintStream(out);

            // read first line of request (ignore the rest)
            String request = in.readLine();
            if (request==null) {
                Log.i(getClass().getName(),"Client session ended");
                return;
            }
            log(connection, request);
            while (true) {
                String misc = in.readLine();
                if (misc==null || misc.length()==0)
                    break;
            }

            // parse the line
            if (!request.startsWith("GET") || request.length()<14 ||
                    !(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))) {
                // bad request
                errorReport(pout, connection, "400", "Bad Request",
                        "Your browser sent a request that " +
                                "this server could not understand.");
            } else {
                String req = request.substring(4, request.length()-9).trim();
                if (req.indexOf("..")!=-1 ||
                        req.indexOf("/.ht")!=-1 || req.endsWith("~")) {
                    // evil hacker trying to read non-wwwhome or secret file
                    errorReport(pout, connection, "403", "Forbidden",
                            "You don't have permission to access the requested URL.");
                } else {
                    String subpath = URLDecoder.decode(req,"UTF-8");
                    String path = wwwhome + "/" + subpath;
                    LocalPathContent lpc = new LocalPathContent(path);
                    boolean isDir = MainActivity.getRootHelperClient().isDir(lpc);
//                    File f = new File(path);
                    if (isDir && !path.endsWith("/")) {
                        // redirect browser if referring to directory without final '/'
                        pout.print("HTTP/1.0 301 Moved Permanently\r\n" +
                                "Location: http://" +
                                connection.getLocalAddress().getHostAddress() + ":" +
                                connection.getLocalPort() + (req.startsWith("/")?"":"/") + req + "/\r\n\r\n");
                        log(connection, "301 Moved Permanently");
                    }
                    else {
                        if (isDir) {
                                /*// if directory, implicitly add 'index.html'
                                path = path + "index.html";
                                f = new File(path);*/

                            // generate and send HTML with dir content
                            try {
                                pout.print("HTTP/1.0 200 OK\r\n" +
                                        "Content-Type: text/html\r\n" +
                                        "Date: " + new Date() + "\r\n" +
                                        "Server: SimpleHTTPServer 1.0\r\n\r\n");
                                pout.print(getHeader(subpath)+assembleDirList(lpc)+footer);
                            } catch (Exception e) {
                                // file not found
                                errorReport(pout, connection, "400", "Not Found",
                                        "The requested URL was not found on this server.");
                            }
                        }
                        else {
                            // stat in order to populate Content-Length header with file size
                            long contentLength;
                            try {
                                SingleStatsItem stat = MainActivity.getRootHelperClient().statFile(new LocalPathContent(path));
                                contentLength = stat.size;
                            }
                            catch(Exception e) {
                                e.printStackTrace();
                                contentLength = -1;
                            }

                            try (RobustLocalFileInputStream file = new RobustLocalFileInputStream(path)) {
                                // send file
//                                InputStream file = new FileInputStream(f);
                                pout.print("HTTP/1.0 200 OK\r\n" +
                                        "Content-Type: " + guessContentType(path) + "\r\n" +
                                        ((contentLength>=0)?("Content-Length: "+contentLength+"\r\n"):"")+
                                        "Date: " + new Date() + "\r\n" +
                                        "Server: SimpleHTTPServer 1.0\r\n\r\n");
                                sendFile(file, out); // send raw file
                                log(connection, "200 OK");
                            }
                            catch (FileNotFoundException e) {
                                Log.e("HTTPSessionThread", "FNFException", e);
                                // file not found
                                errorReport(pout, connection, "404", "Not Found",
                                        "The requested URL was not found on this server.");
                            }
                        }
                    }
                }
            }
            out.flush();
        }
        catch (IOException e) {
            Log.e(getClass().getName(),"IOException", e);
        }
        try {
            connection.close();
        }
        catch (Exception e) {
            Log.e(getClass().getName(),"Exception on close", e);
        }
        Log.i(getClass().getName(),"Client session ended");
    }
}
