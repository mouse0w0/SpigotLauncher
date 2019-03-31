package spigotlauncher;

import spigotlauncher.api.ClassDefiner;
import spigotlauncher.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class StaticTransformer extends TransformExecutor implements ClassDefiner {

    private final File serverFile;
    private final File outputFile;

    private JarOutputStream output;

    public StaticTransformer(File serverFile, File outputFile) {
        this.serverFile = serverFile;
        this.outputFile = outputFile;
    }

    public void start() {
        try {
            Launch.getLogger().info("Transforming server...");
            try (JarFile serverJar = new JarFile(serverFile);
                 JarOutputStream output = new JarOutputStream(new FileOutputStream(outputFile))) {
                this.output = output;
                Enumeration<JarEntry> entries = serverJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (entry.isDirectory()) {
                        output.putNextEntry(new JarEntry(entry.getName()));
                        continue;
                    }

                    try (InputStream source = serverJar.getInputStream(entry)) {
                        String fileName = entry.getName();
                        if (!fileName.endsWith(".class")) {
                            output.putNextEntry(new JarEntry(entry.getName()));
                            Utils.copy(source, output);
                            continue;
                        }

                        String className = fileName.substring(0, fileName.length() - ".class".length()).replace('/', '.');
                        if (!isIncludedTransform(className) || isExcludedTransform(className)) {
                            output.putNextEntry(new JarEntry(entry.getName()));
                            Utils.copy(source, output);
                            continue;
                        }

                        byte[] bytes = Utils.readAllBytes(source);
                        bytes = transform(className, bytes);
                        output.putNextEntry(new JarEntry(entry.getName()));
                        output.write(bytes);
                    }
                }
            }
            Launch.getLogger().info("Transformed server to file {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            Launch.getLogger().error("Failed to transform server.", e);
        }
    }


    @Override
    public void define(String name, byte[] b, int off, int len) {
        try {
            output.putNextEntry(new JarEntry(name.replace(".","/").concat(".class")));
            output.write(b, off, len);
        } catch (IOException e) {
            Launch.getLogger().error("Cannot define class.", e);
        }
    }
}
