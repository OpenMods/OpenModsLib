package openmods.renderer.rotations;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.RenderBlocks;
import openmods.geometry.Orientation;

public interface IRendererSetup {
	@SideOnly(Side.CLIENT)
	public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer);

	@SideOnly(Side.CLIENT)
	public void exit(RenderBlocks renderer);
}
