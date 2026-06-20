package com.tradegui.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Одна активная торговля между двумя игроками.
 * Используется ОДИН общий Inventory объект — его видят оба игрока.
 * Это обычный ванильный инвентарь, поэтому он одинаково работает
 * и на Java-клиенте, и на Bedrock (через Geyser/Floodgate).
 */
public class TradeSession {

    // Слоты-зоны
    public static final int[] SLOTS_P1 = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17};
    public static final int[] SLOTS_P2 = {18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35};
    public static final int SLOT_MONEY_P1 = 45;
    public static final int SLOT_CONFIRM_P1 = 46;
    public static final int SLOT_CONFIRM_P2 = 52;
    public static final int SLOT_MONEY_P2 = 53;

    private final TradeGUIPlugin plugin;
    private final UUID p1;
    private final UUID p2;
    private final Inventory inventory;

    private double moneyP1 = 0;
    private double moneyP2 = 0;

    private boolean confirmP1 = false;
    private boolean confirmP2 = false;

    private boolean locked = false; // когда оба подтвердили - блокируем редактирование
    private BukkitTask countdownTask;

    public TradeSession(TradeGUIPlugin plugin, UUID p1, UUID p2) {
        this.plugin = plugin;
        this.p1 = p1;
        this.p2 = p2;
        this.inventory = Bukkit.createInventory(null, 54,
                net.kyori.adventure.text.Component.text("Торговля"));
        decorate();
    }

    private void decorate() {
        ItemStack glass = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 36; i <= 44; i++) inventory.setItem(i, glass);
        for (int i = 47; i <= 51; i++) inventory.setItem(i, glass);
        refreshMoneyItems();
        refreshConfirmButtons();
    }

    private ItemStack named(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void refreshMoneyItems() {
        inventory.setItem(SLOT_MONEY_P1, named(Material.GOLD_NUGGET,
                "§6Деньги " + nameOf(p1) + ": §a" + moneyP1 + "$ §7(/trade money <сумма>)"));
        inventory.setItem(SLOT_MONEY_P2, named(Material.GOLD_NUGGET,
                "§6Деньги " + nameOf(p2) + ": §a" + moneyP2 + "$ §7(/trade money <сумма>)"));
    }

    public void refreshConfirmButtons() {
        inventory.setItem(SLOT_CONFIRM_P1, named(
                confirmP1 ? Material.LIME_DYE : Material.RED_DYE,
                confirmP1 ? "§a" + nameOf(p1) + " готов ✔" : "§c" + nameOf(p1) + " не готов (нажми ПКМ)"));
        inventory.setItem(SLOT_CONFIRM_P2, named(
                confirmP2 ? Material.LIME_DYE : Material.RED_DYE,
                confirmP2 ? "§a" + nameOf(p2) + " готов ✔" : "§c" + nameOf(p2) + " не готов (нажми ПКМ)"));
    }

    private String nameOf(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : "???";
    }

    public boolean isSlotInOwnZone(UUID who, int slot) {
        if (who.equals(p1)) {
            for (int s : SLOTS_P1) if (s == slot) return true;
        } else if (who.equals(p2)) {
            for (int s : SLOTS_P2) if (s == slot) return true;
        }
        return false;
    }

    public boolean isItemSlot(int slot) {
        for (int s : SLOTS_P1) if (s == slot) return true;
        for (int s : SLOTS_P2) if (s == slot) return true;
        return false;
    }

    /** Сброс подтверждений при изменении предложения (если кто-то поменял предметы/деньги после готовности) */
    public void resetConfirms() {
        confirmP1 = false;
        confirmP2 = false;
        cancelCountdown();
        refreshConfirmButtons();
    }

    public void setConfirm(UUID who, boolean value) {
        if (who.equals(p1)) confirmP1 = value;
        else if (who.equals(p2)) confirmP2 = value;
        refreshConfirmButtons();
    }

    public boolean bothConfirmed() {
        return confirmP1 && confirmP2;
    }

    public boolean bothConfirmedP1() {
        return confirmP1;
    }

    public boolean bothConfirmedP2() {
        return confirmP2;
    }

    public void setMoney(UUID who, double amount) {
        if (who.equals(p1)) moneyP1 = amount;
        else if (who.equals(p2)) moneyP2 = amount;
        resetConfirms();
        refreshMoneyItems();
    }

    public double getMoney(UUID who) {
        return who.equals(p1) ? moneyP1 : moneyP2;
    }

    public UUID getOther(UUID who) {
        return who.equals(p1) ? p2 : p1;
    }

    public UUID getP1() { return p1; }
    public UUID getP2() { return p2; }
    public Inventory getInventory() { return inventory; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setCountdownTask(BukkitTask task) { this.countdownTask = task; }
    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        locked = false;
    }
    public boolean involves(UUID uuid) {
        return uuid.equals(p1) || uuid.equals(p2);
    }
}
