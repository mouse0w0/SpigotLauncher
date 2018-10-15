package spigotlauncher.plugin;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spigotlauncher.api.plugin.CorePlugin;

public class PluginContainer {
	
	private final Path source;
	private final String name;
	private final Logger logger;
	private final PluginClassLoader classLoader;
	
	private CorePlugin instance;
	
	public PluginContainer(Path source, String name) {
		this.source = source;
		this.name = name;
		this.logger = LoggerFactory.getLogger(name);
		this.classLoader = new PluginClassLoader(this, source);
	}
	
	public Path getSource() {
		return source;
	}
	
	public String getName() {
		return name;
	}

	public PluginClassLoader getClassLoader() {
		return classLoader;
	}

	public CorePlugin getInstance() {
		return instance;
	}

	public Logger getLogger() {
		return logger;
	}
	
	void setInstance(CorePlugin instance) {
		if(this.instance != null)
			throw new UnsupportedOperationException("Instance has been set.");
		this.instance = instance;
	}
}
