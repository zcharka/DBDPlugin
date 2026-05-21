package pl.dbd.exitgate;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.dbd.DBDPlugin;

public class ExitGateListener implements Listener {
    private final DBDPlugin plugin;

    public ExitGateListener(DBDPlugin plugin) {
        this.plugin = plugin;
    }

    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> openingTasks = new java.util.HashMap<>();

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block b = e.getClickedBlock();
            if (b != null && b.getType() == Material.IRON_DOOR) {
                // IGNORUJEMY SZAFKI
                if (plugin.getLockerManager() != null
                        && plugin.getLockerManager().getLockerAt(b.getLocation()) != null) {
                    return;
                }

                // Sprawdź dystans gracza od bloku
                if (e.getPlayer().getLocation().distance(b.getLocation()) > 3.5) {
                    return;
                }

                // SPRAWDZENIE CZY TO JEST BRAMA
                boolean isGate = false;
                for (pl.dbd.exitgate.ExitGate gateEntry : plugin.getExitGateManager().getAllExitGates()) {
                    if (gateEntry.getLocation().getWorld().equals(b.getWorld())
                            && gateEntry.getLocation().distanceSquared(b.getLocation()) < 25.0) {
                        isGate = true;
                        break;
                    }
                }

                if (!isGate) {
                    return; // Zwykłe żelazne drzwi, ignorujemy
                }

                boolean isTester = e.getPlayer().hasPermission("*") || e.getPlayer().isOp();
                org.bukkit.entity.Player p = e.getPlayer();
                pl.dbd.game.GameManager gm = plugin.getGameManager();

                // Pozwalamy otworzyć TYLKO Ocalałym lub Testerom
                if (gm.isKiller(p)) {
                    p.sendMessage("§cZabójcy nie potrafią otwierać bram wyjściowych!");
                    return;
                }

                if (gm.isSurvivor(p) || isTester) {
                    boolean isLastSurvivor = gm.getActiveSurvivorsCount() == 1;
                    boolean canOpen = plugin.getGeneratorManager().getCompletedCount() >= gm.getRequiredGenerators()
                            || isTester
                            || isLastSurvivor;
                    Openable door = (Openable) b.getBlockData();

                    if (!door.isOpen()) {
                        if (canOpen) {
                            if (openingTasks.containsKey(p.getUniqueId())) {
                                return;
                            }

                            p.sendMessage(pl.dbd.DBDPlugin.getMsg("erozpoczynaszotwiera"));

                            org.bukkit.Location startLoc = p.getLocation().clone();

                            openingTasks.put(p.getUniqueId(), new org.bukkit.scheduler.BukkitRunnable() {
                                int ticks = 0;
                                final int MAX_TICKS = 400; // 20s = 400 ticków

                                @Override
                                public void run() {
                                    if (!p.isOnline() || startLoc.getWorld() != p.getWorld()
                                            || startLoc.distanceSquared(p.getLocation()) > 1.0) {
                                        p.sendMessage(pl.dbd.DBDPlugin.getMsg("cprzerwanootwieranie"));
                                        openingTasks.remove(p.getUniqueId());
                                        cancel();
                                        return;
                                    }

                                    ticks += 5; // interwał 5 ticków

                                    if (ticks % 20 == 0) {
                                        int sek = (MAX_TICKS - ticks) / 20;
                                        // Używamy subtitle zamiast ActionBara by uniknąć nadpisywania przez GameManager
                                        p.sendTitle("", "§eOtwieranie bramy... §f" + sek + "s", 0, 25, 0);

                                        // Mruganie lampami (Redstone Lamps)
                                        int flashTime = plugin.getConfig().getInt("exit-gate.lamp-flash-time", 5);
                                        if (sek <= flashTime) {
                                            boolean turnOn = (sek % 2 != 0); // Mrugaj co sekundę
                                            org.bukkit.block.data.Lightable lightable;
                                            for (int x = -8; x <= 8; x++) {
                                                for (int y = -8; y <= 8; y++) {
                                                    for (int z = -8; z <= 8; z++) {
                                                        Block lamp = b.getRelative(x, y, z);
                                                        if (lamp.getType() == Material.REDSTONE_LAMP) {
                                                            lightable = (org.bukkit.block.data.Lightable) lamp
                                                                    .getBlockData();
                                                            lightable.setLit(turnOn);
                                                            lamp.setBlockData(lightable);
                                                        }
                                                    }
                                                }
                                            }
                                            if (turnOn) {
                                                b.getWorld().playSound(b.getLocation(),
                                                        org.bukkit.Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.5f);
                                            }
                                        }
                                    }

                                    if (ticks >= MAX_TICKS) {
                                        door.setOpen(true);
                                        b.setBlockData(door);

                                        if (door instanceof org.bukkit.block.data.type.Door) {
                                            org.bukkit.block.data.type.Door d = (org.bukkit.block.data.type.Door) door;
                                            Block otherHalf = d.getHalf() == org.bukkit.block.data.type.Door.Half.TOP
                                                    ? b.getRelative(org.bukkit.block.BlockFace.DOWN)
                                                    : b.getRelative(org.bukkit.block.BlockFace.UP);
                                            if (otherHalf.getType() == Material.IRON_DOOR) {
                                                org.bukkit.block.data.Openable otherDoor = (org.bukkit.block.data.Openable) otherHalf
                                                        .getBlockData();
                                                otherDoor.setOpen(true);
                                                otherHalf.setBlockData(otherDoor);
                                            }
                                        }

                                        p.sendTitle("§aBRAMA OTWARTA!", "§7Uciekaj!", 10, 50, 20);
                                        org.bukkit.Bukkit.broadcastMessage("§c§lBrama wyjściowa została otwarta!");

                                        // Zapal wszystkie lampy na stałe (gdy otwarte)
                                        for (int x = -8; x <= 8; x++) {
                                            for (int y = -8; y <= 8; y++) {
                                                for (int z = -8; z <= 8; z++) {
                                                    Block lamp = b.getRelative(x, y, z);
                                                    if (lamp.getType() == Material.REDSTONE_LAMP) {
                                                        org.bukkit.block.data.Lightable lightable = (org.bukkit.block.data.Lightable) lamp
                                                                .getBlockData();
                                                        lightable.setLit(true);
                                                        lamp.setBlockData(lightable);
                                                    }
                                                }
                                            }
                                        }

                                        // Zaktualizuj stan bramy w menedżerze
                                        for (pl.dbd.exitgate.ExitGate gateEntry : plugin.getExitGateManager()
                                                .getAllExitGates()) {
                                            if (gateEntry.getLocation().getWorld().equals(b.getWorld()) && gateEntry
                                                    .getLocation().distanceSquared(b.getLocation()) < 25.0) {
                                                if (gateEntry.getInsidePos() == null) {
                                                    gateEntry.setInsidePos(p.getLocation().toVector());
                                                }
                                                plugin.getExitGateManager().openExitGate(gateEntry);
                                            }
                                        }

                                        // Endgame Collapse: 2 minuty na opuszczenie mapy (boss bar pomarańczowy)
                                        plugin.getExitGateManager().startEndgameCollapse();

                                        openingTasks.remove(p.getUniqueId());
                                        cancel();
                                    }
                                }
                            }.runTaskTimer(plugin, 5L, 5L));
                        } else {
                            if (gm.getActiveSurvivorsCount() > 1) {
                                p.sendMessage(pl.dbd.DBDPlugin.getMsg("cpotrzeba5ukoczonych"));
                            }
                        }
                    } else {
                        // Jeśli brama jest już otwarta:
                        p.sendMessage(pl.dbd.DBDPlugin.getMsg("cbramajestjuotwartap"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
        // Ignoruj ruch kamerą
        if (e.getFrom().getBlockX() == e.getTo().getBlockX()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()
                && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }

        org.bukkit.entity.Player p = e.getPlayer();
        pl.dbd.game.GameManager gm = plugin.getGameManager();
        boolean isTester = p.hasPermission("*") || p.isOp();

        if (gm.isSurvivor(p) || isTester) {
            // Upewnijmy się, że gracz jeszcze nie uciekł, aby nie spamować
            if (gm.hasEscaped(p)) {
                return;
            }

            // Sprawdzenie, czy brama z tej mapy jest aktualnie otwarta w pobliżu
            boolean aroundOpenGate = false;
            for (pl.dbd.exitgate.ExitGate gate : plugin.getExitGateManager().getAllExitGates()) {
                if (gate.isOpened() && gate.getLocation().getWorld().equals(p.getWorld())) {
                    // dystans 5 bloków powinien z dużym zapasem uchwycić ułożenie
                    if (p.getLocation().distanceSquared(gate.getLocation()) < 25.0) {
                        org.bukkit.block.Block b = gate.getLocation().getBlock();

                        org.bukkit.block.Block doorBlock = null;
                        for (int x = -2; x <= 2; x++) {
                            for (int y = -2; y <= 2; y++) {
                                for (int z = -2; z <= 2; z++) {
                                    if (b.getRelative(x, y, z).getType() == Material.IRON_DOOR) {
                                        doorBlock = b.getRelative(x, y, z);
                                        break;
                                    }
                                }
                                if (doorBlock != null)
                                    break;
                            }
                            if (doorBlock != null)
                                break;
                        }

                        if (doorBlock != null && gate.getInsidePos() != null) {
                            org.bukkit.util.Vector doorCenter = doorBlock.getLocation().add(0.5, 0.5, 0.5).toVector();

                            // Zabezpieczenie na wypadek nakładających się punktów
                            if (gate.getInsidePos().distanceSquared(doorCenter) > 0.01
                                    && p.getLocation().toVector().distanceSquared(doorCenter) > 0.01) {
                                org.bukkit.util.Vector insideVec = gate.getInsidePos().clone().subtract(doorCenter)
                                        .normalize();
                                org.bukkit.util.Vector playerVec = p.getLocation().toVector().subtract(doorCenter)
                                        .normalize();

                                double dot = insideVec.dot(playerVec);
                                double distToDoor = p.getLocation().toVector().distanceSquared(doorCenter);

                                // Jeśli dot < -0.2, gracz zmierza w przeciwnym kierunku do środka, a odległość
                                // > 0.6 oznacza, że przekroczył drzwi
                                if (dot < -0.2 && distToDoor > 0.6) {
                                    aroundOpenGate = true;
                                    break;
                                }
                            }
                        } else {
                            // Fallback, jeśli nie znaleziono żelaznych drzwi lub pozycji startowej
                            if (p.getLocation().distanceSquared(gate.getLocation()) < 1.0) {
                                aroundOpenGate = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (aroundOpenGate) {
                // Gracz przeszedł przez otwarte żelazne drzwi przyległe do bramy ExitGate
                gm.markEscaped(p);
                p.sendMessage(pl.dbd.DBDPlugin.getMsg("azdoaeuciec"));

                if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                    org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(),
                            "lp user " + p.getName() + " parent add uciekli");
                }

                // Teleport na wskazane w zleceniu koordynaty (lobby uciekinierów)
                org.bukkit.World lobbyWorld = org.bukkit.Bukkit.getWorld("lobby");
                if (lobbyWorld != null) {
                    p.teleport(new org.bukkit.Location(lobbyWorld, 288.647, -1, -75.465));
                } else {
                    if (gm.getLobbySpawn() != null)
                        p.teleport(gm.getLobbySpawn());
                }

                p.setGameMode(org.bukkit.GameMode.SPECTATOR);

                if (gm.getGameState() == pl.dbd.game.GameManager.GameState.IN_GAME
                        && !gm.getSurvivorUUIDs().isEmpty()) {
                    boolean allDone = true;
                    for (java.util.UUID survId : gm.getSurvivorUUIDs()) {
                        org.bukkit.entity.Player s = org.bukkit.Bukkit.getPlayer(survId);
                        if (s != null && s.isOnline()) {
                            if (!gm.hasEscaped(s) && !gm.isDead(s)) {
                                allDone = false;
                                break;
                            }
                        } else {
                            // Jeśli gracza nie ma w grze (rozłączony), ale nie został wyrzucony z listy
                            // survivors
                            // Traktujemy go jako "nie blokującego" końca gry.
                        }
                    }

                    if (allDone) {
                        gm.endGame(pl.dbd.game.GameManager.GameEndReason.SURVIVORS_ESCAPED);
                    }
                }
            }
        }
    }
}