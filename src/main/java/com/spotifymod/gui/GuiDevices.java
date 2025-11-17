package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiDevices extends GuiScreen {
    private final GuiScreen parentScreen;
    private final SpotifyAPI api;
    private List<SpotifyAPI.Device> devices = new ArrayList<>();
    private String statusMessage = "";
    private boolean loading = true;

    public GuiDevices(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
        this.api = SpotifyMod.instance.getSpotifyAPI();
    }

    @Override
    public void initGui() {
        buttonList.clear();
        
        // Back button
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 30, 200, 20, "Back"));
        
        // Refresh button
        buttonList.add(new GuiButton(1, width / 2 - 100, 40, 200, 20, "Refresh Devices"));
        
        if (!loading && !devices.isEmpty()) {
            int startY = 80;
            for (int i = 0; i < devices.size(); i++) {
                SpotifyAPI.Device device = devices.get(i);
                String label = device.name + " (" + device.type + ")";
                if (device.isActive) {
                    label += " [ACTIVE]";
                }
                buttonList.add(new GuiButton(100 + i, width / 2 - 150, startY + (i * 25), 300, 20, label));
            }
        }
        
        loadDevices();
    }

    private void loadDevices() {
        loading = true;
        statusMessage = "Loading devices...";
        api.getAvailableDevices().thenAccept(deviceList -> {
            devices = deviceList;
            loading = false;
            if (devices.isEmpty()) {
                statusMessage = "No devices found. Make sure Spotify is open on a device!";
            } else {
                statusMessage = "Found " + devices.size() + " device(s). Click to activate.";
            }
            mc.addScheduledTask(this::initGui);
        });
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // Back
                mc.displayGuiScreen(parentScreen);
                break;
            case 1: // Refresh
                loadDevices();
                break;
            default:
                if (button.id >= 100) {
                    int deviceIndex = button.id - 100;
                    if (deviceIndex < devices.size()) {
                        SpotifyAPI.Device device = devices.get(deviceIndex);
                        statusMessage = "Transferring playback to " + device.name + "...";
                        api.transferPlayback(device.id).thenAccept(success -> {
                            if (success) {
                                statusMessage = "Playback transferred to " + device.name;
                                mc.addScheduledTask(() -> {
                                    try {
                                        Thread.sleep(1000);
                                        loadDevices();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                statusMessage = "Failed to transfer playback";
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
        
        drawCenteredString(fontRendererObj, "Spotify Devices", centerX, 15, 0xFFFFFF);
        
        if (loading) {
            drawCenteredString(fontRendererObj, "Loading...", centerX, height / 2, 0xAAAAAA);
        } else if (devices.isEmpty()) {
            drawCenteredString(fontRendererObj, "No devices found", centerX, height / 2 - 20, 0xFF5555);
            drawCenteredString(fontRendererObj, "Open Spotify on your phone,", centerX, height / 2, 0xAAAAAA);
            drawCenteredString(fontRendererObj, "computer, or another device", centerX, height / 2 + 12, 0xAAAAAA);
            drawCenteredString(fontRendererObj, "Then click 'Refresh Devices'", centerX, height / 2 + 24, 0xAAAAAA);
        }
        
        if (!statusMessage.isEmpty()) {
            drawString(fontRendererObj, statusMessage, 10, height - 15, 0x55FF55);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
