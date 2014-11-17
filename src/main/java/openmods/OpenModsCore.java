package openmods;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.event.FMLConstructionEvent;

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
		FMLCommonHandler.instance().registerCrashCallable(new ICrashCallable() {

			@Override
			public String call() throws Exception {
				return OpenModsClassTransformer.instance().listStates();
			}

			@Override
			public String getLabel() {
				return "OpenModsLib crash transformers";
			}
		});
	}
}
