package spigotlaunchwrapper;

import java.io.File;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Launch {

	private static final Logger LOGGER = LoggerFactory.getLogger("Launch");

	private static LaunchClassLoader classLoader;

	public static void main(String[] args) {
		OptionParser parser = new OptionParser();
		OptionSpec<String> server = parser.accepts("serverFile").withRequiredArg().defaultsTo("server.jar");
		OptionSet options = parser.parse(args);

		File serverFile = new File(options.valueOf(server));
		if (!serverFile.exists()) {
			getLogger().error("Server file {} isn't exists. ", serverFile.getAbsolutePath());
			return;
		}
		if (!serverFile.isFile()) {
			getLogger().error("Server file {} isn't file. ", serverFile.getAbsolutePath());
			return;
		}
		if (!serverFile.getName().endsWith(".jar")) {
			getLogger().error("Server file {} isn't jar. ", serverFile.getAbsolutePath());
			return;
		}

		getLogger().info("Finded server file: {} Initializing...", serverFile.getAbsolutePath());

		try {
			classLoader = new LaunchClassLoader(serverFile);
			Thread.currentThread().setContextClassLoader(classLoader);

			Class<?> craftBukkitMain = Class.forName("org.bukkit.craftbukkit.Main", false, classLoader);
			Method main = craftBukkitMain.getMethod("main", String[].class);
			main.invoke(null, new Object[] { args });
		} catch (Exception e) {
			getLogger().error("Initialize server failed.", e);
		}
	}

	public static Logger getLogger() {
		return LOGGER;
	}
}
