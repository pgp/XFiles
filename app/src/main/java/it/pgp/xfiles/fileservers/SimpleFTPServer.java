package it.pgp.xfiles.fileservers;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;

import it.pgp.xfiles.MainActivity;
import it.pgp.xfiles.R;
import it.pgp.xfiles.utils.Pair;

public class SimpleFTPServer extends SimpleFileServer {

    private FtpServer server;
    public static final int defaultPort = 2121;

    SimpleFTPServer() {
        port = defaultPort;
        serverButtonRes = R.id.ftpServerButton;
    }

    @Override
    public void stopServer() {
        try { server.stop(); }
        catch (Exception e) {e.printStackTrace();}
        server = null;
        MainActivity.showToast("FTP server stopped");
        notifyObservers(new Pair<>(FileServer.FTP.ordinal(), false));
    }

    @Override
    public void startServer() {
        if (server != null) stopServer();
        FtpServerFactory serverFactory = new FtpServerFactory();
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);

        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(port);
        serverFactory.addListener("default", factory.createListener());

        BaseUser user = new BaseUser();
        user.setName("anonymous");
        user.setHomeDirectory(rootPath);
        try {
            serverFactory.getUserManager().save(user);
            server = serverFactory.createServer();
            server.start();
            MainActivity.showToast("FTP server started on port "+port+", root path: "+rootPath);
            notifyObservers(new Pair<>(FileServer.FTP.ordinal(), true));
        }
        catch (Exception e) {
            MainActivity.showToast("Error in starting FTP server");
            e.printStackTrace();
            stopServer();
        }
    }

    @Override
    public boolean isAlive() {
        return server != null &&
                !server.isStopped() &&
                !server.isSuspended();
    }
}
