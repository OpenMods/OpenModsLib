package openmods.proxy;

import com.google.common.base.Optional;
import java.io.File;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.network.INetHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

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
}