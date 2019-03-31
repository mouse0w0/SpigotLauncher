package spigotlauncher.api.plugin;

public interface Transformer {

    byte[] transform(String className, byte[] bytecode);

}
