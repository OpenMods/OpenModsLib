package openmods.proxy;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.animation.ITimeValue;
import net.minecraftforge.common.model.animation.IAnimationStateMachine;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.network.IGuiHandler;
import openmods.geometry.Hitbox;
import openmods.geometry.IHitboxSupplier;
import openmods.gui.CommonGuiHandler;

public final class OpenServerProxy implements IOpenModsProxy {
	private static MinecraftServer getServer() {
		return LogicalSidedProvider.INSTANCE.get(LogicalSide.SERVER);
	}

	@Override
	public World getClientWorld() {
		return null;
	}

	@Override
	public World getServerWorld(DimensionType id) {
		return getServer().getWorld(id);
	}

	@Override
	public Optional<String> getLanguage() {
		return Optional.absent();
	}

	@Override
	public IGuiHandler wrapHandler(IGuiHandler modSpecificHandler) {
		return new CommonGuiHandler(modSpecificHandler);
	}

	@Override
	public void earlySyncInit() {
	}

	@Override
	public void clientInit() {}

	@Override
	public void setNowPlayingTitle(String nowPlaying) {}

	private static final IHitboxSupplier DUMMY_HITBOX_SUPPLIER = new IHitboxSupplier() {
		@Override
		public List<Hitbox> asList() {
			throw new UnsupportedOperationException("Not available on server");
		}

		@Override
		public Map<String, Hitbox> asMap() {
			throw new UnsupportedOperationException("Not available on server");
		}
	};

	@Override
	public IHitboxSupplier getHitboxes(ResourceLocation location) {
		return DUMMY_HITBOX_SUPPLIER;
	}

	@Override
	public IAnimationStateMachine loadAsm(ResourceLocation location, ImmutableMap<String, ITimeValue> parameters) {
		return null;
	}

}
