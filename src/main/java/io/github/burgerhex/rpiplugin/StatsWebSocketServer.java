// code adapted from https://github.com/TooTallNate/Java-WebSocket/blob/master/src/main/example/ChatServer.java

package io.github.burgerhex.rpiplugin;

/*
 * Copyright (c) 2010-2020 Nathan Rajlich
 *
 *  Permission is hereby granted, free of charge, to any person
 *  obtaining a copy of this software and associated documentation
 *  files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use,
 *  copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following
 *  conditions:
 *
 *  The above copyright notice and this permission notice shall be
 *  included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 *  OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 *  HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 */

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 */
public class StatsWebSocketServer extends WebSocketServer {
    Logger logger;

    public StatsWebSocketServer(String host, int port, Logger logger) throws UnknownHostException {
        super(new InetSocketAddress(host, port));
        this.logger = logger;
        start();
        logger.info("ws open on: " + getAddress());
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conn.send("{\"message\": \"welcome!\"}");
        logger.info("new connection: " + conn.getRemoteSocketAddress() + ", " +
                           handshake.getResourceDescriptor());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info("destroyed connection: " + conn.getRemoteSocketAddress() + ", reason = " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.info("new message from " + conn.getResourceDescriptor() + ": " + message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        onMessage(conn, new String(message.array()));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        logger.info("server started");
//        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

}