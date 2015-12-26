package openmods.renderer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.renderer.Tessellator;
import openmods.LibConfig;
import openmods.Log;

public class TessellatorPool {

	private final Queue<Tessellator> pool = new ConcurrentLinkedQueue<Tessellator>();
	private final AtomicInteger count = new AtomicInteger();

	public static final TessellatorPool instance = new TessellatorPool();

	private TessellatorPool() {}

	public static interface TessellatorUser {
		public void execute(Tessellator tes);
	}

	private Tessellator reserveTessellator() {
		Tessellator tes = pool.poll();

		if (tes == null) {
			int id = count.incrementAndGet();
			if (id > LibConfig.tessellatorPoolLimit) Log.warn("Maximum number of tessellators in use reached. Something may leak them!");
			tes = new Tessellator();
		}

		return tes;
	}

	public void startDrawing(TessellatorUser user, int primitive) {
		final Tessellator tes = reserveTessellator();

		tes.startDrawing(primitive);
		user.execute(tes);
		tes.draw();

		pool.add(tes);
	}

	public void startDrawingQuads(TessellatorUser user) {
		final Tessellator tes = reserveTessellator();

		tes.startDrawingQuads();
		user.execute(tes);
		tes.draw();

		pool.add(tes);
	}
}
