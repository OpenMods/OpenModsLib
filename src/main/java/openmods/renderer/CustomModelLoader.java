package openmods.renderer;

import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class CustomModelLoader {
	public static class ModelLoadFailed extends RuntimeException {
		private static final long serialVersionUID = -545310636164059408L;

		public ModelLoadFailed(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final CustomModelLoader instance = new CustomModelLoader();

	private final Multimap<ResourceLocation, String> modelsToLoad = HashMultimap.create();

	public void registerModel(ResourceLocation modelLocation, String... location) {
		registerModel(modelLocation, Arrays.asList(location));
	}

	public void registerModel(ResourceLocation modelLocation, Iterable<String> variants) {
		modelsToLoad.putAll(modelLocation, variants);
	}

	private void resolveModelTextures(ResourceLocation modelLocation, Set<ResourceLocation> texturesToLoad, Set<ResourceLocation> checkedModels) {
		if (checkedModels.contains(modelLocation)) return;

		final IModel model;
		try {
			model = ModelLoaderRegistry.getModel(modelLocation);
		} catch (Exception e) {
			throw new ModelLoadFailed(modelLocation.toString(), e);
		}

		final Collection<ResourceLocation> textures = model.getTextures();
		texturesToLoad.addAll(textures);

		for (ResourceLocation dependency : model.getDependencies())
			resolveModelTextures(dependency, texturesToLoad, checkedModels);
	}

	@SubscribeEvent
	public void onTextureStitchEvent(TextureStitchEvent.Pre event) {
		Set<ResourceLocation> texturesToLoad = Sets.newHashSet();
		Set<ResourceLocation> checkedModels = Sets.newHashSet();

		for (ResourceLocation model : modelsToLoad.keys())
			resolveModelTextures(model, texturesToLoad, checkedModels);

		for (ResourceLocation textureLocation : texturesToLoad)
			event.map.registerSprite(textureLocation);
	}

	@SubscribeEvent
	public void onModelBakeEvent(ModelBakeEvent event) {
		for (Map.Entry<ResourceLocation, Collection<String>> e : modelsToLoad.asMap().entrySet())
			loadModel(event, e.getKey(), e.getValue());
	}

	private static void loadModel(ModelBakeEvent event, ResourceLocation location, Iterable<String> variants) {
		try {
			final IModel model = event.modelLoader.getModel(location);
			final TextureMap textureMapBlocks = Minecraft.getMinecraft().getTextureMapBlocks();

			Set<String> variantsToRegister = Sets.newHashSet();
			for (String variant : variants)
				if (event.modelRegistry.getObject(new ModelResourceLocation(location, variant)) == null) variantsToRegister.add(variant);

			if (variantsToRegister.isEmpty()) return;

			final IBakedModel bakedModel = model.bake(model.getDefaultState(), DefaultVertexFormats.ITEM,
					new Function<ResourceLocation, TextureAtlasSprite>() {
						@Override
						public TextureAtlasSprite apply(ResourceLocation input) {
							return textureMapBlocks.getAtlasSprite(input.toString());
						}
					});

			for (String variant : variantsToRegister)
				event.modelRegistry.putObject(new ModelResourceLocation(location, variant), bakedModel);

		} catch (Exception e) {
			throw new ModelLoadFailed(location.toString(), e);
		}
	}

}
