package openmods.infobook;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;

public interface ICustomBookEntryProvider {

	public class Entry {
		public final String name;
		@Nonnull
		public final ItemStack stack;

		public Entry(String name, @Nonnull ItemStack stack) {
			this.name = name;
			this.stack = stack;
		}
	}

	public Iterable<Entry> getBookEntries();
}
