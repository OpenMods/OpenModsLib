package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandlerModifiable;
import openmods.sync.ISyncListener;
import openmods.utils.bitmap.IReadableBitMap;

public class SidedItemHandlerAdapter {

	private final IItemHandlerModifiable inventory;

	private class SlotConfig {
		private final int index;
		private final IReadableBitMap<Direction> reachability;
		private final boolean canInsert;
		private final boolean canExtract;

		private SlotConfig(int index, IReadableBitMap<Direction> sideFlags, boolean canInsert, boolean canExtract) {
			this.index = index;
			this.reachability = sideFlags;
			this.canInsert = canInsert;
			this.canExtract = canExtract;
		}

		private boolean canAccessFromSide(Direction side) {
			return side == null || reachability.get(side);
		}
	}

	private class ItemHandler implements IItemHandlerModifiable {

		private final List<SlotConfig> slots;

		public ItemHandler(List<SlotConfig> slots) {
			this.slots = ImmutableList.copyOf(slots);
		}

		@Override
		public int getSlots() {
			return slots.size();
		}

		@Override
		@Nonnull
		public ItemStack getStackInSlot(int slot) {
			final SlotConfig mappedSlot = slots.get(slot);
			return inventory.getStackInSlot(mappedSlot.index);
		}

		@Override
		public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
			final SlotConfig mappedSlot = slots.get(slot);
			inventory.setStackInSlot(mappedSlot.index, stack);
		}

		@Override
		@Nonnull
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			final SlotConfig mappedSlot = slots.get(slot);
			if (!mappedSlot.canInsert) return stack;

			return inventory.insertItem(mappedSlot.index, stack, simulate);
		}

		@Override
		@Nonnull
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			final SlotConfig mappedSlot = slots.get(slot);
			if (!mappedSlot.canExtract) return ItemStack.EMPTY;

			return inventory.extractItem(mappedSlot.index, amount, simulate);
		}

		@Override
		public int getSlotLimit(int slot) {
			return inventory.getSlotLimit(slot);
		}

		@Override
		public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
			return inventory.isItemValid(slot, stack);
		}
	}

	private class SideHandler {
		private final Direction side;

		private boolean isValid;
		private ItemHandler value;

		public SideHandler(Direction side) {
			this.side = side;
		}

		public ItemHandler get() {
			if (isValid)
				return value;

			value = createHandlerForSide(side);
			isValid = true;
			return value;
		}

		public void invalidate() {
			isValid = false;
			value = null;
		}
	}

	private final Map<Direction, SideHandler> handlers;

	{
		final Map<Direction, SideHandler> handlers = Maps.newEnumMap(Direction.class);
		for (Direction side : Direction.values())
			handlers.put(side, new SideHandler(side));

		this.handlers = Collections.unmodifiableMap(handlers);
	}

	private final SideHandler selfHandler = new SideHandler(null);

	// this map is only here to ensure we have at most one config for slot
	private final Map<Integer, SlotConfig> slots = Maps.newHashMap();

	public SidedItemHandlerAdapter(IItemHandlerModifiable inventory) {
		this.inventory = inventory;
	}

	public IItemHandlerModifiable getHandler(Direction side) {
		return (side != null? handlers.get(side) : selfHandler).get();
	}

	public boolean hasHandler(Direction side) {
		return getHandler(side) != null;
	}

	private ItemHandler createHandlerForSide(Direction side) {
		final List<SlotConfig> sideConfig = Lists.newArrayListWithCapacity(slots.size());
		for (SlotConfig config : slots.values())
			if (config.canAccessFromSide(side))
				sideConfig.add(config);

		return sideConfig.isEmpty()? null : new ItemHandler(sideConfig);
	}

	public void invalidate() {
		for (SideHandler handler : handlers.values())
			handler.invalidate();

		selfHandler.invalidate();
	}

	public ISyncListener createSyncListener() {
		return changes -> invalidate();
	}

	public void registerSlots(int start, int count, IReadableBitMap<Direction> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = start; i < start + count; i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	public void registerAllSlots(IReadableBitMap<Direction> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = 0; i < inventory.getSlots(); i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	public void registerSlot(Enum<?> slot, IReadableBitMap<Direction> sideFlags, boolean canInsert, boolean canExtract) {
		registerSlot(slot.ordinal(), sideFlags, canInsert, canExtract);
	}

	public void registerSlot(int slot, IReadableBitMap<Direction> sideFlags, boolean canInsert, boolean canExtract) {
		final int sizeInventory = inventory.getSlots();
		Preconditions.checkArgument(slot >= 0 && slot < sizeInventory, "Tried to register invalid slot: %s (inventory size: %s)", slot, sizeInventory);
		slots.put(slot, new SlotConfig(slot, sideFlags, canInsert, canExtract));
	}

}
