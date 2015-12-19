package openmods.api;

import java.lang.annotation.*;

/**
 * Marker for any documenting mod.
 * Note: this does not have any effect in OpenModsLib
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface VisibleForDocumentation {

}
