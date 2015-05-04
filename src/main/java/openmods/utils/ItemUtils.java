package openmods.utils;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.output.NullOutputStream;

import com.google.common.base.Throwables;

import cpw.mods.fml.common.registry.GameData;

public class ItemUtils {

	public static ItemStack consumeItem(ItemStack stack) {
		if (stack.stackSize == 1) {
			final Item item = stack.getItem();
			if (item.hasContainerItem(stack)) return item.getContainerItem(stack);
			return null;
		}
		stack.splitStack(1);

		return stack;
	}

	public static NBTTagCompound getItemTag(ItemStack stack) {
		if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
		return stack.stackTagCompound;
	}

	public static Integer getInt(ItemStack stack, String tagName) {
		NBTTagCompound tag = getItemTag(stack);
		NBTBase data = tag.getTag(tagName);
		return (data != null)? ((NBTTagInt)data).func_150287_d() : null;
	}

	public static EntityItem createDrop(Entity dropper, ItemStack is) {
		return createEntityItem(dropper.worldObj, dropper.posX, dropper.posY, dropper.posZ, is);
	}

	public static EntityItem createEntityItem(World world, double x, double y, double z, ItemStack is) {
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
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	// backports from MC1.8
	public static Item getByNameOrId(String id)
	{
		final Item item = GameData.getItemRegistry().getObject(id);
		if (item != null) return item;

		try {
			final int numericId = Integer.parseInt(id);
			return GameData.getItemRegistry().getObjectById(numericId);
		} catch (NumberFormatException numberformatexception) {}

		return null;
	}

	public static ItemStack readStack(NBTTagCompound nbt) {
		final Item item;
		if (nbt.hasKey("id", Constants.NBT.TAG_STRING)) {
			item = getByNameOrId(nbt.getString("id"));
		} else {
			item = Item.getItemById(nbt.getShort("id"));
		}

		if (item == null) return null;

		final int stackSize = nbt.getByte("Count");
		final int itemDamage = nbt.getShort("Damage");

		final ItemStack result = new ItemStack(item, stackSize, itemDamage);

		if (nbt.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
			result.stackTagCompound = nbt.getCompoundTag("tag");
		}
		return result;
	}

	public static NBTTagCompound writeStack(ItemStack stack) {
		NBTTagCompound result = new NBTTagCompound();
		stack.writeToNBT(result);

		// if possible, replace with string representation
		final Item item = stack.getItem();
		if (item != null) {
			final String id = GameData.getItemRegistry().getNameForObject(item);
			if (id != null) {
				result.setString("id", id);
			}
		}

		return result;
	}
}
