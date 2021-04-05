package openmods.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import java.util.function.Function;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.RenderMaterial;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.data.IDynamicBakedModel;

public abstract class CustomBakedModel implements IDynamicBakedModel {
	protected final boolean isAmbientOcclusion;
	protected final boolean isGui3d;
	protected final boolean isSideLit;
	protected final TextureAtlasSprite particle;
	protected final IModelTransform transforms;

	protected CustomBakedModel(final IModelConfiguration owner, final Function<RenderMaterial, TextureAtlasSprite> spriteGetter, final IModelTransform transforms) {
		isAmbientOcclusion = owner.useSmoothLighting();
		isGui3d = owner.isShadedInGui();
		isSideLit = owner.isSideLit();
		this.transforms = transforms;

		RenderMaterial particleLocation = owner.resolveTexture("particle");
		particle = spriteGetter.apply(particleLocation);
	}

	@Override
	public boolean isAmbientOcclusion() {
		return isAmbientOcclusion;
	}

	@Override
	public boolean isGui3d() {
		return isGui3d;
	}

	@Override
	public boolean isSideLit() {
		return isSideLit;
	}

	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return particle;
	}

	@Override
	public boolean doesHandlePerspectives() {
		return true;
	}

	@Override
	public IBakedModel handlePerspective(ItemCameraTransforms.TransformType cameraTransformType, MatrixStack mat) {
		return PerspectiveMapWrapper.handlePerspective(this, transforms, cameraTransformType, mat);
	}
}
