package openmods.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PlayerDamageEvent extends Event {
	public final EntityPlayer player;
	public final DamageSource damageSource;
	public float amount;

	public PlayerDamageEvent(EntityPlayer player, DamageSource damageSource, float amount) {
		this.player = player;
		this.damageSource = damageSource;
		this.amount = amount;
	}

	public static float post(EntityPlayer player, DamageSource damageSource, float amount) {
		final PlayerDamageEvent evt = new PlayerDamageEvent(player, damageSource, amount);
		MinecraftForge.EVENT_BUS.post(evt);
		return evt.isCanceled()? 0 : evt.amount;
	}
}
