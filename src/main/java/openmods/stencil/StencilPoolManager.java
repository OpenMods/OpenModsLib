package openmods.stencil;

import java.util.Iterator;
import java.util.Set;

import net.minecraftforge.client.MinecraftForgeClient;

import com.google.common.collect.Sets;

public class StencilPoolManager {

	public interface StencilPoolImpl {
		public StencilBitAllocation acquire();

		public void release(StencilBitAllocation bit);

		public int getSize();

		public String getType();
	}

	private static final StencilPoolImpl DUMMY = new StencilPoolImpl() {

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

	private static final StencilPoolImpl FORGE = new StencilPoolImpl() {
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
			return getForgeStencilFlag()? "forge (forced)" : "forge";
		}
	};

	private static final StencilPoolImpl INTERNAL = new StencilPoolImpl() {
		private final Set<StencilBitAllocation> bits = Sets.newIdentityHashSet();

		{
			for (int i = 0; i < 8; i++)
				bits.add(new StencilBitAllocation(i));
		}

		@Override
		public void release(StencilBitAllocation allocation) {
			bits.add(allocation);
		}

		@Override
		public StencilBitAllocation acquire() {
			Iterator<StencilBitAllocation> it = bits.iterator();
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
			return "internal";
		}
	};

	public static StencilPoolImpl pool() {
		if (MinecraftForgeClient.getStencilBits() > 0) return FORGE;
		if (FramebufferHooks.STENCIL_BUFFER_INJECTED) return INTERNAL;
		return DUMMY;
	}

	private static boolean getForgeStencilFlag() {
		return Boolean.parseBoolean(System.getProperty("forge.forceDisplayStencil", "false"));
	}

}
