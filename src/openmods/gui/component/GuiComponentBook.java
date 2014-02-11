package openmods.gui.component;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import openmods.sync.SyncableString;
import openmods.utils.render.FakeIcon;

import com.google.common.collect.Lists;

public class GuiComponentBook extends BaseComponent implements IComponentListener {

	private GuiComponentSprite imgLeftBackground;
	private GuiComponentSprite imgRightBackground;
	private GuiComponentSpriteButton imgPrev;
	private GuiComponentSpriteButton imgNext;
	private GuiComponentLabel pageNumberLeft;
	private GuiComponentLabel pageNumberRight;

	private SyncableString strPageNumberLeft;
	private SyncableString strPageNumberRight;

	public static Icon iconPageLeft = FakeIcon.createSheetIcon(-45, 0, -211, 180);
	public static Icon iconPageRight = FakeIcon.createSheetIcon(0, 0, 211, 180);
	public static Icon iconPrev = FakeIcon.createSheetIcon(57, 226, 18, 10);
	public static Icon iconNext = FakeIcon.createSheetIcon(57, 213, 18, 10);
	public static Icon iconPrevHover = FakeIcon.createSheetIcon(80, 226, 18, 10);
	public static Icon iconNextHover = FakeIcon.createSheetIcon(80, 213, 18, 10);

	private static final ResourceLocation texture = new ResourceLocation("openmodslib:textures/gui/book.png");

	public List<BaseComponent> pages;

	private int index = 0;

	public GuiComponentBook() {
		super(0, 0);

		imgLeftBackground = new GuiComponentSprite(0, 0, iconPageLeft, texture);
		imgRightBackground = new GuiComponentSprite(0, 0, iconPageRight, texture);

		imgPrev = new GuiComponentSpriteButton(24, 158, iconPrev, iconPrevHover, texture);
		imgPrev.addListener(this);
		imgNext = new GuiComponentSpriteButton(380, 158, iconNext, iconNextHover, texture);
		imgNext.addListener(this);

		strPageNumberLeft = new SyncableString("[page]");
		strPageNumberRight = new SyncableString("[page]");
		pageNumberLeft = new GuiComponentLabel(85, 163, 100, 10, strPageNumberLeft);
		pageNumberLeft.setScale(0.5f);
		pageNumberRight = new GuiComponentLabel(295, 163, 100, 10, strPageNumberRight);
		pageNumberRight.setScale(0.5f);

		addComponent(imgLeftBackground);
		addComponent(imgRightBackground);
		addComponent(imgPrev);
		addComponent(imgNext);
		addComponent(pageNumberLeft);
		addComponent(pageNumberRight);

		pages = Lists.newArrayList();

	}

	@Override
	public int getWidth() {
		return iconPageRight.getIconHeight() * 2;
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
			addPage(new GuiComponentStandardRecipePage(fullName, description, video, stack));
			return true;
		}
		return false;
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
		strPageNumberLeft.setValue(String.format("Page %s of %s", index + 1, totalPageCount));
		strPageNumberRight.setValue(String.format("Page %s of %s", index + 2, totalPageCount));
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		imgRightBackground.setX(iconPageRight.getIconWidth());
		if (index + 1 < pages.size()) {
			pages.get(index + 1).setX(iconPageRight.getIconWidth());
		}
		super.render(minecraft, offsetX, offsetY, mouseX, mouseY);
	}

	@Override
	public void componentMouseDown(BaseComponent component, int offsetX, int offsetY, int button) {
		int oldIndex = index;
		if (component == imgPrev) {
			if (index > 0) {
				index -= 2;
			}
		}
		if (component == imgNext) {
			if (index < pages.size() - 2) {
				index += 2;
			}
		}
		if (oldIndex != index) {
			Minecraft mc = Minecraft.getMinecraft();
			mc.sndManager.playSoundFX("openmodslib:pageturn", 1.0F, 1.0F);
		}
		enablePages();
	}

	@Override
	public void componentMouseDrag(BaseComponent component, int offsetX, int offsetY, int button, long time) {}

	@Override
	public void componentMouseMove(BaseComponent component, int offsetX, int offsetY) {}

	@Override
	public void componentMouseUp(BaseComponent component, int offsetX, int offsetY, int button) {}

	@Override
	public void componentKeyTyped(BaseComponent component, char par1, int par2) {}
}
