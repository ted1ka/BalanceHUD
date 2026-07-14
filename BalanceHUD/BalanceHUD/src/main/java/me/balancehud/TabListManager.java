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

        String header =
                ChatColor.GREEN + "▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄\n" +
                ChatColor.WHITE + "      " + ChatColor.DARK_GREEN + "N" + ChatColor.GREEN + "yvora " + ChatColor.WHITE + "SMP\n" +
                ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

        String footer =
                ChatColor.GREEN + "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\n" +
                ChatColor.WHITE + "Онлайн: " + ChatColor.GREEN + online + ChatColor.GRAY + "/" + ChatColor.GREEN + max + "\n" +
                ChatColor.GREEN + "▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄";

        player.setPlayerListHeaderFooter(header, footer);
    }

    private void applyName(Player player) {
        player.setPlayerListName(ChatColor.GREEN + "● " + ChatColor.WHITE + player.getName());
    }
}
