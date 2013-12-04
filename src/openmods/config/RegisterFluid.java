package openmods.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/* This is only applicable for registering fluids
 * with no block implementation
 * 
 * Fluids initialized with no block implementation receive a block ID of -1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterFluid {
	public String name();

	public int density() default 80;
	
	public int viscosity() default 1;
	
	public int luminosity() default 0;
	
	public boolean gaseous() default false;
	
}
