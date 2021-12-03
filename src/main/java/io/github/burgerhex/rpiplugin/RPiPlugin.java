package io.github.burgerhex.rpiplugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

public final class RPiPlugin extends JavaPlugin implements Listener {
//    public static final String HOST = "68.181.16.101";
    public static final int PORT = 57944;

    public static final int MAX_ROTARY_READING = 1023;
//    public static final int RADIUS = 15;

//    private Socket socket;
    private ServerSocket serverSocket;
    private StatsWebSocketServer wss;
    private RPiReader reader;
//    private BukkitTask readLoopTask;

    private World world;

    private boolean isParkour1 = true;
    private boolean isButtonExecuting = false;

    private final Map<UUID, Integer> initialJumps = new HashMap<>();
    private final Map<UUID, Integer> initialRuns = new HashMap<>();


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        try {
            getLogger().info("Opening WebSocket server...");
            wss = new StatsWebSocketServer("0.0.0.0", PORT + 1, getLogger());
//            getLogger().info("Connecting to Raspberry Pi (" + HOST + ":" + PORT + ")...");
//            socket = new Socket(HOST, PORT);
//            getLogger().info("Connected to Raspberry Pi!");
            getLogger().info("Opening other TCP server...");
            serverSocket = new ServerSocket(PORT);

            reader = new RPiReader(serverSocket, getLogger());

            Optional<World> worldOpt = Bukkit.getServer().getWorlds().stream().filter(
                    (w) -> w.getEnvironment().equals(World.Environment.NORMAL)).findFirst();
            worldOpt.ifPresentOrElse(w -> {
//                center = new Location(w, 218, 73, 78);
                world = w;
            }, () -> getLogger().info("Couldn't get world!"));

//            readLoop = new BukkitRunnable() {
//                @Override
//                public void run() {
//                    readAndPlaceInside();
//                }
//            };
//            readLoopTask = readLoop.runTaskTimer(this, 0, 20);

            //    private BukkitRunnable readLoop;
            BukkitRunnable readRunsLoop = new BukkitRunnable() {
                @Override
                public void run() {
                    checkRunDistances();
                }
            };
            readRunsLoop.runTaskTimer(this, 0, 20);

            BukkitRunnable setFireLoop = new BukkitRunnable() {
                @Override
                public void run() {
                    setFire();
                }
            };
            setFireLoop.runTaskTimer(this, 0, 20);

            BukkitRunnable setParkourLoop = new BukkitRunnable() {
                @Override
                public void run() {
                    setParkourJumps();
                }
            };
            setParkourLoop.runTaskTimer(this, 0, 10);

            BukkitRunnable setWallsLoop = new BukkitRunnable() {
                @Override
                public void run() {
                    setWalls();
                }
            };
            setWallsLoop.runTaskTimer(this, 0, 5);
        } catch (IOException e) {
            getLogger().warning("Couldn't start server; aborting!");
        }

        getLogger().info("Done with enable!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("stopread")) {
                player.sendMessage("Stopping RPi reading and closing socket");
                onDisable();
                return true;
//            } else if (command.getName().equalsIgnoreCase("read")) {
//                player.sendMessage("Setting border and inside");
//                placeBorder();
//                readAndPlaceInside();
//                return true;
//            } else if (command.getName().equalsIgnoreCase("border")) {
//                player.sendMessage("Setting border");
//                placeBorder();
//                return true;
//            } else if (command.getName().equalsIgnoreCase("inside")) {
//                player.sendMessage("Setting inside");
//                readAndPlaceInside();
//                return true;
//            } else if (command.getName().equalsIgnoreCase("period")) {
//                player.sendMessage("Setting period");
//                double period;
//                try {
//                    period = Double.parseDouble(args[0]);
//                } catch (NumberFormatException e) {
//                    player.sendMessage("Invalid period: " + args[0]);
//                    return true;
//                }
////                readLoopTask.cancel();
////                readLoop.cancel();
//
//                // TODO: check this
////                readLoop = new BukkitRunnable() {
////                    @Override
////                    public void run() {
////                        readAndPlaceInside();
////                    }
////                };
////                readLoopTask = readLoop.runTaskTimer(this, 0L, (long) (period * 20));
//
//                return true;
            }
        }

        return false;
    }

    private void setFire() {
        // hard coded for convenience but could use in-game commands to set these
        int fireY = 73;
        int fireEndX = 268;
        int fireStartX = 258;
        int fireEndZ = 150;
        int fireStartZ = 118;

        if (reader.isLatestTempSeen())
            return;

        int temp = reader.getLatestTemp();

        if (temp == -1)
            return;

        int minTemp = 24;
        int maxTemp = 32;

        if (temp < minTemp)
            temp = minTemp;
        if (temp > maxTemp)
            temp = maxTemp;

        double chance = (double) (temp - minTemp) / (maxTemp - minTemp);

        Random r = new Random(PORT); // seeded
        for (int x = fireStartX; x <= fireEndX; x++) {
            for (int z = fireStartZ; z <= fireEndZ; z++) {
                Material mat = (r.nextDouble() < chance)? Material.FIRE : Material.AIR;
                if (world.getBlockAt(new Location(world, x, fireY - 1, z)).getType() == Material.NETHER_BRICKS)
                    mat = Material.AIR;
                world.getBlockAt(new Location(world, x, fireY, z)).setType(mat);
            }
        }
    }

    private void setParkourJumps() {
        if (!reader.getButtonPressed() || isButtonExecuting)
            return;

        isButtonExecuting = true;

        setParkourTo(Material.CRACKED_STONE_BRICKS, isParkour1);

        new BukkitRunnable() {
            @Override
            public void run() {
                setParkourTo(Material.STONE_BRICKS, !isParkour1);
            }
        }.runTaskLater(this, 15); // 1s delay

        new BukkitRunnable() {
            @Override
            public void run() {
                setParkourTo(Material.AIR, isParkour1);
                isParkour1 = !isParkour1;
                isButtonExecuting = false;
            }
        }.runTaskLater(this, 15); // 2s delay
    }


    private void setParkourTo(Material material, boolean isSetting1) {
        int parkourStartX2 = 265;
        int parkourStartX1 = 259;
        int parkourStartZ = 182;
        int parkourY = 78;

        int start = isSetting1? parkourStartX1 : parkourStartX2;

        // hard coded
        int[] lengths = {3, 3, 3, 4, 1, 1, 1, 1, 1, 1, 1, 3, 3};

        int z = parkourStartZ;
        for (int i = 0; i < lengths.length; i++) {
            Material toSet = (i % 2 == 0)? material : Material.AIR;
            for (int length = 0; length < lengths[i]; length++) {
                for (int x = start; x < start + 3; x++) {
                    world.getBlockAt(new Location(world, x, parkourY, z)).setType(toSet);
                }
                z++;
            }
        }
    }

    private void setWalls() {
        int wallsMinX = 258;
        int wallsMaxX = 268;
        int numSpaces = wallsMaxX - wallsMinX + 1;
        int wallsStartZ = 88;
        int wallsStartY = 73;
        int wallsHeight = 4;
        int wallsLength = 7;
        int wallDistance = 5;
        int numWalls = 5;
//        int[] offsets = new int[] {1, 4, 4, 1, 5};
        int[] offsets = new int[] {0, 0, 0, 0, 0};
        boolean[] directions = new boolean[] {true, false, true, false, true};
        // directions[n] is true if wall n starts on the left side (i.e. at a higher x)

        if (reader.isLatestRotarySeen())
            return;

        int reading = reader.getLatestRotary();

        if (reading == -1)
            return;

        int distanceMoved = (int) (numSpaces * 2.0 * reading / MAX_ROTARY_READING);

        for (int wall = 0; wall < numWalls; wall++) {
            int z = wallsStartZ + wall * wallDistance;
            int offset = offsets[wall];
            int startX = directions[wall]? wallsMinX + distanceMoved + offset :
                    wallsMaxX - distanceMoved - offset;

            while (startX > wallsMaxX)
                startX -= numSpaces;
            while (startX < wallsMinX)
                startX += numSpaces;

            int x = startX;

            for (int l = 0; l < numSpaces; l++) {
                Material mat = (l < wallsLength)? Material.STONE_BRICKS : Material.AIR;

                for (int y = wallsStartY; y < wallsStartY + wallsHeight; y++) {
                    world.getBlockAt(new Location(world, x, y, z)).setType(mat);
                }

                x += directions[wall]? 1 : -1;
                if (x > wallsMaxX)
                    x -= numSpaces;
                if (x < wallsMinX)
                    x += numSpaces;
            }
        }
    }

//    private int dzToHeight(int dz) {
//        if (Math.abs(dz) > RADIUS)
//            return -1;
//        else
//            return (int) Math.round(Math.sqrt(RADIUS * RADIUS - dz * dz));
//    }
//
//    private void placeBorder() {
//        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
//            if (dz == 0)
//                continue;
//            int prevHeight = dzToHeight((dz > 0)? dz - 1 : dz + 1);
//            for (int dy = dzToHeight(dz) + 1; dy <= Math.max(dzToHeight(dz) + 1, prevHeight); dy++) {
//                Location newLoc = center.clone().add(0, dy, dz);
//                world.getBlockAt(newLoc).setType(Material.OBSIDIAN);
//            }
//        }
//
//        Location leftLoc = center.clone().add(0, 0, -RADIUS);
//        Location rightLoc = center.clone().add(0, 0, RADIUS);
//        Location topLoc = center.clone().add(0, dzToHeight(0) + 1, 0);
//        world.getBlockAt(leftLoc).setType(Material.OBSIDIAN);
//        world.getBlockAt(rightLoc).setType(Material.OBSIDIAN);
//        world.getBlockAt(topLoc).setType(Material.OBSIDIAN);
//    }
//
//    private void readAndPlaceInside() {
//        if (reader.isLatestSeen())
//            return;
//
//        Integer reading = reader.getLatest();
//
//        if (reading == null)
//            return;
//
//        getLogger().info("Received reading from RPi: " + reading);
//        double portion = reading / (double) MAX_READING;
//        double angle = portion * Math.PI;
//        double tan = Math.tan(angle);
//        double dx = 0;
//
//        for (int dz = -RADIUS + 1; dz <= RADIUS - 1; dz++) {
//            int height = dzToHeight(dz);
//            double maxY = -tan * dz;
//
//            for (int dy = 0; dy <= height; dy++) {
//                Location newLoc = center.clone().add(dx, dy, dz);
//                Material mat = ((dy <= maxY && portion < 0.5) || (dy > maxY && portion >= 0.5))?
//                        Material.STONE : Material.AIR;
//                world.getBlockAt(newLoc).setType(mat);
//            }
//        }
//
//        world.getBlockAt(center).setType(Material.STONE);
//
//        if (0 < portion && portion < 1)
//            return;
//
//        Material bottomMat = (portion == 0)? Material.AIR : Material.STONE;
//        for (int dz = 1; dz <= RADIUS - 1; dz++) {
//            Location newLoc = center.clone().add(0, 0, dz);
//            world.getBlockAt(newLoc).setType(bottomMat);
//        }
//    }

    @Override
    public void onDisable() {
        try {
            System.out.println("Closing socket...");
            if (serverSocket != null)
                serverSocket.close();
        } catch (IOException e) {
            getLogger().warning("Couldn't close socket gracefully!");
        }

        try {
            System.out.println("Closing reader...");
            if (reader != null)
                reader.stop();
        } catch (Exception e) {
            getLogger().warning("Couldn't close reader gracefully!");
        }

        try {
            System.out.println("Closing WebSocket server...");
            if (wss != null)
                wss.stop();
        } catch (InterruptedException e) {
            getLogger().warning("Couldn't close WebSocket server gracefully!");
        }
    }

    @EventHandler
    public void onJump(PlayerStatisticIncrementEvent e) {
        if (e.getStatistic() == Statistic.JUMP) {
            Player p = e.getPlayer();
            UUID id = p.getUniqueId();
            getLogger().info("jump from " + p.getDisplayName() + "(" + id + ")");

            if (!initialJumps.containsKey(id))
                initialJumps.put(id, e.getPlayer().getStatistic(Statistic.JUMP) - 1);

            int newJumps = e.getNewValue() - initialJumps.get(id);
            wss.broadcast("{\"jump\": {\"" + p.getDisplayName() + "\": " + newJumps + "}}");
        }
    }

    private void checkRunDistances() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        if (players.isEmpty())
            return;

        StringBuilder json = new StringBuilder("{\"run\": {");

        for (Player p : players) {
            UUID id = p.getUniqueId();
            if (!initialRuns.containsKey(id))
                initialRuns.put(id, p.getStatistic(Statistic.SPRINT_ONE_CM));

            int cmDistance = p.getStatistic(Statistic.SPRINT_ONE_CM) - initialRuns.get(id);
            double newDistance = cmDistance / 100.0;

            json.append("\"").append(p.getDisplayName()).append("\":")
                    .append(newDistance).append(",");
        }

        json.deleteCharAt(json.length() - 1);
        json.append("}}");
        wss.broadcast(json.toString());
    }
}
