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

    /**
     * @return Minecraft version. For example "1.12.2".
     */
    public static String getMinecraftVersion() {
        return platformProvider.getMinecraftVersion();
    }

    /**
     * @return CraftBukkit version. For example "v1_12_R1".
     */
    public static String getCraftBukkitVersion() {
        return platformProvider.getCraftBukkitVersion();
    }

    public static void setPlatformProvider(PlatformProvider platformProvider) {
        if (Platform.platformProvider != null)
            throw new UnsupportedOperationException("Platform has been initialized.");
        Platform.platformProvider = platformProvider;
    }
}
