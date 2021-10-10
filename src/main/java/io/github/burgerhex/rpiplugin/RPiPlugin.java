package io.github.burgerhex.rpiplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.function.Function;

//@SuppressWarnings("unused")
public final class RPiPlugin extends JavaPlugin {
    public static final String HOST = "avivshai-pi";
    public static final int PORT = 57944;

    public static final int MAX_READING = 1023;
    public static final int RADIUS = 10;

    private Socket socket;
//    private BukkitRunnable receiveThread;
//    private PrintWriter out;
    private BufferedReader in;

    private World world;
    private Location center;

    @Override
    public void onEnable() {
        getLogger().info("Connecting to Raspberry Pi...");

        try {
            socket = new Socket(HOST, PORT);
            getLogger().info("Connected to Raspberry Pi!");
//            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

//            out.println("Hello RPi");
            BukkitRunnable receiveThread = new BukkitRunnable() {
                @Override
                public void run() {
                    recvLoop();
                }
            };
            receiveThread.runTask(this);
            getLogger().info("after");
        } catch (IOException e) {
            getLogger().warning("Couldn't connect to server; aborting!");
        }

        Iterator<? extends Player> iterator = Bukkit.getServer().getOnlinePlayers().iterator();
        getLogger().info("Getting players...");
        if (iterator.hasNext()) {
            Player p = iterator.next();
            world = p.getWorld();
            getLogger().info("Got player: " + p.getDisplayName());
            center = new Location(world, 218, 73, 78);
        } else {
            getLogger().info("No players available, world could not be fetched!");
        }
        getLogger().info("done with enable");
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
        Function<Integer, Integer> dzToHeight = (dz) -> {
            if (Math.abs(dz) > RADIUS)
                return -1;
            else
                return (int) Math.round(Math.sqrt(RADIUS * RADIUS - dz * dz));
        };

        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            if (dz == 0)
                continue;
            int prevHeight = dzToHeight.apply((dz > 0)? dz - 1 : dz + 1);
            if (dzToHeight.apply(dz) == prevHeight) {
                Location newLoc = center.clone().add(0, dzToHeight.apply(dz) + 1, dz);
                world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
            } else {
                for (int dy = dzToHeight.apply(dz) + 1; dy <= prevHeight; dy++) {
                    Location newLoc = center.clone().add(0, dy, dz);
                    world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
                }
            }
        }

//        for (int dz = -RADIUS; dz < 0; dz++) {
//            if ((int) dzToHeight.apply(dz) == dzToHeight.apply(dz + 1)) {
//                Location newLoc = center.clone().add(0, dzToHeight.apply(dz) + 1, dz);
//                world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
//            } else {
//                for (int dy = dzToHeight.apply(dz) + 1; dy <= dzToHeight.apply(dz + 1); dy++) {
//                    Location newLoc = center.clone().add(0, dy, dz);
//                    world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
//                }
//            }
//        }
//        for (int dz = RADIUS; dz > 0; dz--) {
//            if ((int) dzToHeight.apply(dz) == dzToHeight.apply(dz - 1)) {
//                Location newLoc = center.clone().add(0, dzToHeight.apply(dz) + 1, dz);
//                world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
//            } else {
//                for (int dy = dzToHeight.apply(dz) + 1; dy <= dzToHeight.apply(dz - 1); dy++) {
//                    Location newLoc = center.clone().add(0, dy, dz);
//                    world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
//                }
//            }
//        }
        Location leftLoc = center.clone().add(0, 0, -RADIUS);
        Location rightLoc = center.clone().add(0, 0, RADIUS);
        Location topLoc = center.clone().add(0, dzToHeight.apply(0) + 1, 0);
        world.getBlockAt(leftLoc).setType(Material.OBSIDIAN);
        world.getBlockAt(rightLoc).setType(Material.OBSIDIAN);
        world.getBlockAt(topLoc).setType(Material.OBSIDIAN);

        int i = 0;
        while (i == 0) {
            String line;
            try {
                line = in.readLine();
                getLogger().info("Received reading from RPi: " + line);
//                Bukkit.broadcastMessage("reading from rpi: " + line);
                getLogger().info("Attempting to set blocks...");
                double portion = Integer.parseInt(line) / (double) MAX_READING;
                double angle = portion * Math.PI;
                getLogger().info("portion = " + portion + ", and angle = " +
                                 Math.toDegrees(angle) + " degrees");
                double tan = Math.tan(angle);
                double dx = 0;

                for (int dz = -RADIUS + 1; dz <= RADIUS - 1; dz++) {
                    int height = dzToHeight.apply(dz);
                    double maxY = -tan * dz;
                    getLogger().info("starting dz = " + dz + ", which has height = " +
                                     "sqrt(" + RADIUS + "^2 - " + "(" + dz + ")^2) = " + height +
                                     ", and should stop placing at " + maxY);
//                    getLogger().info("tan = " + tan + ", -tan * dz = " + maxY);

                    for (int dy = 0; dy <= height; dy++) {
                        Location newLoc = center.clone().add(dx, dy, dz);
                        Material mat = ((dy <= maxY && portion < 0.5) || (dy > maxY && portion >= 0.5))?
                                Material.STONE : Material.AIR;
//                        getLogger().info("setting (dz=" + dz + ", dy=" + dy + ") or (" +
//                                         newLoc.getBlockX() + ", " + newLoc.getBlockY() + ", " +
//                                         newLoc.getBlockZ() + ") to " + mat.name());
                        world.getBlockAt(newLoc).setType(mat);
                    }
                }


            } catch (IOException e) {
                getLogger().warning("Error in receiving from RPi! Stopping...");
                e.printStackTrace();
                break;
            }
            i++;
        }
    }

    @Override
    public void onDisable() {
        try {
            System.out.println("Closing socket...");
            if (socket != null)
                socket.close();
//            if (receiveThread != null)
//                receiveThread.interrupt();
        } catch (IOException e) {
            getLogger().warning("Couldn't close socket gracefully!");
        }
    }
}
