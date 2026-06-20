who, double amount) {
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
