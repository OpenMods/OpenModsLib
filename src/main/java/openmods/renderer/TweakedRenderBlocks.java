package openmods.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

/**
 * This renderer flips bottom side vertically. This allows to re-orient blocks with just rotations.
 */
public class TweakedRenderBlocks extends FixedRenderBlocks {

	@Override
	public void renderFaceYNeg(Block p_147768_1_, double p_147768_2_, double p_147768_4_, double p_147768_6_, IIcon p_147768_8_) {
		Tessellator tessellator = Tessellator.instance;

		if (hasOverrideBlockTexture()) {
			p_147768_8_ = this.overrideBlockTexture;
		}

		double d3 = p_147768_8_.getInterpolatedU(this.renderMinX * 16.0D);
		double d4 = p_147768_8_.getInterpolatedU(this.renderMaxX * 16.0D);
		double d5 = p_147768_8_.getInterpolatedV(this.renderMinZ * 16.0D);
		double d6 = p_147768_8_.getInterpolatedV(this.renderMaxZ * 16.0D);

		if (this.renderMinX < 0.0D || this.renderMaxX > 1.0D) {
			d3 = p_147768_8_.getMinU();
			d4 = p_147768_8_.getMaxU();
		}

		if (this.renderMinZ < 0.0D || this.renderMaxZ > 1.0D) {
			d5 = p_147768_8_.getMinV();
			d6 = p_147768_8_.getMaxV();
		}

		double d7 = d4;
		double d8 = d3;
		double d9 = d5;
		double d10 = d6;

		if (this.uvRotateBottom == 2) {
			d3 = p_147768_8_.getInterpolatedU(this.renderMinZ * 16.0D);
			d5 = p_147768_8_.getInterpolatedV(16.0D - this.renderMaxX * 16.0D);
			d4 = p_147768_8_.getInterpolatedU(this.renderMaxZ * 16.0D);
			d6 = p_147768_8_.getInterpolatedV(16.0D - this.renderMinX * 16.0D);
			d9 = d5;
			d10 = d6;
			d7 = d3;
			d8 = d4;
			d5 = d6;
			d6 = d9;
		} else if (this.uvRotateBottom == 1) {
			d3 = p_147768_8_.getInterpolatedU(16.0D - this.renderMaxZ * 16.0D);
			d5 = p_147768_8_.getInterpolatedV(this.renderMinX * 16.0D);
			d4 = p_147768_8_.getInterpolatedU(16.0D - this.renderMinZ * 16.0D);
			d6 = p_147768_8_.getInterpolatedV(this.renderMaxX * 16.0D);
			d7 = d4;
			d8 = d3;
			d3 = d4;
			d4 = d8;
			d9 = d6;
			d10 = d5;
		} else if (this.uvRotateBottom == 3) {
			d3 = p_147768_8_.getInterpolatedU(16.0D - this.renderMinX * 16.0D);
			d4 = p_147768_8_.getInterpolatedU(16.0D - this.renderMaxX * 16.0D);
			d5 = p_147768_8_.getInterpolatedV(16.0D - this.renderMinZ * 16.0D);
			d6 = p_147768_8_.getInterpolatedV(16.0D - this.renderMaxZ * 16.0D);
			d7 = d4;
			d8 = d3;
			d9 = d5;
			d10 = d6;
		}

		double d11 = p_147768_2_ + this.renderMinX;
		double d12 = p_147768_2_ + this.renderMaxX;
		double d13 = p_147768_4_ + this.renderMinY;
		double d14 = p_147768_6_ + this.renderMinZ;
		double d15 = p_147768_6_ + this.renderMaxZ;

		if (this.renderFromInside) {
			d11 = p_147768_2_ + this.renderMaxX;
			d12 = p_147768_2_ + this.renderMinX;
		}

		if (this.enableAO) {
			tessellator.setColorOpaque_F(this.colorRedTopLeft, this.colorGreenTopLeft, this.colorBlueTopLeft);
			tessellator.setBrightness(this.brightnessTopLeft);
			tessellator.addVertexWithUV(d11, d13, d15, d7, d10);
			tessellator.setColorOpaque_F(this.colorRedBottomLeft, this.colorGreenBottomLeft, this.colorBlueBottomLeft);
			tessellator.setBrightness(this.brightnessBottomLeft);
			tessellator.addVertexWithUV(d11, d13, d14, d4, d5);
			tessellator.setColorOpaque_F(this.colorRedBottomRight, this.colorGreenBottomRight, this.colorBlueBottomRight);
			tessellator.setBrightness(this.brightnessBottomRight);
			tessellator.addVertexWithUV(d12, d13, d14, d8, d9);
			tessellator.setColorOpaque_F(this.colorRedTopRight, this.colorGreenTopRight, this.colorBlueTopRight);
			tessellator.setBrightness(this.brightnessTopRight);
			tessellator.addVertexWithUV(d12, d13, d15, d3, d6);
		} else {
			tessellator.addVertexWithUV(d11, d13, d15, d8, d10);
			tessellator.addVertexWithUV(d11, d13, d14, d3, d5);
			tessellator.addVertexWithUV(d12, d13, d14, d7, d9);
			tessellator.addVertexWithUV(d12, d13, d15, d4, d6);
		}
	}

	public static <T extends Block> IBlockRenderer<T> wrap(IBlockRenderer<T> blockRenderer) {
		return new WrappedBlockRenderer<T>(blockRenderer) {
			@Override
			protected RenderBlocks createWrapper(RenderBlocks renderer) {
				return new TweakedRenderBlocks();
			}
		};
	}
}
