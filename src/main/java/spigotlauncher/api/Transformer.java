package spigotlauncher.api;

public interface Transformer {
	
	byte[] transform(String className, byte[] bytecode);

}
