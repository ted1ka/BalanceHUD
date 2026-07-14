package me.balancehud;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RtpCommand implements CommandExecutor, Listener {

    private final BalanceHudPlugin plugin;
    private final boolean quick;
    private final Random random = new Random();

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingWarmups = new HashMap<>();
    private final Map<UUID, Location> warmupStartLoc = new HashMap<>();

    public RtpCommand(BalanceHudPlugin plugin, boolean quick) {
        this.plugin = plugin;
        this.quick = quick;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Само играчи могат да ползват тази команда.");
            return true;
        }
        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        String path = quick ? "rtpq" : "rtp";
        int cooldownSeconds = plugin.getConfig().getInt(path + ".cooldown-seconds", quick ? 300 : 60);

        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null) {
            long remaining = (last + cooldownSeconds * 1000L - now) / 1000L;
            if (remaining > 0) {
                player.sendMessage(ChatColor.RED + "Изчакай още " + remaining + " сек. преди да ползваш /" + path + " отново.");
                return true;
            }
        }

        if (pendingWarmups.containsKey(uuid)) {
            player.sendMessage(ChatColor.RED + "Вече имаш чакащ телепорт.");
            return true;
        }

        String worldName = plugin.getConfig().getString(path + ".world", player.getWorld().getName());
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = player.getWorld();

        int minRadius = plugin.getConfig().getInt(path + ".min-radius", 500);
        int maxRadius = plugin.getConfig().getInt(path + ".max-radius", 5000);

        player.sendMessage(ChatColor.YELLOW + "Търсене на безопасно място...");
        findSafeLocationAsync(world, minRadius, maxRadius, 0, player,
                () -> cooldowns.put(uuid, System.currentTimeMillis()));

        return true;
    }

    private void findSafeLocationAsync(World world, int minRadius, int maxRadius, int attempt,
                                        Player player, Runnable onSuccess) {
        if (attempt >= 30) {
            player.sendMessage(ChatColor.RED + "Не успях да намеря безопасно място, опитай пак.");
            return;
        }

        double angle = random.nextDouble() * Math.PI * 2;
        int distance = minRadius + random.nextInt(Math.max(1, maxRadius - minRadius));
        int x = (int) (Math.cos(angle) * distance);
        int z = (int) (Math.sin(angle) * distance);

        World finalWorld = world;
        new BukkitRunnable() {
            @Override
            public void run() {
                int y = finalWorld.getHighestBlockYAt(x, z);
                Block block = finalWorld.getBlockAt(x, y, z);
                Material type = block.getType();

                boolean unsafe = type == Material.WATER || type == Material.LAVA
                        || type == Material.CACTUS || type == Material.FIRE
                        || type == Material.MAGMA_BLOCK;

                if (unsafe || y <= finalWorld.getMinHeight() + 1) {
                    findSafeLocationAsync(finalWorld, minRadius, maxRadius, attempt + 1, player, onSuccess);
                    return;
                }

                Location destination = new Location(finalWorld, x + 0.5, y + 1, z + 0.5);

                if (quick) {
                    teleportNow(player, destination, onSuccess);
                } else {
                    startWarmup(player, destination, onSuccess);
                }
            }
        }.runTask(plugin);
    }

    private void startWarmup(Player player, Location destination, Runnable onSuccess) {
        int warmupSeconds = plugin.getConfig().getInt("rtp.warmup-seconds", 3);
        UUID uuid = player.getUniqueId();
        warmupStartLoc.put(uuid, player.getLocation());

        player.sendMessage(ChatColor.YELLOW + "Телепортиране след " + warmupSeconds + " сек. Не мърдай!");

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingWarmups.remove(uuid);
            warmupStartLoc.remove(uuid);
            teleportNow(player, destination, onSuccess);
        }, warmupSeconds * 20L);

        pendingWarmups.put(uuid, task);
    }

    private void teleportNow(Player player, Location destination, Runnable onSuccess) {
        player.teleport(destination);
        player.sendMessage(ChatColor.GREEN + "Телепортиран си на случайно място!");
        onSuccess.run();
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingWarmups.containsKey(uuid)) return;

        Location start = warmupStartLoc.get(uuid);
        if (start == null || event.getTo() == null) return;

        if (start.getBlockX() != event.getTo().getBlockX()
                || start.getBlockY() != event.getTo().getBlockY()
                || start.getBlockZ() != event.getTo().getBlockZ()) {
            cancelWarmup(event.getPlayer(), "Движение прекъсна телепорта.");
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (pendingWarmups.containsKey(player.getUniqueId())) {
            cancelWarmup(player, "Получи щета - телепортът е прекъснат.");
        }
    }

    private void cancelWarmup(Player player, String reason) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = pendingWarmups.remove(uuid);
        warmupStartLoc.remove(uuid);
        if (task != null) {
            task.cancel();
            player.sendMessage(ChatColor.RED + reason);
        }
    }
}
