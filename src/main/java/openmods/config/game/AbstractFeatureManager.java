package openmods.config.game;

public abstract class AbstractFeatureManager {

	public static final String CATEGORY_ITEMS = "items";

	public static final String CATEGORY_BLOCKS = "blocks";

	public abstract boolean isEnabled(String category, String name);

	public boolean isBlockEnabled(String name) {
		return isEnabled(CATEGORY_BLOCKS, name);
	}

	public boolean isItemEnabled(String name) {
		return isEnabled(CATEGORY_ITEMS, name);
	}

}
