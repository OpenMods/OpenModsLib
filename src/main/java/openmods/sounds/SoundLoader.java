package openmods.sounds;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SoundLoader {

	private final static String[] soundFiles = new String[] {
			"pageturn.ogg"
	};

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void loadingSounds(SoundLoadEvent event) {
		for (String soundFile : soundFiles) {
			event.manager.addSound("openmodslib:" + soundFile);
		}
	}
}
