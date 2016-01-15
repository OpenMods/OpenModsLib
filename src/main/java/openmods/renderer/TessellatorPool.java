package openmods.renderer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.VertexFormat;
import openmods.LibConfig;
import openmods.Log;

public class TessellatorPool {

	private final Queue<Tessellator> pool = new ConcurrentLinkedQueue<Tessellator>();
	private final AtomicInteger count = new AtomicInteger();

	public static final TessellatorPool instance = new TessellatorPool();

	private TessellatorPool() {}

	public static interface WorldRendererUser {
		public void execute(WorldRenderer wr);
	}

	private Tessellator reserveTessellator() {
		Tessellator tes = pool.poll();

		if (tes == null) {
			int id = count.incrementAndGet();
			if (id > LibConfig.tessellatorPoolLimit) Log.warn("Maximum number of tessellators in use reached. Something may leak them!");
			tes = new Tessellator(0x8000); // Maybe?
		}

		return tes;
	}

	public void startDrawing(int primitive, VertexFormat vertexFormat, WorldRendererUser user) {
		final Tessellator tes = reserveTessellator();

		final WorldRenderer wr = tes.getWorldRenderer();
		wr.begin(primitive, vertexFormat);
		user.execute(wr);
		tes.draw();

		pool.add(tes);
	}
}
