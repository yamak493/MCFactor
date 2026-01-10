package net.enabify.mcfactor.bungee.command;

import net.enabify.mcfactor.bungee.MCFactorBungee;
import net.enabify.mcfactor.command.TwoFactorCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

public class BungeeTwoFactorCommand extends Command {
    private final BungeeTwoFactorHandler handler;
    
    public BungeeTwoFactorCommand(MCFactorBungee plugin) {
        super("2fa", null, "2f");
        this.handler = new BungeeTwoFactorHandler(plugin);
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("This command can only be used by players."));
            return;
        }
        
        ProxiedPlayer player = (ProxiedPlayer) sender;
        handler.execute(player, args);
    }
}

class BungeeTwoFactorHandler extends TwoFactorCommand {
    private final MCFactorBungee plugin;
    
    public BungeeTwoFactorHandler(MCFactorBungee plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    public void execute(ProxiedPlayer player, String[] args) {
        UUID playerUuid = player.getUniqueId();
        
        if (args.length == 0) {
            // /2fa - Unlink
            handleUnlink(playerUuid);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                handleHelp(playerUuid, player.hasPermission("mcfactor.admin"));
                break;
                
            case "disconnect":
            case "d":
                if (args.length < 2) {
                    player.sendMessage(new TextComponent(
                        plugin.getLanguageManager().getMessage("command.disconnect.usage")
                    ));
                    return;
                }
                handleDisconnect(playerUuid, args[1], player.hasPermission("mcfactor.admin"));
                break;
                
            default:
                // /2fa <appkey> - Register
                if (args[0].length() == 6) {
                    handleRegister(playerUuid, player.getName(), args[0]);
                } else {
                    handleHelp(playerUuid, player.hasPermission("mcfactor.admin"));
                }
                break;
        }
    }
    
    @Override
    protected void sendMessage(UUID playerUuid, String message) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(playerUuid);
        if (player != null && player.isConnected()) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }
}
