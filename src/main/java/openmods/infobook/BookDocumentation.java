package openmods.infobook;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BookDocumentation {
	public static abstract class EMPTY implements ICustomBookEntryProvider {}

	public String customName() default "";

	public Class<? extends ICustomBookEntryProvider> customProvider() default EMPTY.class;
}