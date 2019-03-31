package spigotlauncher.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class Utils {

    private static final int BUFFER_SIZE = 8192;

    public static void copy(InputStream source, OutputStream target) throws IOException
    {
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = source.read(buf)) > 0) {
            target.write(buf, 0, n);
        }
    }
}
