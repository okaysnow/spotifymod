package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.config.SpotifyConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class GuiHudSettings extends GuiScreen {
    private final GuiScreen parentScreen;
    private GuiButton backButton;
    private GuiButton toggleHudButton;
    private GuiButton toggleBackgroundButton;
    private GuiButton colorSchemeButton;
    private GuiButton prevColorButton;
    private GuiButton nextColorButton;
    private String statusMessage = "";

    public GuiHudSettings(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        
        int centerX = width / 2;
        
        backButton = new GuiButton(0, centerX - 100, height - 30, 200, 20, "Back");
        buttonList.add(backButton);
        
        SpotifyConfig config = SpotifyMod.instance.getConfig();
        
        // Toggle HUD button
        String toggleText = config.isHudEnabled() ? "Disable HUD" : "Enable HUD";
        toggleHudButton = new GuiButton(1, centerX - 100, 80, 200, 20, toggleText);
        buttonList.add(toggleHudButton);
        
        // Toggle Background button
        String bgText = config.isHudBackground() ? "Disable Background" : "Enable Background";
        toggleBackgroundButton = new GuiButton(5, centerX - 100, 105, 200, 20, bgText);
        buttonList.add(toggleBackgroundButton);
        
        // Color scheme navigation
        prevColorButton = new GuiButton(2, centerX - 100, 145, 45, 20, "<");
        nextColorButton = new GuiButton(3, centerX + 55, 145, 45, 20, ">");
        
        HudColorScheme currentScheme = HudColorScheme.fromString(config.getHudColorScheme());
        colorSchemeButton = new GuiButton(4, centerX - 50, 145, 100, 20, currentScheme.getDisplayName());
        colorSchemeButton.enabled = false; // Display only
        
        buttonList.add(prevColorButton);
        buttonList.add(colorSchemeButton);
        buttonList.add(nextColorButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        SpotifyConfig config = SpotifyMod.instance.getConfig();
        
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parentScreen);
                break;
                
            case 1: // Toggle HUD
                config.setHudEnabled(!config.isHudEnabled());
                config.save();
                statusMessage = config.isHudEnabled() ? "HUD Enabled" : "HUD Disabled";
                initGui();
                break;
                
            case 5: // Toggle Background
                config.setHudBackground(!config.isHudBackground());
                config.save();
                statusMessage = config.isHudBackground() ? "Background Enabled" : "Background Disabled";
                initGui();
                break;
                
            case 2: // Previous color scheme
                HudColorScheme currentScheme = HudColorScheme.fromString(config.getHudColorScheme());
                HudColorScheme prevScheme = currentScheme.previous();
                config.setHudColorScheme(prevScheme.name());
                config.save();
                statusMessage = "Color: " + prevScheme.getDisplayName();
                initGui();
                break;
                
            case 3: // Next color scheme
                currentScheme = HudColorScheme.fromString(config.getHudColorScheme());
                HudColorScheme nextScheme = currentScheme.next();
                config.setHudColorScheme(nextScheme.name());
                config.save();
                statusMessage = "Color: " + nextScheme.getDisplayName();
                initGui();
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        
        drawCenteredString(fontRendererObj, "HUD Settings", centerX, 20, 0xFFFFFF);
        
        drawCenteredString(fontRendererObj, "HUD Display:", centerX, 68, 0xAAAAAA);
        drawCenteredString(fontRendererObj, "Color Scheme:", centerX, 133, 0xAAAAAA);
        
        // Draw color preview with current scheme
        SpotifyConfig config = SpotifyMod.instance.getConfig();
        HudColorScheme scheme = HudColorScheme.fromString(config.getHudColorScheme());
        long ticks = mc.theWorld != null ? mc.theWorld.getTotalWorldTime() : System.currentTimeMillis() / 50;
        
        int previewY = 180;
        drawCenteredString(fontRendererObj, "Preview:", centerX, previewY - 12, 0xAAAAAA);
        
        // Draw background preview if enabled
        if (config.isHudBackground()) {
            drawRect(centerX - 80, previewY - 2, centerX + 80, previewY + 34, 0x80000000);
        }
        
        int color1 = scheme.getColor(0, ticks);
        int color2 = scheme.getColor(1, ticks);
        int color3 = scheme.getColor(2, ticks);
        
        drawCenteredString(fontRendererObj, "Now: Example Song", centerX, previewY, color1);
        drawCenteredString(fontRendererObj, "Artist Name", centerX, previewY + 12, color2);
        drawCenteredString(fontRendererObj, "1:23 / 3:45", centerX, previewY + 24, color3);
        
        // Draw status message
        if (!statusMessage.isEmpty()) {
            drawString(fontRendererObj, statusMessage, 10, height - 15, 0x55FF55);
        }
        
        // Instructions
        drawCenteredString(fontRendererObj, "\u00a77Use \u00a7f/spotifyhud drag\u00a77 to reposition the HUD", centerX, height - 65, 0x888888);
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
