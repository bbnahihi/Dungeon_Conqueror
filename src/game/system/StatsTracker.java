package game.system;

public class StatsTracker {
    private int enemiesKilled;
    private int itemsCollected;
    private int damageTaken;
    private int upgradesChosen;
    private int levelReached;
    private long startTimeMillis;
    private long endTimeMillis;

    public void startRun(int startingLevel) {
        enemiesKilled = 0;
        itemsCollected = 0;
        damageTaken = 0;
        upgradesChosen = 0;
        levelReached = startingLevel;
        startTimeMillis = System.currentTimeMillis();
        endTimeMillis = 0;
    }

    public void reset() {
        enemiesKilled = 0;
        itemsCollected = 0;
        damageTaken = 0;
        upgradesChosen = 0;
        levelReached = 0;
        startTimeMillis = 0;
        endTimeMillis = 0;
    }

    public void endRun() {
        if (startTimeMillis > 0 && endTimeMillis == 0) {
            endTimeMillis = System.currentTimeMillis();
        }
    }

    public void recordEnemyKilled() {
        enemiesKilled++;
    }

    public void recordItemCollected() {
        itemsCollected++;
    }

    public void recordDamageTaken(int amount) {
        if (amount > 0) {
            damageTaken += amount;
        }
    }

    public void recordUpgradeChosen() {
        upgradesChosen++;
    }

    public void setLevelReached(int level) {
        if (level > levelReached) {
            levelReached = level;
        }
    }

    public int getEnemiesKilled() {
        return enemiesKilled;
    }

    public int getItemsCollected() {
        return itemsCollected;
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public int getUpgradesChosen() {
        return upgradesChosen;
    }

    public int getLevelReached() {
        return levelReached;
    }

    public long getSurvivalTimeMillis() {
        if (startTimeMillis == 0) return 0;

        long finalTime = (endTimeMillis > 0) ? endTimeMillis : System.currentTimeMillis();
        return finalTime - startTimeMillis;
    }

    public String getSurvivalTimeText() {
        long totalSeconds = getSurvivalTimeMillis() / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
