package spigotlaunchwrapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

public class LaunchClassLoader extends URLClassLoader {
	
	private static final String JAVA_PACKAGE_PREFIX = "java.";
	
	public LaunchClassLoader() {
		super(new URL[0]);
	}
	
	@Override
	public void addURL(URL url) {
		super.addURL(url);
	}
	
	public void addPath(Path path) {
		try {
			addURL(path.toUri().toURL());
		} catch (MalformedURLException e) {
			e.printStackTrace();
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
