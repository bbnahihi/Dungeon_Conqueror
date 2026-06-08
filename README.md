# Dungeon Conqueror

**Dungeon Conqueror** is a 2D top-down action game built with Java Swing.  
The game focuses on fast combat, character-based playstyles, stage clearing, upgrades, story progression, and a final boss fight.

---

## Overview

The player enters a corrupted Rift to fight through multiple areas and confront the final boss, **Seraphine, the Abyssal Rose**.

The main gameplay loop is:

1. Choose a character.
2. Clear a stage by defeating all enemies.
3. Enter the portal.
4. Choose one upgrade.
5. Continue to the next stage.
6. Fight the final boss.
7. View the ending and run statistics.

---

## Gameplay Flow

Current game flow:

- **Title / Lobby**
- **Character Selection**
- **Opening Story**
- **Level 1: Forest**
- **Upgrade Selection**
- **Level 2: Ice**
- **Upgrade Selection**
- **Level 3: Desert**
- **Final Upgrade**
- **Pre-Boss Story**
- **Final Boss: Seraphine, the Abyssal Rose**
- **Ending Story**
- **Statistics Screen**

---

## Playable Characters

### Ranger

A ranged character focused on attacking from a safe distance.

Main traits:

- Uses ranged attacks.
- Safer positioning.
- Good for kiting enemies.
- Benefits strongly from attack speed and damage upgrades.

### Swordsman

A melee character focused on close-range combat.

Main traits:

- Uses sword attacks.
- Higher close-range burst damage.
- Stronger direct combat style.
- Benefits from melee damage, range, and attack cooldown upgrades.

---

## Main Features

- Top-down player movement.
- Keyboard movement and mouse-based interaction.
- Ranger and Swordsman character classes.
- Melee and ranged combat.
- Normal enemies, ranged enemies, elite enemies, and a final boss.
- Item drops during combat.
- Upgrade selection after clearing stages.
- Multiple themed stages: Forest, Ice, Desert, and Boss Arena.
- Image-background maps with props, walkable areas, and collision.
- Story screens before and after key moments.
- Final boss with phase 2 visual effects.
- Screen shake and visual feedback when taking damage.
- Boss death effects.
- Music and sound effects.
- Score, best score, and run statistics.
- Mouse-supported menu UI.
- Resizable game window.

---

## Controls

| Action | Control |
|---|---|
| Move | `WASD` or Arrow Keys |
| Aim / Menu interaction | Mouse |
| Attack / Select | Left Mouse Button |
| Use skill | `Space` |
| Confirm / Advance story | `Enter` or Left Mouse Button |
| Pause / Back | `Esc` |

Some menus can be controlled with both keyboard and mouse.

---

## Running the Game

### Requirements

- Java Development Kit (JDK)
- Java compiler available from terminal or command prompt

---

### Windows

Run:

```bat
run_game.bat
```

Or compile and run manually:

```bat
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp "out;." game.main.Main
```

---

### Linux / macOS

Run:

```bash
./run_game.sh
```

Or compile and run manually:

```bash
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp "out:." game.main.Main
```

---

### PowerShell Compile Command

If PowerShell has trouble reading `@sources.txt`, use:

```powershell
javac --% -encoding UTF-8 -d out @sources.txt
```

Then run:

```powershell
java -cp "out;." game.main.Main
```

---

## Project Structure

```text
src/
└── game/
    ├── main/      # Program entry point
    ├── core/      # Game loop, collision, pathfinding, main game panel
    ├── entity/    # Player, monsters, bullets, items, particles
    ├── input/     # Keyboard and mouse input
    ├── system/    # Sound, upgrades, story, difficulty, statistics
    ├── tile/      # Tile map support and fallback map loading
    └── ui/        # Menus, HUD, story screens, result screens

res/
├── maps/          # Map data, objects, walkable areas, and legacy maps
├── music/         # Background music
├── portal/        # Portal animation assets
├── boss/          # Boss sprites
└── ...            # Other images and sound assets
```

---

## Technical Notes

The game uses a fixed internal game resolution for logic and UI layout, then scales the rendered scene when the window is resized.  
This keeps collision, camera movement, and mouse interaction stable while still allowing a larger window.

Maps use a combination of:

- background images,
- object definitions,
- walkable area files,
- collision rectangles,
- fallback tile maps.

This allows the stages to look more natural while still supporting reliable gameplay collision.

---

## Final Boss

The final boss is **Seraphine, the Abyssal Rose**.

During the fight, Seraphine enters a second phase when her HP drops below half.  
Phase 2 adds stronger visual feedback such as flash effects, particles, aura, and screen shake.

After being defeated, Seraphine dissolves into the Rift, leading into the ending story and the statistics screen.

---

## Notes

This project is designed as a playable Java desktop game.  
For the best demo experience, run the game from the provided batch or shell script and play through the full flow from title screen to final statistics.
