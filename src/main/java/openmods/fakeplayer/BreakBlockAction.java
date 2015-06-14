package openmods.fakeplayer;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.world.DropCapture;
import openmods.world.DropCapture.CaptureContext;

import com.google.common.collect.Lists;

public class BreakBlockAction implements PlayerUserReturning<List<EntityItem>> {
	private final World worldObj;
	private final int x;
	private final int y;
	private final int z;

	public BreakBlockAction(World worldObj, int x, int y, int z) {
		this.worldObj = worldObj;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public List<EntityItem> usePlayer(OpenModsFakePlayer fakePlayer) {
		fakePlayer.inventory.currentItem = 0;
		fakePlayer.inventory.setInventorySlotContents(0, new ItemStack(Items.diamond_pickaxe, 0, 0));

		if (!worldObj.canMineBlock(fakePlayer, x, y, z)) return Lists.newArrayList();

		final Block block = worldObj.getBlock(x, y, z);
		final int metadata = worldObj.getBlockMetadata(x, y, z);

		CaptureContext dropsCapturer = DropCapture.instance.start(x, y, z);

		final List<EntityItem> drops;
		try {
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(x, y, z, worldObj, block, metadata, fakePlayer);
			if (MinecraftForge.EVENT_BUS.post(event)) return Lists.newArrayList();

			boolean canHarvest = block.canHarvestBlock(fakePlayer, metadata);

			block.onBlockHarvested(worldObj, x, y, z, metadata, fakePlayer);
			boolean canRemove = block.removedByPlayer(worldObj, fakePlayer, x, y, z, canHarvest);

			if (canRemove) {
				block.onBlockDestroyedByPlayer(worldObj, x, y, z, metadata);
				if (canHarvest) block.harvestBlock(worldObj, fakePlayer, x, y, z, metadata);
				worldObj.playAuxSFX(2001, x, y, z, Block.getIdFromBlock(block) + (metadata << 12));
			}
		} finally {
			drops = dropsCapturer.stop();
		}

		return drops;
	}
}