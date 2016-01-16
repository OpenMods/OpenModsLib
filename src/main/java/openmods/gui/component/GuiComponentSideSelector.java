package openmods.gui.component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import openmods.api.IValueReceiver;
import openmods.gui.IComponentParent;
import openmods.gui.listener.IListenerBase;
import openmods.gui.misc.*;
import openmods.gui.misc.SidePicker.HitCoord;
import openmods.gui.misc.SidePicker.Side;
import openmods.gui.misc.Trackball.TrackballWrapper;
import openmods.utils.FakeBlockAccess;
import openmods.utils.MathUtils;
import openmods.utils.bitmap.IReadableBitMap;
import openmods.utils.render.RenderUtils;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

public class GuiComponentSideSelector extends BaseComponent implements IValueReceiver<Set<EnumFacing>> {

	private static final double SQRT_3 = Math.sqrt(3);

	public static interface ISideSelectedListener extends IListenerBase {
		public void onSideToggled(EnumFacing side, boolean currentState);
	}

	private final TrackballWrapper trackball = new TrackballWrapper(1, 40);

	private final int diameter;
	private final double scale;
	private EnumFacing lastSideHovered;
	private final Set<EnumFacing> selectedSides = EnumSet.noneOf(EnumFacing.class);
	private boolean highlightSelectedSides = true;

	private boolean isInInitialPosition;

	private ISideSelectedListener sideSelectedListener;

	private IBlockState blockState;
	private TileEntity te;

	public GuiComponentSideSelector(IComponentParent parent, int x, int y, double scale, IBlockState blockState, TileEntity te, boolean highlightSelectedSides) {
		super(parent, x, y);
		this.scale = scale;
		this.diameter = MathHelper.ceiling_double_int(scale * SQRT_3);
		this.blockState = blockState;
		this.te = te;
		this.highlightSelectedSides = highlightSelectedSides;
	}

	@Override
	public void render(int offsetX, int offsetY, int mouseX, int mouseY) {
		if (isInInitialPosition == false || Mouse.isButtonDown(2)) {
			final Entity rve = parent.getMinecraft().getRenderViewEntity();
			trackball.setTransform(MathUtils.createEntityRotateMatrix(rve));
			isInInitialPosition = true;
		}

		final int width = getWidth();
		final int height = getWidth();
		// assumption: block is rendered in (0,0,0) - (1,1,1) coordinates
		GL11.glPushMatrix();
		GL11.glTranslatef(offsetX + x + width / 2, offsetY + y + height / 2, diameter);
		GL11.glScaled(scale, -scale, scale);
		trackball.update(mouseX - width, -(mouseY - height));
		if (te != null) TileEntityRendererDispatcher.instance.renderTileEntityAt(te, -0.5, -0.5, -0.5, 0.0F);
		if (blockState != null) drawBlock();

		SidePicker picker = new SidePicker(0.5);

		List<Pair<Side, Integer>> selections = Lists.newArrayListWithCapacity(6 + 1);
		final HitCoord coord = picker.getNearestHit();
		if (coord != null) selections.add(Pair.of(coord.side, 0x444444));

		if (highlightSelectedSides) {
			for (EnumFacing dir : selectedSides)
				selections.add(Pair.of(Side.fromForgeDirection(dir), 0xCC0000));
		}

		if (selections != null) drawHighlight(selections);

		lastSideHovered = coord == null? null : coord.side.toForgeDirection();

		GL11.glPopMatrix();
	}

	private void drawBlock() {
		final Tessellator tessellator = Tessellator.getInstance();
		final WorldRenderer wr = tessellator.getWorldRenderer();
		wr.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

		FakeBlockAccess access = new FakeBlockAccess(blockState);

		final BlockRendererDispatcher dispatcher = parent.getMinecraft().getBlockRendererDispatcher();
		final IBakedModel model = dispatcher.getModelFromBlockState(blockState, access, FakeBlockAccess.ORIGIN);
		dispatcher.getBlockModelRenderer().renderModel(access, model, blockState, FakeBlockAccess.ORIGIN, wr, false);
		wr.setTranslation(0.0D, 0.0D, 0.0D);
		tessellator.draw();
	}

	private static void drawHighlight(List<Pair<Side, Integer>> selections) {
		GlStateManager.disableLighting();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableBlend();
		GlStateManager.disableDepth();
		GlStateManager.disableTexture2D();

		GL11.glBegin(GL11.GL_QUADS);
		for (Pair<Side, Integer> p : selections) {
			final Integer color = p.getRight();
			RenderUtils.setColor(color);

			switch (p.getLeft()) {
				case XPos:
					GL11.glVertex3d(0.5, -0.5, -0.5);
					GL11.glVertex3d(0.5, 0.5, -0.5);
					GL11.glVertex3d(0.5, 0.5, 0.5);
					GL11.glVertex3d(0.5, -0.5, 0.5);
					break;
				case YPos:
					GL11.glVertex3d(-0.5, 0.5, -0.5);
					GL11.glVertex3d(-0.5, 0.5, 0.5);
					GL11.glVertex3d(0.5, 0.5, 0.5);
					GL11.glVertex3d(0.5, 0.5, -0.5);
					break;
				case ZPos:
					GL11.glVertex3d(-0.5, -0.5, 0.5);
					GL11.glVertex3d(0.5, -0.5, 0.5);
					GL11.glVertex3d(0.5, 0.5, 0.5);
					GL11.glVertex3d(-0.5, 0.5, 0.5);
					break;
				case XNeg:
					GL11.glVertex3d(-0.5, -0.5, -0.5);
					GL11.glVertex3d(-0.5, -0.5, 0.5);
					GL11.glVertex3d(-0.5, 0.5, 0.5);
					GL11.glVertex3d(-0.5, 0.5, -0.5);
					break;
				case YNeg:
					GL11.glVertex3d(-0.5, -0.5, -0.5);
					GL11.glVertex3d(0.5, -0.5, -0.5);
					GL11.glVertex3d(0.5, -0.5, 0.5);
					GL11.glVertex3d(-0.5, -0.5, 0.5);
					break;
				case ZNeg:
					GL11.glVertex3d(-0.5, -0.5, -0.5);
					GL11.glVertex3d(-0.5, 0.5, -0.5);
					GL11.glVertex3d(0.5, 0.5, -0.5);
					GL11.glVertex3d(0.5, -0.5, -0.5);
					break;
				default:
					break;
			}
		}
		GL11.glEnd();

		GlStateManager.disableBlend();
		GlStateManager.enableDepth();
		GlStateManager.enableTexture2D();
	}

	private void toggleSide(EnumFacing side) {
		boolean wasntPresent = !selectedSides.remove(side);
		if (wasntPresent) selectedSides.add(side);
		notifyListeners(side, wasntPresent);
	}

	private void notifyListeners(EnumFacing side, boolean wasntPresent) {
		if (sideSelectedListener != null) sideSelectedListener.onSideToggled(side, wasntPresent);
	}

	@Override
	public void mouseUp(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);
		if (button == 0 && lastSideHovered != null) {
			toggleSide(lastSideHovered);
		}
	}

	@Override
	public void mouseDown(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);
		lastSideHovered = null;
	}

	@Override
	public int getWidth() {
		return diameter;
	}

	@Override
	public int getHeight() {
		return diameter;
	}

	@Override
	public void setValue(Set<EnumFacing> dirs) {
		selectedSides.clear();
		selectedSides.addAll(dirs);
	}

	public void setValue(IReadableBitMap<EnumFacing> dirs) {
		selectedSides.clear();

		for (EnumFacing dir : EnumFacing.VALUES)
			if (dirs.get(dir)) selectedSides.add(dir);
	}

	public void setListener(ISideSelectedListener sideSelectedListener) {
		this.sideSelectedListener = sideSelectedListener;
	}
}
