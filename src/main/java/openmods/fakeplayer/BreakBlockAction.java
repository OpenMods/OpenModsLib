package openmods.fakeplayer;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.world.DropCapture;
import openmods.world.DropCapture.CaptureContext;

public class BreakBlockAction implements PlayerUserReturning<List<EntityItem>> {
	private final World worldObj;
	private final BlockPos blockPos;

	private ItemStack stackToUse;

	public BreakBlockAction(World worldObj, BlockPos blockPos) {
		this.worldObj = worldObj;
		this.blockPos = blockPos;
		this.stackToUse = new ItemStack(Items.diamond_pickaxe, 0, 0);
	}

	public BreakBlockAction setStackToUse(ItemStack stack) {
		this.stackToUse = stack;
		return this;
	}

	private boolean removeBlock(EntityPlayer player, BlockPos pos, IBlockState state, boolean canHarvest) {
		final Block block = state.getBlock();
		block.onBlockHarvested(worldObj, pos, state, player);
		final boolean result = block.removedByPlayer(worldObj, pos, player, canHarvest);
		if (result) block.onBlockDestroyedByPlayer(worldObj, pos, state);
		return result;
	}

	@Override
	public List<EntityItem> usePlayer(OpenModsFakePlayer fakePlayer) {
		fakePlayer.inventory.currentItem = 0;
		fakePlayer.inventory.setInventorySlotContents(0, stackToUse);

		if (!worldObj.isBlockModifiable(fakePlayer, blockPos)) return Lists.newArrayList();

		// this mirrors ItemInWorldManager.tryHarvestBlock
		final IBlockState state = worldObj.getBlockState(blockPos);

		final CaptureContext dropsCapturer = DropCapture.instance.start(blockPos);

		final List<EntityItem> drops;
		try {
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(worldObj, blockPos, state, fakePlayer);
			if (MinecraftForge.EVENT_BUS.post(event)) return Lists.newArrayList();

			final TileEntity te = worldObj.getTileEntity(blockPos); // OHHHHH YEEEEAAAH

			boolean canHarvest = state.getBlock().canHarvestBlock(worldObj, blockPos, fakePlayer);
			boolean isRemoved = removeBlock(fakePlayer, blockPos, state, canHarvest);
			if (isRemoved && canHarvest) {
				state.getBlock().harvestBlock(worldObj, fakePlayer, blockPos, state, te);
				worldObj.playAuxSFXAtEntity(fakePlayer, 2001, blockPos, Block.getStateId(state));
			}

		} finally {
			drops = dropsCapturer.stop();
		}

		return drops;
	}
}