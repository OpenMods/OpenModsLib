package openmods.structured;

import java.lang.annotation.*;

/**
 * This annotation is used to mark members of {@link FieldContainer} that should be wrapped as structure elements
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StructureField {}
