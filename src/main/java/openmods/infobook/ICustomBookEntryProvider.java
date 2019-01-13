package openmods.infobook;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;

public interface ICustomBookEntryProvider {

	class Entry {
		public final String name;
		@Nonnull
		public final ItemStack stack;

		public Entry(String name, @Nonnull ItemStack stack) {
			this.name = name;
			this.stack = stack;
		}
	}

	Iterable<Entry> getBookEntries();
}
