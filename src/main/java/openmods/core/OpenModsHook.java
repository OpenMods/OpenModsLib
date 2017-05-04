package openmods.core;

import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLCallHook;

public class OpenModsHook implements IFMLCallHook {

	@Override
	public Void call() throws Exception {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		BundledJarUnpacker.setup(data);
	}

}
