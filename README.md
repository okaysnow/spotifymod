# Spotify Mod for Minecraft 1.8.9

A Forge mod that integrates Spotify into Minecraft, allowing you to control your music playback directly from the game.

## Server Compatibility

✅ **Works on ANY server (and singleplayer)!**

This mod is **100% client-side** - it doesn't modify server behavior or require server-side installation. It only controls your local Spotify application through the Spotify Web API. You can use it on:
- Vanilla servers
- Modded servers
- Minigames servers (Hypixel, Mineplex, etc.)
- Private/whitelisted servers
- Singleplayer worlds

The listening party feature works within your Minecraft client session and doesn't send any data through the Minecraft server.

## Features

- **Multi-User Support**: Multiple Spotify accounts can be used on the same Minecraft installation
- **Profile Management**: Create and manage multiple user profiles with separate authentication
- **Listening Parties**: Create or join listening parties to sync playback with friends
- **In-game Spotify Control**: Full playback controls (play, pause, next, previous)
- **Playlist Browser**: Browse and play your Spotify playlists and tracks
- **Draggable HUD Overlay**: Shows currently playing track with customizable position
- **GUI Screen**: Detailed control panel with track information and progress bar
- **Keybindings**: Customizable hotkeys for quick control
- **OAuth Authentication**: Secure login with Spotify
- **Volume Control**: Adjust Spotify volume from in-game
- **Commands**: /spotifyhud for HUD customization, /spotify for party and profile management

## Commands

### HUD Commands
- `/spotifyhud drag` - Toggle drag mode to reposition the HUD
- `/spotifyhud toggle` - Show/hide the HUD overlay
- `/spotifyhud reset` - Reset HUD position to default

### Spotify Commands
- `/spotify party create <name>` - Create a new listening party
- `/spotify party join <code>` - Join a party using the code
- `/spotify party leave` - Leave the current party
- `/spotify party info` - Show current party information
- `/spotify profile list` - List all profiles
- `/spotify profile info` - Show active profile information

## Setup Instructions

### 1. Create a Spotify Application

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in with your Spotify account
3. Click "Create an App"
4. Fill in the app name and description
5. Add `http://127.0.0.1:8888/callback` to the Redirect URIs
6. Save your app
7. Copy your **Client ID** and **Client Secret**

### 2. Configure the Mod

Open `src/main/java/com/spotifymod/api/SpotifyAPI.java` and replace:

```java
private static final String CLIENT_ID = "YOUR_CLIENT_ID_HERE";
private static final String CLIENT_SECRET = "YOUR_CLIENT_SECRET_HERE";
```

**Important**: Make sure your Spotify app has the following scopes enabled:
- `user-read-playback-state`
- `user-modify-playback-state`
- `user-read-currently-playing`
- `playlist-read-private`
- `playlist-read-collaborative`

### 3. Build the Mod

**Windows:**
```powershell
gradlew.bat setupDecompWorkspace
gradlew.bat build
```

**Linux/Mac:**
```bash
./gradlew setupDecompWorkspace
./gradlew build
```

The compiled mod will be in `build/libs/spotifymod-1.0.jar`

**Note:** First build may take 10-15 minutes as it downloads Minecraft and dependencies.

### 4. Install the Mod

1. Copy the JAR file to your `.minecraft/mods` folder
2. Launch Minecraft 1.8.9 with Forge installed
3. Press **P** in-game to open the Spotify GUI
4. Click "Authenticate with Spotify" and log in
5. Start controlling your music!

## Usage

### Keybindings

**Configure in Game:** Go to `ESC > Options > Controls` and scroll to the "Spotify Controls" category.

**Default Bindings:**
- **P** - Open Spotify Control GUI
- **Play/Pause** - Toggle playback (unbound by default - assign your preferred key)
- **Next Track** - Skip to next song (unbound by default - assign your preferred key)
- **Previous Track** - Go to previous song (unbound by default - assign your preferred key)

**Tips:**
- Bind to media keys (if your keyboard supports them)
- Use `/spotify keys` in-game to see keybinding help
- All controls work even when in other GUIs or playing

### Profile Management

**Multiple Users on One PC:**
Each person can have their own profile with separate Spotify authentication:

1. Press **P** and click **Profiles**
2. Enter a profile name and click **Create Profile**
3. Click on a profile to activate it
4. Authenticate the profile with Spotify
5. Switch between profiles anytime

**Profile Features:**
- Each profile stores its own Spotify tokens
- Switch profiles without re-authenticating
- Delete profiles you no longer need
- Active profile shown in the main GUI

### Listening Parties

**Listen Together with Friends:**
Create or join a listening party to sync playback with other players:

**Creating a Party:**
1. Press **P** and click **Party**
2. Enter a party name and click **Create Listening Party**
3. Share the party code (e.g., "A3B7C9D2") with friends
4. As host, your playback controls the party
5. Or use command: `/spotify party create My Party`

**Joining a Party:**
1. Press **P** and click **Party**
2. Enter the party code or click an available party
3. Your playback will automatically sync with the host
4. Or use command: `/spotify party join A3B7C9D2`

**Party Commands:**
- `/spotify party create <name>` - Create a new listening party
- `/spotify party join <code>` - Join a party using the code
- `/spotify party leave` - Leave the current party
- `/spotify party info` - Show party details and members

**How It Works:**
- The party **host** controls playback for everyone
- **Members** have their Spotify automatically synced
- Works across different Spotify accounts
- Syncs track, position, and play/pause state
- Automatic drift correction keeps everyone in sync
- Up to 10 members per party (configurable)

### GUI Features

- Real-time track information display
- Play/Pause button
- Previous/Next track buttons
- Volume controls
- Progress bar with time display
- Authentication button
- **Browse Playlists** - Access all your playlists and play specific tracks

### Playlist Browser

- View all your Spotify playlists
- See track count for each playlist
- Browse tracks in each playlist
- Play entire playlists or individual tracks
- Scroll through large libraries

### HUD Overlay

The HUD displays:
- Current track name
- Artist name
- Track progress time

**Color Schemes:**
Choose from multiple animated color schemes for the HUD - all with flowing animations:
- **Default** - Classic white/gray with subtle flow
- **Chroma** - Fast-flowing rainbow gradient
- **Ocean** - Flowing waves from light blue to dark blue
- **Sunset** - Flowing red to orange to yellow gradient
- **Forest** - Flowing light green to dark green
- **Purple** - Flowing light purple to deep indigo
- **Fire** - Flowing orange to red gradient
- **Ice** - Flowing cyan to light blue
- **Gold** - Flowing golden yellow to orange
- **Pink** - Flowing light pink to hot pink

All color schemes feature smooth, cascading animations with multiple waves for a dynamic effect.

Access color settings by pressing **P** and clicking **HUD** button.

**HUD Commands:**
- `/spotifyhud drag` - Toggle drag mode to reposition the HUD
- `/spotifyhud toggle` - Show/hide the HUD overlay
- `/spotifyhud reset` - Reset HUD position to default (5, 5)

**How to customize HUD:**
1. Press **P** to open Spotify GUI
2. Click **HUD** button
3. Enable/disable HUD display
4. **Enable/Disable Background** - Toggle the semi-transparent dark background
5. Use **<** and **>** to cycle through color schemes
6. See live animated preview of colors with/without background

**How to reposition HUD:**
1. Type `/spotifyhud drag` in chat
2. Click and drag the HUD to your preferred location
3. Type `/spotifyhud drag` again to save and exit drag mode

## Server Compatibility & Technical Details

### ✅ Server Compatibility
This mod is **100% client-side only** and works on:
- ✅ Any vanilla Minecraft server
- ✅ Any modded server (doesn't need to be installed server-side)
- ✅ Minigame servers (Hypixel, Mineplex, etc.)
- ✅ Private/whitelisted servers
- ✅ Singleplayer worlds
- ✅ LAN worlds

**Why it works everywhere:**
- The mod only communicates with Spotify's Web API (not the Minecraft server)
- No server-side installation required
- No server modifications needed
- Listening parties are local to your client session
- Commands are client-side only

### 🔒 What the Server Sees
- Nothing! The mod doesn't send any data through the Minecraft server
- All Spotify API calls go directly from your client to Spotify
- Parties exist only in your game client's memory

## Requirements

- Minecraft 1.8.9
- Minecraft Forge 1.8.9-11.15.1.2318 or later
- Active Spotify Premium account (for each user)
- Spotify Desktop app running (for each user)
- Internet connection for API calls

## Notes

- Multiple users can use the mod by creating separate profiles
- Each profile needs to authenticate with their own Spotify account
- Listening parties sync playback across all members in real-time
- You must have Spotify open and playing on a device for the mod to work
- The mod uses the Spotify Web API, which requires an active internet connection
- Authentication tokens are saved in `config/spotify_profiles.json`
- HUD settings stored in `config/spotifymod.cfg`
- The mod does not pause your game when the GUI is open
- Party sync updates every second to keep playback synchronized

## Troubleshooting

**Authentication fails:**
- Make sure the redirect URI is exactly `http://127.0.0.1:8888/callback` in your Spotify app settings
- Check that port 8888 is not being used by another application

**No track information showing:**
- Ensure Spotify is running and playing music on any device
- Check that you have an active Spotify Premium subscription
- Try refreshing by reopening the GUI

**Mod won't load:**
- Verify you're using Minecraft 1.8.9 with Forge installed
- Check the Forge logs for any errors
**Build/Gradle issues:**
- Make sure you have Java 8 (JDK 1.8) installed (Gradle 2.14.1 + ForgeGradle 2.2 won't run on Java 9+)
- First build takes 10-15 minutes - this is normal
- If setupDecompWorkspace fails, try running it again
- Clear Gradle cache: delete `.gradle` folder and run build again
- On Windows, use `gradlew.bat` not `gradlew`
- Make sure you have internet connection for downloading dependencies
- If OkHttp dependency fails, check your internet firewall settings
- If you see `Could not run build action using connection to Gradle distribution ... 2.14.1-bin.zip` while using Java 17+, switch to JDK 8 (set JAVA8_HOME) or install Temurin JDK 8.
- Use provided scripts: `BuildMod.bat` (enforces Java 8) and `RunMod.bat` (auto-detects JDK 8 and launches dev client).


**Party sync not working:**
- Make sure all members have Spotify open and authenticated
- Host must be actively playing music
- Check that you're in the same party (use `/spotify party info`)
- Parties are local to the Minecraft session (not persistent across restarts)

**Multiple users issues:**
- Each user must create their own profile
- Make sure the correct profile is active before authenticating
- Profiles are stored per-computer, not per-Minecraft-world

**Server kicked me for commands:**
- This should never happen as commands are client-side only
- If it does, the server may have strict command filtering
- Use the GUI instead (Press P)

## Credits

Created with Minecraft Forge and the Spotify Web API.

## License

This mod is for educational purposes. Spotify is a trademark of Spotify AB.
