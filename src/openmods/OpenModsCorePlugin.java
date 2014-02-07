package openmods;

import java.util.Map;
import java.util.logging.Logger;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@TransformerExclusions({ "openmods.asm", "openmods.include" })
public class OpenModsCorePlugin implements IFMLLoadingPlugin {

	public static Logger log;

	static {
		log = Logger.getLogger("OpenModsCore");
		log.setParent(FMLLog.getLogger());
	}

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
	public void injectData(Map<String, Object> data) {}

	@Override
	@Deprecated
	public String[] getLibraryRequestClass() {
		return null;
	}

}
