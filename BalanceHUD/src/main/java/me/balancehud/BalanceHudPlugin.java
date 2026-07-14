package me.balancehud;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BalanceHudPlugin extends JavaPlugin {

    private static Economy economy = null;
    private final Map<UUID, Double> lastBalances = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy не е намерена! Изключвам BalanceHUD.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("rtp").setExecutor(new RtpCommand(this, false));
        getCommand("rtpq").setExecutor(new RtpCommand(this, true));

        startHudTask();
        getLogger().info("BalanceHUD стартиран успешно.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    private void startHudTask() {
        int period = getConfig().getInt("hud.update-interval-ticks", 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    double balance = economy.getBalance(player);
                    UUID uuid = player.getUniqueId();
                    Double previous = lastBalances.get(uuid);

                    if (previous != null && balance > previous + 0.001) {
                        double gained = balance - previous;
                        notifyGain(player, gained);
                    }

                    lastBalances.put(uuid, balance);

                    if (getConfig().getBoolean("hud.enabled", true)) {
                        sendHud(player, balance);
                    }
                }
            }
        }.runTaskTimer(this, 20L, period);
    }

    private void sendHud(Player player, double balance) {
        String format = getConfig().getString("hud.format", "&aБаланс: &f${balance}");
        String message = format
                .replace("{balance}", String.format("%,.2f", balance))
                .replace('&', '\u00A7');
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(message)
        );
    }

    private void notifyGain(Player player, double amount) {
        if (!getConfig().getBoolean("notify.enabled", true)) return;
        String format = getConfig().getString("notify.format", "&a+${amount} &7получени");
        String message = format
                .replace("{amount}", String.format("%,.2f", amount))
                .replace('&', '\u00A7');
        player.sendMessage(message);

        if (getConfig().getBoolean("notify.sound", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }
}
