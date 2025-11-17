package com.spotifymod.gui;

import com.spotifymod.debug.LogBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.io.IOException;

public class GuiSpotifyDebug extends GuiScreen {
    private GuiButton clearBtn;
    private GuiButton closeBtn;

    @Override
    public void initGui() {
        int centerX = this.width / 2;
        int y = 20;
        this.buttonList.clear();
        this.clearBtn = new GuiButton(1, centerX - 100, y, 80, 20, "Clear");
        this.closeBtn = new GuiButton(2, centerX + 20, y, 80, 20, "Close");
        this.buttonList.add(clearBtn);
        this.buttonList.add(closeBtn);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            LogBuffer.get().clear();
        } else if (button.id == 2) {
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(this.fontRendererObj, "Spotify Debug Log", this.width / 2, 6, 0xFFFFFF);

        int x = 8;
        int y = 50;
        int maxY = this.height - 8;
        int lineHeight = this.fontRendererObj.FONT_HEIGHT + 2;
        LogBuffer.LogEntry[] entries = LogBuffer.get().snapshot();

        // Draw newest at bottom
        for (int i = Math.max(0, entries.length - ((maxY - y) / lineHeight)); i < entries.length; i++) {
            String line = entries[i].formatted(new java.text.SimpleDateFormat("HH:mm:ss.SSS"));
            this.fontRendererObj.drawString(line, x, y, colorFor(entries[i]));
            y += lineHeight;
            if (y >= maxY) break;
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int colorFor(LogBuffer.LogEntry e) {
        switch (e.level) {
            case TRACE: return 0xFFAAAAAA;
            case INFO: return 0xFFFFFFFF;
            case WARN: return 0xFFFFFF55;
            case ERROR: return 0xFFFF5555;
            default: return 0xFFFFFFFF;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
