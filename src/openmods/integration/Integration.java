package openmods.integration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import openmods.Mods;
import openmods.integration.ModuleBuildCraft.ModuleBuildCraftLive;

import com.google.common.base.Throwables;

import cpw.mods.fml.common.Loader;

public class Integration {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface Module {
		public String modId();

		public Class<?> live();
	}

	@Module(modId = Mods.BUILDCRAFT, live = ModuleBuildCraftLive.class)
	private static ModuleBuildCraft buildCraft = new ModuleBuildCraft();

	public static ModuleBuildCraft modBuildCraft() {
		return buildCraft;
	}

	public static void selectModules() {
		for (Field f : Integration.class.getDeclaredFields()) {
			Module mod = f.getAnnotation(Module.class);
			if (mod != null && Loader.isModLoaded(mod.modId())) {
				try {
					Class<?> liveReplacementCls = mod.live();
					f.setAccessible(true);
					Object replacement = liveReplacementCls.newInstance();
					f.set(null, replacement);
				} catch (Exception e) {
					Throwables.propagate(e);
				}
			}
		}
	}
}
