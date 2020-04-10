package openmods;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.ObjectHolder;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
@ObjectHolder(OpenMods.MODID)
public class Sounds {
	private static final String ID_PAGETURN = "pageturn";

	@ObjectHolder(ID_PAGETURN)
	public static final SoundEvent PAGE_TURN = null;

	@SubscribeEvent
	public static void registerSounds(final RegistryEvent.Register<SoundEvent> evt) {
		final IForgeRegistry<SoundEvent> registry = evt.getRegistry();
		registerSound(registry, ID_PAGETURN);
	}

	private static void registerSound(final IForgeRegistry<SoundEvent> registry, final String id) {
		final ResourceLocation resourceLocation = OpenMods.location(id);
		registry.register(new SoundEvent(resourceLocation).setRegistryName(resourceLocation));
	}
}