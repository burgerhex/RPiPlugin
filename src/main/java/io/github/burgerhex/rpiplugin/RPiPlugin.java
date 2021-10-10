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
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Optional;

public final class RPiPlugin extends JavaPlugin {
    public static final String HOST = "avivshai-pi";
    public static final int PORT = 57944;

    public static final int MAX_READING = 1023;
    public static final int RADIUS = 10;

    private Socket socket;
    private RPiReader reader;
    private BukkitRunnable readLoop;
    private BukkitTask readLoopTask;

    private World world;
    private Location center;


    @Override
    public void onEnable() {
        getLogger().info("Connecting to Raspberry Pi...");

        try {
            socket = new Socket(HOST, PORT);
            getLogger().info("Connected to Raspberry Pi!");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            reader = new RPiReader(in, getLogger());

            Optional<World> worldOpt = Bukkit.getServer().getWorlds().stream().filter(
                    (w) -> w.getEnvironment().equals(World.Environment.NORMAL)).findFirst();
            worldOpt.ifPresentOrElse(w -> {
                center = new Location(w, 218, 73, 78);
                world = w;
            }, () -> getLogger().info("Couldn't get world!"));

            readLoop = new BukkitRunnable() {
                @Override
                public void run() {
                    readAndPlaceInside();
                }
            };
            readLoopTask = readLoop.runTaskTimer(this, 0, 20);
        } catch (IOException e) {
            getLogger().warning("Couldn't connect to server; aborting!");
        }

        getLogger().info("done with enable");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("stopread")) {
                player.sendMessage("Stopping RPi reading and closing socket");
                onDisable();
                return true;
            } else if (command.getName().equalsIgnoreCase("read")) {
                player.sendMessage("Setting border and inside");
                placeBorder();
                readAndPlaceInside();
                return true;
            } else if (command.getName().equalsIgnoreCase("border")) {
                player.sendMessage("Setting border");
                placeBorder();
                return true;
            } else if (command.getName().equalsIgnoreCase("inside")) {
                player.sendMessage("Setting inside");
                readAndPlaceInside();
                return true;
            } else if (command.getName().equalsIgnoreCase("period")) {
                player.sendMessage("Setting period");
                double period;
                try {
                    period = Double.parseDouble(args[0]);
                } catch (NumberFormatException e) {
                    player.sendMessage("Invalid period: " + args[0]);
                    return true;
                }
                readLoopTask.cancel();
                readLoop.cancel();

                // TODO: check this
                readLoop = new BukkitRunnable() {
                    @Override
                    public void run() {
                        readAndPlaceInside();
                    }
                };
                readLoopTask = readLoop.runTaskTimer(this, 0L, (long) (period * 20));

                return true;
            }
        }

        return false;
    }

    private int dzToHeight(int dz) {
        if (Math.abs(dz) > RADIUS)
            return -1;
        else
            return (int) Math.round(Math.sqrt(RADIUS * RADIUS - dz * dz));
    }

    private void placeBorder() {
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            if (dz == 0)
                continue;
            int prevHeight = dzToHeight((dz > 0)? dz - 1 : dz + 1);
//            if (dzToHeight.apply(dz) == prevHeight) {
//                Location newLoc = center.clone().add(0, dzToHeight.apply(dz) + 1, dz);
//                world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
//            } else {
            for (int dy = dzToHeight(dz) + 1; dy <= Math.max(dzToHeight(dz) + 1, prevHeight); dy++) {
                Location newLoc = center.clone().add(0, dy, dz);
                world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
            }
//            }
        }

        Location leftLoc = center.clone().add(0, 0, -RADIUS);
        Location rightLoc = center.clone().add(0, 0, RADIUS);
        Location topLoc = center.clone().add(0, dzToHeight(0) + 1, 0);
        world.getBlockAt(leftLoc).setType(Material.OBSIDIAN);
        world.getBlockAt(rightLoc).setType(Material.OBSIDIAN);
        world.getBlockAt(topLoc).setType(Material.OBSIDIAN);
    }

    @SuppressWarnings("CommentedOutCode")
    private void readAndPlaceInside() {
//        int i = 0;
//        while (true) {
//            String line;
//            try {
        Integer reading = reader.getNext();
        if (reading == null)
            return;

        getLogger().info("Received reading from RPi: " + reading);
//                Bukkit.broadcastMessage("reading from rpi: " + line);
//                getLogger().info("Attempting to set blocks...");
        double portion = reading / (double) MAX_READING;
        double angle = portion * Math.PI;
//                getLogger().info("portion = " + portion + ", and angle = " +
//                                 Math.toDegrees(angle) + " degrees");
        double tan = Math.tan(angle);
        double dx = 0;

        for (int dz = -RADIUS + 1; dz <= RADIUS - 1; dz++) {
            int height = dzToHeight(dz);
            double maxY = -tan * dz;
//                    getLogger().info("starting dz = " + dz + ", which has height = " +
//                                     "sqrt(" + RADIUS + "^2 - " + "(" + dz + ")^2) = " + height +
//                                     ", and should stop placing at " + maxY);
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


//            } catch (IOException e) {
//                getLogger().warning("Error in receiving from RPi! Stopping...");
//                e.printStackTrace();
////                break;
//            }
//            i++;
    }
//    }

    @Override
    public void onDisable() {
        try {
            System.out.println("Closing socket...");
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            getLogger().warning("Couldn't close socket gracefully!");
        }
    }
}
