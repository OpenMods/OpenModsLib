package openmods.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import openmods.item.ItemOpenBlock;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterBlock {
	public String name();

	public Class<? extends ItemBlock> itemBlock() default ItemOpenBlock.class;

	public Class<? extends TileEntity> tileEntity() default TileEntity.class;
}
