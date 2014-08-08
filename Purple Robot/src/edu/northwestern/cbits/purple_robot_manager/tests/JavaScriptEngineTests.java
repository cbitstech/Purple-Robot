package edu.northwestern.cbits.purple_robot_manager.tests;

import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import android.content.Context;

/**
 * Tests the methods in JavaScriptEngine.
 * @author mohrlab
 *
 */
public class JavaScriptEngineTests {
	private static final String CN = "JavaScriptEngineTests";
	private static final String persistValue = "T";

	public static void broadcastIntent(Context ctx, int testCount) {  String MN = "broadcastIntent";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void canRun(Context ctx, int testCount) {  String MN = "canRun";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void emitReading(Context ctx, int testCount) {  String MN = "emitReading";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchConfig(Context ctx, int testCount) {  String MN = "fetchConfig";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchNamespaces(Context ctx, int testCount) {  String MN = "fetchNamespaces";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchSnapshotIds(Context ctx, int testCount) {  String MN = "fetchSnapshotIds";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchSnapshotIds_var01(Context ctx, int testCount) {  String MN = "fetchSnapshotIds_var01";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchSnapshotIds_var02(Context ctx, int testCount) {  String MN = "fetchSnapshotIds_var02";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchTriggerIds(Context ctx, int testCount) {  String MN = "fetchTriggerIds";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void fetchWidget(Context ctx, int testCount) {  String MN = "fetchWidget";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void JSONnativeToJson(Context ctx, int testCount) {  String MN = "JSONnativeToJson";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void launchApplication(Context ctx, int testCount) {  String MN = "launchApplication";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void loadLibrary(Context ctx, int testCount) {  String MN = "loadLibrary";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void log(Context ctx, int testCount) {  String MN = "log";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void models(Context ctx, int testCount) {  String MN = "models";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void nativeToJson(Context ctx, int testCount) {  String MN = "nativeToJson";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void predictions(Context ctx, int testCount) {  String MN = "predictions";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void readings(Context ctx, int testCount) {  String MN = "readings";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void runScript(Context ctx, int testCount) {  String MN = "runScript";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void runScript_var01(Context ctx, int testCount) {  String MN = "runScript_var01";
	// TODO: complete this. The following line causes the test method to die silently, preventing subsequent tests from executing. Not sure why.
	//		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, "PurpleRobot.runScript('PurpleRobot.log(\"test message\");');" + BaseScriptEngineTests.getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	public static void runScript_var02(Context ctx, int testCount) {  String MN = "runScript_var02";
	// TODO: complete this. The following line probably (given the state of runScript_var01) causes the test method to die silently, preventing subsequent tests from executing.
//		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, "PurpleRobot.runScript('PurpleRobot.log(\"test message from " + MN + "\");', '', {});" + BaseScriptEngineTests.getScriptForPersistStringOnTestCompletedDefault(MN), CN, MN, false, false, false);
	}

	public static void showApplicationLaunchNotification(Context ctx, int testCount) { String MN = "showApplicationLaunchNotification";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void showApplicationLaunchNotification_var01(Context ctx, int testCount) {  String MN = "showApplicationLaunchNotification_var01";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void showApplicationLaunchNotification_var02(Context ctx, int testCount) {  String MN = "showApplicationLaunchNotification_var02";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void updateConfig(Context ctx, int testCount) {  String MN = "updateConfig";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void updateTrigger(Context ctx, int testCount) {  String MN = "updateTrigger";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void updateWidget(Context ctx, int testCount) {  String MN = "updateWidget";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	public static void widgets(Context ctx, int testCount) {  String MN = "widgets";
		BaseScriptEngineTests.runTest(ctx, persistValue, MN, testCount, null, CN, MN, false, false, false);
		
	}

	
}