package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.auth.OAuthCallbackServer;
import com.spotifymod.user.ProfileManager;
import com.spotifymod.user.UserProfile;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.List;

public class GuiProfileManager extends GuiScreen {
    private final GuiScreen parentScreen;
    private final ProfileManager profileManager;
    private List<UserProfile> profiles;
    private String statusMessage = "";
    
    private GuiButton backButton;
    private GuiButton createButton;
    private GuiButton authenticateButton;
    private GuiTextField profileNameField;
    private UserProfile selectedProfile;

    public GuiProfileManager(GuiScreen parent) {
        this.parentScreen = parent;
        this.profileManager = SpotifyMod.instance.getProfileManager();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        
        profiles = profileManager.getAllProfiles();
        int centerX = width / 2;
        
        backButton = new GuiButton(0, centerX - 100, height - 30, 200, 20, "Back");
        buttonList.add(backButton);
        
        // Profile name input
        profileNameField = new GuiTextField(1, fontRendererObj, centerX - 100, 50, 200, 20);
        profileNameField.setMaxStringLength(32);
        profileNameField.setText("New Profile");
        
        createButton = new GuiButton(2, centerX - 100, 75, 200, 20, "Create Profile");
        buttonList.add(createButton);
        
        // List existing profiles
        int startY = 110;
        for (int i = 0; i < profiles.size() && i < 6; i++) {
            UserProfile profile = profiles.get(i);
            String buttonText = profile.getProfileName();
            if (profile.isActive()) {
                buttonText += " [ACTIVE]";
            }
            if (!profile.hasValidTokens()) {
                buttonText += " (Not Authenticated)";
            }
            
            GuiButton profileButton = new GuiButton(100 + i, centerX - 150, startY + (i * 25), 200, 20, buttonText);
            buttonList.add(profileButton);
            
            GuiButton deleteButton = new GuiButton(200 + i, centerX + 55, startY + (i * 25), 45, 20, "Delete");
            buttonList.add(deleteButton);
        }
        
        // Authenticate button for selected profile
        if (selectedProfile != null && !selectedProfile.hasValidTokens()) {
            authenticateButton = new GuiButton(300, centerX - 100, height - 60, 200, 20, "Authenticate Selected");
            buttonList.add(authenticateButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == 2) {
            // Create profile
            String name = profileNameField.getText().trim();
            if (!name.isEmpty()) {
                UserProfile newProfile = profileManager.createProfile(name);
                profileManager.setActiveProfile(newProfile);
                statusMessage = "Profile created: " + name;
                initGui();
            }
        } else if (button.id >= 100 && button.id < 200) {
            // Select profile
            int index = button.id - 100;
            if (index < profiles.size()) {
                selectedProfile = profiles.get(index);
                profileManager.setActiveProfile(selectedProfile);
                statusMessage = "Switched to: " + selectedProfile.getProfileName();
                initGui();
            }
        } else if (button.id >= 200 && button.id < 300) {
            // Delete profile
            int index = button.id - 200;
            if (index < profiles.size()) {
                UserProfile toDelete = profiles.get(index);
                profileManager.deleteProfile(toDelete.getProfileId());
                statusMessage = "Deleted profile: " + toDelete.getProfileName();
                selectedProfile = null;
                initGui();
            }
        } else if (button.id == 300 && selectedProfile != null) {
            // Authenticate selected profile
            authenticateProfile(selectedProfile);
        }
    }

    private void authenticateProfile(UserProfile profile) {
        profileManager.setActiveProfile(profile);
        
        OAuthCallbackServer server = new OAuthCallbackServer();
        server.start().thenAccept(authCode -> {
            SpotifyMod.instance.getSpotifyAPI().authenticate(authCode).thenAccept(success -> {
                if (success) {
                    statusMessage = "Authentication successful for " + profile.getProfileName();
                    mc.addScheduledTask(this::initGui);
                } else {
                    statusMessage = "Authentication failed!";
                }
            });
        });

        try {
            Desktop.getDesktop().browse(URI.create(SpotifyMod.instance.getSpotifyAPI().getAuthUrl()));
            statusMessage = "Opening browser for authentication...";
        } catch (Exception e) {
            statusMessage = "Failed to open browser";
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        
        drawCenteredString(fontRendererObj, "Profile Manager", centerX, 20, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Create New Profile:", centerX, 38, 0xAAAAAA);
        
        profileNameField.drawTextBox();
        
        drawCenteredString(fontRendererObj, "Your Profiles:", centerX, 98, 0xAAAAAA);
        
        if (profiles.isEmpty()) {
            drawCenteredString(fontRendererObj, "No profiles yet", centerX, height / 2, 0x888888);
        }
        
        if (!statusMessage.isEmpty()) {
            drawString(fontRendererObj, statusMessage, 10, height - 15, 0x55FF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        profileNameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (profileNameField.isFocused()) {
            profileNameField.textboxKeyTyped(typedChar, keyCode);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        profileNameField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }
}
