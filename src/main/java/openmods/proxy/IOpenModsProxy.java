package openmods.proxy;

import com.google.common.base.Optional;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import openmods.geometry.IHitboxSupplier;

public interface IOpenModsProxy {

	World getClientWorld();

	Optional<String> getLanguage();

	void eventInit();

	void earlySyncInit();

	void clientInit();

	void setNowPlayingTitle(ITextComponent nowPlaying);

	IHitboxSupplier getHitboxes(ResourceLocation location);

	//IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters);
}