package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import com.spotifymod.debug.LogBuffer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiPlaylistTracks extends GuiScreen {
    private final SpotifyAPI api;
    private final GuiScreen parentScreen;
    private final SpotifyAPI.Playlist playlist;
    private List<SpotifyAPI.PlaylistTrack> tracks;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private boolean isLoading = true;
    private String statusMessage = "";

    private GuiButton backButton;
    private GuiButton playAllButton;
    private GuiButton scrollUpButton;
    private GuiButton scrollDownButton;

    public GuiPlaylistTracks(GuiScreen parent, SpotifyAPI.Playlist playlist) {
        this.api = SpotifyMod.instance.getSpotifyAPI();
        this.parentScreen = parent;
        this.playlist = playlist;
        this.tracks = new ArrayList<>();
        loadTracks();
    }

    private void loadTracks() {
        isLoading = true;
        statusMessage = "Loading tracks...";
        api.getPlaylistTracks(playlist.id).thenAccept(result -> {
            tracks = result;
            isLoading = false;
            if (tracks.isEmpty()) {
                statusMessage = "No tracks found";
                LogBuffer.get().warn("getPlaylistTracks returned empty list for playlist: " + playlist.id);
            } else {
                statusMessage = "";
                LogBuffer.get().info("Loaded " + tracks.size() + " tracks for playlist: " + playlist.name);
            }
            // Update GUI on main thread
            mc.addScheduledTask(() -> {
                if (this.equals(mc.currentScreen)) {
                    updateTrackButtons();
                }
            });
        });
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;

        backButton = new GuiButton(0, centerX - 155, height - 30, 90, 20, "Back");
        playAllButton = new GuiButton(1, centerX - 45, height - 30, 200, 20, "Play Playlist");
        scrollUpButton = new GuiButton(2, width - 30, 60, 20, 20, "^");
        scrollDownButton = new GuiButton(3, width - 30, height - 60, 20, 20, "v");

        buttonList.add(backButton);
        buttonList.add(playAllButton);
        buttonList.add(scrollUpButton);
        buttonList.add(scrollDownButton);

        updateTrackButtons();
    }

    private void updateTrackButtons() {
        // Remove old track buttons
        buttonList.removeIf(button -> button.id >= 100);

        int centerX = width / 2;
        int startY = 60;
        int buttonHeight = 18;
        int spacing = 20;

        int maxVisible = Math.min(ITEMS_PER_PAGE, tracks.size() - scrollOffset);
        for (int i = 0; i < maxVisible; i++) {
            int trackIndex = scrollOffset + i;
            if (trackIndex >= tracks.size()) break;

            SpotifyAPI.PlaylistTrack track = tracks.get(trackIndex);
            String buttonText = (trackIndex + 1) + ". " + track.name + " - " + track.artist + " [" + track.getDurationString() + "]";
            
            // Truncate if too long
            if (fontRendererObj.getStringWidth(buttonText) > 280) {
                while (fontRendererObj.getStringWidth(buttonText + "...") > 280 && buttonText.length() > 10) {
                    buttonText = buttonText.substring(0, buttonText.length() - 1);
                }
                buttonText += "...";
            }
            
            GuiButton trackButton = new GuiButton(
                    100 + i,  // Use relative position, not absolute trackIndex
                    centerX - 150,
                    startY + (i * spacing),
                    300,
                    buttonHeight,
                    buttonText
            );
            buttonList.add(trackButton);
        }

        scrollUpButton.enabled = scrollOffset > 0;
        scrollDownButton.enabled = scrollOffset + ITEMS_PER_PAGE < tracks.size();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parentScreen);
                break;
            case 1: // Play All 
                statusMessage = "Starting playlist...";
                api.playPlaylist(playlist.uri).thenAccept(success -> {
                    if (success) {
                        statusMessage = "Playing playlist: " + playlist.name;
                    } else {
                        statusMessage = "Failed! Go to main menu > Devices > Select your device";
                    }
                });
                break;
            case 2: // Scroll Up
                if (scrollOffset > 0) {
                    scrollOffset--;
                    updateTrackButtons();
                }
                break;
            case 3: // Scroll Down
                if (scrollOffset + ITEMS_PER_PAGE < tracks.size()) {
                    scrollOffset++;
                    updateTrackButtons();
                }
                break;
            default:
                if (button.id >= 100) {
                    int relativeIndex = button.id - 100;
                    int trackIndex = scrollOffset + relativeIndex;
                    if (trackIndex >= 0 && trackIndex < tracks.size()) {
                        SpotifyAPI.PlaylistTrack track = tracks.get(trackIndex);
                        statusMessage = "Starting: " + track.name;
                        api.playTrackFromPlaylist(playlist.uri, track.uri).thenAccept(success -> {
                            if (success) {
                                statusMessage = "Playing: " + track.name;
                            } else {
                                statusMessage = "Failed! Go to Devices menu";
                            }
                        });
                    }
                }
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int centerX = width / 2;

        // Title
        drawCenteredString(fontRendererObj, playlist.name, centerX, 20, 0xFFFFFF);
        drawCenteredString(fontRendererObj, playlist.trackCount + " tracks", centerX, 35, 0xAAAAAA);

        if (isLoading) {
            drawCenteredString(fontRendererObj, "Loading tracks...", centerX, height / 2, 0xAAAAAA);
        } else if (tracks.isEmpty()) {
            drawCenteredString(fontRendererObj, "No tracks found", centerX, height / 2, 0xFF5555);
        } else {
            // Page indicator
            int totalPages = (int) Math.ceil((double) tracks.size() / ITEMS_PER_PAGE);
            int currentPage = (scrollOffset / ITEMS_PER_PAGE) + 1;
            String pageInfo = "Page " + currentPage + " / " + totalPages;
            drawCenteredString(fontRendererObj, pageInfo, centerX, 48, 0x888888);
        }

        if (!statusMessage.isEmpty()) {
            drawString(fontRendererObj, statusMessage, 10, height - 15, 0x55FF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel > 0) {
                // Scroll up
                if (scrollOffset > 0) {
                    scrollOffset--;
                    updateTrackButtons();
                }
            } else {
                // Scroll down
                if (scrollOffset + ITEMS_PER_PAGE < tracks.size()) {
                    scrollOffset++;
                    updateTrackButtons();
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
