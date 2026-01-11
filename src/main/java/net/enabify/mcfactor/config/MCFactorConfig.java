package net.enabify.mcfactor.config;

public interface MCFactorConfig {
    boolean isEnabled();

    String getLanguage();

    boolean isCodeAuthentication();

    boolean isAppAuthentication();

    String getRestApiHost();

    int getRestApiPort();

    String getServerId();

    void setServerId(String serverId);

    String getSecretKey();

    void setSecretKey(String secretKey);

    boolean isProxyMode();

    void saveConfig();

    void reload();

    boolean isDebug();
}
