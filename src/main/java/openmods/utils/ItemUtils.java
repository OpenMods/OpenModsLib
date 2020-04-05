package openmods.utils;

import com.google.common.io.BaseEncoding;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nonnull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.world.World;
import org.apache.commons.io.output.NullOutputStream;

public class ItemUtils {

	@Nonnull
	public static ItemStack consumeItem(@Nonnull ItemStack stack) {
		if (stack.getCount() == 1) {
			final Item item = stack.getItem();
			if (item.hasContainerItem(stack)) return item.getContainerItem(stack);
			return ItemStack.EMPTY;
		}
		stack.split(1);

		return stack;
	}

	public static ItemEntity createDrop(Entity dropper, @Nonnull ItemStack is) {
		return createEntityItem(dropper.world, dropper.posX, dropper.posY, dropper.posZ, is);
	}

	public static ItemEntity createEntityItem(World world, double x, double y, double z, @Nonnull ItemStack is) {
		return new ItemEntity(world, x, y, z, is.copy());
	}

	/**
	 * This function returns fingerprint of NBTTag. It can be used to compare two tags
	 */
	public static String getNBTHash(CompoundNBT tag) {
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			OutputStream dump = new NullOutputStream();
			DigestOutputStream hasher = new DigestOutputStream(dump, digest);
			DataOutput output = new DataOutputStream(hasher);
			CompressedStreamTools.write(tag, output);
			byte[] hash = digest.digest();
			return BaseEncoding.base16().encode(hash);
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	// because vanilla is not really good with null stacks
	public static void setEntityItemStack(ItemEntity entity, @Nonnull ItemStack stack) {
		if (stack.isEmpty()) {
			entity.remove();
		} else {
			entity.setItem(stack);
		}
	}
}
