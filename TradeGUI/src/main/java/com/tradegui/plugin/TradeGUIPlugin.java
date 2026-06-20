package com.tradegui.plugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class TradeGUIPlugin extends JavaPlugin {

    private static TradeGUIPlugin instance;
    private TradeManager tradeManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupEconomy()) {
            getLogger().severe("Vault/Экономика не найдена! Плагин выключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.tradeManager = new TradeManager(this);

        TradeCommand tradeCommand = new TradeCommand(this);
        getCommand("trade").setExecutor(tradeCommand);
        getCommand("trade").setTabCompleter(tradeCommand);

        getServer().getPluginManager().registerEvents(new TradeListener(this), this);

        getLogger().info("TradeGUI включен. Экономика: " + economy.getName());
    }

    @Override
    public void onDisable() {
        if (tradeManager != null) {
            tradeManager.cancelAllOnShutdown();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static TradeGUIPlugin getInstance() {
        return instance;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public Economy getEconomy() {
        return economy;
    }
}
