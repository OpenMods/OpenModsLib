package openmods.core;

import java.util.Map;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import openmods.Log;

//must be lower than all dependent ones
@SortingIndex(16)
@TransformerExclusions({ "openmods.asm.", "openmods.include.", "openmods.core.", "openmods.injector.", "openmods.Log" })
public class OpenModsCorePlugin implements IFMLLoadingPlugin {

	public static final String CORE_MARKER = "OpenModsCoreLoaded";

	public OpenModsCorePlugin() {
		Log.debug("<OpenModsLib %s>\\o", "$LIB-VERSION$");
		Launch.blackboard.put(CORE_MARKER, "$LIB-VERSION$");
	}

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
