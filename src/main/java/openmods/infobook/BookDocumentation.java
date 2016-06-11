package openmods.infobook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BookDocumentation {
	public static abstract class EMPTY implements ICustomBookEntryProvider {}

	public String customName() default "";

	public Class<? extends ICustomBookEntryProvider> customProvider() default EMPTY.class;

	public boolean hasVideo() default false;
}