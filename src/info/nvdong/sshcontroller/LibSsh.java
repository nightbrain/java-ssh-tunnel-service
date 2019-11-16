package info.nvdong.sshcontroller;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;

import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.List;

public class LibSsh {
  private SshClient client;
  private ClientSession session;
  private int port = -1;

  public boolean connect(String host, String username, String password) {
    System.out.println("Connect: " + host + "|" + username + "|" + password + ".");
    boolean isConnected = false;
    client = SshClient.setUpDefaultClient();
    client.setForwardingFilter(AcceptAllForwardingFilter.INSTANCE);
    client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
    client.start();
    try {
      session = client.connect(username, host, 22).verify(5000).getSession();
      session.addPasswordIdentity(password);
      session.auth().verify(10000);
      SshdSocketAddress sshdSocketAddress =
          session.startDynamicPortForwarding(new SshdSocketAddress("127.0.0.1", getFreePort()));
      Proxy proxy =
          new Proxy(
              Proxy.Type.SOCKS,
              new InetSocketAddress(sshdSocketAddress.getHostName(), sshdSocketAddress.getPort()));
      HttpURLConnection connection =
          (HttpURLConnection) new URL("https://www.googleapis.com/").openConnection(proxy);
      connection.setConnectTimeout(2500);
      if (connection.getResponseCode() == 404) {
        connection.disconnect();
        try {
          this.port = sshdSocketAddress.getPort();
          isConnected = true;
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        connection.disconnect();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return isConnected;
  }

  public void disconnect() {
    if (this.port != -1) System.out.println("Disconnect: " + port + ".");
    try {
      if (session != null) session.close();
    } catch (Exception ignored) {
    }
    try {
      if (client != null) client.stop();
    } catch (Exception ignored) {
    }
    session = null;
    client = null;
  }

  public static void disableLogger() {
    List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
    loggers.add(LogManager.getRootLogger());
    for (Logger logger : loggers) {
      logger.setLevel(Level.OFF);
    }
  }

  private int getFreePort() {
    int port = -1;
    do {
      try {
        ServerSocket s = new ServerSocket(0);
        s.close();
        port = s.getLocalPort();
      } catch (Error | IOException e) {
        e.printStackTrace();
      }
    } while (port == -1);
    return port;
  }

  public int getPort() {
    return port;
  }
}
