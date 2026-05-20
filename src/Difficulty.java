public enum Difficulty {
    EASY("EASY", 0.85, 0.85, -1, 1.20, 1.25, 0.90, -1, 1.20, 0.75),
    NORMAL("NORMAL", 1.00, 1.00, 0, 1.00, 1.00, 1.00, 0, 1.00, 1.00),
    HARD("HARD", 1.15, 1.15, 1, 0.85, 0.85, 1.15, 1, 0.85, 1.25);

    private final String displayName;
    private final double enemyCountMultiplier;
    private final double enemyHpMultiplier;
    private final int enemySpeedBonus;
    private final double rangedCooldownMultiplier;
    private final double itemDropMultiplier;
    private final double bossHpMultiplier;
    private final int bossSpeedBonus;
    private final double bossCooldownMultiplier;
    private final double bossMinionChanceMultiplier;

    Difficulty(String displayName, double enemyCountMultiplier, double enemyHpMultiplier,
               int enemySpeedBonus, double rangedCooldownMultiplier, double itemDropMultiplier,
               double bossHpMultiplier, int bossSpeedBonus, double bossCooldownMultiplier,
               double bossMinionChanceMultiplier) {
        this.displayName = displayName;
        this.enemyCountMultiplier = enemyCountMultiplier;
        this.enemyHpMultiplier = enemyHpMultiplier;
        this.enemySpeedBonus = enemySpeedBonus;
        this.rangedCooldownMultiplier = rangedCooldownMultiplier;
        this.itemDropMultiplier = itemDropMultiplier;
        this.bossHpMultiplier = bossHpMultiplier;
        this.bossSpeedBonus = bossSpeedBonus;
        this.bossCooldownMultiplier = bossCooldownMultiplier;
        this.bossMinionChanceMultiplier = bossMinionChanceMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Difficulty next() {
        Difficulty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public Difficulty previous() {
        Difficulty[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }

    public int applyEnemyCount(int baseCount) {
        return Math.max(5, (int) Math.round(baseCount * enemyCountMultiplier));
    }

    public int applyEnemyHp(int baseHp) {
        return Math.max(1, (int) Math.round(baseHp * enemyHpMultiplier));
    }

    public int applyEnemySpeed(int baseSpeed) {
        return Math.max(1, baseSpeed + enemySpeedBonus);
    }

    public void applyMonsterStats(Monster monster) {
        monster.maxHp = applyEnemyHp(monster.maxHp);
        monster.speed = applyEnemySpeed(monster.speed);
        if (this != NORMAL && monster.isElite) {
            monster.hp = Math.max(1, (int) Math.round(monster.hp * enemyHpMultiplier));
        }
    }

    public int applyRangedCooldown(int baseCooldown) {
        return Math.max(30, (int) Math.round(baseCooldown * rangedCooldownMultiplier));
    }

    public double applyItemDropChance(double baseChance) {
        return clamp(baseChance * itemDropMultiplier, 0.05, 0.80);
    }

    public int applyBossHp(int baseHp) {
        return Math.max(1, (int) Math.round(baseHp * bossHpMultiplier));
    }

    public void applyBossStats(Monster boss) {
        boss.maxHp = applyBossHp(boss.maxHp);
        boss.hp = boss.maxHp;
        boss.speed = getBossSpeed(false);
    }

    public int getBossSpeed(boolean enraged) {
        int baseSpeed = enraged ? 6 : 4;
        return Math.max(1, baseSpeed + bossSpeedBonus);
    }

    public int getBossShootCooldown(boolean enraged) {
        int baseCooldown = enraged ? 25 : 75;
        return Math.max(18, (int) Math.round(baseCooldown * bossCooldownMultiplier));
    }

    public double applyBossMinionChance(double baseChance) {
        return clamp(baseChance * bossMinionChanceMultiplier, 0.10, 0.85);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
