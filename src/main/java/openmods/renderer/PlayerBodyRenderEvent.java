package openmods.renderer;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PlayerBodyRenderEvent extends Event {

	public final AbstractClientPlayer player;

	public final float partialTickTime;

	public PlayerBodyRenderEvent(AbstractClientPlayer player, float partialTickTime) {
		this.player = player;
		this.partialTickTime = partialTickTime;
	}
}
