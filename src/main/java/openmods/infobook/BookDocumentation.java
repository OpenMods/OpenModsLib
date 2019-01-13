package openmods.infobook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BookDocumentation {
	abstract class EMPTY implements ICustomBookEntryProvider {}

	String customName() default "";

	Class<? extends ICustomBookEntryProvider> customProvider() default EMPTY.class;

	boolean hasVideo() default false;
}