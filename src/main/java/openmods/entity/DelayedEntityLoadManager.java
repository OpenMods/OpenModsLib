package openmods.entity;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class DelayedEntityLoadManager {
	public static final DelayedEntityLoadManager instance = new DelayedEntityLoadManager();

	private DelayedEntityLoadManager() {}

	private Multimap<Integer, IEntityLoadListener> delayedLoads = Multimaps.newSetMultimap(
			new HashMap<Integer, Collection<IEntityLoadListener>>(),
			new Supplier<Set<IEntityLoadListener>>() {
				@Override
				public Set<IEntityLoadListener> get() {
					return Sets.newSetFromMap(new WeakHashMap<IEntityLoadListener, Boolean>());
				}
			});

	@SubscribeEvent
	public void onEntityCreate(EntityJoinWorldEvent evt) {
		final Entity entity = evt.entity;
		for (IEntityLoadListener callback : delayedLoads.removeAll(entity.getEntityId()))
			callback.onEntityLoaded(entity);
	}

	public void registerLoadListener(World world, IEntityLoadListener listener, int entityId) {
		Entity entity = world.getEntityByID(entityId);
		if (entity == null) delayedLoads.put(entityId, listener);
		else listener.onEntityLoaded(entity);
	}
}
