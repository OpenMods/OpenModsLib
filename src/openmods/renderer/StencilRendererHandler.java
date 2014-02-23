package openmods.renderer;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

public abstract class StencilRendererHandler {

	public abstract void render(RenderGlobal context, float partialTickTime);

	private boolean renderThisTick;

	public StencilRendererHandler() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	public void markForRender() {
		renderThisTick = true;
	}

	@ForgeSubscribe
	public void drawStenciledBackground(RenderWorldLastEvent evt) {
		if (!renderThisTick) return;
		renderThisTick = false;

		render(evt.context, evt.partialTicks);
	}
}
