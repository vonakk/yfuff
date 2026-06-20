package com.tradegui.plugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeCommand implements CommandExecutor, TabCompleter {

    private final TradeGUIPlugin plugin;

    public TradeCommand(TradeGUIPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков.");
            return true;
        }

        TradeManager tm = plugin.getTradeManager();

        if (args.length == 0) {
            player.sendMessage("§e/trade <ник> §7- предложить торговлю");
            player.sendMessage("§e/trade accept §7- принять");
            player.sendMessage("§e/trade deny §7- отклонить");
            player.sendMessage("§e/trade money <сумма> §7- выставить деньги в открытой торговле");
            player.sendMessage("§e/trade cancel §7- отменить текущую торговлю");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "accept" -> {
                UUID inviterId = tm.getInviter(player.getUniqueId());
                if (inviterId == null) {
                    player.sendMessage("§cУ вас нет активных приглашений.");
                    return true;
                }
                Player inviter = Bukkit.getPlayer(inviterId);
                tm.clearInvite(player.getUniqueId());
                if (inviter == null || !inviter.isOnline()) {
                    player.sendMessage("§cИгрок вышел с сервера.");
                    return true;
                }
                if (tm.isTrading(player.getUniqueId()) || tm.isTrading(inviterId)) {
                    player.sendMessage("§cОдин из вас уже в торговле.");
                    return true;
                }
                tm.startTrade(inviter, player);
            }
            case "deny" -> {
                UUID inviterId = tm.getInviter(player.getUniqueId());
                if (inviterId == null) {
                    player.sendMessage("§cУ вас нет активных приглашений.");
                    return true;
                }
                tm.clearInvite(player.getUniqueId());
                Player inviter = Bukkit.getPlayer(inviterId);
                if (inviter != null) inviter.sendMessage("§c" + player.getName() + " отклонил торговлю.");
                player.sendMessage("§aПриглашение отклонено.");
            }
            case "cancel" -> {
                TradeSession session = tm.getSession(player.getUniqueId());
                if (session == null) {
                    player.sendMessage("§cВы не в торговле.");
                    return true;
                }
                tm.cancelTrade(session, player.getName() + " отменил торговлю.");
            }
            case "money" -> {
                if (args.length < 2) {
                    player.sendMessage("§cИспользование: /trade money <сумма>");
                    return true;
                }
                TradeSession session = tm.getSession(player.getUniqueId());
                if (session == null) {
                    player.sendMessage("§cВы не в торговле.");
                    return true;
                }
                if (session.isLocked()) {
                    player.sendMessage("§cНельзя менять сумму во время подтверждённого обмена.");
                    return true;
                }
                double amount;
                try {
                    amount = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cНеверное число.");
                    return true;
                }
                if (amount < 0) {
                    player.sendMessage("§cСумма не может быть отрицательной.");
                    return true;
                }
                if (!plugin.getEconomy().has(player, amount)) {
                    player.sendMessage("§cУ вас нет столько денег.");
                    return true;
                }
                session.setMoney(player.getUniqueId(), amount);
                player.sendMessage("§aВы выставили на обмен: " + amount + "$");
                Player other = Bukkit.getPlayer(session.getOther(player.getUniqueId()));
                if (other != null) other.sendMessage("§e" + player.getName() + " изменил сумму денег в обмене.");
            }
            default -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage("§cИгрок не найден или не в сети.");
                    return true;
                }
                if (target.equals(player)) {
                    player.sendMessage("§cНельзя торговать с самим собой.");
                    return true;
                }
                if (tm.isTrading(player.getUniqueId())) {
                    player.sendMessage("§cВы уже в торговле.");
                    return true;
                }
                if (tm.isTrading(target.getUniqueId())) {
                    player.sendMessage("§cЭтот игрок уже в торговле.");
                    return true;
                }
                if (tm.hasPendingInvite(target.getUniqueId())) {
                    player.sendMessage("§cУ этого игрока уже есть активное приглашение.");
                    return true;
                }
                tm.sendInvite(player, target);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            result.add("accept");
            result.add("deny");
            result.add("cancel");
            result.add("money");
            for (Player p : Bukkit.getOnlinePlayers()) {
                result.add(p.getName());
            }
        }
        return result;
    }
}
