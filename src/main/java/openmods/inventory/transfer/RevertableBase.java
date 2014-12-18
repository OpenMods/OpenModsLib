package openmods.inventory.transfer;

import net.minecraft.item.ItemStack;

import com.google.common.base.Preconditions;

public abstract class RevertableBase implements IRevertable {

	private static class Holder {
		private final ItemStack value;

		public Holder(ItemStack value) {
			this.value = value;
		}
	}

	private boolean changed;
	private Holder originalStack;
	private Holder modifedStack;

	@Override
	public boolean commit() {
		if (changed) {
			Preconditions.checkState(modifedStack != null);
			final ItemStack newStack = modifedStack.value;
			if (newStack == null || newStack.stackSize <= 0) setStack(null);
			else setStack(newStack);
			reset();
			markDirty();
			return true;
		}

		return false;
	}

	@Override
	public boolean hasChanges() {
		return changed;
	}

	private void reset() {
		modifedStack = null;
		originalStack = null;
		changed = false;
	}

	@Override
	public void abort() {
		reset();
	}

	private boolean isSameObject(ItemStack stack) {
		if (originalStack == null || stack == null) return false;
		return originalStack.value == stack;
	}

	protected void setCachedStack(ItemStack stack) {
		Preconditions.checkState(!isSameObject(stack));
		modifedStack = new Holder(stack);
		this.changed = true;
	}

	protected ItemStack getCachedStack() {
		if (modifedStack != null) return modifedStack.value;

		if (originalStack == null) originalStack = new Holder(getStack());
		return originalStack.value;
	}

	protected ItemStack getModifiableStack() {
		if (modifedStack != null) return modifedStack.value;

		ItemStack original = getCachedStack();
		final ItemStack copy = ItemStack.copyItemStack(original);
		modifedStack = new Holder(copy);
		return copy;
	}

	protected abstract ItemStack getStack();

	protected abstract void markDirty();

	protected abstract void setStack(ItemStack stack);

}
