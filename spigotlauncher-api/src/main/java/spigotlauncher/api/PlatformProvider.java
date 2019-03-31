package spigotlauncher.api;

import org.slf4j.Logger;

public interface PlatformProvider {

    boolean isDebug();

    boolean isStaticMode();

    Logger getLogger();

    /**
     * @return Minecraft version. For example "1.12.2".
     */
    String getMinecraftVersion();

    /**
     * @return CraftBukkit version. For example "v1_12_R1".
     */
    String getCraftBukkitVersion();
}
