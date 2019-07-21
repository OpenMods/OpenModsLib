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
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.config.game.ICustomItemModelProvider;
import openmods.geometry.IHitboxSupplier;

public interface IOpenModsProxy {

	World getClientWorld();

	World getServerWorld(int dimension);

	PlayerEntity getThePlayer();

	boolean isClientPlayer(Entity player);

	long getTicks(World worldObj);

	File getMinecraftDir();

	Optional<String> getLanguage();

	String getLogFileName();

	IGuiHandler wrapHandler(IGuiHandler modSpecificHandler);

	void preInit();

	void init();

	void postInit();

	void setNowPlayingTitle(String nowPlaying);

	PlayerEntity getPlayerFromHandler(INetHandler handler);

	void bindItemModelToItemMeta(Item item, int metadata, ResourceLocation resourceLocation);

	void registerCustomItemModel(Item item, int metadata, ResourceLocation resourceLocation);

	void runCustomItemModelProvider(ResourceLocation itemLocation, Item item, Class<? extends ICustomItemModelProvider> providerCls);

	IHitboxSupplier getHitboxes(ResourceLocation location);

	IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters);
}