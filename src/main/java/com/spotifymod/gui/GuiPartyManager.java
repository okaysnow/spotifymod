package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.party.ListeningParty;
import com.spotifymod.party.PartyManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.List;

public class GuiPartyManager extends GuiScreen {
    private final GuiScreen parentScreen;
    private final PartyManager partyManager;
    private String statusMessage = "";
    
    private GuiButton backButton;
    private GuiButton createPartyButton;
    private GuiButton leavePartyButton;
    private GuiButton refreshButton;
    private GuiTextField partyNameField;
    private GuiTextField joinCodeField;

    public GuiPartyManager(GuiScreen parent) {
        this.parentScreen = parent;
        this.partyManager = SpotifyMod.instance.getPartyManager();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        
        int centerX = width / 2;
        
        backButton = new GuiButton(0, centerX - 100, height - 30, 200, 20, "Back");
        buttonList.add(backButton);
        
        if (!partyManager.isInParty()) {
            // Create party section
            partyNameField = new GuiTextField(1, fontRendererObj, centerX - 100, 60, 200, 20);
            partyNameField.setMaxStringLength(32);
            partyNameField.setText("My Party");
            
            createPartyButton = new GuiButton(1, centerX - 100, 85, 200, 20, "Create Listening Party");
            buttonList.add(createPartyButton);
            
            // Join party section
            joinCodeField = new GuiTextField(2, fontRendererObj, centerX - 100, 130, 200, 20);
            joinCodeField.setMaxStringLength(8);
            joinCodeField.setText("");
            
            // List available parties
            List<ListeningParty> parties = PartyManager.getAllParties();
            int startY = 165;
            for (int i = 0; i < Math.min(parties.size(), 5); i++) {
                ListeningParty party = parties.get(i);
                String buttonText = party.getPartyName() + " [" + party.getPartyId() + "] (" + 
                                   party.getMemberCount() + "/" + party.getMaxMembers() + ")";
                GuiButton joinButton = new GuiButton(100 + i, centerX - 150, startY + (i * 25), 300, 20, buttonText);
                if (party.isFull()) {
                    joinButton.enabled = false;
                }
                buttonList.add(joinButton);
            }
            
            refreshButton = new GuiButton(2, centerX - 100, height - 60, 200, 20, "Refresh Party List");
            buttonList.add(refreshButton);
        } else {
            // Already in a party
            leavePartyButton = new GuiButton(3, centerX - 100, height - 60, 200, 20, "Leave Party");
            buttonList.add(leavePartyButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        int centerX = width / 2;
        
        if (button.id == 0) {
            mc.displayGuiScreen(parentScreen);
        } else if (button.id == 1) {
            // Create party
            String name = partyNameField.getText().trim();
            if (!name.isEmpty()) {
                ListeningParty party = partyManager.createParty(name);
                if (party != null) {
                    statusMessage = "Party created! Code: " + party.getPartyId();
                    initGui();
                } else {
                    statusMessage = "Failed to create party. Do you have an active profile?";
                }
            }
        } else if (button.id == 2) {
            // Refresh
            statusMessage = "Party list refreshed";
            initGui();
        } else if (button.id == 3) {
            // Leave party
            partyManager.leaveCurrentParty();
            statusMessage = "Left the party";
            initGui();
        } else if (button.id >= 100 && button.id < 200) {
            // Join party
            List<ListeningParty> parties = PartyManager.getAllParties();
            int index = button.id - 100;
            if (index < parties.size()) {
                ListeningParty party = parties.get(index);
                if (partyManager.joinParty(party.getPartyId())) {
                    statusMessage = "Joined party: " + party.getPartyName();
                    initGui();
                } else {
                    statusMessage = "Failed to join party";
                }
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        int centerX = width / 2;
        
        drawCenteredString(fontRendererObj, "Listening Party", centerX, 20, 0xFFFFFF);
        
        if (!partyManager.isInParty()) {
            drawCenteredString(fontRendererObj, "Create a Party:", centerX, 48, 0xAAAAAA);
            partyNameField.drawTextBox();
            
            drawCenteredString(fontRendererObj, "Or Join by Code:", centerX, 118, 0xAAAAAA);
            joinCodeField.drawTextBox();
            
            drawCenteredString(fontRendererObj, "Available Parties:", centerX, 153, 0xAAAAAA);
            
            List<ListeningParty> parties = PartyManager.getAllParties();
            if (parties.isEmpty()) {
                drawCenteredString(fontRendererObj, "No active parties", centerX, 180, 0x888888);
            }
        } else {
            ListeningParty party = partyManager.getCurrentParty();
            drawCenteredString(fontRendererObj, "Currently in Party:", centerX, 50, 0xAAAAAA);
            drawCenteredString(fontRendererObj, party.getPartyName(), centerX, 70, 0xFFFFFF);
            drawCenteredString(fontRendererObj, "Party Code: " + party.getPartyId(), centerX, 90, 0x55FF55);
            drawCenteredString(fontRendererObj, "Members: " + party.getMemberCount() + "/" + party.getMaxMembers(), centerX, 110, 0xAAAAAA);
            
            // List members
            drawCenteredString(fontRendererObj, "Party Members:", centerX, 135, 0xAAAAAA);
            List<ListeningParty.PartyMember> members = party.getMembers();
            int startY = 155;
            for (int i = 0; i < members.size(); i++) {
                ListeningParty.PartyMember member = members.get(i);
                String memberText = member.getDisplayName();
                if (member.isHost()) {
                    memberText += " [HOST]";
                }
                drawCenteredString(fontRendererObj, memberText, centerX, startY + (i * 15), 0xFFFFFF);
            }
            
            if (partyManager.isPartyHost()) {
                drawCenteredString(fontRendererObj, "You are the host - others will sync to your playback", centerX, height - 80, 0x55FF55);
            } else {
                drawCenteredString(fontRendererObj, "Syncing with host's playback...", centerX, height - 80, 0xFFFF55);
            }
        }
        
        if (!statusMessage.isEmpty()) {
            drawString(fontRendererObj, statusMessage, 10, height - 15, 0x55FF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (partyNameField != null) {
            partyNameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        if (joinCodeField != null) {
            joinCodeField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (partyNameField != null && partyNameField.isFocused()) {
            partyNameField.textboxKeyTyped(typedChar, keyCode);
        } else if (joinCodeField != null && joinCodeField.isFocused()) {
            if (typedChar == '\r' || typedChar == '\n') {
                // Join party when Enter is pressed
                String code = joinCodeField.getText().trim().toUpperCase();
                if (!code.isEmpty()) {
                    if (partyManager.joinParty(code)) {
                        statusMessage = "Joined party: " + code;
                        initGui();
                    } else {
                        statusMessage = "Failed to join party";
                    }
                }
            } else {
                joinCodeField.textboxKeyTyped(typedChar, keyCode);
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void updateScreen() {
        if (partyNameField != null) {
            partyNameField.updateCursorCounter();
        }
        if (joinCodeField != null) {
            joinCodeField.updateCursorCounter();
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
