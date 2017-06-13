package openmods.block;

import java.util.Set;
import net.minecraft.block.properties.IProperty;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
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

	Orientation getOrientationFacing(EnumFacing side);

	// per Minecraft convention, front should be same as placement side - unless not possible, where it's on the same axis
	EnumFacing getFront(Orientation orientation);

	// When front ='north', top should be 'up'. Also, for most modes for n|s|w|e top = 'up'
	EnumFacing getTop(Orientation orientation);

	LocalDirections getLocalDirections(Orientation orientation);

	Orientation getPlacementOrientationFromEntity(BlockPos pos, EntityLivingBase player);

	boolean toolRotationAllowed();

	EnumFacing[] getToolRotationAxes();

	Orientation calculateToolRotation(Orientation currentOrientation, EnumFacing axis);

}