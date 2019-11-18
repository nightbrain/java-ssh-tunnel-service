package info.nvdong.sshcontroller;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class Main {
  private static Jedis jedis2;
  private static Semaphore getSshSph = new Semaphore(1);
  private static Semaphore publishSph = new Semaphore(1);
  private static Semaphore clearSph = new Semaphore(1);
  private static ArrayList<String[]> sshLists = new ArrayList();
  private static HashMap<Integer, LibSsh> connecterLists = new HashMap();
  private static boolean isCleared = false;

  public static void main(String[] args) {
    LibSsh.disableLogger();
    try {
      Jedis jedis = new Jedis();
      jedis2 = new Jedis();
      new Thread(
              () -> {
                while (true) {
                  try {
                    long listeners = jedis2.publish("SSH_CHANNEL_OK", "-1");
                    if (listeners==0) {
                      jedis2.publish("SSH_CHANNEL", "clear|~");
                    }
                    Thread.sleep(3000);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
              })
          .start();
      JedisPubSub jedisPubSub =
          new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
              String[] splitedMessage = message.split("\\|");
              if (splitedMessage.length == 2) {
                Action action = new Action();
                action.setAction(splitedMessage[0]);
                action.setData(splitedMessage[1]);
                Main.onMessage(action);
              }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
              super.onSubscribe(channel, subscribedChannels);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
              super.onUnsubscribe(channel, subscribedChannels);
            }
          };
      jedis.subscribe(jedisPubSub, "SSH_CHANNEL");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void onMessage(Action action) {
    switch (action.getAction()) {
      case "connect":
        try {
          clearSph.acquire();
          isCleared = false;
          clearSph.release();
        } catch (Exception e) {
          e.printStackTrace();
        }
        int time = Integer.parseInt(action.getData());
        for (int i = 0; i < time; i++) {
          new Thread(Main::startConnect).start();
        }
        break;
      case "disconnect":
        int port = Integer.parseInt(action.getData());
        if (connecterLists.containsKey(port)) {
          connecterLists.get(port).disconnect();
          connecterLists.remove(port);
        }
        break;
      case "old":
      case "current":
        for (LibSsh value : connecterLists.values()) {
          jedis2.publish("SSH_CHANNEL_OK", value.getPort() + "");
        }
        break;
      case "clear":
        try {
          clearSph.acquire();
          isCleared = true;
          clearSph.release();
        } catch (Exception e) {
          e.printStackTrace();
        }
        for (LibSsh value : connecterLists.values()) {
          value.disconnect();
        }
        connecterLists.clear();
        break;
    }
  }

  private static void startConnect() {
    LibSsh libSsh = new LibSsh();
    boolean isConnected;
    do {
      String[] ssh = getSsh();
      isConnected = libSsh.connect(ssh[0], ssh[1], ssh[2]);
      if (isConnected) {
        if (connecterLists.containsKey(libSsh.getPort())) {
          connecterLists.get(libSsh.getPort()).disconnect();
          connecterLists.remove(libSsh.getPort());
        }
        boolean dis = false;
        try {
          clearSph.acquire();
          dis = isCleared;
          clearSph.release();
        } catch (Exception e) {
          e.printStackTrace();
        }
        if (dis) {
          libSsh.disconnect();
        } else {
          connecterLists.put(libSsh.getPort(), libSsh);
          try {
            publishSph.acquire();
            jedis2.publish("SSH_CHANNEL_OK", libSsh.getPort() + "");
            publishSph.release();
          } catch (Exception e) {
            e.printStackTrace();
          }
          System.out.println("Tunnel: " + ssh[0] + " -> :" + libSsh.getPort() + ".");
        }
      } else libSsh.disconnect();
    } while (!isConnected);
  }

  private static String[] getSsh() {
    String[] ssh = null;
    try {
      getSshSph.acquire();
      if (sshLists.size() == 0) {
        String content = readFile("ssh.txt");
        if (content != null) {
          String[] lines = content.split("\n");
          for (String line : lines) {
            String[] lineData = line.split("\\|");
            if (lineData.length > 2) {
              sshLists.add(lineData);
            }
          }
          Collections.shuffle(sshLists);
        }
      }
      if (sshLists.size() > 0) {
        ssh = sshLists.remove(0);
      }
      getSshSph.release();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return ssh;
  }

  private static String readFile(String fileName) {
    try (FileReader fileReader = new FileReader(fileName)) {
      int ch;
      StringBuilder str = new StringBuilder();
      while ((ch = fileReader.read()) != -1) {
        str.append((char) ch);
      }
      return str.toString().trim();
    } catch (IOException ignored) {
    }
    return null;
  }
}
