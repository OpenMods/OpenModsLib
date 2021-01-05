package openmods.gui.component;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import openmods.api.IValueReceiver;
import openmods.gui.listener.IListenerBase;
import openmods.gui.misc.SidePicker;
import openmods.gui.misc.SidePicker.HitCoord;
import openmods.gui.misc.SidePicker.Side;
import openmods.gui.misc.Trackball.TrackballWrapper;
import openmods.utils.FakeBlockAccess;
import openmods.utils.MathUtils;
import openmods.utils.bitmap.IReadableBitMap;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public class GuiComponentSideSelector extends BaseComponent implements IValueReceiver<Set<Direction>> {

	private static final double SQRT_3 = Math.sqrt(3);

	@FunctionalInterface
	public interface ISideSelectedListener extends IListenerBase {
		void onSideToggled(Direction side, boolean currentState);
	}

	private final TrackballWrapper trackball = new TrackballWrapper(40);

	private final int diameter;
	private final float scale;
	private Direction lastSideHovered;
	private final Set<Direction> selectedSides = EnumSet.noneOf(Direction.class);
	private final boolean highlightSelectedSides;
	private boolean isDragging;

	private boolean setToInitialPosition = true;

	private ISideSelectedListener sideSelectedListener;

	private final BlockState blockState;
	private final TileEntity te;
	private final FakeBlockAccess access;

	public GuiComponentSideSelector(int x, int y, float scale, BlockState blockState, TileEntity te, boolean highlightSelectedSides) {
		super(x, y);
		this.scale = scale;
		this.diameter = MathHelper.ceil(scale * SQRT_3);
		this.blockState = blockState;
		this.te = te;
		this.access = new FakeBlockAccess(blockState, te);
		this.highlightSelectedSides = highlightSelectedSides;
	}

	@Override
	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		final Minecraft minecraft = parent.getMinecraft();
		if (setToInitialPosition) {
			final Entity rve = minecraft.getRenderViewEntity();
			trackball.setTransform(MathUtils.createEntityRotateMatrix(rve));
			setToInitialPosition = false;
		}

		final int width = getWidth();
		final int height = getHeight();
		// assumption: block is rendered in (0,0,0) - (1,1,1) coordinates
		matrixStack.push();
		final Matrix4f cubeTransform = new Matrix4f();
		cubeTransform.setIdentity();
		cubeTransform.mul(Matrix4f.makeTranslate((float)((double)(offsetX + x + width / 2)), (float)((double)(offsetY + y + height / 2)), (float)(double)diameter));
		cubeTransform.mul(Matrix4f.makeScale(scale, -scale, scale));
		trackball.update(cubeTransform, mouseX - width, -(mouseY - height), isDragging);

		matrixStack.getLast().getMatrix().mul(cubeTransform);

		parent.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.enableTexture();
		GlStateManager.enableDepthTest();

		// TODO 1.16 Figure out TESR rendering
		//if (te != null) TileEntityRendererDispatcher.instance.render(te, -0.5, -0.5, -0.5, 0.0F);

		parent.bindTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
		if (blockState != null) {
			final IModelData modelData = te != null? te.getModelData() : EmptyModelData.INSTANCE;
			drawBlock(matrixStack, modelData);
		}

		SidePicker picker = new SidePicker(0.5f);

		final HitCoord coord;
		List<Pair<Side, Integer>> selections = Lists.newArrayListWithCapacity(6 + 1);
		if (!isDragging) {
			Matrix4f invPose = cubeTransform.copy();
			invPose.invert();
			int mx = offsetX + mouseX;
			int my = offsetY + mouseY;
			final Vector4f near = createCursorVector(invPose, mx, my, 1000);
			final Vector4f far = createCursorVector(invPose, mx, my, -1000);

			coord = picker.getNearestHit(new Vector3f(near.getX(), near.getY(), near.getZ()), new Vector3f(far.getX(), far.getY(), far.getZ()));
			if (coord != null) {
				selections.add(Pair.of(coord.side, 0x444444));
			}
		} else {
			coord = null;
		}

		if (highlightSelectedSides) {
			for (Direction dir : selectedSides) {
				selections.add(Pair.of(Side.fromForgeDirection(dir), 0xCC0000));
			}
		}

		drawHighlight(matrixStack, selections);

		lastSideHovered = coord == null? null : coord.side.toForgeDirection();

		matrixStack.pop();
	}

	private Vector4f createCursorVector(Matrix4f invPose, int mouseX, int mouseY, int depth) {
		final Vector4f result = new Vector4f(mouseX, mouseY, depth, 1);
		result.transform(invPose);
		float w = result.getW();
		result.set(result.getX() / w, result.getY() / w, result.getZ() / w, 1.0f);
		return result;
	}

	private void drawBlock(MatrixStack matrixStack, IModelData modelData) {
		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder wr = tessellator.getBuffer();
		final BlockRendererDispatcher dispatcher = parent.getMinecraft().getBlockRendererDispatcher();
		matrixStack.push();
		matrixStack.translate(-0.5f, -0.5f, -0.5f);

		for (RenderType layer : RenderType.getBlockRenderTypes()) {
			if (RenderTypeLookup.canRenderInLayer(blockState, layer)) {
				net.minecraftforge.client.ForgeHooksClient.setRenderLayer(layer);
				wr.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
				dispatcher.renderModel(blockState, FakeBlockAccess.ORIGIN, access, matrixStack, wr, false, new Random(), modelData);
				tessellator.draw();
			}
		}

		net.minecraftforge.client.ForgeHooksClient.setRenderLayer(null);
		matrixStack.pop();
	}

	private static void drawHighlight(MatrixStack matrixStack, List<Pair<Side, Integer>> selections) {
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		//GlStateManager.disableDepthTest();
		GlStateManager.disableTexture();
		GlStateManager.disableCull();

		Matrix4f matrix = matrixStack.getLast().getMatrix();

		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		for (Pair<Side, Integer> p : selections) {
			final int color = p.getRight();

			final float r = (float)((color >> 16) & 0xFF) / 255;
			final float g = (float)((color >> 8) & 0xFF) / 255;
			final float b = (float)((color >> 0) & 0xFF) / 255;
			final float a = 0.5f;

			switch (p.getLeft()) {
				case XPos:
					buffer.pos(matrix, 0.5f, -0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, 0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, 0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, -0.5f, 0.5f).color(r, g, b, a).endVertex();
					break;
				case YPos:
					buffer.pos(matrix, -0.5f, 0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, 0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, 0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, 0.5f, -0.5f).color(r, g, b, a).endVertex();
					break;
				case ZPos:
					buffer.pos(matrix, -0.5f, -0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, -0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, 0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, 0.5f, 0.5f).color(r, g, b, a).endVertex();
					break;
				case XNeg:
					buffer.pos(matrix, -0.5f, -0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, -0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, 0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, 0.5f, -0.5f).color(r, g, b, a).endVertex();
					break;
				case YNeg:
					buffer.pos(matrix, -0.5f, -0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, -0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, -0.5f, 0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, -0.5f, 0.5f).color(r, g, b, a).endVertex();
					break;
				case ZNeg:
					buffer.pos(matrix, -0.5f, -0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, -0.5f, 0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, 0.5f, -0.5f).color(r, g, b, a).endVertex();
					buffer.pos(matrix, 0.5f, -0.5f, -0.5f).color(r, g, b, a).endVertex();
					break;
				default:
					break;
			}
		}
		Tessellator.getInstance().draw();

		GlStateManager.disableBlend();
		GlStateManager.enableDepthTest();
		GlStateManager.enableTexture();
		GlStateManager.enableCull();
	}

	private void toggleSide(Direction side) {
		boolean wasntPresent = !selectedSides.remove(side);
		if (wasntPresent) {
			selectedSides.add(side);
		}
		notifyListeners(side, wasntPresent);
	}

	private void notifyListeners(Direction side, boolean wasntPresent) {
		if (sideSelectedListener != null) {
			sideSelectedListener.onSideToggled(side, wasntPresent);
		}
	}

	@Override
	public boolean mouseUp(int mouseX, int mouseY, int button) {
		boolean result = super.mouseDown(mouseX, mouseY, button);
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			isDragging = false;
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && lastSideHovered != null) {
			toggleSide(lastSideHovered);
			return true;
		}
		return result;
	}

	@Override
	public boolean mouseDown(int mouseX, int mouseY, int button) {
		lastSideHovered = null;
		if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			isDragging = true;
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
			setToInitialPosition = true;
		}
		return super.mouseDown(mouseX, mouseY, button);
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
	public void setValue(Set<Direction> dirs) {
		selectedSides.clear();
		selectedSides.addAll(dirs);
	}

	public void setValue(IReadableBitMap<Direction> dirs) {
		selectedSides.clear();

		for (Direction dir : Direction.values()) {
			if (dirs.get(dir)) {
				selectedSides.add(dir);
			}
		}
	}

	public void setListener(ISideSelectedListener sideSelectedListener) {
		this.sideSelectedListener = sideSelectedListener;
	}
}
