package openmods.proxy;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.network.INetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.config.game.ICustomItemModelProvider;
import openmods.geometry.Hitbox;

public interface IOpenModsProxy {

	public World getClientWorld();

	public World getServerWorld(int dimension);

	public EntityPlayer getThePlayer();

	public boolean isClientPlayer(Entity player);

	public long getTicks(World worldObj);

	public File getMinecraftDir();

	public Optional<String> getLanguage();

	public String getLogFileName();

	public IGuiHandler wrapHandler(IGuiHandler modSpecificHandler);

	public void preInit();

	public void init();

	public void postInit();

	public void setNowPlayingTitle(String nowPlaying);

	public EntityPlayer getPlayerFromHandler(INetHandler handler);

	public void bindItemModelToItemMeta(Item item, int metadata, ResourceLocation resourceLocation);

	public void registerCustomItemModel(Item item, int metadata, ResourceLocation resourceLocation);

	public void runCustomItemModelProvider(ResourceLocation itemLocation, Item item, Class<? extends ICustomItemModelProvider> providerCls);

	public Supplier<List<Hitbox>> getHitboxes(ResourceLocation location);

	public IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters);
}