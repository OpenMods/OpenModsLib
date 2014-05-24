package openmods.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.UniqueIdentifier;

public class BlockProperties {

	public static String getBlockName(Block block) {
		return GameRegistry.findUniqueIdentifierFor(block).toString();
	}

	public static String getBlockName(Coord c, World world) {
		return getBlockName(getBlock(c, world));
	}

	public static Block getBlockByName(String blockName) {
		UniqueIdentifier ident = new UniqueIdentifier(blockName);
		return GameRegistry.findBlock(ident.modId, ident.name);
	}

	public static Block getBlock(Coord c, World world) {
		return world.getBlock(c.x, c.y, c.z);
	}

	public static int getBlockMetadata(Coord c, World world) {
		return world.getBlockMetadata(c.x, c.y, c.z);
	}

	public static boolean isAirBlock(Coord c, World world) {
		return world.isAirBlock(c.x, c.y, c.z);
	}

	public static boolean isFlower(Coord c, World world) {
		Block block = getBlock(c, world);
		return block instanceof BlockFlower;
	}

	public static boolean isBlockNormalCube(Coord c, World world) {
		return world.isBlockNormalCubeDefault(c.x, c.y, c.z, false);
	}

	public static boolean isBlockOpaqueCube(Coord c, World world) {
		// TODO: Fix
		// return world.(c.x, c.y, c.z);
		return true;
	}

	public static boolean isWood(Coord c, World world) {
		Block block = getBlock(c, world);
		return block != null && block.isWood(world, c.x, c.y, c.z);
	}

	public static boolean isLeaves(Coord c, World world) {
		Block block = getBlock(c, world);
		return block != null && block.isLeaves(world, c.x, c.y, c.z);
	}

	public static BiomeGenBase getBiomeGenBase(Coord c, World world) {
		return world.getBiomeGenForCoords(c.x, c.z);
	}
}
