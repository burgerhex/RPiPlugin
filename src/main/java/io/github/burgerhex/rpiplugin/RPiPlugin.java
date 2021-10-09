package io.github.burgerhex.rpiplugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

//@SuppressWarnings("unused")
public final class RPiPlugin extends JavaPlugin {

    public static final int PORT = 57944;

    private ServerSocket serverSocket;
    private Socket socket;

    private PrintWriter out;
    private BufferedReader in;

    @Override
    public void onEnable() {
        getLogger().info("Starting server...");

        try {
            // TODO: start a new thread with this, since this is
            //  blocking and probably shouldn't be done in onEnable()
            serverSocket = new ServerSocket(PORT);
            getLogger().info("Waiting for connection from RPi...");
            socket = serverSocket.accept();
            getLogger().info("Got a connection!");
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Hello RPi");

            String line = in.readLine();
            while (line == null)
                line = in.readLine();

            getLogger().info("Received this from RPi: " + line);
        } catch (IOException e) {
            getLogger().warning("Couldn't start server and get connection; aborting!");
//            return;
        }

    }

    @Override
    public void onDisable() {
        try {
            if (serverSocket != null)
                serverSocket.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            getLogger().warning("Couldn't close sockets!");
        }
    }
}
