package openmods.utils;

import net.minecraft.entity.Entity;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

public class MathUtils{
    public static final double lengthSq(double x, double y, double z){
        return sqr(x) + sqr(y) + sqr(z);
    }
    
    public static final double lengthSq(double x, double z){
        return sqr(x) + sqr(z);
    }
    
    public static Matrix4f createEntityRotateMatrix(Entity entity){
        double yaw = Math.toRadians(entity.rotationYaw - 180);
        double pitch = Math.toRadians(entity.rotationPitch);
        
        Matrix4f initial = new Matrix4f();
        initial.rotate((float) pitch, new Vector3f(1, 0, 0));
        initial.rotate((float) yaw, new Vector3f(0, 1, 0));
        return initial;
    }
    
    public static int getSphericalDistance(Coord startPos, Coord endPos){
        return (int) Math.round(Math.sqrt(sqr(endPos.x - startPos.x) + sqr(endPos.z - startPos.z) + sqr(endPos.y - startPos.y)));
    }
    
    public static int getCubicDistance(Coord startPos, Coord endPos){
        return Math.abs(endPos.x - startPos.x) + Math.abs(endPos.y - startPos.y) + Math.abs(endPos.z - startPos.z);
    }
    
    public static int getHorSquaredDistance(Coord startPos, Coord endPos){
        return Math.abs(endPos.x - startPos.x) + Math.abs(endPos.z - startPos.z);
    }
    
    public static int getVerDistance(Coord startPos, Coord endPos){
        return Math.abs(endPos.y - startPos.y);
    }
    
    public static double getDistanceRatioToCenter(int point1, int point2, int pos){
        double radius = Math.abs(point2 - point1) / 2D;
        double dar = Math.abs(Math.abs(pos - point1) - radius);
        return radius != 0.0D ? dar / radius : 0.0D;
    }
    
    // slightly faster than the Java implementation
    public static int sqr(int value){
        return value * value;
    }
    
    // slightly faster than the Java implementation
    public static long sqr(long value){
        return value * value;
    }
    
    // slightly faster than the Java implementation
    public static float sqr(float value){
        return value * value;
    }
    
    // slightly faster than the Java implementation
    public static double sqr(double value){
        return value * value;
    }
    
    public static int parseInt(String string){
        return parseInt(string, 0);
    }
    
    public static int parseInt(String string, int defaultValue){
        try{
            return Integer.parseInt(string.trim());
        }
        catch (NumberFormatException ex){
            return defaultValue;
        }
    }
    
}
