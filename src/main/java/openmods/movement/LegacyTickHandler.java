package openmods.movement;

import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

public class LegacyTickHandler implements ITickHandler {

	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		if (type.contains(TickType.CLIENT)) {
			EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
			if (player != null) PlayerMovementManager.updateMovementState(player.movementInput, player);
		}
	}

	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT);
	}

	@Override
	public String getLabel() {
		return "OpenModsMovementManager";
	}

}
