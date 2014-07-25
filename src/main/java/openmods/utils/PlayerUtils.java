package openmods.utils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class PlayerUtils {

	public static NBTTagCompound getModPlayerPersistTag(EntityPlayer player, String modName) {

		NBTTagCompound tag = player.getEntityData();

		NBTTagCompound persistTag = null;
		if (tag.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
			persistTag = tag.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
		} else {
			persistTag = new NBTTagCompound();
			tag.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistTag);
		}

		NBTTagCompound modTag = null;
		if (persistTag.hasKey(modName)) {
			modTag = persistTag.getCompoundTag(modName);
		} else {
			modTag = new NBTTagCompound();
			persistTag.setTag(modName, modTag);
		}

		return modTag;
	}
}
