package it.pgp.xfiles.fileservers;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;

public class SimpleHTTPServer extends SimpleFileServer {

    private ServerSocket acceptorSocket;
    private Thread acceptorThread;

    private static final int defaultPort = 8000;

    public SimpleHTTPServer() {
        serverButtonRes = R.id.httpServerButton;
        port = defaultPort;
    }

    private void createServer() {
        if (acceptorSocket != null) {
            Log.e(getClass().getName(),"Server object already exists, closing existing...");
            stopServer();
        }
        try {
            acceptorSocket = new ServerSocket(port);
        } catch (IOException e) {
            Log.e(getClass().getName(), "IOException - Could not start server: ", e);
            stopServer();
            return;
        }
        MainActivity.showToastOnUIWithHandler("SimpleHTTPServer accepting connections on port " + port +", root path: "+rootPath);
        MainActivity.mainActivity.runOnUiThread(()->FileServer.HTTP.refresh_button_color(MainActivity.mainActivity,true));
    }

    private void acceptorLoop(ServerSocket socket) {
        for(;;) {
            try {
                Socket connection = socket.accept();
                new HTTPSessionThread(connection,rootPath).start();
            }
            catch (Exception e) {
                e.printStackTrace();
                acceptorSocket = null;
                MainActivity.showToastOnUIWithHandler("SimpleHTTPServer: "+(e instanceof SocketException?"acceptor closed":"accept error"));
                MainActivity.mainActivity.runOnUiThread(()->FileServer.HTTP.refresh_button_color(MainActivity.mainActivity,false));
                return;
            }
        }
    }

    @Override
    public void startServer() {
        createServer();
        acceptorThread = new Thread(() -> acceptorLoop(acceptorSocket));
        acceptorThread.start();
    }

    @Override
    public void stopServer() {
        try {
            acceptorSocket.close();
        } catch (Exception ignored) {}
        acceptorSocket = null;
    }

    @Override
    public boolean isAlive() {
        return (acceptorThread != null
                && !Thread.State.NEW.equals(acceptorThread.getState())
                && !Thread.State.TERMINATED.equals(acceptorThread.getState()));
    }
}