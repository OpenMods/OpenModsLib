package openmods;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import openmods.config.gui.OpenModsConfigScreen;

public class GuiFactory implements IModGuiFactory {

	public static class ConfigScreen extends OpenModsConfigScreen {
		public ConfigScreen(GuiScreen parent) {
			super(parent, OpenMods.MODID, "OpenModsLib");
		}
	}

	@Override
	public void initialize(Minecraft minecraftInstance) {}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return ImmutableSet.of();
	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		return new ConfigScreen(parentScreen);
	}
}
