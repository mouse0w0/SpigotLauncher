package spigotlaunchwrapper.api;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.xml.transform.Transformer;

public interface CorePlugin {
	
	void acceptOptions(List<String> args, Path serverDir);
	
	Collection<Transformer> getTransformers();
}
