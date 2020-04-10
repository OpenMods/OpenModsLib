package openmods.proxy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.INetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.geometry.IHitboxSupplier;

public interface IOpenModsProxy {

	World getClientWorld();

	World getServerWorld(final DimensionType dimension);

	Optional<String> getLanguage();

	IGuiHandler wrapHandler(IGuiHandler modSpecificHandler);

	void earlySyncInit();

	void clientInit();

	void setNowPlayingTitle(String nowPlaying);

	IHitboxSupplier getHitboxes(ResourceLocation location);

	IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters);
}