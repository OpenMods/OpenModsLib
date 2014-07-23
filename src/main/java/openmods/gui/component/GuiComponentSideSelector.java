package openmods.gui.component;

import java.util.EnumSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.IValueReceiver;
import openmods.gui.listener.IListenerBase;
import openmods.gui.misc.*;
import openmods.gui.misc.SidePicker.HitCoord;
import openmods.gui.misc.SidePicker.Side;
import openmods.gui.misc.Trackball.TrackballWrapper;
import openmods.utils.MathUtils;
import openmods.utils.bitmap.IReadableBitMap;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class GuiComponentSideSelector extends BaseComponent implements IValueReceiver<Set<ForgeDirection>> {

	public static interface ISideSelectedListener extends IListenerBase {
		public void onSideToggled(ForgeDirection side, boolean currentState);
	}

	private final RenderBlocks blockRender = new RenderBlocks();

	private final TrackballWrapper trackball = new TrackballWrapper(1, 40);

	public double scale;
	private ForgeDirection lastSideHovered;
	private final Set<ForgeDirection> selectedSides = EnumSet.noneOf(ForgeDirection.class);
	private boolean highlightSelectedSides = true;
	private int ticksSinceLastMouseEvent;

	private boolean isInitialized;

	private ISideSelectedListener sideSelectedListener;

	private Block block;
	private int meta;
	private TileEntity te;

	public GuiComponentSideSelector(int x, int y, double scale, Block block, int meta, TileEntity te, boolean highlightSelectedSides) {
		super(x, y);
		this.scale = scale;
		this.block = block;
		this.meta = meta;
		this.te = te;
		this.highlightSelectedSides = highlightSelectedSides;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		ticksSinceLastMouseEvent++;
		if (isInitialized == false || Mouse.isButtonDown(2)) {
			trackball.setTransform(MathUtils.createEntityRotateMatrix(minecraft.renderViewEntity));
			isInitialized = true;
		}
		GL11.glPushMatrix();
		Tessellator tessellator = Tessellator.instance;
		GL11.glTranslated(offsetX + x + (scale / 2), offsetY + y + (scale / 2), scale);
		GL11.glScaled(scale, -scale, scale);
		// TODO: replace with proper width,height
		// TODO: Get Mikee to check that I did this right -- NeverCast
		trackball.update(mouseX - getWidth(), -(mouseY - getHeight()));
		if (te != null) TileEntityRendererDispatcher.instance.renderTileEntityAt(te, -0.5, -0.5, -0.5, 0.0F);
		if (block != null) drawBlock(minecraft.renderEngine, tessellator);

		SidePicker picker = new SidePicker(0.5);

		HitCoord coord = picker.getNearestHit();

		if (coord != null) drawHighlight(tessellator, coord.side, 0x444444);

		if (highlightSelectedSides) {
			for (ForgeDirection dir : selectedSides) {
				drawHighlight(tessellator, Side.fromForgeDirection(dir), 0xCC0000);
			}
		}

		lastSideHovered = coord == null? ForgeDirection.UNKNOWN : coord.side.toForgeDirection();

		GL11.glPopMatrix();
	}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	private static void drawHighlight(Tessellator t, SidePicker.Side side, int color) {
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		t.startDrawingQuads();
		t.setColorRGBA_I(color, 64);
		switch (side) {
			case XPos:
				t.addVertex(0.5, -0.5, -0.5);
				t.addVertex(0.5, 0.5, -0.5);
				t.addVertex(0.5, 0.5, 0.5);
				t.addVertex(0.5, -0.5, 0.5);
				break;
			case YPos:
				t.addVertex(-0.5, 0.5, -0.5);
				t.addVertex(-0.5, 0.5, 0.5);
				t.addVertex(0.5, 0.5, 0.5);
				t.addVertex(0.5, 0.5, -0.5);
				break;
			case ZPos:
				t.addVertex(-0.5, -0.5, 0.5);
				t.addVertex(0.5, -0.5, 0.5);
				t.addVertex(0.5, 0.5, 0.5);
				t.addVertex(-0.5, 0.5, 0.5);
				break;
			case XNeg:
				t.addVertex(-0.5, -0.5, -0.5);
				t.addVertex(-0.5, -0.5, 0.5);
				t.addVertex(-0.5, 0.5, 0.5);
				t.addVertex(-0.5, 0.5, -0.5);
				break;
			case YNeg:
				t.addVertex(-0.5, -0.5, -0.5);
				t.addVertex(0.5, -0.5, -0.5);
				t.addVertex(0.5, -0.5, 0.5);
				t.addVertex(-0.5, -0.5, 0.5);
				break;
			case ZNeg:
				t.addVertex(-0.5, -0.5, -0.5);
				t.addVertex(-0.5, 0.5, -0.5);
				t.addVertex(0.5, 0.5, -0.5);
				t.addVertex(0.5, -0.5, -0.5);
				break;
			default:
				break;
		}
		t.draw();
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
	}

	private void drawBlock(TextureManager manager, Tessellator t) {
		GL11.glColor4f(1, 1, 1, 1);
		manager.bindTexture(TextureMap.locationBlocksTexture);
		blockRender.setRenderBoundsFromBlock(block);
		t.startDrawingQuads();

		blockRender.renderFaceXNeg(Blocks.stone, -0.5, -0.5, -0.5, block.getIcon(4, meta));
		blockRender.renderFaceXPos(Blocks.stone, -0.5, -0.5, -0.5, block.getIcon(5, meta));
		blockRender.renderFaceYPos(Blocks.stone, -0.5, -0.5, -0.5, block.getIcon(1, meta));
		blockRender.renderFaceYNeg(Blocks.stone, -0.5, -0.5, -0.5, block.getIcon(0, meta));
		blockRender.renderFaceZNeg(Blocks.stone, -0.5, -0.5, -0.5, block.getIcon(2, meta));
		blockRender.renderFaceZPos(Blocks.stone, -0.5, -0.5, -0.5, block.getIcon(3, meta));

		t.draw();
	}

	private void toggleSide(ForgeDirection side) {
		boolean wasntPresent = !selectedSides.remove(side);
		if (wasntPresent) selectedSides.add(side);
		notifyListeners(side, wasntPresent);
	}

	private void notifyListeners(final ForgeDirection side, final boolean wasntPresent) {
		if (sideSelectedListener != null) sideSelectedListener.onSideToggled(side, wasntPresent);
	}

	@Override
	public void mouseUp(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);
		if (button == 0 && ticksSinceLastMouseEvent < 10 && lastSideHovered != null && lastSideHovered != ForgeDirection.UNKNOWN) {
			toggleSide(lastSideHovered);
			ticksSinceLastMouseEvent = 0;
		}
	}

	@Override
	public void mouseDown(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);
		ticksSinceLastMouseEvent = 0;
		lastSideHovered = null;
	}

	@Override
	public int getWidth() {
		return 50;
	}

	@Override
	public int getHeight() {
		return 50;
	}

	@Override
	public void setValue(Set<ForgeDirection> dirs) {
		selectedSides.clear();
		selectedSides.addAll(dirs);
	}

	public void setValue(IReadableBitMap<ForgeDirection> dirs) {
		selectedSides.clear();

		for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			if (dirs.get(dir)) selectedSides.add(dir);
	}

	public void setListener(ISideSelectedListener sideSelectedListener) {
		this.sideSelectedListener = sideSelectedListener;
	}
}
