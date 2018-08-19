package spigotlauncher;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import spigotlauncher.plugin.PluginContainer;
import spigotlauncher.plugin.PluginLoader;

public final class Launch {

    private static final Logger LOGGER = LoggerFactory.getLogger("Launch");

    private static Path serverDir = Paths.get("");
    private static LaunchClassLoader classLoader;
    private static List<PluginContainer> plugins;

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();
        OptionSpec<String> server = parser.accepts("serverFile").withRequiredArg().defaultsTo("server.jar");
        OptionSet options = parser.parse(args);

        File serverFile = new File(options.valueOf(server));
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

        getLogger().info("Finded server file: {} Initializing...", serverFile.getAbsolutePath());

        try {
            classLoader = new LaunchClassLoader(serverFile);
            Thread.currentThread().setContextClassLoader(classLoader);

            Launch.getLogger().info("Loading core plugin...");
            PluginLoader pluginLoader = new PluginLoader(Paths.get("plugins"));
            plugins = pluginLoader.loadAllCorePlugin();
            printAllPlugin();

            acceptOptions(Arrays.asList(args));
            initTransformers();

            Launch.getLogger().info("Launching server...");
            Class<?> craftBukkitMain = Class.forName("org.bukkit.craftbukkit.Main", false, classLoader);
            Method main = craftBukkitMain.getMethod("main", String[].class);
            main.invoke(null, new Object[]{args});
        } catch (Exception e) {
            getLogger().error("Initialize server failed.", e);
        }
    }

    private static void acceptOptions(List<String> args) {
        for (PluginContainer container : plugins) {
            container.getInstance().acceptOptions(args, serverDir);
        }
    }

    private static void initTransformers() {
        for (PluginContainer container : plugins) {
            classLoader.addTransformer(container.getInstance().getTransformers());
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
}
