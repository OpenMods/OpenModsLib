package openmods.core;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

//must be lower than all dependent ones
@SortingIndex(16)
@TransformerExclusions({ "openmods.asm.", "openmods.include.", "openmods.core.", "openmods.Log" })
public class OpenModsCorePlugin implements IFMLLoadingPlugin {

	@Override
	public String[] getASMTransformerClass() {
		return new String[] { "openmods.core.OpenModsClassTransformer" };
	}

	@Override
	public String getModContainerClass() {
		return "openmods.core.OpenModsCore";
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		Bootstrap.injectData(data);
	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}

}
