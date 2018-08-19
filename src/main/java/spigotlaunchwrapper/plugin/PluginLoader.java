package spigotlaunchwrapper.plugin;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import spigotlaunchwrapper.Launch;
import spigotlaunchwrapper.api.CorePlugin;

public class PluginLoader {

    private static final String COREPLUGIN_NAME_KEY = "CorePlugin-Name";
    private static final String COREPLUGIN_MAIN_KEY = "CorePlugin-Main";

    private final Path pluginDir;

    public PluginLoader(Path pluginDir) {
        this.pluginDir = pluginDir;
    }

    public List<PluginContainer> loadAllCorePlugin() throws IOException {
        final List<PluginContainer> containers = new ArrayList<>();
        Files.walkFileTree(pluginDir, Collections.<FileVisitOption>emptySet(), 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.getFileName().toString().endsWith(".jar"))
                    return FileVisitResult.CONTINUE;

                try (JarFile jarFile = new JarFile(file.toString())) {
                    Manifest manifest = jarFile.getManifest();

                    String name = manifest.getMainAttributes().getValue(COREPLUGIN_NAME_KEY);
                    String main = manifest.getMainAttributes().getValue(COREPLUGIN_MAIN_KEY);
                    if (name == null || main == null)
                        return FileVisitResult.CONTINUE;

                    try {
                        PluginContainer container = loadCorePlugin(file, name, main);
                        containers.add(container);
                        Launch.getLogger().info("Loaded core plugin: {}", name);
                    } catch (PluginException e) {
                        Launch.getLogger().warn(e.getMessage(), e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        });
        return containers;
    }

    public PluginContainer loadCorePlugin(Path source, String name, String main) {
        PluginContainer container = new PluginContainer(source, name);
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
