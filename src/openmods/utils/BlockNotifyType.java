package openmods.utils;

/**
 * Provides constants for the types of block notifications possible in World.setBlock()
 * 
 * @author: bspkrs
 */

public class BlockNotifyType{
    public static final int NONE          = 0;
    public static final int COMPARATOR    = 1;
    public static final int CLIENT_SERVER = 2;
    public static final int ALL           = 3;
    public static final int CLIENT_ONLY   = 4;
}
