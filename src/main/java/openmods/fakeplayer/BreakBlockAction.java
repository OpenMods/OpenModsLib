package openmods.fakeplayer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openmods.LibConfig;
import openmods.Log;
import openmods.config.properties.ConfigurationChange;
import openmods.fakeplayer.FakePlayerPool.PlayerUserReturning;
import openmods.world.DropCapture;
import openmods.world.DropCapture.CaptureContext;

public class BreakBlockAction implements PlayerUserReturning<List<EntityItem>> {
	private final World worldObj;
	private final BlockPos blockPos;

	@Nonnull
	private ItemStack stackToUse;

	private boolean findEffectiveTool;

	@EventBusSubscriber
	private static class ConfigAccess {
		private static ItemStack[] probeTools;

		@SubscribeEvent
		public static void onConfigUpdate(ConfigurationChange evt) {
			if (evt.check("feature", "fakePlayerBlockBreakTools")) {
				probeTools = null;
				effectiveToolCache.invalidateAll();
			}
		}

		public static ItemStack[] probeTools() {
			if (probeTools == null) {
				final List<ItemStack> items = Lists.newArrayList();
				for (String itemId : LibConfig.toolProbes) {
					final Item item = Item.REGISTRY.getObject(new ResourceLocation(itemId));
					if (item != null) {
						items.add(createToolStack(item));
					} else {
						Log.warn("Failed to find item: %s", itemId);
					}
				}
				probeTools = items.toArray(new ItemStack[items.size()]);
			}

			return probeTools;
		}
	}

	private static Cache<IBlockState, ItemStack> effectiveToolCache = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.HOURS)
			.build();

	public BreakBlockAction(World worldObj, BlockPos blockPos) {
		this.worldObj = worldObj;
		this.blockPos = blockPos;
		this.stackToUse = createToolStack(Items.DIAMOND_PICKAXE);
	}

	private static ItemStack createToolStack(Item tool) {
		return new ItemStack(tool, 1, 0);
	}

	public BreakBlockAction setStackToUse(@Nonnull ItemStack stack) {
		this.stackToUse = stack;
		return this;
	}

	public BreakBlockAction findEffectiveTool() {
		this.findEffectiveTool = true;
		return this;
	}

	private void selectTool(IBlockState state, OpenModsFakePlayer fakePlayer) {
		if (findEffectiveTool) {
			final ItemStack optimalTool = effectiveToolCache.getIfPresent(state);

			if (optimalTool != null) {
				setPlayerTool(fakePlayer, optimalTool);
			} else {
				for (ItemStack tool : ConfigAccess.probeTools()) {
					setPlayerTool(fakePlayer, tool);

					if (ForgeHooks.canHarvestBlock(state.getBlock(), fakePlayer, worldObj, blockPos)) {
						effectiveToolCache.put(state, tool);
						return;
					}
				}

				// default clause - use most universal one
				final ItemStack fallbackTool = createToolStack(Items.DIAMOND_PICKAXE);
				effectiveToolCache.put(state, fallbackTool);
				setPlayerTool(fakePlayer, fallbackTool);
			}
		} else {
			setPlayerTool(fakePlayer, stackToUse);
		}
	}

	private static void setPlayerTool(OpenModsFakePlayer fakePlayer, final ItemStack tool) {
		fakePlayer.inventory.setInventorySlotContents(0, tool.copy());
	}

	private boolean removeBlock(EntityPlayer player, BlockPos pos, IBlockState state, boolean canHarvest) {
		final Block block = state.getBlock();
		final boolean result = block.removedByPlayer(state, worldObj, pos, player, canHarvest);
		if (result) block.onBlockDestroyedByPlayer(worldObj, pos, state);
		return result;
	}

	@Override
	public List<EntityItem> usePlayer(OpenModsFakePlayer fakePlayer) {
		if (!worldObj.isBlockModifiable(fakePlayer, blockPos)) return Lists.newArrayList();

		// this mirrors PlayerInteractionManager.tryHarvestBlock
		final IBlockState state = worldObj.getBlockState(blockPos);

		fakePlayer.inventory.currentItem = 0;
		selectTool(state, fakePlayer);

		final CaptureContext dropsCapturer = DropCapture.instance.start(blockPos);

		final List<EntityItem> drops;
		try {
			BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(worldObj, blockPos, state, fakePlayer);
			if (MinecraftForge.EVENT_BUS.post(event)) return Lists.newArrayList();

			final TileEntity te = worldObj.getTileEntity(blockPos); // OHHHHH YEEEEAAAH

			boolean canHarvest = state.getBlock().canHarvestBlock(worldObj, blockPos, fakePlayer);
			boolean isRemoved = removeBlock(fakePlayer, blockPos, state, canHarvest);
			if (isRemoved && canHarvest) {
				state.getBlock().harvestBlock(worldObj, fakePlayer, blockPos, state, te, stackToUse);
				worldObj.playEvent(fakePlayer, 2001, blockPos, Block.getStateId(state));
			}

		} finally {
			drops = dropsCapturer.stop();
		}

		return drops;
	}
}