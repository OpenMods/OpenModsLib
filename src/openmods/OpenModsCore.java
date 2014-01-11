package openmods;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;

public class OpenModsCore extends DummyModContainer {

	public OpenModsCore() {
		super(new ModMetadata());
		ModMetadata meta = getMetadata();
		meta.modId = "OpenModsCore";
		meta.name = "OpenModsCore";
		meta.version = "@VERSION@";
		meta.authorList = Arrays.asList("Mikee", "NeverCast", "boq");
		meta.url = "http://openmods.info/";
		meta.description = "This is where the magic happens";
	}

	@Override
	public boolean registerBus(EventBus bus, LoadController controller) {
		return true;
	}

}
