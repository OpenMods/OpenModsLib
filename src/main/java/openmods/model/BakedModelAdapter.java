package openmods.model;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import javax.vecmath.Matrix4f;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.IPerspectiveAwareModel;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

public class BakedModelAdapter implements IPerspectiveAwareModel {

	protected final IBakedModel base;
	private final ImmutableMap<TransformType, TRSRTransformation> cameraTransforms;

	public BakedModelAdapter(IBakedModel base, ImmutableMap<TransformType, TRSRTransformation> cameraTransforms) {
		this.base = base;
		this.cameraTransforms = cameraTransforms;
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		return base.getQuads(state, side, rand);
	}

	@Override
	public boolean isAmbientOcclusion() {
		return base.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return base.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return base.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return base.getParticleTexture();
	}

	@Override
	@Deprecated
	public ItemCameraTransforms getItemCameraTransforms() {
		return ItemCameraTransforms.DEFAULT;
	}

	@Override
	public ItemOverrideList getOverrides() {
		return base.getOverrides();
	}

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(TransformType cameraTransformType) {
		return IPerspectiveAwareModel.MapWrapper.handlePerspective(this, cameraTransforms, cameraTransformType);
	}
}
