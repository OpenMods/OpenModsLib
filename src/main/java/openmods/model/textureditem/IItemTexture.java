package openmods.model.textureditem;

import java.util.Optional;
import net.minecraft.util.ResourceLocation;

@FunctionalInterface
public interface IItemTexture {
	public Optional<ResourceLocation> getTexture();
}
