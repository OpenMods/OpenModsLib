package openmods.gui.component;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import openmods.gui.component.page.StandardRecipePage;
import openmods.gui.listener.IMouseDownListener;
import openmods.utils.render.FakeIcon;

import com.google.common.collect.Lists;

public class GuiComponentBook extends BaseComposite {

	private static final ResourceLocation PAGETURN = new ResourceLocation("openmodslib", "pageturn");

	private GuiComponentSprite imgLeftBackground;
	private GuiComponentSprite imgRightBackground;
	private GuiComponentSpriteButton imgPrev;
	private GuiComponentSpriteButton imgNext;
	private GuiComponentLabel pageNumberLeft;
	private GuiComponentLabel pageNumberRight;

	public static IIcon iconPageLeft = FakeIcon.createSheetIcon(211, 0, -211, 180);
	public static IIcon iconPageRight = FakeIcon.createSheetIcon(0, 0, 211, 180);
	public static IIcon iconPrev = FakeIcon.createSheetIcon(57, 226, 18, 10);
	public static IIcon iconNext = FakeIcon.createSheetIcon(57, 213, 18, 10);
	public static IIcon iconPrevHover = FakeIcon.createSheetIcon(80, 226, 18, 10);
	public static IIcon iconNextHover = FakeIcon.createSheetIcon(80, 213, 18, 10);

	private static final ResourceLocation texture = new ResourceLocation("openmodslib:textures/gui/book.png");

	public List<BaseComponent> pages;

	private int index = 0;

	public GuiComponentBook() {
		super(0, 0);

		imgLeftBackground = new GuiComponentSprite(0, 0, iconPageLeft, texture);
		imgRightBackground = new GuiComponentSprite(0, 0, iconPageRight, texture);

		imgRightBackground.setX(iconPageRight.getIconWidth());

		imgPrev = new GuiComponentSpriteButton(24, 158, iconPrev, iconPrevHover, texture);
		imgPrev.setListener(new IMouseDownListener() {
			@Override
			public void componentMouseDown(BaseComponent component, int x, int y, int button) {
				if (index > 0) changePage(index - 2);
				enablePages();
			}
		});
		imgNext = new GuiComponentSpriteButton(380, 158, iconNext, iconNextHover, texture);
		imgNext.setListener(new IMouseDownListener() {
			@Override
			public void componentMouseDown(BaseComponent component, int x, int y, int button) {
				if (index < pages.size() - 2) changePage(index + 2);
				enablePages();
			}
		});

		pageNumberLeft = new GuiComponentLabel(85, 163, 100, 10, "XXX");
		pageNumberLeft.setScale(0.5f);
		pageNumberRight = new GuiComponentLabel(295, 163, 100, 10, "XXX");
		pageNumberRight.setScale(0.5f);

		addComponent(imgLeftBackground);
		addComponent(imgRightBackground);
		addComponent(imgPrev);
		addComponent(imgNext);
		addComponent(pageNumberLeft);
		addComponent(pageNumberRight);

		pages = Lists.newArrayList();

	}

	public boolean gotoPage(BaseComponent page) {
		int pageIndex = pages.indexOf(page);
		if (pageIndex > -1) {
			index = pageIndex % 2 == 1? pageIndex - 1 : pageIndex;
			enablePages();
			return true;
		}
		return false;
	}

	public int getNumberOfPages() {
		return pages.size();
	}

	@Override
	public int getWidth() {
		return iconPageRight.getIconWidth() * 2;
	}

	@Override
	public int getHeight() {
		return iconPageRight.getIconHeight();
	}

	public void addPage(BaseComponent page) {
		addComponent(page);
		page.setEnabled(false);
		pages.add(page);
	}

	public boolean addStandardRecipePage(String modId, String name, Object item) {
		ItemStack stack = null;
		String type = "";
		if (item instanceof ItemStack) {
			stack = (ItemStack)item;
			type = (stack.getItem() instanceof ItemBlock)? "tile" : "item";
		}
		if (item instanceof Item) {
			stack = new ItemStack((Item)item);
			type = "item";
		} else if (item instanceof Block) {
			stack = new ItemStack((Block)item);
			type = "tile";
		}
		if (stack != null) {
			String fullName = String.format("%s.%s.%s.name", type, modId, name);
			String description = String.format("%s.%s.%s.description", type, modId, name);
			String video = String.format("%s.%s.%s.video", type, modId, name);
			addPage(new StandardRecipePage(fullName, description, video, stack));
			return true;
		}
		return false;
	}

	public void gotoIndex(int i) {
		index = i;
		enablePages();
	}

	public void enablePages() {
		int i = 0;
		for (BaseComponent page : pages) {
			page.setEnabled(i == index || i == index + 1);
			i++;
		}

		int totalPageCount = i % 2 == 0? i : i + 1;

		imgNext.setEnabled(index < pages.size() - 2);
		imgPrev.setEnabled(index > 0);
		pageNumberLeft.setText(StatCollector.translateToLocalFormatted("openblocks.misc.page", index + 1, totalPageCount));
		pageNumberRight.setText(StatCollector.translateToLocalFormatted("openblocks.misc.page", index + 2, totalPageCount));
	}

	@Override
	public void renderComponentBackground(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		if (index + 1 < pages.size()) {
			pages.get(index + 1).setX(iconPageRight.getIconWidth());
		}
	}

	private void changePage(int newPage) {
		if (newPage != index) {
			index = newPage;
			enablePages();
			Minecraft mc = Minecraft.getMinecraft();
			mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(PAGETURN, 1.0f));
		}
	}

}
