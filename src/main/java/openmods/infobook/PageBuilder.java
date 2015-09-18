package openmods.infobook;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import openmods.Log;
import openmods.gui.component.BaseComponent;
import openmods.gui.component.GuiComponentBook;
import openmods.gui.component.page.ItemStackTocPage;
import openmods.gui.component.page.StandardRecipePage;
import openmods.gui.listener.IMouseDownListener;
import openmods.utils.CachedInstanceFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;

public class PageBuilder {
	public interface StackProvider<T> {
		public ItemStack createStack(String modId, String name, T item);
	}

	private static final CachedInstanceFactory<ICustomBookEntryProvider> PROVIDERS = CachedInstanceFactory.create();

	private static class Entry {
		public final BaseComponent page;
		public final ItemStack stack;

		public Entry(BaseComponent page, ItemStack stack) {
			this.page = page;
			this.stack = stack;
		}
	}

	private final SortedMap<String, Entry> pages = Maps.newTreeMap();

	private Set<String> modIds;

	private List<ItemStackTocPage> tocPages;

	public <T> void addPages(String type, FMLControlledNamespacedRegistry<T> registry, StackProvider<T> provider) {
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

			if (modIds != null && !modIds.contains(itemModId)) continue;

			final String itemName = components.next();
			final Class<? extends ICustomBookEntryProvider> customProviderCls = doc.customProvider();

			if (customProviderCls == BookDocumentation.EMPTY.class) {
				final ItemStack stack = provider.createStack(itemModId, itemName, obj);
				if (stack == null) continue;
				final String customName = doc.customName();
				addPage(Strings.isNullOrEmpty(customName)? itemName : customName, itemModId.toLowerCase(Locale.ENGLISH), type, stack);
			} else {
				ICustomBookEntryProvider customProvider = PROVIDERS.getOrCreate(customProviderCls);
				for (ICustomBookEntryProvider.Entry e : customProvider.getBookEntries())
					addPage(e.name, itemModId.toLowerCase(Locale.ENGLISH), type, e.stack);
			}
		}
	}

	public void insertTocPages(GuiComponentBook book, int rows, int columns, float scale) {
		Preconditions.checkState(tocPages == null, "Table Of Contents page already added");
		tocPages = Lists.newArrayList();

		int tocEntriesCount = pages.size();
		while (tocEntriesCount > 0) {
			ItemStackTocPage page = new ItemStackTocPage(rows, columns, scale);
			tocEntriesCount -= page.getCapacity();
			tocPages.add(page);
			book.addPage(page);
		}
	}

	public void insertPages(GuiComponentBook book) {
		for (Entry e : pages.values()) {
			if (tocPages != null) {
				final int target = book.getNumberOfPages();
				addToToc(book, e.stack, target);
			}

			book.addPage(e.page);
		}
	}

	private void addToToc(final GuiComponentBook book, ItemStack stack, final int target) {
		for (ItemStackTocPage tocPage : tocPages)
			if (tocPage.addEntry(stack, new IMouseDownListener() {
				@Override
				public void componentMouseDown(BaseComponent component, int x, int y, int button) {
					book.changePage(target);
				}
			})) return;

		throw new IllegalStateException(String.format("Tried to add more TOC entries than allocated"));
	}

	private void addPage(String id, String modId, String type, ItemStack stack) {
		final String nameKey = getTranslationKey(id, modId, type, "name");
		final String descriptionKey = getTranslationKey(id, modId, type, "description");
		final String mediaKey = getTranslationKey(id, modId, type, "video");

		final String translatedName = StatCollector.translateToLocal(nameKey);
		final StandardRecipePage page = new StandardRecipePage(nameKey, descriptionKey, mediaKey, stack);
		pages.put(translatedName + ":" + id, new Entry(page, stack));
	}

	protected String getTranslationKey(String name, String modId, String type, String category) {
		return String.format("%s.%s.%s.%s", type, modId, name, category);
	}

	public void includeModId(String modid) {
		if (modIds == null) modIds = Sets.newHashSet();
		modIds.add(modid);
	}

	public void addItemPages(StackProvider<Item> provider) {
		addPages("item", GameData.getItemRegistry(), provider);
	}

	public void createItemPages() {
		addItemPages(new StackProvider<Item>() {
			@Override
			public ItemStack createStack(String itemModId, String itemName, Item item) {
				return new ItemStack(item);
			}
		});
	}

	public void addBlockPages(StackProvider<Block> provider) {
		addPages("tile", GameData.getBlockRegistry(), provider);
	}

	public void createBlockPages() {
		addBlockPages(new StackProvider<Block>() {
			@Override
			public ItemStack createStack(String blockModId, String blockName, Block block) {
				return new ItemStack(block);
			}
		});
	}
}
