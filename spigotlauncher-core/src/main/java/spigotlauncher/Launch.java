package spigotlauncher;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
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
        OptionSpec<String> server = parser.accepts("server-file").withRequiredArg().defaultsTo("server.jar");
        OptionSpec<Void> debug = parser.accepts("debug");
        OptionSpec<Void> staticMode = parser.accepts("static-mode");
        OptionSpec<String> staticOutput = parser.accepts("static-output").withRequiredArg().defaultsTo("server_transformed.jar");
        OptionSpec<String> bukkitArgs = parser.accepts("bukkit-args").withOptionalArg();
        OptionSet options = parser.parse(args);

        Launch.debug = options.has(debug);
        Launch.staticMode = options.has(staticMode);

        if (Launch.debug)
            getLogger().warn("Debug mode has been enabled.");

        if (Launch.staticMode) {
            Launch.staticOutputFile = new File(staticOutput.value(options));
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

        getLogger().info("Found server file: {}", serverFile.getAbsolutePath());

        try {
            getLogger().info("Initializing...");

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
            launchServer(serverFile, collectLaunchArguments(bukkitArgs.value(options)));
        }
    }

    private static void launchServer(File serverFile, String[] args) {
        try {
            getLogger().info("Launch arguments is {}", Arrays.asList(args));
            getLogger().info("Launching server...");
            classLoader = new LaunchClassLoader(serverFile);
            classLoader.getTransformExecutor().addPluginTransformers(plugins);
            Thread.currentThread().setContextClassLoader(classLoader);

//            ClassLoader systemClassLoader = Launch.class.getClassLoader();
//            Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
//            addUrl.setAccessible(true);
//            addUrl.invoke(systemClassLoader, serverFile.toURI().toURL());

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

    private static String[] collectLaunchArguments(String bukkitArgsFile) {
        List<String> args = new LinkedList<>();

        if (bukkitArgsFile != null) {
            Path bukkitArgsPath = Paths.get(bukkitArgsFile);
            getLogger().info("Try to load bukkit arguments from {}", bukkitArgsPath.toAbsolutePath());
            if (Files.exists(bukkitArgsPath)) {
                try {
                    List<String> lines = Files.readAllLines(bukkitArgsPath);
                    if (lines.isEmpty()) {
                        getLogger().warn("Bukkit arguments file is empty.");
                    } else {
                        Collections.addAll(args, lines.get(0).split(" "));
                        getLogger().info("Loaded bukkit arguments.");
                    }
                } catch (IOException e) {
                    getLogger().error(e.getMessage(), e);
                }
            }
        }

        for (PluginContainer container : plugins) {
            String[] pluginArgs = container.getInstance().getLaunchArguments();
            if (pluginArgs != null)
                Collections.addAll(args, pluginArgs);
        }
        return args.toArray(new String[0]);
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
