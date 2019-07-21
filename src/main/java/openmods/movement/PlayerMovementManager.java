package openmods.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import openmods.movement.PlayerMovementEvent.Type;

@EventBusSubscriber(Side.CLIENT)
public class PlayerMovementManager {

	private static boolean wasJumping = false;
	private static boolean wasSneaking = false;

	private PlayerMovementManager() {}

	@SubscribeEvent
	public static void updateMovementState(InputUpdateEvent evt) {
		final MovementInput input = evt.getMovementInput();
		final PlayerEntity owner = evt.getEntityPlayer();
		if (input.jump && !wasJumping) input.jump = postMovementEvent(owner, PlayerMovementEvent.Type.JUMP);
		if (input.sneak && !wasSneaking) input.sneak = postMovementEvent(owner, PlayerMovementEvent.Type.SNEAK);

		wasJumping = input.jump;
		wasSneaking = input.sneak;
	}

	private static boolean postMovementEvent(PlayerEntity player, Type type) {
		return !MinecraftForge.EVENT_BUS.post(new PlayerMovementEvent(player, type));
	}
}
