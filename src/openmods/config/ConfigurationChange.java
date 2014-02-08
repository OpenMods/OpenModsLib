package openmods.config;

import net.minecraftforge.event.Cancelable;
import net.minecraftforge.event.Event;

public class ConfigurationChange extends Event {

	public final String name;
	public final String category;

	public ConfigurationChange(String name, String category) {
		this.name = name;
		this.category = category;
	}

	@Cancelable
	public static class Pre extends ConfigurationChange {
		public String[] proposedValues;

		public Pre(String name, String category, String[] proposedValues) {
			super(name, category);
			this.proposedValues = proposedValues;
		}
	}

	public static class Post extends ConfigurationChange {
		public Post(String name, String category) {
			super(name, category);
		}
	}

}
