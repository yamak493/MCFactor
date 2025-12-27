package net.enabify.mcfactor;

import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.config.ConfigManager;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;

import java.io.File;

public interface MCFactorPlugin {
    ConfigManager getConfigManager();
    LanguageManager getLanguageManager();
    LinkStorage getLinkStorage();
    AuthenticationManager getAuthManager();
    File getDataFolder();
    void log(String message);
    void logError(String message, Exception e);
}
