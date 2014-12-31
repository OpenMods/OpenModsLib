package openmods.infobook;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import openmods.Log;
import openmods.gui.component.BaseComponent;
import openmods.gui.component.GuiComponentBook;
import openmods.gui.component.page.StandardRecipePage;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

public class PageBuilder {
	public interface StackProvider<T> {
		public ItemStack createStack(String modId, String name, T item);
	}

	private static final Map<Class<? extends ICustomBookEntryProvider>, ICustomBookEntryProvider> PROVIDERS = Maps.newHashMap();

	private final SortedMap<String, BaseComponent> pages = Maps.newTreeMap();

	private static ICustomBookEntryProvider getProvider(Class<? extends ICustomBookEntryProvider> cls) {
		ICustomBookEntryProvider provider = PROVIDERS.get(cls);

		if (provider == null) {
			try {
				provider = cls.newInstance();
			} catch (Throwable t) {
				throw Throwables.propagate(t);
			}
			PROVIDERS.put(cls, provider);

		}

		return provider;
	}

	public <T> void addPages(String modId, String type, FMLControlledNamespacedRegistry<T> registry, StackProvider<T> provider) {
		@SuppressWarnings("unchecked")
		Set<String> ids = registry.getKeys();

		Splitter splitter = Splitter.on(':');

		for (String id : ids) {
			final T obj = registry.getObject(id);
			if (obj == null) continue;

			final BookDocumentation doc;

			final Class<?> cls = obj.getClass();
			try {
				// other mods can derp here
				doc = cls.getAnnotation(BookDocumentation.class);
			} catch (Throwable t) {
				Log.warn(t, "Failed to get annotation from %s", cls);
				continue;
			}

			if (doc == null) continue;

			Iterator<String> components = splitter.split(id).iterator();
			final String itemModId = components.next();
			final String itemName = components.next();
			final Class<? extends ICustomBookEntryProvider> customProviderCls = doc.customProvider();

			if (customProviderCls == BookDocumentation.EMPTY.class) {
				final ItemStack stack = provider.createStack(itemModId, itemName, obj);
				if (stack == null) continue;
				final String customName = doc.customName();
				addPage(Strings.isNullOrEmpty(customName)? itemName : customName, itemModId.toLowerCase(), type, stack);
			} else {
				ICustomBookEntryProvider customProvider = getProvider(customProviderCls);
				for (ICustomBookEntryProvider.Entry e : customProvider.getBookEntries())
					addPage(e.name, itemModId.toLowerCase(), type, e.stack);
			}
		}
	}

	public void addPages(GuiComponentBook book) {
		for (BaseComponent page : pages.values())
			book.addPage(page);

		pages.clear();
	}

	private void addPage(String id, String modId, String type, ItemStack stack) {
		final String nameKey = getTranslationKey(id, modId, type, "name");
		final String descriptionKey = getTranslationKey(id, modId, type, "description");
		final String mediaKey = getTranslationKey(id, modId, type, "video");

		final String translatedName = StatCollector.translateToLocal(nameKey);
		pages.put(translatedName + ":" + id, new StandardRecipePage(nameKey, descriptionKey, mediaKey, stack));
	}

	protected String getTranslationKey(String name, String modId, String type, String category) {
		return String.format("%s.%s.%s.%s", type, modId, name, category);
	}

	public void addItemPages(String modId, StackProvider<Item> provider) {
		addPages(modId, "item", GameData.getItemRegistry(), provider);
	}

	public void addItemPages(final String modId) {
		addItemPages(modId, new StackProvider<Item>() {
			@Override
			public ItemStack createStack(String itemModId, String itemName, Item item) {
				return modId.equals(itemModId)? new ItemStack(item) : null;
			}
		});
	}

	public void addBlockPages(String modId, StackProvider<Block> provider) {
		addPages(modId, "tile", GameData.getBlockRegistry(), provider);
	}

	public void addBlockPages(final String modId) {
		addBlockPages(modId, new StackProvider<Block>() {
			@Override
			public ItemStack createStack(String blockModId, String blockName, Block block) {
				return modId.equals(blockModId)? new ItemStack(block) : null;
			}
		});
	}
}
