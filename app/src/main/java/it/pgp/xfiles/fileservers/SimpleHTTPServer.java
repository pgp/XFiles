package it.pgp.xfiles.fileservers;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Pair;

public class SimpleHTTPServer extends SimpleFileServer {
    private static final int defaultPort = 8000;

    private AcceptorThread acceptorThread;

    public SimpleHTTPServer() {
        serverButtonRes = R.id.httpServerButton;
        port = defaultPort;
    }

    private class AcceptorThread extends Thread {
        public final ServerSocket acceptorSocket;

        AcceptorThread(int port) throws IOException {
            acceptorSocket = new ServerSocket(port);
        }

        @Override
        public void run() {
            for(;;) {
                try {
                    Socket connection = acceptorSocket.accept();
                    new HTTPSessionThread(connection,rootPath).start();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    SimpleHTTPServer.this.acceptorThread = null;
                    MainActivity.showToastOnUIWithHandler("SimpleHTTPServer: "+(e instanceof SocketException?"acceptor closed":"accept error"));
                    notifyObservers(new Pair<>(FileServer.HTTP.ordinal(), false));
                    return;
                }
            }
        }
    }

    @Override
    public void startServer() {
        if (acceptorThread != null) {
            Log.e(getClass().getName(),"Server object already exists, closing existing...");
            stopServer();
        }
        try {
            acceptorThread = new AcceptorThread(port);
            acceptorThread.start();
        } catch (IOException e) {
            Log.e(getClass().getName(), "IOException - Could not start server: ", e);
            stopServer();
            return;
        }
        MainActivity.showToastOnUIWithHandler("SimpleHTTPServer accepting connections on port " + port +", root path: "+rootPath);
        notifyObservers(new Pair<>(FileServer.HTTP.ordinal(), true));
    }

    @Override
    public void stopServer() {
        try {
            acceptorThread.acceptorSocket.close();
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isAlive() {
        if(acceptorThread != null) {
            Thread.State state = acceptorThread.getState();
            return (!Thread.State.NEW.equals(state)
                    && !Thread.State.TERMINATED.equals(state));
        }
        return false;
    }
}