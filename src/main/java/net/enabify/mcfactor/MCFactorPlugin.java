package net.enabify.mcfactor;

import net.enabify.mcfactor.auth.AuthenticationManager;
import net.enabify.mcfactor.config.MCFactorConfig;
import net.enabify.mcfactor.language.LanguageManager;
import net.enabify.mcfactor.storage.LinkStorage;

import java.io.File;

public interface MCFactorPlugin {
    MCFactorConfig getConfigManager();
    // Platform implementations expose their config managers through MCFactorConfig so callers can
    // interact with shared configuration without depending on platform-specific types.
    LanguageManager getLanguageManager();
    LinkStorage getLinkStorage();
    AuthenticationManager getAuthManager();
    File getDataFolder();
    void log(String message);
    void logDebug(String message);
    void logError(String message, Exception e);
}
