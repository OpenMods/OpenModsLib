package openmods.config.game;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterItem {
	public static final String DEFAULT = "[default]";
	public static final String NONE = "[none]";

	public String name();

	public String unlocalizedName() default DEFAULT;

	public boolean isEnabled() default true;

	public boolean isConfigurable() default true;

	public boolean registerDefaultModel() default true;

	public boolean addToModCreativeTab() default true;

	public Class<? extends ICustomItemModelProvider> customItemModels() default ICustomItemModelProvider.class;
}
