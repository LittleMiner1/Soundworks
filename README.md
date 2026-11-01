# Soundworks (Fabric, Minecraft 26.3)

Press your own MP3 files onto records and play them in the world. Everyone nearby hears the music in stereo, even without owning the files.

## How to use
1. Put MP3 files into the `music` folder in your game directory. In-game you can see them (and open the folder) under **Options > Music & Sounds > Soundworks**.
2. Place a **Record Press**, insert a **Blank Record** (and optionally a dye for the inner ring), pick an MP3 and click *Press*.
3. Put the record into a normal jukebox, the **Turntable** or the **Jukebox**.
   Turntable and Jukebox have sliders for **speed (75 % - 125 %, default 100 %)** and **volume (0 % - 200 %, default 100 %)**.
4. To copy a record: put the pressed record into *Source (copy)*, a blank record into *Blank record*, then click *Copy*.

All blocks are in the creative inventory (Building Blocks tab, the blank record is in Tools & Utilities).

## Recipes
- Blank Record (x2): 4 coal/charcoal in a plus shape, iron nugget in the center
- Record Press: piston on top; iron ingot, black dye, iron ingot in the middle; 3 stone at the bottom
- Turntable: diamond, wool below it, 3 iron ingots below that
- Jukebox: note block, turntable, note block in a row; a chest below

## Multiplayer
When pressing, your client uploads the MP3 to the server (`<world>/soundworks/<sha256>.mp3`). A record only stores the hash,
so anyone who plays it downloads the file automatically into `soundworks_cache/`. Everyone needs the mod, but not the MP3s.
Players who join later hear the song at the same position as everyone else. Works with Essential worlds.

## Building
Java 25 is required: `./gradlew build` produces `build/libs/soundworks-1.0.0.jar`.
The included GitHub workflow (`.github/workflows/build.yml`) builds the jar on every push; push a tag like `v1.0.0` to get a release.
