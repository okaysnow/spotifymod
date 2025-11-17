# Building the Spotify Mod

This guide will help you build the Spotify mod from source.

## Prerequisites

1. **Java Development Kit (JDK) 8**
   - Download from [Oracle](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) or [AdoptOpenJDK](https://adoptopenjdk.net/)
   - Verify installation: `java -version` should show 1.8.x

2. **Git** (optional, for cloning)
   - Download from [git-scm.com](https://git-scm.com/)

3. **Internet Connection**
   - Required for downloading Minecraft, Forge, and dependencies

## Build Steps

### 1. Configure Your Spotify API Credentials

Before building, you need to set up your Spotify API credentials:

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Create a new app
3. Add `http://localhost:8888/callback` to Redirect URIs
4. Copy your Client ID and Client Secret
5. Edit `src/main/java/com/spotifymod/api/SpotifyAPI.java`:
   ```java
   private static final String CLIENT_ID = "YOUR_CLIENT_ID_HERE";
   private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE";
   ```

### 2. Setup Workspace

**Windows:**
```powershell
cd c:\spotify_mod
gradlew.bat setupDecompWorkspace
```

**Linux/Mac:**
```bash
cd /path/to/spotify_mod
./gradlew setupDecompWorkspace
```

**Note:** This step takes 10-15 minutes on first run as it:
- Downloads Minecraft 1.8.9
- Downloads Forge
- Decompiles and deobfuscates Minecraft
- Sets up the development workspace

### 3. Build the Mod

**Windows:**
```powershell
gradlew.bat build
```

**Linux/Mac:**
```bash
./gradlew build
```

The compiled mod JAR will be in: `build/libs/spotifymod-1.0.jar`

### 4. Test in Development Environment (Optional)

To test the mod in a development environment:

**Windows:**
```powershell
gradlew.bat runClient
```

**Linux/Mac:**
```bash
./gradlew runClient
```

This launches Minecraft with the mod pre-loaded.

## Gradle Tasks

- `setupDecompWorkspace` - Sets up the development workspace (run once)
- `build` - Builds the mod JAR
- `runClient` - Runs Minecraft client with the mod
- `clean` - Cleans build artifacts
- `tasks` - Lists all available tasks

## Troubleshooting

### "java" is not recognized
- Install JDK 8
- Add Java to your PATH environment variable

### setupDecompWorkspace fails
- Check internet connection
- Delete `.gradle` folder and try again
- Make sure you have at least 4GB of free disk space

### Build fails with "Cannot resolve dependencies"
- Check internet connection
- Check firewall settings (must allow Gradle to download)
- Try with a VPN if your region blocks certain Maven repositories

### "JAVA_HOME is not set"
**Windows:**
```powershell
setx JAVA_HOME "C:\Program Files\Java\jdk1.8.0_xxx"
```

**Linux/Mac:**
```bash
export JAVA_HOME=/path/to/jdk1.8.0
```

### Build is very slow
- Normal for first build (10-15 minutes)
- Subsequent builds are much faster (1-2 minutes)
- Increase Gradle memory in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4G
  ```

### Mod doesn't work after building
- Make sure you configured Spotify API credentials
- Check that the JAR is in `.minecraft/mods` folder
- Verify you have Forge 1.8.9 installed
- Check Minecraft logs for errors

## Project Structure

```
spotify_mod/
├── build.gradle          # Gradle build configuration
├── gradle.properties     # Gradle properties (memory settings)
├── settings.gradle       # Project settings
├── gradlew              # Gradle wrapper (Unix)
├── gradlew.bat          # Gradle wrapper (Windows)
├── src/
│   └── main/
│       ├── java/        # Java source code
│       │   └── com/spotifymod/
│       └── resources/   # Resources (mcmod.info)
└── build/               # Build output
    └── libs/            # Compiled JAR here
```

## Clean Build

If you encounter issues, try a clean build:

**Windows:**
```powershell
gradlew.bat clean
gradlew.bat setupDecompWorkspace
gradlew.bat build
```

**Linux/Mac:**
```bash
./gradlew clean
./gradlew setupDecompWorkspace
./gradlew build
```

## Dependencies Included

The mod bundles these dependencies in the JAR:
- **OkHttp 3.14.9** - HTTP client for Spotify API
- **Okio 1.17.2** - Required by OkHttp
- **Gson 2.8.0** - JSON parsing (also included in Minecraft)

## Advanced: IDE Setup

### IntelliJ IDEA

1. Run: `gradlew.bat idea` (or `./gradlew idea`)
2. Open the generated `.ipr` file in IntelliJ
3. Run configurations will be automatically created

### Eclipse

1. Run: `gradlew.bat eclipse` (or `./gradlew eclipse`)
2. Import project in Eclipse
3. Right-click project → Run As → Minecraft Client

## Support

If you continue to have build issues:
1. Check you have Java 8 (not 11, not 17)
2. Delete `.gradle` folder completely
3. Delete `build` folder
4. Run `setupDecompWorkspace` again
5. Check Gradle output for specific error messages
