package openmods.utils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

public class PlayerUtils {

	public static CompoundNBT getModPlayerPersistTag(PlayerEntity player, String modName) {

		CompoundNBT tag = player.getEntityData();

		final CompoundNBT persistTag;
		if (tag.hasKey(PlayerEntity.PERSISTED_NBT_TAG)) {
			persistTag = tag.getCompoundTag(PlayerEntity.PERSISTED_NBT_TAG);
		} else {
			persistTag = new CompoundNBT();
			tag.setTag(PlayerEntity.PERSISTED_NBT_TAG, persistTag);
		}

		final CompoundNBT modTag;
		if (persistTag.hasKey(modName)) {
			modTag = persistTag.getCompoundTag(modName);
		} else {
			modTag = new CompoundNBT();
			persistTag.setTag(modName, modTag);
		}

		return modTag;
	}
}
