package spigotlaunchwrapper.plugin;

public class PluginException extends RuntimeException {

	public PluginException() {
	}

	public PluginException(String message, Throwable cause) {
		super(message, cause);
	}

	public PluginException(String message) {
		super(message);
	}

	public PluginException(Throwable cause) {
		super(cause);
	}

	
}
