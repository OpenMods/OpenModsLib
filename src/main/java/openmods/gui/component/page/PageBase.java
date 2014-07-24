package openmods.gui.component.page;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import openmods.gui.component.BaseComposite;

public abstract class PageBase extends BaseComposite {

	public static final PageBase BLANK_PAGE = new PageBase() {};

	public static final ResourceLocation BOOK_TEXTURE = new ResourceLocation("openmodslib:textures/gui/book.png");

	public PageBase() {
		super(0, 0);
	}

	@Override
	public int getWidth() {
		return 220;
	}

	@Override
	public int getHeight() {
		return 200;
	}

	@Override
	protected void renderComponentBackground(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

}
