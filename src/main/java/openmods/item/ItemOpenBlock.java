package openmods.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.block.OpenBlock;

public class ItemOpenBlock extends ItemBlock {

	public ItemOpenBlock(Block block) {
		super(block);
	}

	private static boolean canReplace(Block block, World world, int x, int y, int z) {
		return block != null && block.isReplaceable(world, x, y, z);
	}

	protected void afterBlockPlaced(ItemStack stack, EntityPlayer player, World world, int x, int y, int z) {
		stack.stackSize--;
	}

	protected boolean isStackValid(ItemStack stack, EntityPlayer player) {
		return stack.stackSize >= 0;
	}

	/**
	 * Replicates the super method, but with our own hooks
	 */
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		if (!isStackValid(stack, player)) return false;

		Block block = world.getBlock(x, y, z);

		if (block == Blocks.snow && (world.getBlockMetadata(x, y, z) & 7) < 1) side = 1;

		ForgeDirection sideDir = ForgeDirection.getOrientation(side);

		if (!canReplace(block, world, x, y, z)) {
			x += sideDir.offsetX;
			y += sideDir.offsetY;
			z += sideDir.offsetZ;
		}

		if (!player.canPlayerEdit(x, y, z, side, stack)) return false;

		Block ownBlock = this.field_150939_a;
		if (y == 255 && ownBlock.getMaterial().isSolid()) return false;

		if (!world.canPlaceEntityOnSide(ownBlock, x, y, z, false, side, player, stack)) return false;

		if (ownBlock instanceof OpenBlock) {
			OpenBlock ob = (OpenBlock)ownBlock;
			ForgeDirection blockDirection = ob.calculateSide(player, sideDir);

			int newMeta = calculateBlockMeta(world, ownBlock, x, y, z, side, hitX, hitY, hitZ, stack.getItemDamage());

			if (!ob.canPlaceBlock(world, player, stack, x, y, z, sideDir, blockDirection, hitX, hitY, hitZ, newMeta)) return false;

			if (!placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, newMeta)) return false;

			ob.afterBlockPlaced(world, player, stack, x, y, z, sideDir, blockDirection, hitX, hitY, hitZ, newMeta);
		} else {
			int newMeta = calculateBlockMeta(world, ownBlock, x, y, z, side, hitX, hitY, hitZ, stack.getItemDamage());
			if (!placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, newMeta)) return false;
		}

		world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5, ownBlock.stepSound.getBreakSound(), (ownBlock.stepSound.getVolume() + 1.0F) / 2.0F, ownBlock.stepSound.getPitch() * 0.8F);
		afterBlockPlaced(stack, player, world, x, y, z);

		return true;
	}

	protected int calculateBlockMeta(World world, Block ownBlock, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int damage) {
		int newMeta = getMetadata(damage);
		return ownBlock.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, newMeta);
	}
}
