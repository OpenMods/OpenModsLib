package openmods.model.textureditem;

import com.google.common.base.Optional;
import net.minecraft.util.ResourceLocation;

public interface IItemTexture {
	public Optional<ResourceLocation> getTexture();
}
