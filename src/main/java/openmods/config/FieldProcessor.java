package openmods.config;

import com.google.common.base.Throwables;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import openmods.config.game.RegisterBlock;
import openmods.config.game.RegisterItem;

public class FieldProcessor {

	public interface FieldValueVisitor<T> {
		public void visit(T value);
	}

	private static <A extends Annotation, T> void processEntries(Class<? extends InstanceContainer<T>> container, Class<? extends A> annotationCls, Class<T> fieldCls, FieldValueVisitor<T> visitor) {
		for (Field f : container.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) && f.isAnnotationPresent(annotationCls)) {
				try {
					Object value = f.get(null);
					T item = fieldCls.cast(value);
					if (item != null) visitor.visit(item);
				} catch (Throwable t) {
					throw Throwables.propagate(t);
				}
			}
		}
	}

	public static void processItems(Class<? extends ItemInstances> container, FieldValueVisitor<Item> visitor) {
		processEntries(container, RegisterItem.class, Item.class, visitor);
	}

	public static void processBlocks(Class<? extends BlockInstances> container, FieldValueVisitor<Block> visitor) {
		processEntries(container, RegisterBlock.class, Block.class, visitor);
	}
}
