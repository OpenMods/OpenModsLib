package openmods.utils;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nonnull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.output.NullOutputStream;

public class ItemUtils {

	@Nonnull
	public static ItemStack consumeItem(@Nonnull ItemStack stack) {
		if (stack.getCount() == 1) {
			final Item item = stack.getItem();
			if (item.hasContainerItem(stack)) return item.getContainerItem(stack);
			return ItemStack.EMPTY;
		}
		stack.splitStack(1);

		return stack;
	}

	public static NBTTagCompound getItemTag(@Nonnull ItemStack stack) {
		NBTTagCompound result = stack.getTagCompound();
		if (result == null) {
			result = new NBTTagCompound();
			stack.setTagCompound(result);
		}
		return result;
	}

	public static EntityItem createDrop(Entity dropper, @Nonnull ItemStack is) {
		return createEntityItem(dropper.world, dropper.posX, dropper.posY, dropper.posZ, is);
	}

	public static EntityItem createEntityItem(World world, double x, double y, double z, @Nonnull ItemStack is) {
		return new EntityItem(world, x, y, z, is.copy());
	}

	/**
	 * This function returns fingerprint of NBTTag. It can be used to compare two tags
	 */
	public static String getNBTHash(NBTTagCompound tag) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			OutputStream dump = new NullOutputStream();
			DigestOutputStream hasher = new DigestOutputStream(dump, digest);
			DataOutput output = new DataOutputStream(hasher);
			CompressedStreamTools.write(tag, output);
			byte[] hash = digest.digest();
			return new String(Hex.encodeHex(hash));
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	// because vanilla is not really good with null stacks
	public static void setEntityItemStack(EntityItem entity, @Nonnull ItemStack stack) {
		if (stack.isEmpty()) {
			entity.setDead();
		} else {
			entity.setItem(stack);
		}
	}
}
