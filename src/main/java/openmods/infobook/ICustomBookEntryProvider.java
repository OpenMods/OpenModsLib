package openmods.infobook;

import net.minecraft.item.ItemStack;

public interface ICustomBookEntryProvider {

	public class Entry {
		public final String name;
		public final ItemStack stack;

		public Entry(String name, ItemStack stack) {
			this.name = name;
			this.stack = stack;
		}
	}

	public Iterable<Entry> getBookEntries();
}
