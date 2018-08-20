package spigotlauncher;

import spigotlauncher.api.Transformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LaunchClassLoader extends URLClassLoader {

    private URL serverFileUrl;
    private Manifest serverFileManifest;

    private final List<String> excludedTransformPackages = new ArrayList<>();
    private final List<String> includedTransformPackages = new ArrayList<>();

    private final List<Transformer> transformers = new ArrayList<>();

    public LaunchClassLoader(File serverFile) {
        super(new URL[0]);

        try {
            serverFileUrl = serverFile.toURI().toURL();
            addURL(serverFileUrl);
        } catch (MalformedURLException e) {
            Launch.getLogger().error(e.getMessage(), e);
        }

        try (JarFile serverJarFile = new JarFile(serverFile)) {
            serverFileManifest = serverJarFile.getManifest();
        } catch (IOException e) {
            Launch.getLogger().error(e.getMessage(), e);
        }

        excludedTransformPackages.add("org.bukkit.craftbukkit.libs.");

        includedTransformPackages.add("net.minecraft.server.");
        includedTransformPackages.add("org.bukkit.");
    }

    public void addTransformer(Collection<Transformer> transformers) {
        this.transformers.addAll(transformers);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(name);
        if (clazz != null)
            return clazz;

        final int lastDot = name.lastIndexOf('.');
        final String classFileName = name.replace('.', '/').concat(".class");
        final String packageName = lastDot == -1 ? "" : name.substring(0, lastDot);

        Package pkg = getPackage(packageName);
        if (pkg == null) {
            pkg = definePackage(packageName, serverFileManifest, serverFileUrl);
        }

        if (!isExcludedTransform(name) && isIncludedTransform(name)) {
            final URL classResource = findResource(classFileName);
            if (classResource == null)
                throw new ClassNotFoundException(name);

            try (InputStream stream = classResource.openStream()) {
                byte[] bytes = readAllBytes(stream);
                bytes = transform(name, bytes);
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        return super.findClass(name);
    }

    private byte[] transform(String name, byte[] bytes) {
        for (Transformer transformer : transformers) {
            bytes = transformer.transform(name, bytes);
        }
        return bytes;
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
//        URL resource = findResource(name);
//        if (resource != null)
//            return resource;
//
//        return super.getResource(name);
//    }

    //    @Override
//    public Class<?> loadClass(String name) throws ClassNotFoundException {
//        synchronized (getClassLoadingLock(name)) {
//            Class<?> loadedClass;
//
//            loadedClass = findLoadedClass(name);
//            if (loadedClass != null) {
//                return loadedClass;
//            }
//
//            try {
//                return super.loadClass(name);
//            } catch (ClassNotFoundException e) {
//
//            }
//
//            try {
//                return findClass(name);
//            } catch (ClassNotFoundException e) {
//
//            }
//
//            throw new ClassNotFoundException(name);
//        }
//    }
}
