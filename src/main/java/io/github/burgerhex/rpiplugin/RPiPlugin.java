package io.github.burgerhex.rpiplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

//@SuppressWarnings("unused")
public final class RPiPlugin extends JavaPlugin {
    public static final String HOST = "avivshai-pi";
    public static final int PORT = 57944;

    private Socket socket;

//    private PrintWriter out;
    private BufferedReader in;

    @Override
    public void onEnable() {
        getLogger().info("Connecting to Raspberry Pi...");

        try {
            socket = new Socket(HOST, PORT);
            getLogger().info("Connected to Raspberry Pi!");
//            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

//            out.println("Hello RPi");
            new Thread(this::recvLoop).start();
        } catch (IOException e) {
            getLogger().warning("Couldn't connect to server; aborting!");
        }
    }

    public void recvLoop() {
        while (true) {
            String line;
            try {
                line = in.readLine();
//                getLogger().info("Received reading from RPi: " + line);
                Bukkit.broadcastMessage("reading from rpi: " + line);
            } catch (IOException e) {
                getLogger().warning("Error in receiving from RPi! Stopping...");
                e.printStackTrace();
                break;
            }
        }
    }

    @Override
    public void onDisable() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            getLogger().warning("Couldn't close socket gracefully!");
        }
    }
}
