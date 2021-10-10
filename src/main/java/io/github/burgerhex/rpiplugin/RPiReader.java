package io.github.burgerhex.rpiplugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class RPiReader {
    private final BufferedReader in;
    private final Logger logger;
    private final AtomicInteger latestRead = new AtomicInteger(0);

    public RPiReader(BufferedReader br, Logger logger) {
        in = br;
        this.logger = logger;
        new Thread(this::recvLoop).start();
    }

    private void recvLoop() {
        while (true) {
            String line = null;
            try {
                line = in.readLine();
                latestRead.set(Integer.parseInt(line));
            } catch (IOException e) {
                e.printStackTrace();
                break;
            } catch (NumberFormatException e) {
                logger.info("Malformed message from RPi: " + line);
            }
        }
    }

    public Integer getNext() {
        return latestRead.get();
    }

}
