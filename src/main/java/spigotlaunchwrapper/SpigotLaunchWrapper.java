package spigotlaunchwrapper;

import java.io.File;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class SpigotLaunchWrapper {
	
	public static void main(String[] args) {
		OptionParser parser = new OptionParser();
		OptionSpec<String> server = parser.accepts("server-file").withRequiredArg().defaultsTo("server.jar");
		OptionSet options = parser.parse(args);
		
		File serverFile = new File(options.valueOf(server));
		if(!serverFile.exists()){
			System.err.println("服务端文件不存在. "+serverFile.getAbsolutePath());
			return;
		}
		if(!serverFile.isFile()){
			System.err.println("服务端文件不是文件. "+serverFile.getAbsolutePath());
			return;
		}
		if(!serverFile.getName().endsWith(".jar")){
			System.err.println("服务端文件不是Jar. "+serverFile.getAbsolutePath());
			return;
		}
		
		System.out.println("开始载入服务端： "+serverFile.getAbsolutePath());
		
		try {
			LaunchClassLoader classLoader = new LaunchClassLoader();
			Thread.currentThread().setContextClassLoader(classLoader);
			classLoader.addPath(serverFile.toPath());
			classLoader.loadClass("org.bukkit.craftbukkit.Main").getMethod("main", String[].class).invoke(null,
					new Object[] { args });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
