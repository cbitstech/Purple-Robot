package edu.northwestern.cbits.purple_robot_manager.tests;


import org.mozilla.javascript.NativeJavaObject;

import dclass.Method;
import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
import edu.northwestern.cbits.purple_robot_manager.scripting.JavaScriptEngine;
import android.util.Log;
/**
 * @author mohrlab
 *
 */
public class JavascriptTestCase extends RobotTestCase {

	public static String CN = "JavascriptTestCase";
	
	
	public JavascriptTestCase(Context context, int priority) {
		super(context, priority);
		Log.d(CN+".ctor", "entered");
	}

	/* (non-Javadoc)
	 * @see src.edu.northwestern.cbits.purple_robot_manager.tests.RobotTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		Log.d(CN+".setUp", "entered");
	}

	/* (non-Javadoc)
	 * @see android.test.AndroidTestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		Log.d(CN+".tearDown", "entered");
	}

	@Override
	public String name(Context context) {
		Log.d(CN+".name", "entered");
		return context.getString(R.string.name_javascript_test);
	}

	@Override
	public void test() {
		Log.d(CN+".test", "entered");
		
		if(!this.isSelected(this._context))
			return;
		
		Log.d(CN+".test", fmtTestHeader("BEGIN TESTS!"));
		int testCount = 0;
		
		// TODO: testScriptSizeViaPrnmExecution(++testCount);
		testScriptSizeViaLinearIncreasingNoOpStatements(++testCount);
		testScriptSizeViaLargeNDateInstantiation(++testCount);
		testScriptSizeViaLinearSizeIncreasingObjInstantiation(++testCount);
		
	    BaseScriptEngineTests.addModel(this._context, ++testCount);
	    BaseScriptEngineTests.addNamespace(this._context, ++testCount);
	    BaseScriptEngineTests.cancelScriptNotification(this._context, ++testCount);
	    BaseScriptEngineTests.clearNativeDialogs_var01(this._context, ++testCount);
	    BaseScriptEngineTests.clearNativeDialogs_var02(this._context, ++testCount);
	    BaseScriptEngineTests.clearPassword(this._context, ++testCount);
	    BaseScriptEngineTests.clearTriggers(this._context, ++testCount);
	    BaseScriptEngineTests.dateFromTimestamp(this._context, ++testCount);
	    BaseScriptEngineTests.deleteModel(this._context, ++testCount);
	    BaseScriptEngineTests.deleteTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.disableAutoConfigUpdates(this._context, ++testCount);
	    BaseScriptEngineTests.disableBackgroundImage(this._context, ++testCount);
	    BaseScriptEngineTests.disableEachProbe(this._context, ++testCount);
	    BaseScriptEngineTests.disableModel(this._context, ++testCount);
	    BaseScriptEngineTests.disableProbe(this._context, ++testCount);
	    BaseScriptEngineTests.disableProbes(this._context, ++testCount);
	    BaseScriptEngineTests.disableTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.disableUpdateChecks(this._context, ++testCount);
	    BaseScriptEngineTests.emitToast_var01(this._context, ++testCount);
	    BaseScriptEngineTests.emitToast_var02(this._context, ++testCount);
	    BaseScriptEngineTests.enableAutoConfigUpdates(this._context, ++testCount);
	    BaseScriptEngineTests.enableBackgroundImage(this._context, ++testCount);
	    BaseScriptEngineTests.enableModel(this._context, ++testCount);
	    BaseScriptEngineTests.enableProbe(this._context, ++testCount);
	    BaseScriptEngineTests.enableProbes(this._context, ++testCount);
	    BaseScriptEngineTests.enableTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.enableUpdateChecks(this._context, ++testCount);
	    BaseScriptEngineTests.fetchEncryptedString_var01(this._context, ++testCount);
	    BaseScriptEngineTests.fetchEncryptedString_var02(this._context, ++testCount);
	    BaseScriptEngineTests.fetchLabel(this._context, ++testCount);
	    BaseScriptEngineTests.fetchLabels(this._context, ++testCount);
	    BaseScriptEngineTests.fetchNamespaceMap(this._context, ++testCount);
	    BaseScriptEngineTests.fetchNamespaces(this._context, ++testCount);
//	    BaseScriptEngineTests.fetchSnapshotIds(this._context, ++testCount);
	    BaseScriptEngineTests.fetchString_var01(this._context, ++testCount);
	    BaseScriptEngineTests.fetchString_var02(this._context, ++testCount);
	    BaseScriptEngineTests.fetchTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.fetchTriggerIds(this._context, ++testCount);
	    BaseScriptEngineTests.fetchUserHash(this._context, ++testCount);
	    BaseScriptEngineTests.fetchUserId(this._context, ++testCount);
	    BaseScriptEngineTests.fireTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.formatDate(this._context, ++testCount);
	    BaseScriptEngineTests.launchApplication(this._context, ++testCount);
	    BaseScriptEngineTests.launchInternalUrl(this._context, ++testCount);
	    BaseScriptEngineTests.launchUrl(this._context, ++testCount);
	    BaseScriptEngineTests.log(this._context, ++testCount);
	    BaseScriptEngineTests.now(this._context, ++testCount);
	    BaseScriptEngineTests.packageForApplicationName(this._context, ++testCount);
	    BaseScriptEngineTests.parseDate(this._context, ++testCount);
	    BaseScriptEngineTests.persistEncryptedString_var01(this._context, ++testCount);
	    BaseScriptEngineTests.persistEncryptedString_var02(this._context, ++testCount);
	    BaseScriptEngineTests.persistString(this._context, ++testCount);
	    BaseScriptEngineTests.persistString(this._context, ++testCount);
	    BaseScriptEngineTests.playDefaultTone(this._context, ++testCount);
	    BaseScriptEngineTests.playTone(this._context, ++testCount);
	    BaseScriptEngineTests.probesState(this._context, ++testCount);
	    BaseScriptEngineTests.readUrl(this._context, ++testCount);
	    BaseScriptEngineTests.resetTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.restoreDefaultId(this._context, ++testCount);
	    BaseScriptEngineTests.run(this._context, ++testCount);
//	    BaseScriptEngineTests.runScript_var01(this._context, ++testCount);
//	    BaseScriptEngineTests.runScript_var02(this._context, ++testCount);
	    BaseScriptEngineTests.scheduleScript(this._context, ++testCount);
	    BaseScriptEngineTests.setPassword(this._context, ++testCount);
	    BaseScriptEngineTests.setUserId(this._context, ++testCount);
	    BaseScriptEngineTests.setUserId(this._context, ++testCount);
	    BaseScriptEngineTests.showApplicationLaunchNotification_var01(this._context, ++testCount);
	    BaseScriptEngineTests.showApplicationLaunchNotification_var02(this._context, ++testCount);
	    BaseScriptEngineTests.showNativeDialog_var01(this._context, ++testCount);
	    BaseScriptEngineTests.showNativeDialog_var02(this._context, ++testCount);
	    BaseScriptEngineTests.showScriptNotification_var01(this._context, ++testCount);
	    BaseScriptEngineTests.showScriptNotification_var02(this._context, ++testCount);
	    BaseScriptEngineTests.testLog(this._context, ++testCount);
	    BaseScriptEngineTests.updateConfig(this._context, ++testCount);
	    BaseScriptEngineTests.updateConfigUrl(this._context, ++testCount);
	    BaseScriptEngineTests.updateWidget(this._context, ++testCount);
	    BaseScriptEngineTests.valueFromString(this._context, ++testCount);
	    BaseScriptEngineTests.version(this._context, ++testCount);
	    BaseScriptEngineTests.versionCode(this._context, ++testCount);
	    BaseScriptEngineTests.vibrate(this._context, ++testCount);
	    
	    JavaScriptEngineTests.broadcastIntent(this._context, ++testCount);
	    JavaScriptEngineTests.canRun(this._context, ++testCount);
	    JavaScriptEngineTests.emitReading(this._context, ++testCount);
	    JavaScriptEngineTests.fetchConfig(this._context, ++testCount);
	    JavaScriptEngineTests.fetchNamespaces(this._context, ++testCount);
	    JavaScriptEngineTests.fetchNamespaces(this._context, ++testCount);
	    JavaScriptEngineTests.fetchSnapshotIds_var01(this._context, ++testCount);
	    JavaScriptEngineTests.fetchSnapshotIds_var02(this._context, ++testCount);
	    JavaScriptEngineTests.fetchTriggerIds(this._context, ++testCount);
	    JavaScriptEngineTests.fetchTriggerIds(this._context, ++testCount);
	    JavaScriptEngineTests.fetchWidget(this._context, ++testCount);
	    JavaScriptEngineTests.JSONnativeToJson(this._context, ++testCount); 
	    JavaScriptEngineTests.launchApplication(this._context, ++testCount);
	    JavaScriptEngineTests.loadLibrary(this._context, ++testCount);
	    JavaScriptEngineTests.log(this._context, ++testCount);
	    JavaScriptEngineTests.models(this._context, ++testCount);
	    JavaScriptEngineTests.nativeToJson(this._context, ++testCount);
	    JavaScriptEngineTests.predictions(this._context, ++testCount);
	    JavaScriptEngineTests.readings(this._context, ++testCount);
	    JavaScriptEngineTests.runScript_var01(this._context, ++testCount);
	    JavaScriptEngineTests.runScript_var02(this._context, ++testCount);
	    JavaScriptEngineTests.showApplicationLaunchNotification_var01(this._context, ++testCount);
	    JavaScriptEngineTests.showApplicationLaunchNotification_var02(this._context, ++testCount);
	    JavaScriptEngineTests.updateConfig(this._context, ++testCount);
	    JavaScriptEngineTests.updateTrigger(this._context, ++testCount);
	    JavaScriptEngineTests.updateWidget(this._context, ++testCount);
	    JavaScriptEngineTests.updateWidget(this._context, ++testCount);
	    JavaScriptEngineTests.widgets(this._context, ++testCount);
	    
	    // ----- tests of protected BaseScriptEngine methods
	    BaseScriptEngineTests.language(this._context, ++testCount);
	    BaseScriptEngineTests.transmitData(this._context, ++testCount);
//	    BaseScriptEngineTests.updateWidget(this._context, ++testCount);
	    BaseScriptEngineTests.constructLaunchIntent(this._context, ++testCount);
	    BaseScriptEngineTests.constructDirectLaunchIntent(this._context, ++testCount);
	    BaseScriptEngineTests.updateTrigger(this._context, ++testCount);
	    BaseScriptEngineTests.updateProbe(this._context, ++testCount);
//	    BaseScriptEngineTests.launchApplication(this._context, ++testCount);
//	    BaseScriptEngineTests.showApplicationLaunchNotification_01(this._context, ++testCount);
//	    BaseScriptEngineTests.showApplicationLaunchNotification_var02(this._context, ++testCount);
//	    BaseScriptEngineTests.updateWidget(this._context, ++testCount);
	    BaseScriptEngineTests.fetchWidget(this._context, ++testCount);
	    BaseScriptEngineTests.widgets(this._context, ++testCount);
	    BaseScriptEngineTests.broadcastIntent(this._context, ++testCount);
//	    BaseScriptEngineTests.updateConfig(this._context, ++testCount);

		Log.d(CN+".test", fmtTestHeader("END TESTS!"));
	}


	private void testScriptSizeViaPrnmExecution() {
		//TODO. Ensure PRNM is too big, then here, read PRNM JS file, pass it to runScript.
	}

	private void testScriptSizeViaLinearSizeIncreasingObjInstantiation(int testCount) {
		String MN = "testScriptSizeViaLinearSizeIncreasingObjInstantiation"; 
		Log.d(CN+"."+MN, fmtTestHeader("TEST " + testCount + ": "+MN));
		StringBuilder sb = new StringBuilder();

		int numStatements = 100000;
		// generate some JS
		sb.append("var myObj = { a: { b: 'B' }, c: [0] };");
		sb.append("var myObjArr = new Array();");
		for (int i = 0; i < numStatements; i += (numStatements/10)) {
			if((i % (numStatements/10)) == 0) { Log.d(CN+"."+MN, "i = " + i); }
			sb.append("  myObjArr.push(Object.create(myObj));");
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
		
		Log.d(CN+"."+MN, "script = " + (sb.toString()).substring(0, (sb.toString().length() - 1) > 1000 ? 1000 : sb.toString().length() - 1));		
	}
	
	private void testScriptSizeViaLargeNDateInstantiation(int testCount) {
		String MN = "testScriptSizeViaLargeNDateInstantiation";
		Log.d(CN+"."+MN, fmtTestHeader("TEST " + testCount + ": "+MN));
		StringBuilder sb = new StringBuilder();

		int numStatements = 10000;
		// generate some JS
		for (int i = 0; i < numStatements; i +=  (numStatements/10)) {
			sb.append("for (var i = 0; i < 5; i++) {");
			sb.append("  var d = new Date();");
			sb.append("}");
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
	}

	private void testScriptSizeViaLinearIncreasingNoOpStatements(int testCount) {
		String MN = "testScriptSizeViaLinearIncreasingNoOpStatements";
		Log.d(CN+"."+MN, fmtTestHeader("TEST " + testCount + ": "+MN));
		StringBuilder sb = new StringBuilder();
		int maxScriptSize = 1000000;
		for(int i = 0; i < maxScriptSize; i += (maxScriptSize / 100)) {
			sb.append(";");
			if(i % (maxScriptSize / 10) == 0) { Log.d(CN+"."+MN+":" + testCount, "pre: i = " + i); }
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
	}

	public static String fetchString(Context ctx, String key) {
		Log.d(CN, "1");
		NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(ctx, "PurpleRobot.fetchString('" + key + "');");
		Log.d(CN, "2");
		return (String) persisted.unwrap();
	}
	
	public static String fetchString(Context ctx, String namespace, String key) {
		NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(ctx, "PurpleRobot.fetchString('" + namespace + "','" + key + "');");
		return (String) persisted.unwrap();
	}
	
	public static String fetchEncryptedString(Context ctx, String key) {
		NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(ctx, "PurpleRobot.fetchEncryptedString('" + key + "');");
		return (String) persisted.unwrap();
	}

	public static String fetchEncryptedString(Context ctx, String namespace, String key) {
		NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(ctx, "PurpleRobot.fetchEncryptedString('" + namespace + "','" + key + "');");
		return (String) persisted.unwrap();
	}

	public static String fmtTestHeader(String hdrTitle) {
		return "----- " + hdrTitle + " -----";
	}
}