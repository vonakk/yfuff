package com.tradegui.plugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TradeListener implements Listener {

    private final TradeGUIPlugin plugin;

    public TradeListener(TradeGUIPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TradeSession session = plugin.getTradeManager().getSession(player.getUniqueId());
        if (session == null) return;
        if (!event.getInventory().equals(session.getInventory())) return;

        int rawSlot = event.getRawSlot();

        // Клик по своему инвентарю (внизу) при шифт-клике может закинуть предмет напрямую в верхний -
        // запрещаем такое поведение, разрешаем работу только в пределах GUI.
        if (rawSlot >= 54) {
            // клики в собственном инвентаре игрока разрешены свободно (не трогаем)
            return;
        }

        if (session.isLocked()) {
            event.setCancelled(true);
            player.sendMessage("§cОбмен уже подтверждён, подождите...");
            return;
        }

        // Кнопки подтверждения
        if (rawSlot == TradeSession.SLOT_CONFIRM_P1 || rawSlot == TradeSession.SLOT_CONFIRM_P2) {
            event.setCancelled(true);
            boolean isP1Button = rawSlot == TradeSession.SLOT_CONFIRM_P1;
            boolean clickerIsP1 = player.getUniqueId().equals(session.getP1());
            if (isP1Button != clickerIsP1) {
                player.sendMessage("§cЭто не ваша кнопка подтверждения.");
                return;
            }
            boolean current = clickerIsP1 ? wasConfirmed(session, true) : wasConfirmed(session, false);
            session.setConfirm(player.getUniqueId(), !current);
            if (session.bothConfirmed()) {
                plugin.getTradeManager().startCountdown(session);
            } else {
                session.cancelCountdown();
            }
            return;
        }

        // Слоты с информацией о деньгах - не редактируемые
        if (rawSlot == TradeSession.SLOT_MONEY_P1 || rawSlot == TradeSession.SLOT_MONEY_P2) {
            event.setCancelled(true);
            return;
        }

        // Декоративные слоты (заполнители)
        if (!session.isItemSlot(rawSlot)) {
            event.setCancelled(true);
            return;
        }

        // Разрешаем класть/брать предметы только в своей зоне
        if (!session.isSlotInOwnZone(player.getUniqueId(), rawSlot)) {
            event.setCancelled(true);
            player.sendMessage("§cВы можете изменять только свою половину окна.");
            return;
        }

        // Любое изменение предметов сбрасывает подтверждения
        plugin.getServer().getScheduler().runTask(plugin, session::resetConfirms);
    }

    private boolean wasConfirmed(TradeSession session, boolean p1) {
        // Просто читаем текущее состояние через публичные методы (костыль избегаем — используем геттеры ниже)
        return p1 ? session.bothConfirmedP1() : session.bothConfirmedP2();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        TradeSession session = plugin.getTradeManager().getSession(player.getUniqueId());
        if (session == null) return;
        if (!event.getInventory().equals(session.getInventory())) return;

        if (session.isLocked()) {
            event.setCancelled(true);
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot >= 54) continue;
            if (!session.isItemSlot(slot) || !session.isSlotInOwnZone(player.getUniqueId(), slot)) {
                event.setCancelled(true);
                return;
            }
        }
        plugin.getServer().getScheduler().runTask(plugin, session::resetConfirms);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        TradeSession session = plugin.getTradeManager().getSession(player.getUniqueId());
        if (session == null) return;
        if (!event.getInventory().equals(session.getInventory())) return;
        if (session.isLocked()) return; // идёт финальный обмен, закрытие инициировано плагином

        plugin.getTradeManager().cancelTrade(session, player.getName() + " закрыл окно торговли.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getTradeManager().clearInvite(player.getUniqueId());
        TradeSession session = plugin.getTradeManager().getSession(player.getUniqueId());
        if (session != null) {
            plugin.getTradeManager().cancelTrade(session, player.getName() + " вышел с сервера.");
        }
    }
}
