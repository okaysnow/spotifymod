# Spotify Mod Setup Guide

## Quick Start

### Step 1: Create a Spotify Developer App

1. Visit [Spotify Developer Dashboard](https://developer.spotify.com/dashboard)
2. Log in with your Spotify account
3. Click **"Create an App"**
4. Fill in the details:
   - **App Name**: "Minecraft Spotify Mod" (or any name you prefer)
   - **App Description**: "Integration for controlling Spotify from Minecraft"
   - **Redirect URI**: `http://127.0.0.1:8888/callback` ⚠️ **This is important!**
5. Click **"Save"**
6. You'll now see your app dashboard with:
   - **Client ID** (visible by default)
   - **Client Secret** (click "Show Client Secret" to reveal it)

### Step 2: Configure the Mod

1. Install the mod in your `.minecraft/mods` folder
2. Launch Minecraft once - this will create the config directory
3. Navigate to `.minecraft/config/`
4. You should see a file called `spotify_credentials.json` (if not, it will be created on first run)
5. Open `spotify_credentials.json` in a text editor
6. Replace the placeholders with your actual credentials:

```json
{
  "clientId": "paste_your_client_id_here",
  "clientSecret": "paste_your_client_secret_here"
}
```

### Step 3: Authenticate

1. Launch Minecraft
2. Press **P** to open the Spotify control GUI
3. Click **"Authenticate with Spotify"**
4. Your browser will open (or a URL will be copied to clipboard)
5. Log in and authorize the app
6. You'll be redirected back - authentication complete!

## Troubleshooting

### "Invalid credentials" error
- Make sure you copied the Client ID and Client Secret correctly (no extra spaces)
- Check that the credentials file is valid JSON format
- Verify the file is named exactly `spotify_credentials.json`

### Browser doesn't open
- The URL is automatically copied to your clipboard
- Paste it into any browser manually
- Or click the "Copy URL to Clipboard" button in the GUI

### Authentication keeps failing
- Check that your redirect URI is exactly: `http://127.0.0.1:8888/callback`
- Make sure port 8888 is not blocked by firewall
- Try restarting Minecraft after updating credentials

### Can't control Spotify
- Make sure Spotify is running on your computer
- Open Spotify and start playing any song first
- Go to the mod's GUI → Devices menu and select your device

## Security Notes

⚠️ **Important Security Information:**

- Your `spotify_credentials.json` file contains sensitive information
- **Never share this file** with anyone
- **Never commit it to Git** (it's in .gitignore by default)
- These credentials only allow control of Spotify playback, not account access
- If credentials are compromised, regenerate them in the Spotify Developer Dashboard

## Multi-User Setup

The mod supports multiple Spotify accounts:

1. Each user needs to authenticate once
2. Profiles are saved locally in `.minecraft/config/spotify_profiles.json`
3. Switch profiles using the "Profiles" button in the GUI
4. All users can share the same app credentials from Step 1

## Need Help?

- Check that you followed each step exactly
- Make sure Minecraft 1.8.9 with Forge is installed
- Verify Spotify is installed and running
- Try restarting both Minecraft and Spotify

For more detailed information, see the main README.md file.
