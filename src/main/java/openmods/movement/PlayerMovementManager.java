package openmods.movement;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import openmods.movement.PlayerMovementEvent.Type;

@EventBusSubscriber(Dist.CLIENT)
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
