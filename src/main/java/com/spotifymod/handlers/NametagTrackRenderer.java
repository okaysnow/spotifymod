package com.spotifymod.handlers;

import com.spotifymod.SpotifyMod;
import com.spotifymod.network.ModNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Renders currently playing track above player nametags for mod users.
 */
public class NametagTrackRenderer {
    
    @SubscribeEvent
    public void onRenderPlayerNameTag(RenderLivingEvent.Specials.Pre event) {
        if (!(event.entity instanceof EntityPlayer)) return;
        
        EntityPlayer player = (EntityPlayer) event.entity;
        
        // Don't render for ourselves (already have HUD)
        if (player == Minecraft.getMinecraft().thePlayer) return;
        
        ModNetworkHandler networkHandler = SpotifyMod.instance.getNetworkHandler();
        if (networkHandler == null) return;
        
        ModNetworkHandler.PlayerTrackInfo trackInfo = networkHandler.getPlayerTrack(player.getUniqueID());
        if (trackInfo == null) return;
        
        // Render track info above nametag
        renderTrackAboveHead(player, trackInfo, event.x, event.y, event.z);
    }
    
    private void renderTrackAboveHead(EntityPlayer player, ModNetworkHandler.PlayerTrackInfo trackInfo, double x, double y, double z) {
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        String display = trackInfo.getCompactDisplay();
        
        // Calculate position (above nametag)
        float yOffset = player.height + 0.5F;
        
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y + yOffset, (float) z);
        
        // Face the camera
        GlStateManager.rotate(-Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        
        // Compact scale - smaller than before
        float scale = 0.016666668F * 0.7F; 
        GlStateManager.scale(-scale, -scale, scale);
        
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        
        int stringWidth = fontRenderer.getStringWidth(display);
        int xPos = -stringWidth / 2;
        
        // Draw compact semi-transparent background
        drawRect(xPos - 1, -9, xPos + stringWidth + 1, 0, 0x50000000);
        
        // Animated color for playing tracks
        int color;
        if (trackInfo.isPlaying) {
            // Subtle pulse animation for active tracks
            long time = System.currentTimeMillis();
            float pulse = (float) Math.sin(time * 0.003) * 0.3f + 0.7f;
            int green = (int) (0x55 + (0xAA - 0x55) * pulse);
            color = 0xFF000000 | (green << 8);
        } else {
            color = 0xFFAA00; // Orange for paused
        }
        
        fontRenderer.drawString(display, xPos, -8, color);
        
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
    
    private void drawRect(int left, int top, int right, int bottom, int color) {
        net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }
}
