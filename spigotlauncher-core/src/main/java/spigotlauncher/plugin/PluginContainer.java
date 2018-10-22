package spigotlauncher.plugin;

import java.nio.file.Path;
import java.util.List;

import com.github.mouse0w0.version.ComparableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spigotlauncher.api.plugin.CorePlugin;

public class PluginContainer {

    private final Path source;
    private final String name;
    private final ComparableVersion version;
    private final Logger logger;
    private final PluginClassLoader classLoader;
    private final List<String> befores;
    private final List<String> afters;

    private CorePlugin instance;

    public PluginContainer(Path source, String name, ComparableVersion version, List<String> befores, List<String> afters) {
        this.source = source;
        this.name = name;
        this.version = version;
        this.befores = befores;
        this.afters = afters;
        this.logger = LoggerFactory.getLogger(name);
        this.classLoader = new PluginClassLoader(this, source);
    }

    public Path getSource() {
        return source;
    }

    public String getName() {
        return name;
    }

    public ComparableVersion getVersion() {
        return version;
    }

    public PluginClassLoader getClassLoader() {
        return classLoader;
    }

    public List<String> getBefores() {
        return befores;
    }

    public List<String> getAfters() {
        return afters;
    }

    public CorePlugin getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }

    void setInstance(CorePlugin instance) {
        if (this.instance != null)
            throw new UnsupportedOperationException("Instance has been set.");
        this.instance = instance;
    }
}
