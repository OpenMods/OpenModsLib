package openmods.world;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderFlat;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.ChunkProviderHell;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;
import openmods.Log;

public class StructureRegistry {

	private static <T> void addMapGen(Collection<MapGenStructure> output, Class<T> klazz, T provider, String... names) {
		try {
			MapGenStructure struct = ReflectionHelper.getPrivateValue(klazz, provider, names);
			if (struct != null) output.add(struct);
		} catch (UnableToAccessFieldException e) {
			Log.warn(e, "Can't access fields %s from provider %s. Some structures may not be detected", Arrays.toString(names), provider);
		}
	}

	private StructureRegistry() {
		ImmutableList.Builder<IStructureGenProvider> builder = ImmutableList.builder();

		builder.add(new IStructureGenProvider() {
			@Override
			public boolean canUseOnProvider(IChunkProvider provider) {
				return provider instanceof ChunkProviderGenerate;
			}

			@Override
			public Collection<MapGenStructure> listProviders(IChunkProvider provider) {
				ChunkProviderGenerate cp = (ChunkProviderGenerate)provider;
				List<MapGenStructure> result = Lists.newArrayList();
				addMapGen(result, ChunkProviderGenerate.class, cp, "strongholdGenerator", "field_73225_u");
				addMapGen(result, ChunkProviderGenerate.class, cp, "villageGenerator", "field_73224_v");
				addMapGen(result, ChunkProviderGenerate.class, cp, "mineshaftGenerator", "field_73223_w");
				addMapGen(result, ChunkProviderGenerate.class, cp, "scatteredFeatureGenerator", "field_73233_x");
				return result;
			}
		});

		builder.add(new IStructureGenProvider() {
			@Override
			public boolean canUseOnProvider(IChunkProvider provider) {
				return provider instanceof ChunkProviderFlat;
			}

			@Override
			public Collection<MapGenStructure> listProviders(IChunkProvider provider) {
				ChunkProviderFlat cp = (ChunkProviderFlat)provider;
				List<MapGenStructure> result = Lists.newArrayList();
				try {
					List<MapGenStructure> gen = ReflectionHelper.getPrivateValue(ChunkProviderFlat.class, cp, "structureGenerators", "field_82696_f");
					if (gen != null) result.addAll(gen);
				} catch (UnableToAccessFieldException e) {
					Log.warn(e, "Can't access map gen list from provider %s. Some structures may not be detected", provider);
				}
				return result;
			}
		});

		builder.add(new IStructureGenProvider() {
			@Override
			public boolean canUseOnProvider(IChunkProvider provider) {
				return provider instanceof ChunkProviderHell;
			}

			@Override
			public Collection<MapGenStructure> listProviders(IChunkProvider provider) {
				ChunkProviderHell cp = (ChunkProviderHell)provider;
				List<MapGenStructure> result = Lists.newArrayList();
				addMapGen(result, ChunkProviderHell.class, cp, "genNetherBridge", "field_73172_c");
				return result;
			}
		});
		providers = builder.build();
	}

	public final static StructureRegistry instance = new StructureRegistry();

	private List<IStructureNamer> names = Lists.newArrayList();

	private List<IStructureGenProvider> providers;

	private static IChunkProvider getWrappedChunkProvider(ChunkProviderServer provider) {
		try {
			return ReflectionHelper.getPrivateValue(ChunkProviderServer.class, provider, "currentChunkProvider", "field_73246_d");
		} catch (UnableToAccessFieldException e) {
			Log.warn(e, "Can't access chunk provider data. No structures will be detected");
			return null;
		}
	}

	private interface IStructureVisitor {
		public void visit(MapGenStructure structure);
	}

	private void visitStructures(WorldServer world, IStructureVisitor visitor) {
		ChunkProviderServer provider = world.theChunkProviderServer;
		IChunkProvider inner = getWrappedChunkProvider(provider);

		if (inner != null) {
			for (IStructureGenProvider p : providers)
				if (p.canUseOnProvider(inner)) {
					for (MapGenStructure struct : p.listProviders(inner))
						visitor.visit(struct);
				}
		}
	}

	private String identifyStructure(MapGenStructure structure) {
		for (IStructureNamer n : names) {
			String name = n.identify(structure);
			if (!Strings.isNullOrEmpty(name)) return name;
		}
		return structure.getStructureName();
	}

	public Map<String, BlockPos> getNearestStructures(final WorldServer world, final BlockPos pos) {
		final ImmutableMap.Builder<String, BlockPos> result = ImmutableMap.builder();
		visitStructures(world, new IStructureVisitor() {
			@Override
			public void visit(MapGenStructure structure) {
				try {
					BlockPos structPos = structure.getClosestStrongholdPos(world, pos);

					if (structPos != null) {
						String structType = identifyStructure(structure);
						if (!Strings.isNullOrEmpty(structType)) result.put(structType, structPos);
					}
				} catch (IndexOutOfBoundsException e) {
					// bug in MC, just ignore
					// hopefully fixed by magic of ASM
				}
			}
		});

		return result.build();
	}

	public Set<BlockPos> getNearestInstance(final String name, final WorldServer world, final BlockPos blockPos) {
		final ImmutableSet.Builder<BlockPos> result = ImmutableSet.builder();
		visitStructures(world, new IStructureVisitor() {
			@Override
			public void visit(MapGenStructure structure) {
				String structType = identifyStructure(structure);
				if (name.equals(structType)) {
					try {
						BlockPos structPos = structure.getClosestStrongholdPos(world, blockPos);
						if (structPos != null) result.add(structPos);
					} catch (IndexOutOfBoundsException e) {
						// bug in MC, just ignore
						// hopefully fixed by magic of ASM
					}
				}
			}
		});

		return result.build();
	}
}
