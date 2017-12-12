package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandlerModifiable;
import openmods.sync.ISyncListener;
import openmods.sync.ISyncableObject;
import openmods.utils.bitmap.IReadableBitMap;

public class SidedItemHandlerAdapter {

	private final IItemHandlerModifiable inventory;

	private class SlotConfig {
		private final int index;
		private final IReadableBitMap<EnumFacing> reachability;
		private final boolean canInsert;
		private final boolean canExtract;

		private SlotConfig(int index, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
			this.index = index;
			this.reachability = sideFlags;
			this.canInsert = canInsert;
			this.canExtract = canExtract;
		}

		private boolean canAccessFromSide(EnumFacing side) {
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
		public ItemStack getStackInSlot(int slot) {
			final SlotConfig mappedSlot = slots.get(slot);
			return inventory.getStackInSlot(mappedSlot.index);
		}

		@Override
		public void setStackInSlot(int slot, ItemStack stack) {
			final SlotConfig mappedSlot = slots.get(slot);
			inventory.setStackInSlot(mappedSlot.index, stack);
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			final SlotConfig mappedSlot = slots.get(slot);
			if (!mappedSlot.canInsert) return stack;

			return inventory.insertItem(mappedSlot.index, stack, simulate);
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			final SlotConfig mappedSlot = slots.get(slot);
			if (!mappedSlot.canExtract) return null;

			return inventory.extractItem(mappedSlot.index, amount, simulate);
		}
	}

	private class SideHandler {
		private final EnumFacing side;

		private boolean isValid;
		private ItemHandler value;

		public SideHandler(EnumFacing side) {
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

	private final Map<EnumFacing, SideHandler> handlers;

	{
		final Map<EnumFacing, SideHandler> handlers = Maps.newEnumMap(EnumFacing.class);
		for (EnumFacing side : EnumFacing.VALUES)
			handlers.put(side, new SideHandler(side));

		this.handlers = Collections.unmodifiableMap(handlers);
	}

	private final SideHandler selfHandler = new SideHandler(null);

	// this map is only here to ensure we have at most one config for slot
	private final Map<Integer, SlotConfig> slots = Maps.newHashMap();

	public SidedItemHandlerAdapter(IItemHandlerModifiable inventory) {
		this.inventory = inventory;
	}

	public IItemHandlerModifiable getHandler(EnumFacing side) {
		return (side != null? handlers.get(side) : selfHandler).get();
	}

	public boolean hasHandler(EnumFacing side) {
		return getHandler(side) != null;
	}

	private ItemHandler createHandlerForSide(EnumFacing side) {
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
		return new ISyncListener() {
			@Override
			public void onSync(Set<ISyncableObject> changes) {
				invalidate();
			}
		};
	}

	public void registerSlots(int start, int count, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = start; i < start + count; i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	public void registerAllSlots(IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		for (int i = 0; i < inventory.getSlots(); i++)
			registerSlot(i, sideFlags, canInsert, canExtract);
	}

	public void registerSlot(Enum<?> slot, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		registerSlot(slot.ordinal(), sideFlags, canInsert, canExtract);
	}

	public void registerSlot(int slot, IReadableBitMap<EnumFacing> sideFlags, boolean canInsert, boolean canExtract) {
		final int sizeInventory = inventory.getSlots();
		Preconditions.checkArgument(slot >= 0 && slot < sizeInventory, "Tried to register invalid slot: %s (inventory size: %s)", slot, sizeInventory);
		slots.put(slot, new SlotConfig(slot, sideFlags, canInsert, canExtract));
	}

}
