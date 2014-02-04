package openmods.movement;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovementInput;
import net.minecraftforge.common.MinecraftForge;
import openmods.movement.PlayerMovementEvent.Type;

public class PlayerMovementManager {

	static boolean callbackInjected = false;

	private static boolean wasJumping = false;
	private static boolean wasSneaking = false;

	public static void updateMovementState(MovementInput input, EntityPlayer owner) {
		if (input.jump && !wasJumping) input.jump = postMovementEvent(owner, PlayerMovementEvent.Type.JUMP);
		if (input.sneak && !wasSneaking) input.sneak = postMovementEvent(owner, PlayerMovementEvent.Type.SNEAK);

		wasJumping = input.jump;
		wasSneaking = input.sneak;
	}

	private static boolean postMovementEvent(EntityPlayer player, Type type) {
		return !MinecraftForge.EVENT_BUS.post(new PlayerMovementEvent(player, type));
	}

	public static boolean isCallbackInjected() {
		return callbackInjected;
	}
}
