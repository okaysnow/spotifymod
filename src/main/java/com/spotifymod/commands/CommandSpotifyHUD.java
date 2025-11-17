package com.spotifymod.commands;

import com.spotifymod.SpotifyMod;
import com.spotifymod.gui.SpotifyGuiHandler;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandSpotifyHUD extends CommandBase {

    @Override
    public String getCommandName() {
        return "spotifyhud";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/spotifyhud <drag|toggle|reset>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // No special permissions required
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("\u00a7eUsage: /spotifyhud <drag|toggle|reset>"));
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "drag":
                boolean newDragMode = !SpotifyGuiHandler.isDragMode();
                SpotifyGuiHandler.setDragMode(newDragMode);
                if (newDragMode) {
                    sender.addChatMessage(new ChatComponentText("\u00a7aHUD drag mode enabled! Click and drag the HUD to reposition it."));
                } else {
                    sender.addChatMessage(new ChatComponentText("\u00a7cHUD drag mode disabled. Position saved."));
                }
                break;

            case "toggle":
                boolean hudEnabled = SpotifyMod.instance.getConfig().isHudEnabled();
                SpotifyMod.instance.getConfig().setHudEnabled(!hudEnabled);
                SpotifyMod.instance.getConfig().save();
                if (!hudEnabled) {
                    sender.addChatMessage(new ChatComponentText("\u00a7aSpotify HUD enabled."));
                } else {
                    sender.addChatMessage(new ChatComponentText("\u00a7cSpotify HUD disabled."));
                }
                break;

            case "reset":
                SpotifyMod.instance.getConfig().setHudX(5);
                SpotifyMod.instance.getConfig().setHudY(5);
                SpotifyMod.instance.getConfig().save();
                sender.addChatMessage(new ChatComponentText("\u00a7aHUD position reset to default (5, 5)."));
                break;

            default:
                sender.addChatMessage(new ChatComponentText("\u00a7cUnknown subcommand. Usage: /spotifyhud <drag|toggle|reset>"));
                break;
        }
    }
}
