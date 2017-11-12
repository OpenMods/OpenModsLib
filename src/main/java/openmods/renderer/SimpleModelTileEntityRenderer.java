package openmods.renderer;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import openmods.tileentity.OpenTileEntity;
import openmods.utils.BlockUtils;
import org.lwjgl.opengl.GL11;

public class SimpleModelTileEntityRenderer<T extends OpenTileEntity> extends TileEntitySpecialRenderer<T> {

	private final ITileEntityModel<T> model;
	private final ResourceLocation texture;

	public SimpleModelTileEntityRenderer(ITileEntityModel<T> model, ResourceLocation texture) {
		this.model = model;
		this.texture = texture;
	}

	@Override
	public void render(T te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5, y + 1.0, z + 0.5);
		GL11.glRotatef(180.0F, 1.0F, 0.0F, 0.0F);
		if (te != null) GL11.glRotatef(-BlockUtils.getRotationFromOrientation(te.getOrientation()), 0, 1, 0);
		bindTexture(texture);
		model.render(te, partialTicks);
		GL11.glPopMatrix();
	}

	public static <T extends OpenTileEntity> TileEntitySpecialRenderer<T> create(ITileEntityModel<T> model, ResourceLocation texture) {
		return new SimpleModelTileEntityRenderer<>(model, texture);
	}
}
