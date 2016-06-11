package openmods.renderer;

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.client.entity.AbstractClientPlayer;

public class PlayerBodyRenderEvent extends Event {

	public final AbstractClientPlayer player;

	public final float partialTickTime;

	public PlayerBodyRenderEvent(AbstractClientPlayer player, float partialTickTime) {
		this.player = player;
		this.partialTickTime = partialTickTime;
	}
}
