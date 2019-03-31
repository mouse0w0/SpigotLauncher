package spigotlauncher.plugin;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import com.github.mouse0w0.version.ComparableVersion;
import spigotlauncher.Launch;
import spigotlauncher.api.plugin.CorePlugin;

public class PluginLoader {

    private static final Pattern PLUGIN_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9]+");
    private static final Pattern PLUGINS_LIST_PATTERN = Pattern.compile("\\[([a-zA-Z0-9]+)?(([ ,]+)([a-zA-Z0-9]+))*\\]");

    private static final String COREPLUGIN_NAME_KEY = "CorePlugin-Name";
    private static final String COREPLUGIN_MAIN_KEY = "CorePlugin-Main";
    private static final String COREPLUGIN_VERSION_KEY = "CorePlugin-Version";
    private static final String COREPLUGIN_BEFORE_KEY = "CorePlugin-Before";
    private static final String COREPLUGIN_AFTER_KEY = "CorePlugin-After";

    private final Path pluginDir;

    public PluginLoader(Path pluginDir) {
        this.pluginDir = pluginDir;
    }

    public List<PluginContainer> loadAllCorePlugin() throws IOException {
        final List<PluginContainer> containers = new LinkedList<>();

        if (!Files.exists(pluginDir)) {
            Launch.getLogger().warn("Plugins directory isn't exists. It will be create at {}", pluginDir.toAbsolutePath());
            Files.createDirectory(pluginDir);
        }

        Launch.getLogger().info("Loading core plugins from {}", pluginDir.toAbsolutePath());
        Files.walkFileTree(pluginDir, Collections.<FileVisitOption>emptySet(), 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().endsWith(".jar"))
                    return FileVisitResult.CONTINUE;

                String name = file.getFileName().toString();

                try (JarFile jarFile = new JarFile(file.toString())) {
                    Manifest manifest = jarFile.getManifest();
                    if (manifest == null) {
                        return FileVisitResult.CONTINUE;
                    }

                    Attributes mainAttributes = manifest.getMainAttributes();

                    if (mainAttributes.containsKey(COREPLUGIN_MAIN_KEY)) {
                        return FileVisitResult.CONTINUE;
                    }

                    name = mainAttributes.getValue(COREPLUGIN_NAME_KEY);
                    String main = mainAttributes.getValue(COREPLUGIN_MAIN_KEY);
                    ComparableVersion version = new ComparableVersion(mainAttributes.getValue(COREPLUGIN_VERSION_KEY));
                    List<String> befores = getList(mainAttributes, COREPLUGIN_BEFORE_KEY);
                    List<String> afters = getList(mainAttributes, COREPLUGIN_AFTER_KEY);
                    if (name == null || main == null || befores == null || afters == null)
                        throw new PluginException("Illegal manifest. Cannot load core plugin.");

                    PluginContainer container = loadCorePlugin(file, name, version, main, befores, afters);
                    containers.add(container);
                } catch (Exception e) {
                    Launch.getLogger().warn(String.format("Cannot load core plugin: %s.", name), e);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        containers.sort((o1, o2) -> {
            if (o1.getBefores().contains(o2.getName()) || o2.getAfters().contains(o1.getName()))
                return -1;
            if (o1.getAfters().contains(o2.getName()) || o2.getBefores().contains(o1.getName()))
                return 1;
            return 0;
        });

        return containers;
    }


    private List<String> getList(Attributes attributes, String key) {
        if (!attributes.containsKey(key))
            return Collections.emptyList();

        String value = attributes.getValue(key);
        if (PLUGIN_NAME_PATTERN.matcher(value).matches()) {
            return Collections.singletonList(value);
        } else if (PLUGINS_LIST_PATTERN.matcher(value).matches()) {
            return Arrays.asList(value.split("[ ,]+"));
        } else {
            return null;
        }
    }

    public PluginContainer loadCorePlugin(Path source, String name, ComparableVersion version, String main, List<String> befores, List<String> afters) {
        PluginContainer container = new PluginContainer(source, name, version, befores, afters);
        try {
            Class<?> mainClass = container.getClassLoader().loadClass(main);
            if (!CorePlugin.class.isAssignableFrom(mainClass))
                throw new PluginException("Couldn't load plugin " + container.getName() + ".");

            CorePlugin instance = (CorePlugin) mainClass.newInstance();
            container.setInstance(instance);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new PluginException("Couldn't load plugin " + container.getName() + ".", e);
        }
        return container;
    }

}
