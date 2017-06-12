package openmods.utils;

import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import openmods.geometry.Orientation;
import openmods.inventory.IInventoryProvider;

public class BlockUtils {

	@Deprecated
	public static EnumFacing get2dOrientation(EntityLivingBase entity) {
		return entity.getHorizontalFacing();

	}

	public static float getRotationFromDirection(EnumFacing direction) {
		switch (direction) {
			case NORTH:
				return 180F;
			case SOUTH:
				return 0F;
			case WEST:
				return 90F;
			case EAST:
				return -90F;
			case DOWN:
				return -90f;
			case UP:
				return 90f;
			default:
				return 0f;
		}
	}

	public static float getRotationFromOrientation(Orientation orientation) {
		switch (orientation.x) {
			case POS_X:
				return 180f;
			case NEG_X:
				return 0f;
			case NEG_Z:
				return -90f;
			case POS_Z:
				return 90f;
			default:
				return 0f;
		}
	}

	public static EnumFacing get3dOrientation(EntityLivingBase entity, BlockPos pos) {
		if (MathHelper.abs((float)entity.posX - pos.getX()) < 2.0F
				&& MathHelper.abs((float)entity.posZ - pos.getZ()) < 2.0F) {
			final double eyePos = entity.posY + entity.getEyeHeight();
			final double blockPos = pos.getY();

			if (eyePos - blockPos > 2.0D) return EnumFacing.UP;
			if (blockPos - eyePos > 0.0D) return EnumFacing.DOWN;
		}

		return entity.getHorizontalFacing();
	}

	public static EntityItem dropItemStackInWorld(World worldObj, Vec3i pos, ItemStack stack) {
		return dropItemStackInWorld(worldObj, pos.getX(), pos.getY(), pos.getZ(), stack);
	}

	public static EntityItem dropItemStackInWorld(World worldObj, double x, double y, double z, ItemStack stack) {
		float f = 0.7F;
		float d0 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
		float d1 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
		float d2 = worldObj.rand.nextFloat() * f + (1.0F - f) * 0.5F;
		EntityItem entityitem = new EntityItem(worldObj, x + d0, y + d1, z + d2, stack);
		entityitem.setDefaultPickupDelay();
		if (stack.hasTagCompound()) {
			entityitem.getEntityItem().setTagCompound(stack.getTagCompound().copy());
		}
		worldObj.spawnEntityInWorld(entityitem);
		return entityitem;
	}

	public static EntityItem ejectItemInDirection(World world, double x, double y, double z, EnumFacing direction, ItemStack stack) {
		EntityItem item = BlockUtils.dropItemStackInWorld(world, x, y, z, stack);
		final Vec3i v = direction.getDirectionVec();
		item.motionX = v.getX() / 5F;
		item.motionY = v.getY() / 5F;
		item.motionZ = v.getZ() / 5F;
		return item;
	}

	public static boolean getTileInventoryDrops(TileEntity tileEntity, List<ItemStack> drops) {
		if (tileEntity == null) return false;

		if (tileEntity instanceof IInventory) {
			drops.addAll(InventoryUtils.getInventoryContents((IInventory)tileEntity));
			return true;
		} else if (tileEntity instanceof IInventoryProvider) {
			drops.addAll(InventoryUtils.getInventoryContents(((IInventoryProvider)tileEntity).getInventory()));
			return true;
		}

		return false;
	}

	public static void dropInventory(IInventory inventory, World world, double x, double y, double z) {
		if (inventory == null) { return; }
		for (int i = 0; i < inventory.getSizeInventory(); ++i) {
			ItemStack itemStack = inventory.getStackInSlot(i);
			if (itemStack != null) {
				dropItemStackInWorld(world, x, y, z, itemStack);
			}
		}
	}

	public static void dropInventory(IInventory inventory, World world, int x, int y, int z) {
		dropInventory(inventory, world, x + 0.5, y + 0.5, z + 0.5);
	}

	public static TileEntity getTileInDirection(TileEntity tile, EnumFacing direction) {
		final BlockPos offset = tile.getPos().offset(direction);
		return tile.getWorld().getTileEntity(offset);
	}

	public static TileEntity getTileInDirection(World world, BlockPos coord, EnumFacing direction) {
		return world.getTileEntity(coord.offset(direction));
	}

	public static TileEntity getTileInDirectionSafe(World world, BlockPos coord, EnumFacing direction) {
		BlockPos n = coord.offset(direction);
		return world.isBlockLoaded(n)? world.getTileEntity(n) : null;
	}

	public static AxisAlignedBB expandAround(BlockPos pos, int x, int y, int z) {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + x, pos.getY() + y, pos.getZ() + z);
	}

	public static AxisAlignedBB expandAround(BlockPos pos, double x, double y, double z) {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + x, pos.getY() + y, pos.getZ() + z);
	}

	public static AxisAlignedBB aabbOffset(BlockPos pos, double x1, double y1, double z1, double x2, double y2, double z2) {
		return new AxisAlignedBB(pos.getX() + x1, pos.getY() + y1, pos.getZ() + z1, pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2);
	}

	public static AxisAlignedBB aabbOffset(BlockPos pos, AxisAlignedBB aabb) {
		return aabb.offset(pos.getX(), pos.getY(), pos.getZ());
	}

	public static AxisAlignedBB singleBlock(BlockPos pos) {
		return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
	}

	public static void playSoundAtPos(World world, BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch) {
		world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound, category, volume, pitch, false);
	}
}
