package spigotlauncher.api;

public class Platform {

    private static PlatformProvider platformProvider;

    public static final void setPlatformProvider(PlatformProvider platformProvider) {
        if(Platform.platformProvider != null)
            throw new UnsupportedOperationException("Platform has been initialized.");
        Platform.platformProvider = platformProvider;
    }
}
