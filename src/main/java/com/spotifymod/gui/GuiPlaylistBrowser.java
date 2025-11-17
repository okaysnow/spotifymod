package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiPlaylistBrowser extends GuiScreen {
    private final SpotifyAPI api;
    private final GuiScreen parentScreen;
    private List<SpotifyAPI.Playlist> playlists;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 8;
    private boolean isLoading = true;
    private String statusMessage = "";

    private GuiButton backButton;
    private GuiButton scrollUpButton;
    private GuiButton scrollDownButton;
    private GuiButton refreshButton;

    public GuiPlaylistBrowser(GuiScreen parent) {
        this.api = SpotifyMod.instance.getSpotifyAPI();
        this.parentScreen = parent;
        this.playlists = new ArrayList<>();
        loadPlaylists();
    }

    private void loadPlaylists() {
        isLoading = true;
        statusMessage = "Loading playlists...";
        api.getUserPlaylists().thenAccept(result -> {
            playlists = result;
            isLoading = false;
            if (playlists.isEmpty()) {
                statusMessage = "No playlists found";
            } else {
                statusMessage = "";
            }
        });
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;

        backButton = new GuiButton(0, centerX - 100, height - 30, 90, 20, "Back");
        refreshButton = new GuiButton(1, centerX + 10, height - 30, 90, 20, "Refresh");
        scrollUpButton = new GuiButton(2, width - 30, 50, 20, 20, "^");
        scrollDownButton = new GuiButton(3, width - 30, height - 60, 20, 20, "v");

        buttonList.add(backButton);
        buttonList.add(refreshButton);
        buttonList.add(scrollUpButton);
        buttonList.add(scrollDownButton);

        // Add playlist buttons
        updatePlaylistButtons();
    }

    private void updatePlaylistButtons() {
        // Remove old playlist buttons
        buttonList.removeIf(button -> button.id >= 100);

        int centerX = width / 2;
        int startY = 50;
        int buttonHeight = 20;
        int spacing = 25;

        int maxVisible = Math.min(ITEMS_PER_PAGE, playlists.size() - scrollOffset);
        for (int i = 0; i < maxVisible; i++) {
            int playlistIndex = scrollOffset + i;
            if (playlistIndex >= playlists.size()) break;

            SpotifyAPI.Playlist playlist = playlists.get(playlistIndex);
            String buttonText = playlist.name + " (" + playlist.trackCount + " tracks)";
            
            GuiButton playlistButton = new GuiButton(
                    100 + i,  // Use relative position, not absolute playlistIndex
                    centerX - 125,
                    startY + (i * spacing),
                    250,
                    buttonHeight,
                    buttonText
            );
            buttonList.add(playlistButton);
        }

        scrollUpButton.enabled = scrollOffset > 0;
        scrollDownButton.enabled = scrollOffset + ITEMS_PER_PAGE < playlists.size();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parentScreen);
                break;
            case 1: // Refresh
                loadPlaylists();
                scrollOffset = 0;
                initGui();
                break;
            case 2: // Scroll Up
                if (scrollOffset > 0) {
                    scrollOffset--;
                    updatePlaylistButtons();
                }
                break;
            case 3: // Scroll Down
                if (scrollOffset + ITEMS_PER_PAGE < playlists.size()) {
                    scrollOffset++;
                    updatePlaylistButtons();
                }
                break;
            default:
                if (button.id >= 100) {
                    int relativeIndex = button.id - 100;
                    int playlistIndex = scrollOffset + relativeIndex;
                    if (playlistIndex >= 0 && playlistIndex < playlists.size()) {
                        SpotifyAPI.Playlist playlist = playlists.get(playlistIndex);
                        mc.displayGuiScreen(new GuiPlaylistTracks(this, playlist));
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
        drawCenteredString(fontRendererObj, "My Playlists", centerX, 20, 0xFFFFFF);

        if (isLoading) {
            drawCenteredString(fontRendererObj, "Loading...", centerX, height / 2, 0xAAAAAA);
        } else if (playlists.isEmpty()) {
            drawCenteredString(fontRendererObj, "No playlists found", centerX, height / 2, 0xFF5555);
        } else {
            // Page indicator
            int totalPages = (int) Math.ceil((double) playlists.size() / ITEMS_PER_PAGE);
            int currentPage = (scrollOffset / ITEMS_PER_PAGE) + 1;
            String pageInfo = "Page " + currentPage + " / " + totalPages + " (" + playlists.size() + " playlists)";
            drawCenteredString(fontRendererObj, pageInfo, centerX, 35, 0xAAAAAA);
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
                    updatePlaylistButtons();
                }
            } else {
                // Scroll down
                if (scrollOffset + ITEMS_PER_PAGE < playlists.size()) {
                    scrollOffset++;
                    updatePlaylistButtons();
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
