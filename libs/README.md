# Pixelmon JAR Setup

## Steps to Add Pixelmon

### 1. Download Pixelmon JAR
Download Pixelmon 9.1.13 for Minecraft 1.21.1 from:
- **Official Site**: https://reforged.gg/

Make sure you download the **NeoForge** version (not Forge).

### 2. Place JAR in this Folder
Place the downloaded Pixelmon JAR file in this `libs` folder.

The file name should be something like:
- `Pixelmon-1.21.1-9.1.13-universal.jar`
- `pixelmon-neoforge-9.1.13.jar`
- Or similar variant

### 3. Update build.gradle (if needed)
If the JAR filename is different from what's in `build.gradle`, update this line:

```groovy
implementation files("libs/YOUR-ACTUAL-FILENAME.jar")
```

Current configuration expects:
```
libs/Pixelmon-1.21.1-9.1.13-universal.jar
```

### 4. Refresh Gradle
After placing the JAR file, refresh your Gradle project:

**Command Line:**
```bash
./gradlew --refresh-dependencies
```

**IntelliJ IDEA:**
- Click the Gradle icon on the right sidebar
- Click the refresh button (🔄)

### 5. For Runtime (Running the Game)
You also need to place the Pixelmon JAR in your `run/mods` folder so it loads when you test your mod:

**Option A: Copy manually**
```
run/mods/Pixelmon-1.21.1-9.1.13-universal.jar
```

**Option B: Gradle will handle it**
The mod should be available at runtime through the classpath.

## Verification

Once set up, you should be able to:
1. Import Pixelmon classes in your code
2. Build without errors: `./gradlew build`
3. Run the game: `./gradlew runClient`

## Troubleshooting

### "Cannot resolve symbol" errors
- Make sure the JAR is in the `libs` folder
- Refresh Gradle project
- Invalidate caches in IntelliJ: File → Invalidate Caches → Restart

### ClassNotFoundException at runtime
- Copy the Pixelmon JAR to `run/mods/` folder
- Make sure it's the same version used in `libs/`

### Wrong version
- Check the JAR filename matches `build.gradle`
- Ensure it's for Minecraft 1.21.1 and NeoForge (not Forge)
