package spigotlauncher.api;

import spigotlauncher.api.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public interface ClassDefiner {

    void define(String name, byte[] bytes, int off, int len);

    default void define(String name, byte[] bytes) {
        define(name, bytes, 0, bytes.length);
    }

    default void define(byte[] bytes) {
        define(null, bytes, 0, bytes.length);
    }

    default void define(InputStream inputStream) throws IOException {
        define(IOUtils.readAllBytes(inputStream));
    }

    default void define(URL url) throws IOException {
        try (InputStream inputStream = url.openStream()) {
            define(inputStream);
        }
    }
}
