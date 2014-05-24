package openmods.utils;

import java.io.File;
import java.net.URL;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import openmods.Log;

import com.google.common.collect.Ordering;

/**
 * This class retrieves a version string from a text file at a given URL and
 * compares it against the locally provided version string. It
 * uses NaturalOrderComparator.java to determine if the remote version is newer
 * than our local version.
 * 
 * This is a non-nagging implementation. An example implementation of the source
 * version of this version checker can be found at:
 * https://github.com/bspkrs/bspkrsCore/tree/master/testmod/bspkrs/testmod
 * 
 * @author bspkrs
 */

public class ModVersionChecker {
	private URL versionURL;
	private final String modName;
	private final String proposedVer;
	private final String currentVer;
	private String updateURL;
	private String[] loadMsg;
	private String[] inGameMsg;
	private File trackerFile;
	private File trackerDir;
	private static Configuration versionCheckTracker;
	private final String lastNewVersionFound;

	public ModVersionChecker(String modName, String oldVer, String versionURL, String updateURL, String[] loadMsg, String[] inGameMsg) {
		this(modName, oldVer, versionURL, updateURL, loadMsg, inGameMsg, 3000);
	}

	public ModVersionChecker(String modName, String currentVer, String versionURL, String updateURL, String[] loadMsg, String[] inGameMsg, int timeoutMS) {
		this.modName = modName;
		this.currentVer = currentVer;
		this.updateURL = updateURL;
		this.loadMsg = loadMsg;
		this.inGameMsg = inGameMsg;

		try {
			this.versionURL = new URL(versionURL);
			Log.info("Initializing ModVersionChecker for mod %s", modName);
		} catch (Throwable e) {
			Log.warn("Error initializing ModVersionChecker for mod %s: %s", modName, e.getMessage());
		}

		String[] versionLines = MiscUtils.loadTextFromURL(this.versionURL, new String[] { currentVer }, timeoutMS);

		proposedVer = versionLines[0].trim();

		// Keep track of the versions we've seen to keep from nagging players
		// with new version notifications beyond the first one
		if (trackerDir == null) {
			trackerDir = new File(MCUtils.getConfigDir() + "/OpenModsLib/");
			if (trackerDir.exists() || trackerDir.mkdirs()) trackerFile = new File(trackerDir, "ModVersionCheckerTracking.txt");
		}

		if (versionCheckTracker == null) versionCheckTracker = new Configuration(trackerFile);

		versionCheckTracker.load();
		ConfigCategory cc = versionCheckTracker.getCategory("version_check_tracker");

		if (!cc.containsKey(modName)) versionCheckTracker.get("version_check_tracker", modName, currentVer);

		if (isNewerVersion(currentVer, proposedVer)) lastNewVersionFound = proposedVer;
		else lastNewVersionFound = cc.get(modName).getString();

		cc.get(modName).set(proposedVer);

		versionCheckTracker.save();

		setLoadMessage(loadMsg);
		setInGameMessage(inGameMsg);
	}

	public ModVersionChecker(String modName, String oldVer, String versionURL, String updateURL) {
		this(modName, oldVer, versionURL, updateURL, new String[] { "{modName} {oldVer} is out of date! Visit {updateURL} to download the latest release ({newVer})." }, new String[] { "\247c{modName} {newVer} \247ris out! Download the latest from \247a{updateURL}\247r" });
	}

	public void checkVersionWithLogging() {
		if (!isNewerVersion(currentVer, proposedVer)) for (String msg : loadMsg)
			Log.info(msg);
	}

	public void setLoadMessage(String[] loadMsg) {
		this.loadMsg = loadMsg;

		for (int i = 0; i < this.loadMsg.length; i++)
			this.loadMsg[i] = replaceAllTags(this.loadMsg[i]);
	}

	public void setInGameMessage(String[] inGameMsg) {
		this.inGameMsg = inGameMsg;

		for (int i = 0; i < this.inGameMsg.length; i++)
			this.inGameMsg[i] = replaceAllTags(this.inGameMsg[i]);

	}

	public String[] getLoadMessage() {
		return loadMsg;
	}

	public String[] getInGameMessage() {
		return inGameMsg;
	}

	public boolean isCurrentVersion() {
		return isNewerVersion(lastNewVersionFound, proposedVer);
	}

	public static boolean isNewerVersion(String currentVer, String proposedVer) {
		return Ordering.natural().compare(currentVer, proposedVer) >= 0;
	}

	private String replaceAllTags(String s) {
		return s.replace("{oldVer}", currentVer).replace("{newVer}", proposedVer).replace("{modName}", modName).replace("{updateURL}", updateURL);
	}
}
