package openmods.stencil;

import java.util.Iterator;
import java.util.Set;

import net.minecraftforge.client.MinecraftForgeClient;

import com.google.common.collect.Sets;

public class StencilPoolManager {

	public interface StencilPoolImpl {
		public StencilBitAllocation acquire();

		public void release(StencilBitAllocation bit);
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
	};

	public static StencilPoolImpl pool() {
		if (MinecraftForgeClient.getStencilBits() > 0) return FORGE;
		if (FramebufferHooks.STENCIL_BUFFER_INJECTED) return INTERNAL;
		return DUMMY;
	}

}
