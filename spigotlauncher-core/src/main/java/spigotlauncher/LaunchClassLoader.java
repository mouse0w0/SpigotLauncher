package spigotlauncher;

import spigotlauncher.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LaunchClassLoader extends URLClassLoader {

    private URL serverFileUrl;
    private Manifest serverFileManifest;

    private static final ClassLoader parent = Launch.class.getClassLoader();

    private final Set<String> unfoundClasses = new HashSet<>();
    private final Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<String, Class<?>>();

    private final TransformExecutor transformExecutor = new TransformExecutor();

    public LaunchClassLoader(File serverFile) throws IOException {
        super(new URL[0], parent);

        serverFileUrl = serverFile.toURI().toURL();
        addURL(serverFileUrl);

        try (JarFile serverJarFile = new JarFile(serverFile)) {
            serverFileManifest = serverJarFile.getManifest();
        }
    }

    @Override
    protected void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (unfoundClasses.contains(name))
            throw new ClassNotFoundException(name);

        if (cachedClasses.containsKey(name))
            return cachedClasses.get(name);

        try {
            final int lastDot = name.lastIndexOf('.');
            final String classFileName = name.replace('.', '/').concat(".class");
            final String packageName = lastDot == -1 ? "" : name.substring(0, lastDot);

            Package pkg = getPackage(packageName);
            if (pkg == null) {
                definePackage(packageName, serverFileManifest, serverFileUrl);
            }

            Class<?> clazz;

            final URL classResource = findResource(classFileName);
            if (classResource == null) {
                clazz = parent.loadClass(name);
            } else {
                try (InputStream stream = classResource.openStream()) {
                    byte[] bytes = Utils.readAllBytes(stream);
                    if (transformExecutor.isIncludedTransform(name) && !transformExecutor.isExcludedTransform(name)) {
                        bytes = transformExecutor.transform(name, bytes);
                    }
                    clazz = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }

            if (clazz == null) {
                throw new ClassNotFoundException(name);
            }

            cachedClasses.put(name, clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            unfoundClasses.add(name);
            throw e;
        }
    }

    public TransformExecutor getTransformExecutor() {
        return transformExecutor;
    }
}
