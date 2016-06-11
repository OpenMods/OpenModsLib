package openmods.core;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ICrashCallable;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import java.util.Arrays;
import openmods.injector.InjectorSanityChecker;

public class OpenModsCore extends DummyModContainer {

	public OpenModsCore() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = "OpenModsCore";
		meta.name = "OpenModsCore";
		meta.version = "$LIB-VERSION$";
		meta.authorList = Arrays.asList("Mikee", "NeverCast", "boq");
		meta.url = "http://openmods.info/";
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

		FMLCommonHandler.instance().registerCrashCallable(new InjectorSanityChecker());
	}
}
