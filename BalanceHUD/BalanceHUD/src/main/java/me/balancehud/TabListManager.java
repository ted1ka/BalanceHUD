package me.balancehud;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class TabListManager implements Listener {

    private final BalanceHudPlugin plugin;

    public TabListManager(BalanceHudPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startUpdateTask();
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    applyTab(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        applyTab(event.getPlayer());
        applyName(event.getPlayer());
    }

    private void applyTab(Player player) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();

        String line = ChatColor.DARK_GREEN + "" + ChatColor.STRIKETHROUGH + repeat(" ", 64);

        String header =
                line + "\n" +
                ChatColor.GREEN + "✦ " + ChatColor.BOLD + "" + ChatColor.WHITE + "NYVORA " +
                ChatColor.GRAY + "| " + ChatColor.BOLD + "" + ChatColor.GREEN + "SMP" + ChatColor.RESET + " " + ChatColor.GREEN + "✦\n" +
                line;

        String footer =
                line + "\n" +
                ChatColor.GRAY + "Онлайн: " + ChatColor.GREEN + online + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + max + "\n" +
                line;

        player.setPlayerListHeaderFooter(header, footer);
    }

    private void applyName(Player player) {
        player.setPlayerListName(ChatColor.GREEN + "● " + ChatColor.WHITE + player.getName());
    }

    private static String repeat(String s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(s);
        return sb.toString();
    }
}
