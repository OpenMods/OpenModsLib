package openmods.renderer.rotations;

import net.minecraft.client.renderer.RenderBlocks;
import openmods.OpenMods;
import openmods.geometry.Orientation;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public abstract class RendererSetupProxy {

	public static final IRendererSetup NULL = new IRendererSetup() {
		@Override
		@SideOnly(Side.CLIENT)
		public RenderBlocks enter(Orientation orientation, int metadata, RenderBlocks renderer) {
			return renderer;
		}

		@Override
		@SideOnly(Side.CLIENT)
		public void exit(RenderBlocks renderer) {}
	};

	public abstract IRendererSetup getVanillaRenderer();

	public abstract IRendererSetup getFixedRenderer();

	public abstract IRendererSetup getTweakedRenderer();

	public static class Server extends RendererSetupProxy {
		@Override
		public IRendererSetup getVanillaRenderer() {
			return NULL;
		}

		@Override
		public IRendererSetup getTweakedRenderer() {
			return NULL;
		}

		@Override
		public IRendererSetup getFixedRenderer() {
			return NULL;
		}
	}

	public static class Client extends RendererSetupProxy {
		@Override
		public IRendererSetup getVanillaRenderer() {
			return VanillaSetup.instance;
		}

		@Override
		public IRendererSetup getTweakedRenderer() {
			return ClonerSetup.tweakedSetup;
		}

		@Override
		public IRendererSetup getFixedRenderer() {
			return ClonerSetup.fixedSetup;
		}
	}

	@SidedProxy(clientSide = "openmods.renderer.rotations.RendererSetupProxy$Client",
			serverSide = "openmods.renderer.rotations.RendererSetupProxy$Server",
			modId = OpenMods.MODID)
	public static RendererSetupProxy proxy;

}
