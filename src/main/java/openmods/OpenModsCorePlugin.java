package openmods;

import java.io.File;
import java.util.Map;

import openmods.config.simple.ConfigProcessor;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

//must be lower than all dependent ones
@SortingIndex(16)
@TransformerExclusions({ "openmods.asm", "openmods.include" })
public class OpenModsCorePlugin implements IFMLLoadingPlugin {

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "openmods.OpenModsClassTransformer" };
	}

	@Override
	public String getModContainerClass() {
		return "openmods.OpenModsCore";
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		File mcLocation = (File)data.get("mcLocation");
		File configDir = new File(mcLocation, "config");

		if (!configDir.exists()) configDir.mkdir();

		File configFile = new File(configDir, "OpenModsLibCore.json");

		try {
			ConfigProcessor processor = new ConfigProcessor();
			OpenModsClassTransformer.instance().addConfigValues(processor);
			processor.process(configFile);
		} catch (Throwable t) {
			throw new RuntimeException(String.format("Failed to read config from file %s", configFile), t);
		}
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
