package openmods.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.util.Arrays;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ICrashCallable;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;

public class OpenModsCore extends DummyModContainer {

	public OpenModsCore() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = "openmodscore";
		meta.name = "OpenModsLib Core";
		meta.version = "$LIB-VERSION$";
		meta.authorList = Arrays.asList("Mikee", "NeverCast", "boq");
		meta.url = "https://openmods.info/";
		meta.description = "This is where the magic happens";
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		bus.register(this);
		return true;
	}

	@Subscribe
	public void modConstruction(FMLConstructionEvent evt) {
		OpenModsClassTransformer.instance().injectAsmData(evt.getASMHarvestedData());
		FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {

			@Override
			public String call() throws Exception {
				return OpenModsClassTransformer.instance().listStates();
			}

			@Override
			public String getLabel() {
				return "OpenModsLib class transformers";
			}
		});
	}
}
