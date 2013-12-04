package openmods.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import openmods.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

public class ConfigProcessing {

	private static void getBlock(Configuration configFile, Field field, String description) {
		try {
			int defaultValue = field.getInt(null);
			Property prop = configFile.getBlock("block", field.getName(), defaultValue, description);
			field.set(null, prop.getInt());
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	private static void getItem(Configuration configFile, Field field, String description) {
		try {
			int defaultValue = field.getInt(null);
			Property prop = configFile.getItem("item", field.getName(), defaultValue, description);
			field.set(null, prop.getInt());
		} catch (Throwable e) {
			throw Throwables.propagate(e);
		}
	}

	public static boolean canRegisterBlock(int blockId) {
		if (blockId > 0) {
			Preconditions.checkState(Block.blocksList[blockId] == null,
					"OpenBlocks tried to register a block for ID: %s but it was in use", blockId);
			return true;
		}
		return false; // Block disabled, fail silently
	}

	public static void processAnnotations(Configuration configFile, Class<?> klazz) {
		for (Field f : klazz.getFields()) {
			{
				ItemId a = f.getAnnotation(ItemId.class);
				if (a != null) {
					getItem(configFile, f, a.description());
					continue;
				}
			}

			{
				BlockId a = f.getAnnotation(BlockId.class);
				if (a != null) {
					getBlock(configFile, f, a.description());
				}
			}
		}
	}

	private interface IAnnotationProcessor<I, A extends Annotation> {
		public void process(I entry, A annotation);
	}

	public static <I, A extends Annotation> void processAnnotations(Class<?> config, Class<I> fieldClass, Class<A> annotationClass, IAnnotationProcessor<I, A> processor) {
		for (Field f : config.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && fieldClass.isAssignableFrom(f.getType())) {
				A annotation = f.getAnnotation(annotationClass);
				if (annotation != null) {
					try {
						@SuppressWarnings("unchecked")
						I entry = (I)f.get(null);
						if (entry != null) processor.process(entry, annotation);
						else{
							System.out.println("Entry was null at: " + f.toString() + " " + annotation.toString());
						}
					} catch (Exception e) {
						throw Throwables.propagate(e);
					}
				} else {
					Log.warn("Field %s has valid type %s for registration, but no annotation %s", f, fieldClass, annotationClass);
				}
			}
		}
	}

	public static void registerItems(Class<?> klazz, final String mod) {
		processAnnotations(klazz, Item.class, RegisterItem.class, new IAnnotationProcessor<Item, RegisterItem>() {
			@Override
			public void process(Item item, RegisterItem annotation) {
				String name = String.format("%s.%s", mod, annotation.name());
				GameRegistry.registerItem(item, name);
			}
		});
	}

	public static void registerBlocks(Class<?> klazz, final String mod) {
		processAnnotations(klazz, IRegisterableBlock.class, RegisterBlock.class, new IAnnotationProcessor<IRegisterableBlock, RegisterBlock>() {
			@Override
			public void process(IRegisterableBlock block, RegisterBlock annotation) {
				Class<? extends TileEntity> teClass = annotation.tileEntity();
				if (teClass == TileEntity.class) teClass = null;
				if (block != null) block.setupBlock(mod, annotation.name(), teClass, annotation.itemBlock());
			}
		});
	}
	
	public static void registerFluids(Class<?> klazz, final String mod) {
		 processAnnotations(klazz, Fluid.class, RegisterFluid.class, new IAnnotationProcessor<Fluid, RegisterFluid>() {
			@Override
			public void process(Fluid fluid, RegisterFluid annotation) {
				fluid.setDensity(annotation.density()); //Yes I know you can daisy-chain these calls XD
				fluid.setGaseous(annotation.gaseous());
				fluid.setLuminosity(annotation.luminosity());
				fluid.setViscosity(annotation.viscosity());
				FluidRegistry.registerFluid(fluid);
				fluid.setUnlocalizedName(String.format("%s.%s", mod, annotation.name())  );

			}
		});
	}
}
