package spigotlauncher.api;

import org.slf4j.Logger;

public class Platform {

    private static PlatformProvider platformProvider;

    public static Logger getLogger() {
        return platformProvider.getLogger();
    }

    public static boolean isDebug() {
        return platformProvider.isDebug();
    }

    public static boolean isStaticMode() {
        return platformProvider.isStaticMode();
    }

    public static void setPlatformProvider(PlatformProvider platformProvider) {
        if(Platform.platformProvider != null)
            throw new UnsupportedOperationException("Platform has been initialized.");
        Platform.platformProvider = platformProvider;
    }
}
