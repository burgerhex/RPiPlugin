package io.github.burgerhex.rpiplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collection;

//@SuppressWarnings("unused")
public final class RPiPlugin extends JavaPlugin {
    public static final String HOST = "avivshai-pi";
    public static final int PORT = 57944;

    private Socket socket;
    private Thread receiveThread;
//    private PrintWriter out;
    private BufferedReader in;

    private Sign sign;

    @Override
    public void onEnable() {
        getLogger().info("Connecting to Raspberry Pi...");

        try {
            socket = new Socket(HOST, PORT);
            getLogger().info("Connected to Raspberry Pi!");
//            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

//            out.println("Hello RPi");
            receiveThread = new Thread(this::recvLoop);
            receiveThread.start();
        } catch (IOException e) {
            getLogger().warning("Couldn't connect to server; aborting!");
        }

        Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
        getLogger().info("Getting players...");
        if (players.iterator().hasNext()) {
            Player p = players.iterator().next();
            getLogger().info("Got player: " + p.getDisplayName());
            Location signPos = p.getLocation();
            Block signBlock = p.getWorld().getBlockAt(signPos);
            signBlock.setType(Material.OAK_SIGN);
            getLogger().info("Set block at (" + signPos.getBlockX() + ", " + signPos.getBlockY() +
                             ", " + signPos.getBlockZ() + ") to oak sign");
            sign = (Sign) signBlock.getState();
            getLogger().info("Sign: " + sign);
        } else {
            getLogger().info("No players available, sign will not be created!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (command.getName().equalsIgnoreCase("stopread")) {
                Bukkit.broadcastMessage("Stopping RPi reading and closing socket");
                onDisable();
                return true;
            }
        }

        return false;
    }

    public void recvLoop() {
        while (true) {
            String line;
            try {
                line = in.readLine();
//                getLogger().info("Received reading from RPi: " + line);
//                Bukkit.broadcastMessage("reading from rpi: " + line);
                getLogger().info("Attempting to set sign (" + sign + ")...");
                if (sign != null) {
                    getLogger().info("Set sign successfully!");
                    sign.setLine(0, line);
                } else {
                    getLogger().info("Could not set sign because it was null!");
                }
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
            System.out.println("Closing socket...");
            if (socket != null)
                socket.close();
            if (receiveThread != null)
                receiveThread.interrupt();
        } catch (IOException e) {
            getLogger().warning("Couldn't close socket gracefully!");
        }
    }
}
