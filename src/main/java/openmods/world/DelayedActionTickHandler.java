package openmods.world;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import java.util.Map;
import java.util.Queue;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

// TODO maybe replace with IThreadListener?
public class DelayedActionTickHandler {

	public static final DelayedActionTickHandler INSTANCE = new DelayedActionTickHandler();

	private DelayedActionTickHandler() {}

	private final Map<DimensionType, Queue<Runnable>> callbacks = Maps.newHashMap();

	private Queue<Runnable> getWorldQueue(DimensionType worldId) {
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
		DimensionType worldId = world.getDimension().getType();
		getWorldQueue(worldId).add(callback);
	}

	@SubscribeEvent
	public void onWorldTick(TickEvent.WorldTickEvent evt) {
		if (evt.side == LogicalSide.SERVER && evt.phase == TickEvent.Phase.END) {
			DimensionType worldId = evt.world.getDimension().getType();
			Queue<Runnable> callbacks = getWorldQueue(worldId);

			Runnable callback;
			while ((callback = callbacks.poll()) != null) {
				callback.run();
			}
		}
	}
}
