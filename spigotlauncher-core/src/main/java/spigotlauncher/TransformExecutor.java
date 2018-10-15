package spigotlauncher;

import spigotlauncher.api.Transformer;
import spigotlauncher.plugin.PluginContainer;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class TransformExecutor {

    protected final List<String> excludedTransformPackages = new LinkedList<>();
    protected final List<String> includedTransformPackages = new LinkedList<>();

    protected final List<Transformer> transformers = new LinkedList<>();

    public TransformExecutor() {
        excludedTransformPackages.add("org.bukkit.craftbukkit.libs.");

        includedTransformPackages.add("net.minecraft.server.");
        includedTransformPackages.add("org.bukkit.");
    }

    public void addPluginTransformers(Collection<PluginContainer> plugins) {
        for (PluginContainer container : plugins) {
            addTransformers(container.getInstance().getTransformers());
        }
    }

    public void addTransformers(Collection<Transformer> transformers) {
        this.transformers.addAll(transformers);
    }

    public byte[] transform(String name, byte[] bytes) {
        for (Transformer transformer : transformers) {
            bytes = transformer.transform(name, bytes);
        }
        return bytes;
    }

    public boolean isExcludedTransform(String name) {
        for (String prefix : excludedTransformPackages) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }

    public boolean isIncludedTransform(String name) {
        for (String prefix : includedTransformPackages) {
            if (name.startsWith(prefix))
                return true;
        }
        return false;
    }
}
