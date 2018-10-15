package spigotlauncher.api;

import org.slf4j.Logger;

public interface PlatformProvider {

    boolean isDebug();

    boolean isStaticMode();

    Logger getLogger();
}
