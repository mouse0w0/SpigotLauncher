package spigotlauncher.api.plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public interface CorePlugin {
	
	void acceptOptions(List<String> args, Path serverDir);
	
	Collection<Transformer> getTransformers();

	String[] getLaunchArguments();
}
