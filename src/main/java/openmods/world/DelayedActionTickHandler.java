package openmods.world;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import java.util.Map;
import java.util.Queue;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.relauncher.Side;

// TODO maybe replace with IThreadListener?
public class DelayedActionTickHandler {

	public static final DelayedActionTickHandler INSTANCE = new DelayedActionTickHandler();

	private DelayedActionTickHandler() {}

	private final Map<Integer, Queue<Runnable>> callbacks = Maps.newHashMap();

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
		int worldId = world.provider.getDimension();
		getWorldQueue(worldId).add(callback);
	}

	@SubscribeEvent
	public void onWorldTick(WorldTickEvent evt) {
		if (evt.side == Side.SERVER && evt.phase == Phase.END) {
			int worldId = evt.world.provider.getDimension();
			Queue<Runnable> callbacks = getWorldQueue(worldId);

			Runnable callback;
			while ((callback = callbacks.poll()) != null) {
				callback.run();
			}
		}
	}
}
