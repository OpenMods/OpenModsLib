package openmods.sounds;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SoundLoader {

	private final static String[] soundFiles = new String[] {
			"pageturn.ogg"
	};

	@SideOnly(Side.CLIENT)
	@ForgeSubscribe
	public void loadingSounds(SoundLoadEvent event) {
		for (String soundFile : soundFiles) {
			event.manager.addSound("openmodslib:" + soundFile);
		}
	}
}
