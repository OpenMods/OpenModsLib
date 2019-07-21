package openmods.block;

import java.util.Set;
import net.minecraft.block.properties.IProperty;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import openmods.geometry.LocalDirections;
import openmods.geometry.Orientation;

public interface IBlockRotationMode {

	IProperty<Orientation> getProperty();

	int getMask();

	Orientation fromValue(int value);

	int toValue(Orientation dir);

	boolean isOrientationValid(Orientation dir);

	Set<Orientation> getValidDirections();

	Orientation getOrientationFacing(Direction side);

	// per Minecraft convention, front should be same as placement side - unless not possible, where it's on the same axis
	Direction getFront(Orientation orientation);

	// When front ='north', top should be 'up'. Also, for most modes for n|s|w|e top = 'up'
	Direction getTop(Orientation orientation);

	LocalDirections getLocalDirections(Orientation orientation);

	Orientation getPlacementOrientationFromEntity(BlockPos pos, LivingEntity player);

	boolean toolRotationAllowed();

	Direction[] getToolRotationAxes();

	Orientation calculateToolRotation(Orientation currentOrientation, Direction axis);

}