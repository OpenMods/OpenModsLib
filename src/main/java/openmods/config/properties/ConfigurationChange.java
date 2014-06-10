package openmods.config.properties;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.eventhandler.Cancelable;
import cpw.mods.fml.common.eventhandler.Event;

public class ConfigurationChange extends Event {

	public final String name;
	public final String category;

	public ConfigurationChange(String name, String category) {
		Preconditions.checkNotNull(name);
		this.name = name;

		Preconditions.checkNotNull(category);
		this.category = category;
	}

	public boolean check(String category, String name) {
		return this.category.equals(category) && this.name.equals(name);
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
