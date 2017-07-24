package openmods.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openmods.colors.RGB;
import org.lwjgl.opengl.GL11;

public class RenderUtils {

	public static class FogColorUpdater {
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public void onFogColor(EntityViewRenderEvent.FogColors evt) {
			fogRed = evt.getRed();
			fogGreen = evt.getGreen();
			fogBlue = evt.getBlue();
		}
	}

	private static float fogRed;
	private static float fogGreen;
	private static float fogBlue;

	public static void setupBillboard(Entity rve) {
		GL11.glRotatef(-rve.rotationYaw, 0, 1, 0);
		GL11.glRotatef(rve.rotationPitch, 1, 0, 0);
	}

	/**
	 * Please! For the love of sanity. Do not store this instance ANYWHERE!
	 * If you set it to a TE or Entity, Please remove it after you're done!
	 * THANK YOU!
	 *
	 * @return Returns a world for rendering with
	 */
	public static World getRenderWorld() {
		if (Minecraft.getMinecraft() != null) return Minecraft.getMinecraft().world;
		return null;
	}

	public static double interpolateValue(double current, double prev, float partialTickTime) {
		return prev + partialTickTime * (current - prev);
	}

	public static float interpolateYaw(Entity e, float f) {
		return e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * f;
	}

	public static float interpolatePitch(Entity e, float f) {
		return e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * f;
	}

	public static void translateToPlayer(Entity e, float partialTickTime) {
		GL11.glTranslated(
				interpolateValue(e.posX, e.prevPosX, partialTickTime) - TileEntityRendererDispatcher.staticPlayerX,
				interpolateValue(e.posY, e.prevPosY, partialTickTime) - TileEntityRendererDispatcher.staticPlayerY,
				interpolateValue(e.posZ, e.prevPosZ, partialTickTime) - TileEntityRendererDispatcher.staticPlayerZ);
	}

	public static void translateToWorld(Entity e, float partialTickTime) {
		GL11.glTranslated(
				interpolateValue(e.posX, e.prevPosX, partialTickTime),
				interpolateValue(e.posY, e.prevPosY, partialTickTime),
				interpolateValue(e.posZ, e.prevPosZ, partialTickTime));
	}

	public interface IQuadSink {
		public void addVertex(EnumFacing quad, int vertex, double x, double y, double z);
	}

	public static void renderCube(IQuadSink sink, AxisAlignedBB aabb) {
		sink.addVertex(EnumFacing.NORTH, 0, aabb.minX, aabb.minY, aabb.minZ);
		sink.addVertex(EnumFacing.NORTH, 1, aabb.minX, aabb.maxY, aabb.minZ);
		sink.addVertex(EnumFacing.NORTH, 2, aabb.maxX, aabb.maxY, aabb.minZ);
		sink.addVertex(EnumFacing.NORTH, 3, aabb.maxX, aabb.minY, aabb.minZ);

		sink.addVertex(EnumFacing.SOUTH, 0, aabb.minX, aabb.minY, aabb.maxZ);
		sink.addVertex(EnumFacing.SOUTH, 1, aabb.maxX, aabb.minY, aabb.maxZ);
		sink.addVertex(EnumFacing.SOUTH, 2, aabb.maxX, aabb.maxY, aabb.maxZ);
		sink.addVertex(EnumFacing.SOUTH, 3, aabb.minX, aabb.maxY, aabb.maxZ);

		sink.addVertex(EnumFacing.WEST, 0, aabb.minX, aabb.minY, aabb.minZ);
		sink.addVertex(EnumFacing.WEST, 1, aabb.minX, aabb.minY, aabb.maxZ);
		sink.addVertex(EnumFacing.WEST, 2, aabb.minX, aabb.maxY, aabb.maxZ);
		sink.addVertex(EnumFacing.WEST, 3, aabb.minX, aabb.maxY, aabb.minZ);

		sink.addVertex(EnumFacing.EAST, 0, aabb.maxX, aabb.minY, aabb.minZ);
		sink.addVertex(EnumFacing.EAST, 1, aabb.maxX, aabb.maxY, aabb.minZ);
		sink.addVertex(EnumFacing.EAST, 2, aabb.maxX, aabb.maxY, aabb.maxZ);
		sink.addVertex(EnumFacing.EAST, 3, aabb.maxX, aabb.minY, aabb.maxZ);

		sink.addVertex(EnumFacing.DOWN, 0, aabb.minX, aabb.minY, aabb.minZ);
		sink.addVertex(EnumFacing.DOWN, 1, aabb.maxX, aabb.minY, aabb.minZ);
		sink.addVertex(EnumFacing.DOWN, 2, aabb.maxX, aabb.minY, aabb.maxZ);
		sink.addVertex(EnumFacing.DOWN, 3, aabb.minX, aabb.minY, aabb.maxZ);

		sink.addVertex(EnumFacing.UP, 0, aabb.minX, aabb.maxY, aabb.minZ);
		sink.addVertex(EnumFacing.UP, 1, aabb.minX, aabb.maxY, aabb.maxZ);
		sink.addVertex(EnumFacing.UP, 2, aabb.maxX, aabb.maxY, aabb.maxZ);
		sink.addVertex(EnumFacing.UP, 3, aabb.maxX, aabb.maxY, aabb.minZ);
	}

	public interface IVertexSink {
		public void addVertex(double x, double y, double z);
	}

	public static void renderCube(IVertexSink sink, double x1, double y1, double z1, double x2, double y2, double z2) {
		sink.addVertex(x1, y1, z1);
		sink.addVertex(x1, y2, z1);
		sink.addVertex(x2, y2, z1);
		sink.addVertex(x2, y1, z1);

		sink.addVertex(x1, y1, z2);
		sink.addVertex(x2, y1, z2);
		sink.addVertex(x2, y2, z2);
		sink.addVertex(x1, y2, z2);

		sink.addVertex(x1, y1, z1);
		sink.addVertex(x1, y1, z2);
		sink.addVertex(x1, y2, z2);
		sink.addVertex(x1, y2, z1);

		sink.addVertex(x2, y1, z1);
		sink.addVertex(x2, y2, z1);
		sink.addVertex(x2, y2, z2);
		sink.addVertex(x2, y1, z2);

		sink.addVertex(x1, y1, z1);
		sink.addVertex(x2, y1, z1);
		sink.addVertex(x2, y1, z2);
		sink.addVertex(x1, y1, z2);

		sink.addVertex(x1, y2, z1);
		sink.addVertex(x1, y2, z2);
		sink.addVertex(x2, y2, z2);
		sink.addVertex(x2, y2, z1);
	}

	public static void renderSolidCube(Tessellator tes, double x1, double y1, double z1, double x2, double y2, double z2) {
		final VertexBuffer wr = tes.getBuffer();
		wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
		renderCube(new IVertexSink() {
			@Override
			public void addVertex(double x, double y, double z) {
				wr.pos(x, y, z).endVertex();
			}
		}, x1, y1, z1, x2, y2, z2);
		tes.draw();
	}

	public static void setColor(int rgb) {
		final float r = (float)((rgb >> 16) & 0xFF) / 255;
		final float g = (float)((rgb >> 8) & 0xFF) / 255;
		final float b = (float)((rgb >> 0) & 0xFF) / 255;
		GlStateManager.color(r, g, b);
	}

	public static void setColor(int rgb, float alpha) {
		final float r = (float)((rgb >> 16) & 0xFF) / 255;
		final float g = (float)((rgb >> 8) & 0xFF) / 255;
		final float b = (float)((rgb >> 0) & 0xFF) / 255;

		GlStateManager.color(r, g, b, alpha);
	}

	public static void disableLightmap() {
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.disableTexture2D();
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	public static void enableLightmap() {
		GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GlStateManager.enableTexture2D();
		GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	public static RGB getFogColor() {
		return new RGB(fogRed, fogGreen, fogBlue);
	}

	public static void registerFogUpdater() {
		MinecraftForge.EVENT_BUS.register(new FogColorUpdater());
	}

}
