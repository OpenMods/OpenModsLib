package openmods.config.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.item.Item;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterItem {
	String DEFAULT = "[default]";
	String NONE = "[none]";

	// if left default, will use field type
	Class<? extends Item> type() default Item.class;

	String id();

	String unlocalizedName() default DEFAULT;

	boolean isEnabled() default true;

	boolean isConfigurable() default true;

	boolean registerDefaultModel() default true;

	boolean addToModCreativeTab() default true;

	Class<? extends ICustomItemModelProvider> customItemModels() default ICustomItemModelProvider.class;

	String[] legacyIds() default {};
}
