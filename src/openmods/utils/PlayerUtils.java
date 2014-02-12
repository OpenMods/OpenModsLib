package openmods.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import cpw.mods.fml.common.FMLCommonHandler;

public class PlayerUtils {
	public static boolean isPlayerOp(String username) {
		username = username.toLowerCase();

		MinecraftServer server = FMLCommonHandler.instance().getSidedDelegate().getServer();

		// SP and LAN
		if (server.isSinglePlayer()) {
			if (server instanceof IntegratedServer) return server.getServerOwner().equals(username);
			return server.getConfigurationManager().getOps().contains(username);
		}

		// SMP
		return server.getConfigurationManager().getOps().contains(username);
	}

	public static NBTTagCompound getModPlayerPersistTag(EntityPlayer player, String modName) {

		NBTTagCompound tag = player.getEntityData();
		
		NBTTagCompound persistTag = null;
		if (tag.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
			persistTag = tag.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		} else {
			persistTag = new NBTTagCompound();
			tag.setCompoundTag(EntityPlayer.PERSISTED_NBT_TAG, persistTag);
		}
		
		NBTTagCompound modTag = null;
		if (persistTag.hasKey(modName)) {
			modTag = persistTag.getCompoundTag(modName);
		} else {
			modTag = new NBTTagCompound();
			persistTag.setCompoundTag(modName, modTag);
		}

		return modTag;
	}
}
