package openmods.world;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;

public class StructureRegistry {

	public final static StructureRegistry instance = new StructureRegistry();
	public static final int RADIUS = 128;

	private interface IStructureVisitor {
		void visit(ChunkGenerator generator, String structureName, Structure<?> structure);
	}

	private void visitStructures(ServerWorld world, IStructureVisitor visitor) {
		ServerChunkProvider provider = world.getChunkProvider();
		ChunkGenerator inner = provider.getChunkGenerator();
		Feature.STRUCTURES.forEach((name, structure) -> visitor.visit(inner, name, structure));
	}

	public Map<String, BlockPos> getNearestStructures(final ServerWorld world, final BlockPos pos) {
		final ImmutableMap.Builder<String, BlockPos> result = ImmutableMap.builder();
		visitStructures(world, (generator, name, structure) -> {
			try {
				BlockPos structPos = generator.findNearestStructure(world, name, pos, RADIUS, false);
				if (structPos != null) {
					result.put(name, structPos);
				}
			} catch (IndexOutOfBoundsException e) {
				// bug in MC, just ignore
				// hopefully fixed by magic of ASM
			}
		});

		return result.build();
	}

	public Set<BlockPos> getNearestInstance(final String name, final ServerWorld world, final BlockPos blockPos) {
		final ImmutableSet.Builder<BlockPos> result = ImmutableSet.builder();
		visitStructures(world, (generator, structureName, structure) -> {
			if (name.equals(structureName)) {
				BlockPos structPos = generator.findNearestStructure(world, structureName, blockPos, RADIUS, false);
				if (structPos != null) result.add(structPos);
			}
		});

		return result.build();
	}

	public static String structureNameLocalizationKey(String structure) {
		return "openmodslib.structure." + structure.toLowerCase(Locale.ROOT);
	}
}
