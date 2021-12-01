package io.github.burgerhex.rpiplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class RPiReader {
    private final ServerSocket serverSocket;
    private BufferedReader in;
    private final Logger logger;
    private final AtomicInteger latestRead = new AtomicInteger(-1);
    private final Thread recvThread;
    private final AtomicBoolean isLatestSeen;

    public RPiReader(ServerSocket serverSocket, Logger logger) throws IOException {
        this.serverSocket = serverSocket;
        this.logger = logger;
        recvThread = new Thread(this::recvLoop);
        logger.info("Making new thread: " + recvThread.getName());
        recvThread.start();
        isLatestSeen = new AtomicBoolean(false);
    }

    private void recvLoop() {
        String threadName = Thread.currentThread().getName();

        while (!Thread.interrupted() && !serverSocket.isClosed()) {
            try {
                logger.info("Waiting for new connection from RPi...");
                Socket socket = serverSocket.accept();
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                logger.info("Got a connection!");

                while (!Thread.interrupted() && !serverSocket.isClosed() && !socket.isClosed()) {
                    String line = null;
                    try {
                        line = in.readLine();
//                logger.info(threadName + ": read line: " + line + ", which is " +
//                            (line == null? "" : "not ") + "null");
                        if (line != null)  {
                            latestRead.set(Integer.parseInt(line));
                            isLatestSeen.set(false);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    } catch (NumberFormatException e) {
                        logger.info(threadName + ": malformed message from RPi: " + line);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(threadName + ": stopping!");
    }

    public boolean isLatestSeen() { return isLatestSeen.get(); }

    public Integer getLatest() {
        isLatestSeen.set(true);
        return latestRead.get();
    }

    public void stop() {
        System.out.println("Interrupting " + recvThread.getName() + "...");
        recvThread.interrupt();
    }
}
