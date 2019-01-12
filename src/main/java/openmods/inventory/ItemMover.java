package openmods.inventory;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import openmods.utils.CollectionUtils;
import openmods.utils.InventoryUtils;

public class ItemMover {

	private Set<EnumFacing> sides;

	private boolean randomizeSides = false;

	private boolean breakAfterFirstTry = false;

	private int maxSize = 64;

	private final World world;

	private final BlockPos pos;

	public ItemMover(World world, BlockPos pos) {
		this.world = world;
		this.pos = pos;
	}

	public ItemMover setSides(Set<EnumFacing> sides) {
		this.sides = sides;
		return this;
	}

	public ItemMover randomizeSides() {
		this.randomizeSides = true;
		return this;
	}

	public ItemMover breakAfterFirstTry() {
		this.breakAfterFirstTry = true;
		return this;
	}

	public ItemMover setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		return this;
	}

	private Collection<IItemHandler> findNeighbours() {
		if (sides.isEmpty()) return Collections.emptyList();

		if (breakAfterFirstTry) {
			final EnumFacing selectedSide = randomizeSides? CollectionUtils.getRandom(sides) : CollectionUtils.getFirst(sides);
			final IItemHandler neighbour = InventoryUtils.tryGetHandler(world, pos.offset(selectedSide), selectedSide.getOpposite());
			return neighbour != null? Collections.singletonList(neighbour) : Collections.<IItemHandler> emptyList();
		}

		Collection<EnumFacing> sidesToCheck = sides;
		if (randomizeSides) {
			final List<EnumFacing> tmp = Lists.newArrayList(sides);
			Collections.shuffle(tmp);
			sidesToCheck = tmp;
		}

		final List<IItemHandler> handlers = Lists.newArrayList();
		for (EnumFacing side : sidesToCheck) {
			final IItemHandler neighbour = InventoryUtils.tryGetHandler(world, pos.offset(side), side.getOpposite());
			if (neighbour != null) handlers.add(neighbour);
		}

		return handlers;
	}

	public int pullToSlot(IItemHandler target, int targetSlot) {
		return pullToSlot(target, targetSlot, maxSize, findNeighbours());
	}

	// extracted for testing
	static int pullToSlot(IItemHandler target, int targetSlot, int maxSize, Iterable<IItemHandler> sources) {
		int transferedAmount = 0;
		MAIN:
		for (IItemHandler source : sources) {
			for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
				final ItemStack stackToPull = source.getStackInSlot(sourceSlot);
				if (stackToPull.isEmpty()) continue;

				final ItemStack leftover = target.insertItem(targetSlot, stackToPull, true);
				if (leftover.getCount() < stackToPull.getCount()) {
					final int leftoverAmount = leftover.getCount();
					final int amountToExtract = Math.min(maxSize - transferedAmount, stackToPull.getCount() - leftoverAmount);
					final ItemStack extractedItem = source.extractItem(sourceSlot, amountToExtract, false);
					if (!extractedItem.isEmpty()) {
						// don't care about results here, since target already declared space
						target.insertItem(targetSlot, extractedItem, false);
						transferedAmount += amountToExtract;
					}
				}

				final ItemStack targetContents = target.getStackInSlot(targetSlot);
				if (targetContents != null && targetContents.getCount() >= targetContents.getMaxStackSize()) break MAIN;
			}
		}

		return transferedAmount;
	}

	public int pushFromSlot(IItemHandler source, int sourceSlot) {
		return pushFromSlot(source, sourceSlot, maxSize, findNeighbours());
	}

	// extracted for testing
	static int pushFromSlot(IItemHandler source, int sourceSlot, int maxSize, Iterable<IItemHandler> targets) {
		int transferedAmount = 0;
		MAIN:
		for (IItemHandler target : targets) {
			ItemStack stackToPush = source.getStackInSlot(sourceSlot);
			for (int targetSlot = 0; targetSlot < target.getSlots(); targetSlot++) {
				if (stackToPush.isEmpty()) break MAIN;

				final ItemStack leftover = target.insertItem(targetSlot, stackToPush, true);
				if (leftover.getCount() < stackToPush.getCount()) {
					final int leftoverAmount = leftover.getCount();
					final int amountToExtract = Math.min(maxSize - transferedAmount, stackToPush.getCount() - leftoverAmount);
					final ItemStack extractedItem = source.extractItem(sourceSlot, amountToExtract, false);
					if (!extractedItem.isEmpty()) {
						target.insertItem(targetSlot, extractedItem, false);
						transferedAmount += extractedItem.getCount();
						stackToPush = source.getStackInSlot(sourceSlot);
					}
				}
			}
		}

		return transferedAmount;
	}

}
