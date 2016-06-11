package openmods.block;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.WorldTickEvent;
import cpw.mods.fml.relauncher.Side;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import openmods.LibConfig;
import openmods.Log;
import openmods.utils.Coord;

public class BlockDropsStore {

	public static final BlockDropsStore instance = new BlockDropsStore();

	private static class BlockDrop {
		public final Throwable location;

		public final List<ItemStack> items;

		public BlockDrop(List<ItemStack> drops) {
			this.location = LibConfig.dropsDebug? new Throwable() : null;
			this.items = ImmutableList.copyOf(drops);
		}

		public void logContents(StringBuilder result) {
			result.append("\tItems: ");
			result.append(items.toString());
			result.append('\n');

			if (location != null) {
				result.append("\tLocation:\n");
				for (StackTraceElement e : location.getStackTrace()) {
					result.append("\t\t");
					result.append(e.toString());
					result.append('\n');
				}
			}
		}
	}

	private static class WorldDrops {
		public final Multimap<Coord, BlockDrop> drops = ArrayListMultimap.create();

		public final int dimensionId;

		public WorldDrops(int dimensionId) {
			this.dimensionId = dimensionId;
		}

		public synchronized void storeDrops(int x, int y, int z, List<ItemStack> items) {
			drops.put(new Coord(x, y, z), new BlockDrop(items));
		}

		// vanilla requires that exact return type
		public synchronized ArrayList<ItemStack> harvestDrops(int x, int y, int z) {
			final Coord key = new Coord(x, y, z);
			if (drops.containsKey(key)) {
				ArrayList<ItemStack> result = Lists.newArrayList();
				Iterator<BlockDrop> it = drops.get(key).iterator();
				while (it.hasNext()) {
					final BlockDrop drop = it.next();
					result.addAll(drop.items);
					it.remove();
				}
				return result;
			}

			return null;
		}

		public synchronized void cleanup(String location) {
			if (!drops.isEmpty()) {
				StringBuilder result = new StringBuilder();
				result.append(String.format("Found unharvested drops in world %d after %s\n", dimensionId, location));
				if (!LibConfig.dropsDebug) result.append("To enable stacktrace logging, set config option 'debug.dropsDebug' to true\n");

				for (Map.Entry<Coord, Collection<BlockDrop>> e : drops.asMap().entrySet()) {
					result.append("Drops from location: ");
					result.append(e.getKey());
					result.append('\n');

					for (BlockDrop drop : e.getValue())
						drop.logContents(result);
				}

				Log.warn("%s", result.toString());

				drops.clear();
			}

		}
	}

	private final TIntObjectHashMap<WorldDrops> worldDrops = new TIntObjectHashMap<WorldDrops>();

	private synchronized WorldDrops getDrops(World world) {
		if (world.isRemote) return null;

		final int dimensionId = world.provider.dimensionId;
		WorldDrops result = worldDrops.get(dimensionId);
		if (result == null) {
			result = new WorldDrops(dimensionId);
			worldDrops.put(dimensionId, result);
		}

		return result;
	}

	public void storeDrops(World world, int x, int y, int z, List<ItemStack> items) {
		final WorldDrops drops = getDrops(world);
		if (drops != null) drops.storeDrops(x, y, z, items);
	}

	// ArrayList, since some Minecraft internals need it
	public ArrayList<ItemStack> harvestDrops(World world, int x, int y, int z) {
		final WorldDrops drops = getDrops(world);
		return (drops != null)? drops.harvestDrops(x, y, z) : null;
	}

	public class ForgeListener {
		@SubscribeEvent
		public void onWorldUnload(WorldEvent.Unload evt) {
			final World world = evt.world;
			if (!world.isRemote) cleanup(world, "unload");
		}
	}

	public class FmlListener {
		@SubscribeEvent
		public void onWorldTick(WorldTickEvent evt) {
			if (evt.side == Side.SERVER && evt.phase == Phase.END) cleanup(evt.world, "tick");
		}
	}

	public Object createForgeListener() {
		return new ForgeListener();
	}

	public Object createFmlListener() {
		return new FmlListener();
	}

	private void cleanup(final World world, final String location) {
		final int dimensionId = world.provider.dimensionId;
		final WorldDrops drops = worldDrops.get(dimensionId);
		if (drops != null) drops.cleanup(location);
	}

}
