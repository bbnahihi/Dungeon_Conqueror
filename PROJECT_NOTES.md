# JavaOOP Game Notes

This is a Java Swing 2D game.

## Project structure

- `src/`: Java source code
- `res/`: images, sounds, maps, and assets
- `out/`: compiled output folder

## Compile command

```bash
javac -encoding UTF-8 -d out src/*.java
```

## Run command

```bash
java -cp "out;." Main
```

## Important rules

- Do not move the `res` folder.
- Do not rewrite the whole project unless explicitly asked.
- Keep changes small and reviewable.
- After editing, make sure the game still compiles.
- Use object-oriented design where possible.
- Keep input handling, UI drawing, game logic, and entity behavior separated when possible.

## Current features

- Java Swing game loop
- Tile map
- Player
- Melee enemies
- Ranged enemies
- Elite enemies
- Boss
- Score and high score
- Item drop system
- Harder difficulty balance