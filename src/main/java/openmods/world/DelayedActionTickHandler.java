package openmods.world;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
import java.util.Map;
import java.util.Queue;
import net.minecraft.world.World;

public class DelayedActionTickHandler {

	public static final DelayedActionTickHandler INSTANCE = new DelayedActionTickHandler();

	private DelayedActionTickHandler() {}

	private Map<Integer, Queue<Runnable>> callbacks = Maps.newHashMap();

	private Queue<Runnable> getWorldQueue(int worldId) {
		synchronized (callbacks) {
			Queue<Runnable> result = callbacks.get(worldId);

			if (result == null) {
				result = Queues.newConcurrentLinkedQueue();
				callbacks.put(worldId, result);
			}

			return result;
		}
	}

	public void addTickCallback(World world, Runnable callback) {
		int worldId = world.provider.dimensionId;
		getWorldQueue(worldId).add(callback);
	}

	@SubscribeEvent
	public void onWorldTick(WorldTickEvent evt) {
		if (evt.side == Side.SERVER && evt.phase == Phase.END) {
			int worldId = evt.world.provider.dimensionId;
			Queue<Runnable> callbacks = getWorldQueue(worldId);

			Runnable callback;
			while ((callback = callbacks.poll()) != null) {
				callback.run();
			}
		}
	}
}
