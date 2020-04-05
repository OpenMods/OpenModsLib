package openmods.utils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;

public class PlayerUtils {

	public static CompoundNBT getModPlayerPersistTag(Entity player, String modName) {
		CompoundNBT persistTag = player.getPersistentData();

		final CompoundNBT modTag;
		if (persistTag.contains(modName)) {
			modTag = persistTag.getCompound(modName);
		} else {
			modTag = new CompoundNBT();
			persistTag.put(modName, modTag);
		}

		return modTag;
	}
}
