package openmods.movement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.entity.player.PlayerEvent;

@Cancelable
public class PlayerMovementEvent extends PlayerEvent {

	public enum Type {
		JUMP,
		SNEAK;

		public static final Type[] VALUES = values();
	}

	public Type type;

	public PlayerMovementEvent(EntityPlayer player, Type type) {
		super(player);
		this.type = type;
	}

}
