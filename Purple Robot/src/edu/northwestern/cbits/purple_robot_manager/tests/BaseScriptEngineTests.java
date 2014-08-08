package edu.northwestern.cbits.purple_robot_manager.tests;

import java.util.Map;

import org.mozilla.javascript.NativeJavaObject;

import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.tests.JavascriptTestCase;
import junit.framework.Assert;
import android.content.Context;
import android.util.Log;

/**
 * Tests the methods in BaseScriptEngine.
 * @author mohrlab
 *
 */
public class BaseScriptEngineTests {
	private static final String CN = "BaseScriptEngineTests";
	private static final String persistValue = "T";

	
	// ----------- SUPPORT METHODS ----------
	

	/**
	 * Runs a test, given the script text to run, containing some persisted result value, and the means of fetching that result value upon script completion.  
	 * @param ctx Current context.
	 * @param expectedValue TODO
	 * @param testMethodName Name of the test method.
	 * @param testCount The current test count; indicates relative execution position among all tests.
	 * @param script Script to execute, or null if no test should run (should only ever be null if the test is not yet written).
	 * @param jsStringPersistNamespace Namespace of the result value, if using the 3 parameter variant of persistString/persistEncryptedString and fetchString/fetchEncryptedString, or null if using the 2 parameter variant.
	 * @param jsStringPersistKey Key to the result value.
	 * @param isEncryptedString Indicates whether to use the encrypted variants of persistString & fetchString.
	 * @param use2ParamFetchString Indicates whether to use the 3 or 2 parameter variants of persistString/persistEncryptedString and fetchString/fetchEncryptedString.
	 * @param logResultBeforeAssert TODO
	 */
	public static void runTest(Context ctx, String expectedValue, String testMethodName, int testCount, String script, String jsStringPersistNamespace, String jsStringPersistKey, boolean isEncryptedString, boolean use2ParamFetchString, boolean logResultBeforeAssert) {  String MN = "runTest";
		Log.d(CN+"."+testMethodName, JavascriptTestCase.fmtTestHeader("TEST " + testCount + ": " + jsStringPersistKey));
		if(script != null) {
			StringBuilder sb = new StringBuilder();
			sb.append(script);
			BaseScriptEngine.runScript(ctx, sb.toString());

			Log.d(CN+"."+testMethodName, "fetch for (namespace, key): (" + jsStringPersistNamespace + "," + jsStringPersistKey + ")");
			if(isEncryptedString) {
				if(use2ParamFetchString) {
					String s = JavascriptTestCase.fetchEncryptedString(ctx, jsStringPersistKey);
					if(logResultBeforeAssert) {
						Log.d(CN+"."+testMethodName, "encrypted string at k = " + jsStringPersistKey + ": " + s);
					}
					Assert.assertEquals("In " + testMethodName + ": ", true, expectedValue.equals(s));
				}
				else {
					String s = JavascriptTestCase.fetchEncryptedString(ctx, jsStringPersistNamespace, jsStringPersistKey);
					if(logResultBeforeAssert) {
						Log.d(CN+"."+testMethodName, "encrypted string at n = " + jsStringPersistNamespace + ", k = " + jsStringPersistKey + ": " + s);
					}
					Assert.assertEquals("In " + testMethodName + ": ", true, expectedValue.equals(s));
				}
			}
			else {
				if(use2ParamFetchString) {
					String s = JavascriptTestCase.fetchString(ctx, jsStringPersistKey);
					if(logResultBeforeAssert) {
						Log.d(CN+"."+testMethodName, "string at k = " + jsStringPersistKey + ": " + s);
					}
					Assert.assertEquals("In " + testMethodName + ": ", true, expectedValue.equals(s));
				}
				else {
					String s = JavascriptTestCase.fetchString(ctx, jsStringPersistNamespace, jsStringPersistKey);
					if(logResultBeforeAssert) {
						Log.d(CN+"."+testMethodName, "string at n = " + jsStringPersistNamespace + ", k = " + jsStringPersistKey + ": " + s);
					}
					Assert.assertEquals("In " + testMethodName + ": ", true, expectedValue.equals(s));
				}
			}
		}
	}

	public static String getScriptForPersistStringOnTestCompletedDefault(String mn) {
		return "PurpleRobot.persistString('" + CN + "','" + mn + "','" + persistValue + "');";
	}
	


	
	// ----------- TEST METHODS ----------
		
	public static void addModel(Context ctx, int testCount) {  String MN = "addModel";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void addNamespace(Context ctx, int testCount) {  String MN = "addNamespace";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void cancelScriptNotification(Context ctx, int testCount) {  String MN = "cancelScriptNotification";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void clearNativeDialogs(Context ctx, int testCount) {  String MN = "clearNativeDialogs";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void clearNativeDialogs_var01(Context ctx, int testCount) {  String MN = "clearNativeDialogs_var01";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void clearNativeDialogs_var02(Context ctx, int testCount) {  String MN = "clearNativeDialogs_var02";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void clearPassword(Context ctx, int testCount) {  String MN = "clearPassword";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void clearTriggers(Context ctx, int testCount) {  String MN = "clearTriggers";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void dateFromTimestamp(Context ctx, int testCount) {  String MN = "dateFromTimestamp";
//		Log.d(CN+"."+MN, JavascriptTestCase.fmtTestHeader(hdrTitle))
	}

	public static void deleteModel(Context ctx, int testCount) {  String MN = "deleteModel";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void deleteTrigger(Context ctx, int testCount) {  String MN = "deleteTrigger";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableAutoConfigUpdates(Context ctx, int testCount) {  String MN = "disableAutoConfigUpdates";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableBackgroundImage(Context ctx, int testCount) {  String MN = "disableBackgroundImage";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableEachProbe(Context ctx, int testCount) {  String MN = "disableEachProbe";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableModel(Context ctx, int testCount) {  String MN = "disableModel";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableProbe(Context ctx, int testCount) {  String MN = "disableProbe";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableProbes(Context ctx, int testCount) {  String MN = "disableProbes";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableTrigger(Context ctx, int testCount) {  String MN = "disableTrigger";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void disableUpdateChecks(Context ctx, int testCount) {  String MN = "disableUpdateChecks";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void emitToast(Context ctx, int testCount) {  String MN = "emitToast";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void emitToast_var01(Context ctx, int testCount) {  String MN = "emitToast_var01";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void emitToast_var02(Context ctx, int testCount) {  String MN = "emitToast_var02";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableAutoConfigUpdates(Context ctx, int testCount) {  String MN = "enableAutoConfigUpdates";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableBackgroundImage(Context ctx, int testCount) {  String MN = "enableBackgroundImage";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableModel(Context ctx, int testCount) {  String MN = "enableModel";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableProbe(Context ctx, int testCount) {  String MN = "enableProbe";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableProbes(Context ctx, int testCount) {  String MN = "enableProbes";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableTrigger(Context ctx, int testCount) {  String MN = "enableTrigger";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void enableUpdateChecks(Context ctx, int testCount) {  String MN = "enableUpdateChecks";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchEncryptedString(Context ctx, int testCount) {  String MN = "fetchEncryptedString";
		fetchEncryptedString_var01(ctx, testCount);
	}

	public static void fetchEncryptedString_var01(Context ctx, int testCount) {  String MN = "fetchEncryptedString_var01";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchEncryptedString_var02(Context ctx, int testCount) {  String MN = "fetchEncryptedString_var02";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchLabel(Context ctx, int testCount) {  String MN = "fetchLabel";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchLabels(Context ctx, int testCount) {  String MN = "fetchLabels";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchNamespaceMap(Context ctx, int testCount) {  String MN = "fetchNamespaceMap";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchNamespaces(Context ctx, int testCount) {  String MN = "fetchNamespaces";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchSnapshotIds(Context ctx, int testCount) {  String MN = "fetchSnapshotIds";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchString(Context ctx, int testCount) {  String MN = "fetchString";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchString_var01(Context ctx, int testCount) {  String MN = "fetchString_var01";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchString_var02(Context ctx, int testCount) {  String MN = "fetchString_var02";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchTrigger(Context ctx, int testCount) {  String MN = "fetchTrigger";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchTriggerIds(Context ctx, int testCount) {  String MN = "fetchTriggerIds";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchUserHash(Context ctx, int testCount) {  String MN = "fetchUserHash";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchUserId(Context ctx, int testCount) {  String MN = "fetchUserId";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fireTrigger(Context ctx, int testCount) {  String MN = "fireTrigger";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void formatDate(Context ctx, int testCount) {  String MN = "formatDate";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void launchApplication(Context ctx, int testCount) {  String MN = "launchApplication";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void launchInternalUrl(Context ctx, int testCount) {  String MN = "launchInternalUrl";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void launchUrl(Context ctx, int testCount) {  String MN = "launchUrl";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void log(Context ctx, int testCount) {  String MN = "log";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.log('" + MN + ": test'); PurpleRobot.persistString('" + CN + "','" + MN + "','" + persistValue + "');", CN, MN, false, false, false);
	}

	public static void now(Context ctx, int testCount) {  String MN = "now";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.now(); " + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
		
	}

	public static void packageForApplicationName(Context ctx, int testCount) {  String MN = "packageForApplicationName";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void parseDate(Context ctx, int testCount) {  String MN = "parseDate";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void persistEncryptedString(Context ctx, int testCount) {  String MN = "persistEncryptedString";
		persistEncryptedString_var01(ctx, testCount);
	}

	public static void persistEncryptedString_var01(Context ctx, int testCount) {  String MN = "persistEncryptedString_var01";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.persistEncryptedString('" + CN + "','" + MN + "','" + persistValue + "');", CN, MN, true, false, false);
	}
	
	public static void persistEncryptedString_var02(Context ctx, int testCount) {  String MN = "persistEncryptedString_var02";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.persistEncryptedString('" + CN + MN + "','" + persistValue + "');", null, CN+MN, true, true, false);
	}

	public static void persistString(Context ctx, int testCount) {  String MN = "persistString";
		runTest(ctx, persistValue, CN+"."+MN, testCount, "PurpleRobot.persistString('" + CN+"."+MN + "','" + persistValue + "');", null, CN+"."+MN, false, true, false);
	}

	public static void playDefaultTone(Context ctx, int testCount) {  String MN = "playDefaultTone";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.playDefaultTone(); PurpleRobot.persistString('" + CN + "','" + MN + "','" + persistValue + "');", CN, MN, false, false, false);
	}

	public static void playTone(Context ctx, int testCount) {  String MN = "playTone";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.playTone(null); PurpleRobot.persistString('" + CN + "','" + MN + "','" + persistValue + "');", CN, MN, false, false, false);
	}

	public static void probesState(Context ctx, int testCount) {  String MN = "probesState";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void readUrl(Context ctx, int testCount) {  String MN = "readUrl";
		runTest(ctx, persistValue, MN+".1", testCount,  "PurpleRobot.readUrl('http://www.northwestern.edu'); " + getScriptForPersistStringOnTestCompletedDefault(MN+".1"), CN, MN+".1", false, false, false);
		
	}

	public static void resetTrigger(Context ctx, int testCount) {  String MN = "resetTrigger";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void restoreDefaultId(Context ctx, int testCount) {  String MN = "restoreDefaultId";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void run(Context ctx, int testCount) {  String MN = "run";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

//	public static void runScript_var01(Context ctx, int testCount) {  String MN = "runScript_var01";
//	// runScript(Context context, String script)
//		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.runScript('PurpleRobot.log(\'test message from " + MN + "\');');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
//		
//	}
//
//	public static void runScript_var02(Context ctx, int testCount) {  String MN = "runScript_var02";
//		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
//		
//	}

	public static void scheduleScript(Context ctx, int testCount) {  String MN = "scheduleScript";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}
	
	public static void setPassword(Context ctx, int testCount) {  String MN = "setPassword";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}
	
	public static void setUserId(Context ctx, int testCount) {  String MN = "setUserId";
		runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void showApplicationLaunchNotification_var01(Context ctx, int testCount) { String MN = "showApplicationLaunchNotification_var01";
	// showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, Map<String,Object> launchParams, final String script)
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.showApplicationLaunchNotification('" + MN + " title', '" + MN + " message', 'com.google.android.gm', 0, {}, '');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	public static void showApplicationLaunchNotification_var02(Context ctx, int testCount) { String MN = "showApplicationLaunchNotification_var02";
	// showApplicationLaunchNotification(String title, String message, String applicationName, long displayWhen, boolean persistent, Map<String,Object> launchParams, final String script)
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.showApplicationLaunchNotification('" + MN + " title', '" + MN + " message', 'com.google.android.gm', 0, false, {}, '');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	public static void showNativeDialog_var01(Context ctx, int testCount) {  String MN = "showNativeDialog_var01";
	// showNativeDialog(final String title, final String message, final String confirmTitle, final String cancelTitle, final String confirmScript, final String cancelScript)
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.showNativeDialog('" + MN + " title', '" + MN + " message', 'Yes', 'No', '', '');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);		
	}

	public static void showNativeDialog_var02(Context ctx, int testCount) {  String MN = "showNativeDialog_var02";
	// showNativeDialog(final String title, final String message, final String confirmTitle, final String cancelTitle, final String confirmScript, final String cancelScript, String tag, long priority)
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.showNativeDialog('" + MN + " title', '" + MN + " message', 'Yes', 'No', '', '', '" + (CN+"."+MN) + "', 10);" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);		
	}

	public static void showScriptNotification_var01(Context ctx, int testCount) {  String MN = "showScriptNotification_var01";
	// showScriptNotification(String title, String message, boolean persistent, final String script)	
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.showScriptNotification('showScriptNotification_var01 title', 'showScriptNotification_var01 message', false, '');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	public static void showScriptNotification_var02(Context ctx, int testCount) {  String MN = "showScriptNotification_var02";
	// showScriptNotification(String title, String message, boolean persistent, boolean sticky, final String script)	
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.showScriptNotification('showScriptNotification_var02 title', 'showScriptNotification_var02 message', false, false, '');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	public static void testLog(Context ctx, int testCount) {  String MN = "testLog";
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.testLog('test message from testLog.');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);		
	}

	public static void updateConfig(Context ctx, int testCount) {  String MN = "updateConfig";
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.updateConfig({'config_show_background': true});" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);		
	// TODO: verify that the PR config was actually updated (read the value from the PR cfg)
	}

	public static void updateConfigUrl(Context ctx, int testCount) {  String MN = "updateConfigUrl";
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.updateConfigUrl('http://localhost');" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	// TODO: verify that the PR cfg URL was actually updated (read the PR cfg URL field)
	}

	public static void updateWidget(Context ctx, int testCount) {  String MN = "updateWidget";
	runTest(ctx, persistValue, MN, testCount, "PurpleRobot.updateWidget({identifier: '" + MN + "', title: 'myVal'});" + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);		
	}

	public static void valueFromString(Context ctx, int testCount) {  String MN = "valueFromString";
		runTest(ctx, "myval", MN, testCount, "var v = PurpleRobot.valueFromString('myKey', '{ myKey: \"myval\" }'); PurpleRobot.persistString('" + CN + "','" + MN + "', v);", CN, MN, false, false, true);
	}

	public static void version(Context ctx, int testCount) {  String MN = "version";
		runTest(ctx, persistValue, MN+".1", testCount, "PurpleRobot.version();" + getScriptForPersistStringOnTestCompletedDefault(MN+".1"), CN, MN+".1", false, false, true);
		//TODO: refactor the expected value here so this test doesn't break every time PR is updated!
		runTest(ctx, "1.5.15", MN+".2", testCount, "PurpleRobot.persistString('" + CN + "','" + MN+".2" + "',PurpleRobot.version());", CN, MN+".2", false, false, true);
	}

	public static void versionCode(Context ctx, int testCount) {  String MN = "versionCode";
		runTest(ctx, persistValue, MN+".1", testCount, "PurpleRobot.versionCode();" + getScriptForPersistStringOnTestCompletedDefault(MN+".1"), CN, MN+".1", false, false, true);
		//TODO: refactor the expected value here so this test doesn't break every time PR is updated!
		runTest(ctx, "10515", MN+".2", testCount, "PurpleRobot.persistString('" + CN + "','" + MN+".2" + "',PurpleRobot.versionCode());", CN, MN+".2", false, false, true);
	}

	public static void vibrate(Context ctx, int testCount) {  String MN = "vibrate";
		runTest(ctx, persistValue, MN, testCount, "PurpleRobot.vibrate('buzz'); " + getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	/**
	 * Runs specified function and stores the return value of the function in a persistent string. 
	 * @param mn Name of the test method.
	 * @param fnReturningAValue Code to run in an eval() statement.
	 * @return
	 */
	private static String getJavaScriptForPersistReturnedStringForPassedOp(String mn, String fnReturningAValue) {
		return "PurpleRobot.persistString('" + CN + "','" + mn + "',' eval('" + (fnReturningAValue) + "');";
	}

}