package openmods.utils;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import cpw.mods.fml.common.Loader;

public class MCUtils{
    public static String getLogFileName(){
        try{
            Loader.instance();
            try{
                Minecraft.getMinecraft();
                return "ForgeModLoader-client-0.log";
            }
            catch (Throwable e){
                return "ForgeModLoader-server-0.log";
            }
        }
        catch (Throwable e){
            return "ModLoader.txt";
        }
    }
    
    public static String getMinecraftDir(){
        try{
            return Minecraft.getMinecraft().mcDataDir.getAbsolutePath();
        }
        catch (NoClassDefFoundError e){
            return MinecraftServer.getServer().getFile("").getAbsolutePath();
        }
    }
    
    public static String getConfigDir(){
        File configDir = new File(getMinecraftDir(), "config");
        return configDir.getAbsolutePath();
    }
    
    public static int getFirstNonAirBlockFromTop(World world, int x, int z){
        int y;
        for (y = world.getActualHeight(); world.isAirBlock(x, y - 1, z) && y > 0; y--) {}
        return y;
    }
}
