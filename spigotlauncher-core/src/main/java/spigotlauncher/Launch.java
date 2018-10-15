package spigotlauncher;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import spigotlauncher.api.Platform;
import spigotlauncher.api.PlatformProvider;
import spigotlauncher.plugin.PluginContainer;
import spigotlauncher.plugin.PluginLoader;

public final class Launch {

    private static final Logger LOGGER = LoggerFactory.getLogger("Launch");

    private static Path serverDir = Paths.get("");
    private static LaunchClassLoader classLoader;
    private static List<PluginContainer> plugins;

    private static boolean debug;
    private static boolean staticMode;
    private static File staticOutputFile;

    public static void main(String[] args) {
        Platform.setPlatformProvider(new PlatformProviderImpl());

        OptionParser parser = new OptionParser();
        OptionSpec<String> server = parser.accepts("serverFile").withRequiredArg().defaultsTo("server.jar");
        OptionSpec<Boolean> debug = parser.accepts("debug").withRequiredArg().ofType(Boolean.class).defaultsTo(false);
        OptionSpec<Boolean> staticMode = parser.accepts("staticMode").withOptionalArg().ofType(Boolean.class).defaultsTo(false);
        OptionSpec<String> staticOutputFile = parser.accepts("staticOutputFile").withOptionalArg().defaultsTo("server_transformed.jar");
        OptionSet options = parser.parse(args);

        Launch.debug = debug.value(options);
        Launch.staticMode = staticMode.value(options);

        if (Launch.debug)
            getLogger().warn("Debug mode has been enabled.");

        if (Launch.staticMode) {
            Launch.staticOutputFile = new File(staticOutputFile.value(options));
            getLogger().warn("Static mode has been enabled. Static output file is {} .", Launch.staticOutputFile.getAbsolutePath());
        }

        File serverFile = new File(server.value(options));
        if (!serverFile.exists()) {
            getLogger().error("Server file {} isn't exists. ", serverFile.getAbsolutePath());
            return;
        }
        if (!serverFile.isFile()) {
            getLogger().error("Server file {} isn't file. ", serverFile.getAbsolutePath());
            return;
        }
        if (!serverFile.getName().endsWith(".jar")) {
            getLogger().error("Server file {} isn't jar. ", serverFile.getAbsolutePath());
            return;
        }

        getLogger().info("Finded server file: {}", serverFile.getAbsolutePath());

        try {
            getLogger().info("Initilizing...");

            getLogger().info("Loading core plugins...");
            PluginLoader pluginLoader = new PluginLoader(Paths.get("plugins"));
            plugins = pluginLoader.loadAllCorePlugin();
            printAllPlugin();

            acceptOptions(Arrays.asList(args));
        } catch (Exception e) {
            getLogger().error("Failed to initialize.", e);
        }

        if (Launch.staticMode) {
            StaticTransformExecutor executor = new StaticTransformExecutor(serverFile, Launch.staticOutputFile);
            executor.addPluginTransformers(plugins);
            executor.start();
        } else {
            launchServer(serverFile, args);
        }
    }

    private static void launchServer(File serverFile, String[] args) {
        try {
            Launch.getLogger().info("Launching server...");
            classLoader = new LaunchClassLoader(serverFile);
            classLoader.getTransformExecutor().addPluginTransformers(plugins);
            Thread.currentThread().setContextClassLoader(classLoader);

            ClassLoader systemClassLoader = Launch.class.getClassLoader();
            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke(systemClassLoader, serverFile.toURI().toURL());

            Class<?> craftBukkitMain = Class.forName("org.bukkit.craftbukkit.Main", false, classLoader);
            Method main = craftBukkitMain.getMethod("main", String[].class);
            main.invoke(null, new Object[]{args});
        } catch (Exception e) {
            getLogger().error("Failed to launch server.", e);
        }
    }

    private static void acceptOptions(List<String> args) {
        for (PluginContainer container : plugins) {
            container.getInstance().acceptOptions(args, serverDir);
        }
    }

    private static void printAllPlugin() {
        StringBuilder sb = new StringBuilder("Loaded plugins: { ");
        for (PluginContainer container : plugins) {
            sb.append(container.getName()).append(" ");
        }
        getLogger().info(sb.append("}").toString());
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    private static final class PlatformProviderImpl implements PlatformProvider {

        @Override
        public boolean isDebug() {
            return Launch.debug;
        }

        @Override
        public boolean isStaticMode() {
            return Launch.staticMode;
        }

        @Override
        public Logger getLogger() {
            return Launch.getLogger();
        }
    }
}
