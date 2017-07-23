package openmods;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.common.registry.IForgeRegistry;

@ObjectHolder(OpenMods.MODID)
@EventBusSubscriber
public class Sounds {
	@ObjectHolder("pageturn")
	public static final SoundEvent PAGE_TURN = null;

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> evt) {
		final IForgeRegistry<SoundEvent> registry = evt.getRegistry();
		registerSound(registry, "pageturn");
	}

	private static void registerSound(IForgeRegistry<SoundEvent> registry, String id) {
		final ResourceLocation resourceLocation = OpenMods.location(id);
		registry.register(new SoundEvent(resourceLocation).setRegistryName(resourceLocation));
	}
}