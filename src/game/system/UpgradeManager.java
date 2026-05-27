package game.system;

import game.core.GamePanel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UpgradeManager {
    private GamePanel gp;
    private Map<Integer, Upgrade> upgradeRegistry = new HashMap<>();
    private Upgrade[] currentChoices = new Upgrade[3];

    public UpgradeManager(GamePanel gp) {
        this.gp = gp;
        registerUpgrades();
    }

    private void registerUpgrades() {
        register(new Upgrade(1, "Iron Heart", "+2 Max HP", Color.GREEN) {
            public void apply(GamePanel gp) {
                gp.player.maxHp += 2;
                gp.player.hp += 2;
            }
        });

        register(new Upgrade(2, "Hermes Boots", "+1 Move Speed", Color.CYAN) {
            public void apply(GamePanel gp) {
                gp.player.speed += 1;
            }
        });

        register(new Upgrade(3, "Meditation", "-2s Ultimate Cooldown", Color.MAGENTA) {
            public void apply(GamePanel gp) {
                gp.player.skillMaxCooldown -= 120;
                if (gp.player.skillMaxCooldown < 120) gp.player.skillMaxCooldown = 120;
            }
        });

        register(new Upgrade(4, "Armor-Piercing Shot", "+1 Gun Damage", Color.ORANGE) {
            public void apply(GamePanel gp) {
                gp.player.gunDamage += 1;
            }
        });

        register(new Upgrade(5, "Quick Trigger", "Faster Shooting", Color.YELLOW) {
            public void apply(GamePanel gp) {
                gp.player.attackCooldown -= 3;
            }
        });

        register(new Upgrade(6, "Blood Blade", "+2 Melee Damage", Color.RED) {
            public void apply(GamePanel gp) {
                gp.player.meleeDamage += 2;
            }
        });

        register(new Upgrade(7, "Swift Wrist", "Faster Slashes", Color.ORANGE) {
            public void apply(GamePanel gp) {
                gp.player.attackCooldown -= 5;
            }
        });

        register(new Upgrade(8, "Healing Potion", "Restore 50% HP", Color.PINK) {
            public void apply(GamePanel gp) {
                gp.player.hp += gp.player.maxHp / 2;
                if (gp.player.hp > gp.player.maxHp) gp.player.hp = gp.player.maxHp;
            }
        });

        register(new Upgrade(9, "Wind Step", "+2 Move Speed", Color.CYAN) {
            public void apply(GamePanel gp) {
                gp.player.speed += 2;
            }
        });

        register(new Upgrade(10, "Double Barrel", "Fire 2 Spread Shots", Color.WHITE) {
            public void apply(GamePanel gp) {
                gp.player.doubleShot = true;
            }
        });

        register(new Upgrade(11, "Bullet Storm", "Ultimate 36 Shots", Color.YELLOW) {
            public void apply(GamePanel gp) {
                gp.player.ultiBulletCount = 36;
            }
        });

        register(new Upgrade(12, "Giant Sword", "Longer Slash Range", Color.LIGHT_GRAY) {
            public void apply(GamePanel gp) {
                gp.player.meleeRangeBonus += gp.tileSize;
            }
        });

        register(new Upgrade(13, "Whirlwind Slash", "180 Degree Arc", Color.ORANGE) {
            public void apply(GamePanel gp) {
                gp.player.meleeAngleBonus = Math.PI / 2;
            }
        });
    }

    private void register(Upgrade upgrade) {
        upgradeRegistry.put(upgrade.getId(), upgrade);
    }

    public void rollUpgrades() {
        ArrayList<Integer> pool = new ArrayList<>();

        pool.add(1);
        pool.add(2);
        pool.add(3);
        pool.add(8);
        pool.add(9);

        if (gp.player.classType == 0) {
            pool.add(4);
            pool.add(5);

            if (gp.player.doubleShot == false) {
                pool.add(10);
            }
            if (gp.player.ultiBulletCount == 24) {
                pool.add(11);
            }
        } else if (gp.player.classType == 1) {
            pool.add(6);
            pool.add(7);

            if (gp.player.meleeRangeBonus == 0) {
                pool.add(12);
            }
            if (gp.player.meleeAngleBonus == 0) {
                pool.add(13);
            }
        }

        Collections.shuffle(pool);

        for (int i = 0; i < currentChoices.length; i++) {
            currentChoices[i] = upgradeRegistry.get(pool.get(i));
        }
    }

    public Upgrade getChoice(int index) {
        if (index < 0 || index >= currentChoices.length) return null;
        return currentChoices[index];
    }

    public Upgrade[] getCurrentChoices() {
        return currentChoices;
    }

    public boolean applySelectedUpgrade(int index) {
        Upgrade selectedUpgrade = getChoice(index);
        if (selectedUpgrade != null) {
            selectedUpgrade.apply(gp);
            return true;
        }
        return false;
    }
}
