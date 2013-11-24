package openmods.utils;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MiscUtils {
	public static int getHoliday() {
		Calendar today = Calendar.getInstance();
		int month = today.get(2);
		int day = today.get(5);
		if ((month == 1) && (day == 14)) { return 1; }
		if ((month == 9) && (day == 31)) { return 2; }
		if ((month == 11) && (day >= 24) && (day <= 30)) { return 3; }
		return 0;
	}
    
    public static String stringArrayToString(String[] sa){
        return stringArrayToString(sa, "#");
    }
    
    public static String stringArrayToString(String[] sa, String separator){
        String ret = "";
        for (String s : sa)
            ret += separator + " " + s;
        
        return ret.replaceFirst(separator + " ", "");
    }
    
    public static String[] loadTextFromURL(URL url, Logger logger){
        return loadTextFromURL(url, logger, new String[] { "" }, 0);
    }
    
    public static String[] loadTextFromURL(URL url, Logger logger, int timeoutMS){
        return loadTextFromURL(url, logger, new String[] { "" }, timeoutMS);
    }
    
    public static String[] loadTextFromURL(URL url, Logger logger, String defaultValue){
        return loadTextFromURL(url, logger, new String[] { defaultValue }, 0);
    }
    
    public static String[] loadTextFromURL(URL url, Logger logger, String defaultValue, int timeoutMS){
        return loadTextFromURL(url, logger, new String[] { defaultValue }, timeoutMS);
    }
    
    public static String[] loadTextFromURL(URL url, Logger logger, String[] defaultValue){
        return loadTextFromURL(url, logger, defaultValue, 0);
    }
    
    public static String[] loadTextFromURL(URL url, Logger logger, String[] defaultValue, int timeoutMS){
        List<String> arraylist = new ArrayList<String>();
        Scanner scanner = null;
        try{
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(timeoutMS);
            uc.setConnectTimeout(timeoutMS);
            scanner = new Scanner(uc.getInputStream(), "UTF-8");
        }
        catch (Throwable e){
            logger.log(Level.WARNING, String.format("Error retrieving remote string value! Defaulting to %s", stringArrayToString(defaultValue)));
            return defaultValue;
        }
        
        while (scanner.hasNextLine()){
            arraylist.add(scanner.nextLine());
        }
        scanner.close();
        return arraylist.toArray(new String[arraylist.size()]);
    }
}
