package openmods;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

@ObjectHolder(OpenMods.MODID)
public class Sounds {
	@ObjectHolder("pageturn")
	public static final SoundEvent PAGE_TURN = null;

	public static void register() {
		registerSound("pageturn");
	}

	private static void registerSound(String id) {
		final ResourceLocation resourceLocation = OpenMods.location(id);
		GameRegistry.register(new SoundEvent(resourceLocation).setRegistryName(resourceLocation));
	}
}