package openmods.renderer;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PlayerBodyRenderEvent extends Event {

	public final AbstractClientPlayerEntity player;

	public final float partialTickTime;

	public PlayerBodyRenderEvent(AbstractClientPlayerEntity player, float partialTickTime) {
		this.player = player;
		this.partialTickTime = partialTickTime;
	}
}
