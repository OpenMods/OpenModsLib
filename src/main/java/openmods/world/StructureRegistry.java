package openmods.world;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.ChunkGeneratorFlat;
import net.minecraft.world.gen.ChunkGeneratorHell;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class StructureRegistry {

	private StructureRegistry() {
		ImmutableList.Builder<IStructureGenProvider<?>> builder = ImmutableList.builder();

		builder.add(new IStructureGenProvider<ChunkGeneratorOverworld>() {
			@Override
			public Class<ChunkGeneratorOverworld> getGeneratorCls() {
				return ChunkGeneratorOverworld.class;
			}

			@Override
			public Set<String> listStructureNames(ChunkGeneratorOverworld provider) {
				return ImmutableSet.of("Stronghold", "Mansion", "Monument", "Village", "Mineshaft", "Temple");
			}
		});

		builder.add(new IStructureGenProvider<ChunkGeneratorFlat>() {
			@Override
			public Class<ChunkGeneratorFlat> getGeneratorCls() {
				return ChunkGeneratorFlat.class;
			}

			@Override
			public Set<String> listStructureNames(ChunkGeneratorFlat provider) {
				try {
					final Map<String, MapGenStructure> structures = ReflectionHelper.getPrivateValue(ChunkGeneratorFlat.class, provider, "structureGenerators", "field_82696_f");
					return ImmutableSet.copyOf(structures.keySet());
				} catch (Exception e) {
					return ImmutableSet.of();
				}
			}
		});

		builder.add(new IStructureGenProvider<ChunkGeneratorHell>() {
			@Override
			public Class<ChunkGeneratorHell> getGeneratorCls() {
				return ChunkGeneratorHell.class;
			}

			@Override
			public Set<String> listStructureNames(ChunkGeneratorHell provider) {
				return ImmutableSet.of("Fortress");
			}
		});

		builder.add(new IStructureGenProvider<ChunkGeneratorEnd>() {
			@Override
			public Class<ChunkGeneratorEnd> getGeneratorCls() {
				return ChunkGeneratorEnd.class;
			}

			@Override
			public Set<String> listStructureNames(ChunkGeneratorEnd provider) {
				return ImmutableSet.of("EndCity");
			}
		});
		providers = builder.build();
	}

	public final static StructureRegistry instance = new StructureRegistry();

	private List<IStructureGenProvider<?>> providers;

	private interface IStructureVisitor {
		public void visit(IChunkGenerator generator, String structureName);
	}

	private void visitStructures(WorldServer world, IStructureVisitor visitor) {
		ChunkProviderServer provider = world.getChunkProvider();
		IChunkGenerator inner = provider.chunkGenerator;

		if (inner != null) {
			for (IStructureGenProvider<?> p : providers)
				tryVisit(visitor, inner, p);
		}
	}

	private static <T extends IChunkGenerator> void tryVisit(IStructureVisitor visitor, IChunkGenerator generator, IStructureGenProvider<T> p) {
		final Class<T> generatorCls = p.getGeneratorCls();
		if (generatorCls.isInstance(generator)) {
			final T castGenerator = generatorCls.cast(generator);
			for (String struct : p.listStructureNames(castGenerator))
				visitor.visit(generator, struct);
		}
	}

	public Map<String, BlockPos> getNearestStructures(final WorldServer world, final BlockPos pos) {
		final ImmutableMap.Builder<String, BlockPos> result = ImmutableMap.builder();
		visitStructures(world, (generator, structure) -> {
			try {
				BlockPos structPos = generator.getNearestStructurePos(world, structure, pos, true);

				if (structPos != null) {
					if (!Strings.isNullOrEmpty(structure)) result.put(structure, structPos);
				}
			} catch (IndexOutOfBoundsException e) {
				// bug in MC, just ignore
				// hopefully fixed by magic of ASM
			}
		});

		return result.build();
	}

	public Set<BlockPos> getNearestInstance(final String name, final WorldServer world, final BlockPos blockPos) {
		final ImmutableSet.Builder<BlockPos> result = ImmutableSet.builder();
		visitStructures(world, (generator, structure) -> {
			if (name.equals(structure)) {
				BlockPos structPos = generator.getNearestStructurePos(world, structure, blockPos, true);
				if (structPos != null) result.add(structPos);
			}
		});

		return result.build();
	}

	public static String structureNameLocalizationKey(String structure) {
		return "openmodslib.structure." + structure.toLowerCase(Locale.ROOT);
	}
}
