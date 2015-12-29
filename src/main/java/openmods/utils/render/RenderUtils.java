package openmods.utils.render;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import openmods.utils.ColorUtils.RGB;

import org.lwjgl.opengl.GL11;

public class RenderUtils {

	private static final EnumSet<ForgeDirection> ALL_SIDES = EnumSet.allOf(ForgeDirection.class);

	public static class FogColorUpdater {
		@SubscribeEvent(priority = EventPriority.LOWEST)
		public void onFogColor(EntityViewRenderEvent.FogColors evt) {
			fogRed = evt.red;
			fogGreen = evt.green;
			fogBlue = evt.blue;
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
		if (Minecraft.getMinecraft() != null) return Minecraft.getMinecraft().theWorld;
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
				interpolateValue(e.posX, e.prevPosX, partialTickTime) - RenderManager.renderPosX,
				interpolateValue(e.posY, e.prevPosY, partialTickTime) - RenderManager.renderPosY,
				interpolateValue(e.posZ, e.prevPosZ, partialTickTime) - RenderManager.renderPosZ);
	}

	public static void translateToWorld(Entity e, float partialTickTime) {
		GL11.glTranslated(
				interpolateValue(e.posX, e.prevPosX, partialTickTime),
				interpolateValue(e.posY, e.prevPosY, partialTickTime),
				interpolateValue(e.posZ, e.prevPosZ, partialTickTime));
	}

	public static void renderCube(Tessellator tes, double x1, double y1, double z1, double x2, double y2, double z2) {
		tes.addVertex(x1, y1, z1);
		tes.addVertex(x1, y2, z1);
		tes.addVertex(x2, y2, z1);
		tes.addVertex(x2, y1, z1);

		tes.addVertex(x1, y1, z2);
		tes.addVertex(x2, y1, z2);
		tes.addVertex(x2, y2, z2);
		tes.addVertex(x1, y2, z2);

		tes.addVertex(x1, y1, z1);
		tes.addVertex(x1, y1, z2);
		tes.addVertex(x1, y2, z2);
		tes.addVertex(x1, y2, z1);

		tes.addVertex(x2, y1, z1);
		tes.addVertex(x2, y2, z1);
		tes.addVertex(x2, y2, z2);
		tes.addVertex(x2, y1, z2);

		tes.addVertex(x1, y1, z1);
		tes.addVertex(x2, y1, z1);
		tes.addVertex(x2, y1, z2);
		tes.addVertex(x1, y1, z2);

		tes.addVertex(x1, y2, z1);
		tes.addVertex(x1, y2, z2);
		tes.addVertex(x2, y2, z2);
		tes.addVertex(x2, y2, z1);
	}

	public static void disableLightmap() {
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	public static void enableLightmap() {
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	public static RGB getFogColor() {
		return new RGB(fogRed, fogGreen, fogBlue);
	}

	public static void registerFogUpdater() {
		MinecraftForge.EVENT_BUS.register(new FogColorUpdater());
	}

}
