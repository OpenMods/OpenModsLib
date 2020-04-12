package openmods.world;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

public class StructureRegistry {

	public final static StructureRegistry instance = new StructureRegistry();
	public static final int RADIUS = 128;

	private interface IStructureVisitor {
		void visit(ServerWorld world, Structure<?> structure);
	}

	private void visitStructures(ServerWorld world, IStructureVisitor visitor) {
		ServerChunkProvider provider = world.getChunkProvider();
		Registry.STRUCTURE_FEATURE.forEach(structure -> visitor.visit(world, structure));
	}

	public Map<Structure<?>, BlockPos> getNearestStructures(final ServerWorld world, final BlockPos pos) {
		final ImmutableMap.Builder<Structure<?>, BlockPos> result = ImmutableMap.builder();
		visitStructures(world, (w, structure) -> {
			BlockPos structPos = w.func_241117_a_(structure, pos, RADIUS, false);
			if (structPos != null) {
				result.put(structure, structPos);
			}
		});

		return result.build();
	}

	public Set<BlockPos> getNearestInstance(final Structure<?> structure, final ServerWorld world, final BlockPos blockPos) {
		final ImmutableSet.Builder<BlockPos> result = ImmutableSet.builder();

		BlockPos structPos = world.func_241117_a_(structure, blockPos, RADIUS, false);
		if (structPos != null) {
			result.add(structPos);
		}

		return result.build();
	}

	public static String structureNameLocalizationKey(String structure) {
		return "openmodslib.structure." + structure.toLowerCase(Locale.ROOT);
	}
}
