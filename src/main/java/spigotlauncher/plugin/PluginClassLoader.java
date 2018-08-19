package spigotlauncher.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class PluginClassLoader extends URLClassLoader {

	private static final String JAVA_PACKAGE_PREFIX = "java.";

	private final PluginContainer container;

	public PluginClassLoader(PluginContainer container, Path file) {
		super(new URL[0]);
		this.container = container;
		try {
			addURL(file.toUri().toURL());
		} catch (MalformedURLException e) {
			container.getLogger().error(e.getMessage(), e);
			throw new PluginException("Couldn't load plugin " + container.getName() + ".", e);
		}
	}

	@Override
	public URL getResource(String name) {
		URL resource = findResource(name);
		if (resource != null)
			return resource;

		return super.getResource(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			if (name.startsWith(JAVA_PACKAGE_PREFIX)) {
				return findSystemClass(name);
			}

			Class<?> loadedClass;

			loadedClass = findLoadedClass(name);
			if (loadedClass != null) {
				return loadedClass;
			}

			try {
				return super.loadClass(name);
			} catch (ClassNotFoundException e) {

			}

			try {
				return findClass(name);
			} catch (ClassNotFoundException e) {

			}

			throw new ClassNotFoundException(name);
		}
	}
}
