package openmods.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class PlayerMovementEvent extends PlayerEvent {

	public enum Type {
		JUMP,
		SNEAK
	}

	public final Type type;

	public PlayerMovementEvent(PlayerEntity player, Type type) {
		super(player);
		this.type = type;
	}

}
