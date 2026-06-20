package com.tradegui.plugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeManager {

    private final TradeGUIPlugin plugin;

    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    private final Map<UUID, TradeSession> sessionsByPlayer = new HashMap<>();

    public TradeManager(TradeGUIPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasPendingInvite(UUID target) {
        return pendingInvites.containsKey(target);
    }

    public void sendInvite(Player from, Player to) {
        pendingInvites.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage("§aЗапрос на торговлю отправлен игроку " + to.getName());
        to.sendMessage("§e" + from.getName() + " предлагает торговлю. §a/trade accept §eили §c/trade deny");
    }

    public UUID getInviter(UUID target) {
        return pendingInvites.get(target);
    }

    public void clearInvite(UUID target) {
        pendingInvites.remove(target);
    }

    public boolean isTrading(UUID uuid) {
        return sessionsByPlayer.containsKey(uuid);
    }

    public TradeSession getSession(UUID uuid) {
        return sessionsByPlayer.get(uuid);
    }

    public void startTrade(Player p1, Player p2) {
        TradeSession session = new TradeSession(plugin, p1.getUniqueId(), p2.getUniqueId());
        sessionsByPlayer.put(p1.getUniqueId(), session);
        sessionsByPlayer.put(p2.getUniqueId(), session);
        p1.openInventory(session.getInventory());
        p2.openInventory(session.getInventory());
        p1.sendMessage("§aТорговля с " + p2.getName() + " начата. Положите предметы в свою половину.");
        p2.sendMessage("§aТорговля с " + p1.getName() + " начата. Положите предметы в свою половину.");
    }

    public void cancelTrade(TradeSession session, String reason) {
        session.cancelCountdown();

        Player p1 = Bukkit.getPlayer(session.getP1());
        Player p2 = Bukkit.getPlayer(session.getP2());

        returnItems(session, TradeSession.SLOTS_P1, p1);
        returnItems(session, TradeSession.SLOTS_P2, p2);

        if (p1 != null) {
            p1.closeInventory();
            p1.sendMessage("§cТорговля отменена. " + reason);
        }
        if (p2 != null) {
            p2.closeInventory();
            p2.sendMessage("§cТорговля отменена. " + reason);
        }

        sessionsByPlayer.remove(session.getP1());
        sessionsByPlayer.remove(session.getP2());
    }

    private void returnItems(TradeSession session, int[] slots, Player owner) {
        for (int slot : slots) {
            ItemStack item = session.getInventory().getItem(slot);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                if (owner != null) {
                    var leftover = owner.getInventory().addItem(item);
                    leftover.values().forEach(left -> owner.getWorld().dropItemNaturally(owner.getLocation(), left));
                }
                session.getInventory().setItem(slot, null);
            }
        }
    }

    public void startCountdown(TradeSession session) {
        session.setLocked(true);
        final int[] seconds = {3};

        Player p1 = Bukkit.getPlayer(session.getP1());
        Player p2 = Bukkit.getPlayer(session.getP2());

        var task = new BukkitRunnable() {
            @id86240433 (@Override)
            public void run() {
                if (!session.bothConfirmed() || !session.isLocked()) {
                    this.cancel();
                    return;
                }
                if (seconds[0] <= 0) {
                    executeTrade(session);
                    this.cancel();
                    return;
                }
                if (p1 != null) p1.sendMessage("§eОбмен через " + seconds[0] + "...");
                if (p2 != null) p2.sendMessage("


Новые сообщения
アンドレイ いいえ
アンドレイ いいえ
§eОбмен через " + seconds[0] + "...");
                seconds[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        session.setCountdownTask(task);
    }

    private void executeTrade(TradeSession session) {
        Player p1 = Bukkit.getPlayer(session.getP1());
        Player p2 = Bukkit.getPlayer(session.getP2());

        if (p1 == null || p2 == null) {
            cancelTrade(session, "Один из игроков вышел.");
            return;
        }

        double moneyFromP1 = session.getMoney(session.getP1());
        double moneyFromP2 = session.getMoney(session.getP2());

        var economy = plugin.getEconomy();
        if (moneyFromP1 > 0 && !economy.has(p1, moneyFromP1)) {
            failTrade(session, p1, p2, p1.getName() + " не имеет достаточно денег.");
            return;
        }
        if (moneyFromP2 > 0 && !economy.has(p2, moneyFromP2)) {
            failTrade(session, p1, p2, p2.getName() + " не имеет достаточно денег.");
            return;
        }

        java.util.List<ItemStack> itemsForP2 = collectItems(session, TradeSession.SLOTS_P1);
        java.util.List<ItemStack> itemsForP1 = collectItems(session, TradeSession.SLOTS_P2);

        if (moneyFromP1 > 0) {
            economy.withdrawPlayer(p1, moneyFromP1);
            economy.depositPlayer(p2, moneyFromP1);
        }
        if (moneyFromP2 > 0) {
            economy.withdrawPlayer(p2, moneyFromP2);
            economy.depositPlayer(p1, moneyFromP2);
        }

        giveItems(p2, itemsForP2);
        giveItems(p1, itemsForP1);

        clearSlots(session, TradeSession.SLOTS_P1);
        clearSlots(session, TradeSession.SLOTS_P2);

        p1.closeInventory();
        p2.closeInventory();
        p1.sendMessage("§aОбмен с " + p2.getName() + " успешно завершён!");
        p2.sendMessage("§aОбмен с " + p1.getName() + " успешно завершён!");

        sessionsByPlayer.remove(session.getP1());
        sessionsByPlayer.remove(session.getP2());
    }

    private void failTrade(TradeSession session, Player p1, Player p2, String reason) {
        session.cancelCountdown();
        session.resetConfirms();
        p1.sendMessage("§cОбмен не выполнен: " + reason);
        p2.sendMessage("§cОбмен не выполнен: " + reason);
    }

    private java.util.List<ItemStack> collectItems(TradeSession session, int[] slots) {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (int slot : slots) {
            ItemStack item = session.getInventory().getItem(slot);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                list.add(item.clone());
            }
        }
        return list;
    }

    private void giveItems(Player target, java.util.List<ItemStack> items) {
        for (ItemStack item : items) {
            var leftover = target.getInventory().addItem(item);
            leftover.values().forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }
    }

    private void clearSlots(TradeSession session, int[] slots) {
        for (int slot : slots) {
            session.getInventory().setItem(slot, null);
        }
    }

    public void cancelAllOnShutdown() {
        for (TradeSession session : new java.util.HashSet<>(sessionsByPlayer.values())) {
            cancelTrade(session, "Сервер перезагружается.");
        }
    }
}
