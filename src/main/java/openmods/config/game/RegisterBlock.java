package openmods.config.game;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import openmods.item.ItemOpenBlock;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterBlock {
	public static final String DEFAULT = "[default]";
	public static final String NONE = "[none]";

	public @interface RegisterTileEntity {
		public String name();

		public Class<? extends TileEntity> cls();
	}

	// if left default, will use field type
	public Class<? extends Block> type() default Block.class;

	public String id();

	public boolean registerItemBlock() default true;

	public Class<? extends ItemBlock> itemBlock() default ItemOpenBlock.class;

	public Class<? extends TileEntity> tileEntity() default TileEntity.class;

	public RegisterTileEntity[] tileEntities() default {};

	public String unlocalizedName() default DEFAULT;

	public boolean isEnabled() default true;

	public boolean isConfigurable() default true;

	public boolean registerDefaultItemModel() default true;

	public boolean addToModCreativeTab() default true;

	public Class<? extends ICustomItemModelProvider> customItemModels() default ICustomItemModelProvider.class;

	public String[] legacyIds() default {};
}
