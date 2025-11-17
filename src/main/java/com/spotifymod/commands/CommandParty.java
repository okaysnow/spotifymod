package com.spotifymod.commands;

import com.spotifymod.SpotifyMod;
import com.spotifymod.party.ListeningParty;
import com.spotifymod.debug.LogBuffer;
import com.spotifymod.party.PartyManager;
import com.spotifymod.user.UserProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class CommandParty extends CommandBase {

    @Override
    public String getCommandName() {
        return "spotify";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/spotify <party|profile|debug|keys> [args]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("\u00a77Usage: \u00a7f/spotify \u00a77<party|profile|debug|keys>"));
            return;
        }

        String mainCommand = args[0].toLowerCase();

        if (mainCommand.equals("party")) {
            handlePartyCommand(sender, args);
        } else if (mainCommand.equals("profile")) {
            handleProfileCommand(sender, args);
        } else if (mainCommand.equals("debug")) {
            handleDebugCommand(sender, args);
        } else if (mainCommand.equals("keys") || mainCommand.equals("keybinds")) {
            handleKeysCommand(sender, args);
        } else {
            sender.addChatMessage(new ChatComponentText("\u00a7cUnknown command. \u00a77Usage: \u00a7f/spotify \u00a77<party|profile|debug|keys>"));
        }
    }

    private void handleDebugCommand(ICommandSender sender, String[] args) {
        if (args.length == 1 || args[1].equalsIgnoreCase("open")) {
            net.minecraft.client.Minecraft.getMinecraft().displayGuiScreen(new com.spotifymod.gui.GuiSpotifyDebug());
            sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a77Opened debug window"));
            return;
        }
        if (args[1].equalsIgnoreCase("clear")) {
            LogBuffer.get().clear();
            sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a77Log buffer cleared"));
            return;
        }
        sender.addChatMessage(new ChatComponentText("\u00a77Usage: \u00a7f/spotify debug \u00a77[open|clear]"));
    }

    private void handleKeysCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentText("\u00a7f\u00a7lSpotify Keybindings:"));
        sender.addChatMessage(new ChatComponentText("\u00a77Configure these in \u00a7fESC > Options > Controls"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a7fOpen Spotify GUI \u00a78- \u00a77Default: \u00a7eP"));
        sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a7fPlay/Pause \u00a78- \u00a77Default: \u00a7eUnbound"));
        sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a7fNext Track \u00a78- \u00a77Default: \u00a7eUnbound"));
        sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a7fPrevious Track \u00a78- \u00a77Default: \u00a7eUnbound"));
        sender.addChatMessage(new ChatComponentText(""));
        sender.addChatMessage(new ChatComponentText("\u00a78Tip: Bind to media keys or custom shortcuts!"));
    }

    private void handlePartyCommand(ICommandSender sender, String[] args) {
        PartyManager partyManager = SpotifyMod.instance.getPartyManager();

        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText("\u00a77Usage: \u00a7f/spotify party \u00a77<create|join|leave|info>"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "create":
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText("\u00a77Usage: \u00a7f/spotify party create <name>"));
                    return;
                }
                String partyName = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                ListeningParty party = partyManager.createParty(partyName);
                if (party != null) {
                    sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a77Party created: \u00a7f" + party.getPartyName()));
                    sender.addChatMessage(new ChatComponentText("\u00a77Party Code: \u00a7e" + party.getPartyId()));
                    sender.addChatMessage(new ChatComponentText("\u00a78Share this code with friends to join!"));
                    LogBuffer.get().info("Party created name=" + party.getPartyName() + " id=" + party.getPartyId());
                } else {
                    sender.addChatMessage(new ChatComponentText("\u00a7c\u2717 \u00a77Failed to create party. Check your active profile."));
                    LogBuffer.get().warn("Party creation failed - no active profile");
                }
                break;

            case "join":
                if (args.length < 3) {
                    sender.addChatMessage(new ChatComponentText("\u00a77Usage: \u00a7f/spotify party join <code>"));
                    return;
                }
                String partyCode = args[2].toUpperCase();
                if (partyManager.joinParty(partyCode)) {
                    ListeningParty joinedParty = partyManager.getCurrentParty();
                    sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a77Joined party: \u00a7f" + joinedParty.getPartyName()));
                    sender.addChatMessage(new ChatComponentText("\u00a78Your playback will sync with the host."));
                    LogBuffer.get().info("Joined party id=" + partyCode);
                } else {
                    sender.addChatMessage(new ChatComponentText("\u00a7c\u2717 \u00a77Failed to join party. Check the code."));
                    LogBuffer.get().warn("Failed to join party id=" + partyCode);
                }
                break;

            case "leave":
                if (!partyManager.isInParty()) {
                    sender.addChatMessage(new ChatComponentText("\u00a77You are not in a party."));
                    return;
                }
                String leftPartyName = partyManager.getCurrentParty().getPartyName();
                partyManager.leaveCurrentParty();
                sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a77Left party: \u00a7f" + leftPartyName));
                LogBuffer.get().info("Left party name=" + leftPartyName);
                break;

            case "info":
                if (!partyManager.isInParty()) {
                    sender.addChatMessage(new ChatComponentText("\u00a77You are not in a party."));
                    return;
                }
                ListeningParty currentParty = partyManager.getCurrentParty();
                sender.addChatMessage(new ChatComponentText("\u00a78\u00a7m                \u00a7r \u00a77Party Info \u00a78\u00a7m                "));
                sender.addChatMessage(new ChatComponentText("\u00a78Name: \u00a7f" + currentParty.getPartyName()));
                sender.addChatMessage(new ChatComponentText("\u00a78Code: \u00a7e" + currentParty.getPartyId()));
                sender.addChatMessage(new ChatComponentText("\u00a78Members: \u00a7f" + currentParty.getMemberCount() + "\u00a78/\u00a7f" + currentParty.getMaxMembers()));
                
                if (partyManager.isPartyHost()) {
                    sender.addChatMessage(new ChatComponentText("\u00a7a\u2713 \u00a77You are the host"));
                } else {
                    sender.addChatMessage(new ChatComponentText("\u00a78Syncing with host..."));
                }
                
                sender.addChatMessage(new ChatComponentText("\u00a78\u00a7m                \u00a7r \u00a77Members \u00a78\u00a7m                "));
                for (ListeningParty.PartyMember member : currentParty.getMembers()) {
                    String memberText = "\u00a78\u2022 \u00a7f" + member.getDisplayName();
                    if (member.isHost()) {
                        memberText += " \u00a7e[HOST]";
                    }
                    sender.addChatMessage(new ChatComponentText(memberText));
                }
                break;

            default:
                sender.addChatMessage(new ChatComponentText("\u00a77Unknown command. Usage: \u00a7f/spotify party \u00a77<create|join|leave|info>"));
                break;
        }
    }

    private void handleProfileCommand(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.addChatMessage(new ChatComponentText("\u00a77Usage: \u00a7f/spotify profile \u00a77<list|info>"));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "list":
                sender.addChatMessage(new ChatComponentText("\u00a78\u00a7m                \u00a7r \u00a77Profiles \u00a78\u00a7m                "));
                java.util.List<UserProfile> profiles = SpotifyMod.instance.getProfileManager().getAllProfiles();
                if (profiles.isEmpty()) {
                    sender.addChatMessage(new ChatComponentText("\u00a78No profiles found. Press \u00a7fP \u00a78> \u00a7fProfiles \u00a78to create one."));
                } else {
                    for (UserProfile profile : profiles) {
                        String text = "\u00a78\u2022 \u00a7f" + profile.getProfileName();
                        if (profile.isActive()) {
                            text += " \u00a7a[ACTIVE]";
                        }
                        if (!profile.hasValidTokens()) {
                            text += " \u00a7c[NOT AUTHENTICATED]";
                        }
                        sender.addChatMessage(new ChatComponentText(text));
                    }
                }
                break;

            case "info":
                UserProfile activeProfile = SpotifyMod.instance.getProfileManager().getActiveProfile();
                if (activeProfile == null) {
                    sender.addChatMessage(new ChatComponentText("\u00a77No active profile. Press \u00a7fP \u00a77> \u00a7fProfiles \u00a77to create one."));
                } else {
                    sender.addChatMessage(new ChatComponentText("\u00a78\u00a7m                \u00a7r \u00a77Active Profile \u00a78\u00a7m                "));
                    sender.addChatMessage(new ChatComponentText("\u00a78Name: \u00a7f" + activeProfile.getProfileName()));
                    sender.addChatMessage(new ChatComponentText("\u00a78Authenticated: " + 
                        (activeProfile.hasValidTokens() ? "\u00a7aYes" : "\u00a7cNo")));
                }
                break;

            default:
                sender.addChatMessage(new ChatComponentText("\u00a77Unknown command. Usage: \u00a7f/spotify profile \u00a77<list|info>"));
                break;
        }
    }
}
