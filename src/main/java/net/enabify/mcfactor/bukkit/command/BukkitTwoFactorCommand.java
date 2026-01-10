package net.enabify.mcfactor.bukkit.command;

import net.enabify.mcfactor.bukkit.MCFactorBukkit;
import net.enabify.mcfactor.command.TwoFactorCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitTwoFactorCommand extends TwoFactorCommand implements CommandExecutor {
    private final MCFactorBukkit plugin;
    
    public BukkitTwoFactorCommand(MCFactorBukkit plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("command.only-players"));
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();
        
        if (args.length == 0) {
            // /2fa - Unlink
            handleUnlink(playerUuid);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                handleHelp(playerUuid, player.isOp());
                break;
                
            case "disconnect":
            case "d":
                if (args.length < 2) {
                    player.sendMessage(languageManager.getMessage("command.disconnect.usage"));
                    return true;
                }
                handleDisconnect(playerUuid, args[1], player.isOp());
                break;
                
            default:
                // /2fa <appkey> - Register
                if (args[0].length() == 6) {
                    handleRegister(playerUuid, player.getName(), args[0]);
                } else {
                    handleHelp(playerUuid, player.isOp());
                }
                break;
        }
        
        return true;
    }
    
    @Override
    protected void sendMessage(UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }
}
