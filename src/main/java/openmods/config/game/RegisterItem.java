package openmods.config.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.item.Item;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterItem {
	public static final String DEFAULT = "[default]";
	public static final String NONE = "[none]";

	// if left default, will use field type
	public Class<? extends Item> type() default Item.class;

	public String id();

	public String unlocalizedName() default DEFAULT;

	public boolean isEnabled() default true;

	public boolean isConfigurable() default true;

	public boolean registerDefaultModel() default true;

	public boolean addToModCreativeTab() default true;

	public Class<? extends ICustomItemModelProvider> customItemModels() default ICustomItemModelProvider.class;

	public String[] legacyIds() default {};
}
