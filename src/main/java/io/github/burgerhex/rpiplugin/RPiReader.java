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
    private final Thread recvThread;
    private final AtomicInteger latestRotaryRead = new AtomicInteger(-1);
    private final AtomicBoolean isLatestRotarySeen = new AtomicBoolean(false);
    private final AtomicInteger latestTempRead = new AtomicInteger(-1);
    private final AtomicBoolean isLatestTempSeen = new AtomicBoolean(false);
    private final AtomicInteger latestHumidityRead = new AtomicInteger(-1);
    private final AtomicBoolean isLatestHumiditySeen = new AtomicBoolean(false);
    private final AtomicBoolean isButtonPressed = new AtomicBoolean(false);

    public RPiReader(ServerSocket serverSocket, Logger logger) throws IOException {
        this.serverSocket = serverSocket;
        this.logger = logger;
        recvThread = new Thread(this::recvLoop);
        recvThread.start();
        logger.info("Making new thread: " + recvThread.getName());
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
                            String[] parts = line.split(" ");
                            logger.info(String.join(" ", parts));

                            if (parts[0].equalsIgnoreCase("rotary")) {
                                latestRotaryRead.set(Integer.parseInt(parts[1]));
                                isLatestRotarySeen.set(false);
                            } else if (parts[0].equalsIgnoreCase("temp")) {
                                latestTempRead.set((int) Double.parseDouble(parts[1]));
                                isLatestTempSeen.set(false);
                            } else if (parts[0].equalsIgnoreCase("humidity")) {
                                latestHumidityRead.set((int) Double.parseDouble(parts[1]));
                                isLatestHumiditySeen.set(false);
                            } else if (parts[0].equalsIgnoreCase("button")) {
                                isButtonPressed.set(true);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
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

    public boolean isLatestRotarySeen() { return isLatestRotarySeen.get(); }

    public int getLatestRotary() {
        isLatestRotarySeen.set(true);
        return latestRotaryRead.get();
    }

    public boolean isLatestTempSeen() { return isLatestTempSeen.get(); }

    public int getLatestTemp() {
        isLatestTempSeen.set(true);
        return latestTempRead.get();
    }

    public boolean isLatestHumiditySeen() { return isLatestHumiditySeen.get(); }

    public int getLatestHumidity() {
        isLatestHumiditySeen.set(true);
        return latestHumidityRead.get();
    }

    public boolean getButtonPressed() {
        if (isButtonPressed.get()) {
            isButtonPressed.set(false);
            return true;
        }
        return false;
    }

    public void stop() {
        System.out.println("Interrupting " + recvThread.getName() + "...");
        recvThread.interrupt();
    }
}
