package spigotlauncher;

import spigotlauncher.api.Transformer;
import sun.misc.CompoundEnumeration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
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

    private final List<String> excludedClassLoaderPackages = new ArrayList<>();
    private final List<String> excludedTransformPackages = new ArrayList<>();
    private final List<String> includedTransformPackages = new ArrayList<>();

    private final List<Transformer> transformers = new ArrayList<>();

    public LaunchClassLoader(File serverFile) throws IOException {
        super(new URL[0], parent);

        serverFileUrl = serverFile.toURI().toURL();
        addURL(serverFileUrl);

        try (JarFile serverJarFile = new JarFile(serverFile)) {
            serverFileManifest = serverJarFile.getManifest();
        }

        excludedClassLoaderPackages.add("org.apache.logging.");
        excludedClassLoaderPackages.add("java.");
        excludedClassLoaderPackages.add("javax.");
        excludedClassLoaderPackages.add("sun.");
        excludedClassLoaderPackages.add("com.sun.");
        excludedClassLoaderPackages.add("jdk.");

        excludedTransformPackages.add("org.bukkit.craftbukkit.libs.");

        includedTransformPackages.add("net.minecraft.server.");
        includedTransformPackages.add("org.bukkit.");
    }

    @Override
    protected void addURL(URL url) {
        super.addURL(url);
    }

    public void addTransformer(Collection<Transformer> transformers) {
        this.transformers.addAll(transformers);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return findClass(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (unfoundClasses.contains(name))
            throw new ClassNotFoundException(name);

        if (isExcludedClassLoader(name)) {
            try {
                Class<?> clazz = parent.loadClass(name);
                cachedClasses.put(name, clazz);
                return clazz;
            } catch (ClassNotFoundException e) {
                unfoundClasses.add(name);
                throw e;
            }
        }

        if (cachedClasses.containsKey(name))
            return cachedClasses.get(name);

        try {
            final int lastDot = name.lastIndexOf('.');
            final String classFileName = name.replace('.', '/').concat(".class");
            final String packageName = lastDot == -1 ? "" : name.substring(0, lastDot);

            Package pkg = getPackage(packageName);
            if (pkg == null) {
                pkg = definePackage(packageName, serverFileManifest, serverFileUrl);
            }

            Class<?> clazz;

            if (isIncludedTransform(name) && !isExcludedTransform(name)) {
                final URL classResource = findResource(classFileName);
                if (classResource == null)
                    throw new ClassNotFoundException(name);

                try (InputStream stream = classResource.openStream()) {
                    byte[] bytes = readAllBytes(stream);
                    bytes = transform(name, bytes);
                    clazz = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            } else {
                final URL classResource = findResource(classFileName);
                if (classResource == null)
                    throw new ClassNotFoundException(name);

                try (InputStream stream = classResource.openStream()) {
                    byte[] bytes = readAllBytes(stream);
                    clazz = defineClass(name, bytes, 0, bytes.length);
                    if (clazz == null) {
                        clazz = parent.loadClass(name);
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
                return clazz;
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

    private byte[] transform(String name, byte[] bytes) {
        for (Transformer transformer : transformers) {
            bytes = transformer.transform(name, bytes);
        }
        return bytes;
    }

    private boolean isExcludedClassLoader(String name) {
        for (String prefix : excludedClassLoaderPackages) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    private boolean isExcludedTransform(String name) {
        for (String prefix : excludedTransformPackages) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    private boolean isIncludedTransform(String name) {
        for (String prefix : includedTransformPackages) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private byte[] readAllBytes(InputStream stream) throws IOException {
        int capacity = 16;
        byte[] buf = new byte[capacity];
        int nread = 0;
        int n;
        for (; ; ) {
            while ((n = stream.read(buf, nread, capacity - nread)) > 0)
                nread += n;

            if (n < 0 || (n = stream.read()) < 0)
                break;

            if (capacity <= MAX_BUFFER_SIZE - capacity) {
                capacity = Math.max(capacity << 1, BUFFER_SIZE);
            } else {
                if (capacity == MAX_BUFFER_SIZE)
                    throw new OutOfMemoryError("Required array size too large");
                capacity = MAX_BUFFER_SIZE;
            }
            buf = Arrays.copyOf(buf, capacity);
            buf[nread++] = (byte) n;
        }
        return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
    }

//    @Override
//    public URL getResource(String name) {
//        return parent.getResource(name);
//    }
//
//    @Override
//    public Enumeration<URL> getResources(String name) throws IOException {
//        return parent.getResources(name);
//    }
}
