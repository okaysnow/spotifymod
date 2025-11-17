package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiSearch extends GuiScreen {
    private final SpotifyAPI api;
    private final GuiScreen parentScreen;
    private GuiTextField searchField;
    private List<SpotifyAPI.SearchResult> results;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_PAGE = 10;
    private boolean isLoading = false;
    private String statusMessage = "";
    private String searchType = "track"; // "track", "artist", or "track,artist"
    private String repeatMode = "off"; // "off", "context", "track"
    private SpotifyAPI.SearchResult lastPlayedResult = null;

    private GuiButton backButton;
    private GuiButton searchButton;
    private GuiButton scrollUpButton;
    private GuiButton scrollDownButton;
    private GuiButton typeButton;
    private GuiButton repeatButton;

    public GuiSearch(GuiScreen parent) {
        this.api = SpotifyMod.instance.getSpotifyAPI();
        this.parentScreen = parent;
        this.results = new ArrayList<>();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;

        // Search field
        searchField = new GuiTextField(0, fontRendererObj, centerX - 150, 50, 250, 20);
        searchField.setMaxStringLength(100);
        searchField.setFocused(true);
        searchField.setText("");

        backButton = new GuiButton(0, centerX - 155, height - 30, 70, 20, "Back");
        searchButton = new GuiButton(1, centerX + 110, 50, 50, 20, "Search");
        scrollUpButton = new GuiButton(2, width - 30, 90, 20, 20, "^");
        scrollDownButton = new GuiButton(3, width - 30, height - 60, 20, 20, "v");
        
        String typeText = searchType.equals("track") ? "Tracks" : 
                         searchType.equals("artist") ? "Artists" : "All";
        typeButton = new GuiButton(4, centerX - 80, height - 30, 85, 20, "Type: " + typeText);
        
        String repeatText = "Repeat: " + repeatMode.toUpperCase();
        repeatButton = new GuiButton(5, centerX + 10, height - 30, 90, 20, repeatText);

        buttonList.add(backButton);
        buttonList.add(searchButton);
        buttonList.add(scrollUpButton);
        buttonList.add(scrollDownButton);
        buttonList.add(typeButton);
        buttonList.add(repeatButton);

        updateResultButtons();
    }

    private void updateResultButtons() {
        // Remove old result buttons
        buttonList.removeIf(button -> button.id >= 100);

        int centerX = width / 2;
        int startY = 90;
        int buttonHeight = 18;
        int spacing = 20;

        int maxVisible = Math.min(ITEMS_PER_PAGE, results.size() - scrollOffset);
        for (int i = 0; i < maxVisible; i++) {
            int resultIndex = scrollOffset + i;
            if (resultIndex >= results.size()) break;

            SpotifyAPI.SearchResult result = results.get(resultIndex);
            String buttonText;
            if (result.type.equals("track")) {
                buttonText = result.name + " - " + result.artist + " [" + result.getDurationString() + "]";
            } else {
                buttonText = "\u00a7e[Artist] \u00a7f" + result.name;
            }
            
            // Truncate if too long
            if (fontRendererObj.getStringWidth(buttonText) > 280) {
                while (fontRendererObj.getStringWidth(buttonText + "...") > 280 && buttonText.length() > 10) {
                    buttonText = buttonText.substring(0, buttonText.length() - 1);
                }
                buttonText += "...";
            }
            
            GuiButton resultButton = new GuiButton(
                    100 + i,
                    centerX - 150,
                    startY + (i * spacing),
                    300,
                    buttonHeight,
                    buttonText
            );
            buttonList.add(resultButton);
        }

        scrollUpButton.enabled = scrollOffset > 0;
        scrollDownButton.enabled = scrollOffset + ITEMS_PER_PAGE < results.size();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parentScreen);
                break;
            case 1: // Search
                performSearch();
                break;
            case 2: // Scroll Up
                if (scrollOffset > 0) {
                    scrollOffset--;
                    updateResultButtons();
                }
                break;
            case 3: // Scroll Down
                if (scrollOffset + ITEMS_PER_PAGE < results.size()) {
                    scrollOffset++;
                    updateResultButtons();
                }
                break;
            case 4: // Toggle search type
                if (searchType.equals("track")) {
                    searchType = "artist";
                } else if (searchType.equals("artist")) {
                    searchType = "track,artist";
                } else {
                    searchType = "track";
                }
                initGui();
                break;
            case 5: // Cycle Repeat
                if (repeatMode.equals("off")) {
                    repeatMode = "track";
                } else if (repeatMode.equals("track")) {
                    repeatMode = "context";
                } else {
                    repeatMode = "off";
                }
                api.setRepeat(repeatMode).thenAccept(success -> {
                    if (success) {
                        statusMessage = "Repeat: " + repeatMode.toUpperCase();
                        mc.addScheduledTask(this::initGui);
                    }
                });
                break;
            default:
                if (button.id >= 100) {
                    int relativeIndex = button.id - 100;
                    int resultIndex = scrollOffset + relativeIndex;
                    if (resultIndex >= 0 && resultIndex < results.size()) {
                        SpotifyAPI.SearchResult result = results.get(resultIndex);
                        lastPlayedResult = result;
                        if (result.type.equals("track")) {
                            statusMessage = "Playing: " + result.name;
                            api.playTrack(result.uri).thenAccept(success -> {
                                if (!success) {
                                    statusMessage = "Failed! Select device first";
                                }
                            });
                        } else if (result.type.equals("artist")) {
                            statusMessage = "Playing artist: " + result.name;
                            api.playArtist(result.uri).thenAccept(success -> {
                                if (!success) {
                                    statusMessage = "Failed! Select device first";
                                }
                            });
                        }
                    }
                }
                break;
        }
    }

    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            statusMessage = "Enter a search query";
            return;
        }

        isLoading = true;
        statusMessage = "Searching...";
        scrollOffset = 0;

        api.search(query, searchType).thenAccept(searchResults -> {
            results = searchResults;
            isLoading = false;
            if (results.isEmpty()) {
                statusMessage = "No results found";
            } else {
                statusMessage = "Found " + results.size() + " results";
            }
            mc.addScheduledTask(this::updateResultButtons);
        });
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel > 0) {
                if (scrollOffset > 0) {
                    scrollOffset--;
                    updateResultButtons();
                }
            } else {
                if (scrollOffset + ITEMS_PER_PAGE < results.size()) {
                    scrollOffset++;
                    updateResultButtons();
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int centerX = width / 2;

        // Title
        drawCenteredString(fontRendererObj, "Search Spotify", centerX, 20, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Enter song name, artist, or album", centerX, 35, 0xAAAAAA);

        // Search field
        searchField.drawTextBox();

        if (isLoading) {
            drawCenteredString(fontRendererObj, "Searching...", centerX, height / 2, 0xAAAAAA);
        } else if (results.isEmpty() && !statusMessage.isEmpty()) {
            drawCenteredString(fontRendererObj, statusMessage, centerX, height / 2, 0xFF5555);
        } else if (!results.isEmpty()) {
            // Page indicator
            int totalPages = (int) Math.ceil((double) results.size() / ITEMS_PER_PAGE);
            int currentPage = (scrollOffset / ITEMS_PER_PAGE) + 1;
            String pageInfo = "Page " + currentPage + " / " + totalPages + " (" + results.size() + " results)";
            drawCenteredString(fontRendererObj, pageInfo, centerX, 75, 0x888888);
        }

        if (!statusMessage.isEmpty() && !isLoading) {
            drawString(fontRendererObj, statusMessage, 10, height - 15, 0x55FF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.isFocused() && keyCode == Keyboard.KEY_RETURN) {
            performSearch();
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
        } else {
            searchField.textboxKeyTyped(typedChar, keyCode);
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
