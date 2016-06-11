package openmods.stencil;

import com.google.common.collect.Sets;
import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.Iterator;
import java.util.TreeSet;
import net.minecraftforge.client.MinecraftForgeClient;
import openmods.Log;

public class StencilPoolManager {

	public interface StencilPool {
		public StencilBitAllocation acquire();

		public void release(StencilBitAllocation bit);

		public int getSize();

		public String getType();
	}

	private static final StencilPool DUMMY = new StencilPool() {

		@Override
		public StencilBitAllocation acquire() {
			return null;
		}

		@Override
		public void release(StencilBitAllocation bit) {
			throw new IllegalStateException();
		}

		@Override
		public int getSize() {
			return 0;
		}

		@Override
		public String getType() {
			return "disabled";
		}

	};

	private static class ForgePool implements StencilPool {
		private final boolean isForced;

		public ForgePool(boolean isForced) {
			this.isForced = isForced;
		}

		@Override
		public StencilBitAllocation acquire() {
			int bit = MinecraftForgeClient.reserveStencilBit();
			return bit != -1? new StencilBitAllocation(bit) : null;
		}

		@Override
		public void release(StencilBitAllocation allocation) {
			MinecraftForgeClient.releaseStencilBit(allocation.bit);
		}

		@Override
		public int getSize() {
			return MinecraftForgeClient.getStencilBits();
		}

		@Override
		public String getType() {
			return isForced? "forge (forced)" : "forge";
		}
	}

	private static class InternalPool implements StencilPool {
		private final TreeSet<StencilBitAllocation> bits = Sets.newTreeSet();

		private final boolean isForgeHacked;

		public InternalPool(boolean isForgeHacked) {
			this.isForgeHacked = isForgeHacked;

			for (int i = 0; i < 8; i++)
				bits.add(new StencilBitAllocation(i));
		}

		@Override
		public synchronized void release(StencilBitAllocation allocation) {
			bits.add(allocation);
		}

		@Override
		public synchronized StencilBitAllocation acquire() {
			// reversed, so there is smaller chance we don't collide with hacked stencils
			Iterator<StencilBitAllocation> it = bits.descendingIterator();
			if (it.hasNext()) {
				StencilBitAllocation result = it.next();
				it.remove();
				return result;
			}

			return null;
		}

		@Override
		public int getSize() {
			return 8;
		}

		@Override
		public String getType() {
			return isForgeHacked? "internal (hacked forge)" : "internal";
		}
	}

	private static StencilPool pool;

	public static StencilPool pool() {
		if (pool == null) pool = selectPool();
		return pool;
	}

	private static StencilPool selectPool() {
		final boolean forgeHasDeclaredBits = MinecraftForgeClient.getStencilBits() > 0;
		final boolean forgeHasActualBits = !isForgePoolEmpty();
		if (forgeHasDeclaredBits) {
			if (forgeHasActualBits) return new ForgePool(getForgeStencilFlag());
			else return new InternalPool(true); // stencil is enabled, but Forge pool is always empty
		}

		if (FramebufferHooks.STENCIL_BUFFER_INJECTED) return new InternalPool(false);
		return DUMMY;
	}

	private static boolean isForgePoolEmpty() {
		try {
			Field f = MinecraftForgeClient.class.getDeclaredField("stencilBits");
			f.setAccessible(true);
			BitSet pool = (BitSet)f.get(null);
			return pool.isEmpty();
		} catch (Exception e) {
			Log.warn(e, "Failed to get field!");
		}

		return false;
	}

	private static boolean getForgeStencilFlag() {
		return Boolean.parseBoolean(System.getProperty("forge.forceDisplayStencil", "false"));
	}

}
