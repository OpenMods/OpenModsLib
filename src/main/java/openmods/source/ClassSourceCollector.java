package openmods.source;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import net.minecraftforge.fml.common.API;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import openmods.Log;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ClassSourceCollector {

	public static class ApiInfo {
		public final String api;
		public final String owner;
		public final String version;

		public ApiInfo(Map<String, Object> data) {
			this.api = (String)data.get("provides");
			this.owner = (String)data.get("owner");
			this.version = (String)data.get("apiVersion");
		}

		public ApiInfo(API api) {
			this.api = api.provides();
			this.owner = api.owner();
			this.version = api.apiVersion();
		}
	}

	public static class ClassMeta {
		public final Class<?> cls;

		public final URL loadedSource;

		public final ApiInfo api;

		public final Map<File, Set<String>> providerMods;

		public ClassMeta(Class<?> cls, URL loadedSource, ApiInfo api, Map<File, Set<String>> providerMods) {
			this.cls = cls;
			this.loadedSource = loadedSource;
			this.api = api;
			this.providerMods = ImmutableMap.copyOf(providerMods);
		}

		public String source() {
			return loadedSource != null? loadedSource.toString() : "?";
		}
	}

	private final ASMDataTable table;

	public ClassSourceCollector(ASMDataTable table) {
		this.table = table;
	}

	public ClassMeta getClassInfo(String clsName) throws ClassNotFoundException {
		try {
			Class<?> cls = Class.forName(clsName);
			return getClassInfo(cls);
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (Throwable t) {
			throw Throwables.propagate(t);
		}
	}

	public ClassMeta getClassInfo(Class<?> cls) {
		final Package pkg = cls.getPackage();

		URL loadedFrom = null;

		try {
			loadedFrom = cls.getProtectionDomain().getCodeSource().getLocation();
		} catch (Throwable t) {
			Log.warn(t, "Failed to get source for %s", cls);
		}

		final API apiAnnotation = pkg.getAnnotation(API.class);
		final ApiInfo apiInfo = apiAnnotation != null? new ApiInfo(apiAnnotation) : null;

		Map<File, Set<String>> mods = Maps.newHashMap();
		for (ModCandidate candidate : table.getCandidatesFor(pkg.getName())) {
			if (!candidate.getClassList().contains(cls.getName().replace('.', '/'))) continue;

			final File candidateFile = candidate.getModContainer();

			Set<String> modIds = Sets.newHashSet();
			mods.put(candidateFile, modIds);
			for (ModContainer mod : candidate.getContainedMods())
				modIds.add(mod.getModId());
		}

		return new ClassMeta(cls, loadedFrom, apiInfo, mods);
	}
}
