package spigotlauncher;

import spigotlauncher.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class StaticTransformExecutor extends TransformExecutor {

    private final File serverFile;
    private final File outputFile;

    public StaticTransformExecutor(File serverFile, File outputFile) {
        this.serverFile = serverFile;
        this.outputFile = outputFile;
    }

    public void start() {
        try {
            Launch.getLogger().info("Transforming server...");
            try (JarFile serverJar = new JarFile(serverFile);
                 JarOutputStream output = new JarOutputStream(new FileOutputStream(outputFile))) {
                Enumeration<JarEntry> entries = serverJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    output.putNextEntry(new JarEntry(entry));

                    if (entry.isDirectory()) {
                        continue;
                    }

                    try (InputStream source = serverJar.getInputStream(entry)) {
                        String fileName = entry.getName();
                        if (!fileName.endsWith(".class")) {
                            Utils.copy(source, output);
                            continue;
                        }

                        String className = fileName.substring(0, fileName.length() - ".class".length()).replace('/', '.');
                        if (!isIncludedTransform(className) || isExcludedTransform(className)) {
                            Utils.copy(source, output);
                            continue;
                        }

                        byte[] bytes = Utils.readAllBytes(source);
                        bytes = transform(className, bytes);
                        output.write(bytes);
                    }
                }
            }
            Launch.getLogger().info("Transformed server to file {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            Launch.getLogger().error("Failed to transform server.", e);
        }
    }


}
