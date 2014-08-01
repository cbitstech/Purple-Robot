package edu.northwestern.cbits.purple_robot_manager.tests;

import org.mozilla.javascript.NativeJavaObject;

import dclass.Method;
import junit.framework.Assert;
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

	private static String fetchString(Context ctx, String namespace, String key) {
		NativeJavaObject persisted = (NativeJavaObject) BaseScriptEngine.runScript(ctx, "PurpleRobot.fetchString('" + namespace + "','" + key + "');");
		return (String) persisted.unwrap();
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
		
		BaseScriptEngineTests.testLog(this._context, ++testCount);
		BaseScriptEngineTests.testplayTone(this._context, ++testCount);

		Log.d(CN+".test", fmtTestHeader("END TESTS!"));
	}
	
	
	/**
	 * Tests the methods in BaseScriptEngine.
	 * @author mohrlab
	 *
	 */
	public static class BaseScriptEngineTests {
		private static final String persistValue = "T";

		public static void testplayTone(Context ctx, int testCount) {
			String MN = "testplayTone";
			Log.d(CN+"."+MN, fmtTestHeader("TEST " + testCount + ": " + MN));
			StringBuilder sb = new StringBuilder();
			sb.append("PurpleRobot.playTone(null); PurpleRobot.persistString('" + CN + "','" + MN + "','" + persistValue + "');");
			BaseScriptEngine.runScript(ctx, sb.toString());
			Assert.assertEquals(true, persistValue.equals(JavascriptTestCase.fetchString(ctx, CN, MN)));
		}
		
		public static void testLog(Context ctx, int testCount) {
			String MN = "testPurpleRobotDotLog";
			Log.d(CN+"."+MN, fmtTestHeader("TEST " + testCount + ": " + MN));
			StringBuilder sb = new StringBuilder();
			sb.append("PurpleRobot.log('" + MN + ": test'); PurpleRobot.persistString('" + CN + "','" + MN + "','" + persistValue + "');");
			BaseScriptEngine.runScript(ctx, sb.toString());
			Assert.assertEquals(true, persistValue.equals(JavascriptTestCase.fetchString(ctx, CN, MN)));
		}
		
	}
	

	private void testScriptSizeViaPrnmExecution() {
		//TODO. Ensure PRNM is too big, then here, read PRNM JS file, pass it to runScript.
	}

	private void testScriptSizeViaLinearSizeIncreasingObjInstantiation(int testCount) {
		Log.d(CN+".testScriptSizeViaLinearSizeIncreasingObjInstantiation", fmtTestHeader("TEST " + testCount + ": testScriptSizeViaLinearSizeIncreasingObjInstantiation"));
		StringBuilder sb = new StringBuilder();

		int numStatements = 100000;
		// generate some JS
		sb.append("var myObj = { a: { b: 'B' }, c: [0] };");
		sb.append("var myObjArr = new Array();");
		for (int i = 0; i < numStatements; i += (numStatements/10)) {
			if((i % (numStatements/10)) == 0) { Log.d(CN+".testScriptSizeViaLinearSizeIncreasingObjInstantiation", "i = " + i); }
//			sb.append("for (var i = 0; i < 5; i++) {");
			sb.append("  myObjArr.push(Object.create(myObj));");
//			sb.append("}");
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
		
		Log.d(CN+".test", "script = " + (sb.toString()).substring(0, (sb.toString().length() - 1) > 1000 ? 1000 : sb.toString().length() - 1));		
	}
	
	private void testScriptSizeViaLargeNDateInstantiation(int testCount) {
		Log.d(CN+".testScriptSizeViaLargeNDateInstantiation", fmtTestHeader("TEST " + testCount + ": testScriptSizeViaLargeNDateInstantiation"));
		StringBuilder sb = new StringBuilder();

		int numStatements = 10000;
		// generate some JS
		for (int i = 0; i < numStatements; i +=  (numStatements/10)) {
			sb.append("for (var i = 0; i < 5; i++) {");
			sb.append("  var d = new Date();");
//			if((i % (numStatements/10)) == 0) {
//				sb.append("  PurpleRobot.log('d = ' + d);");
//			}
			sb.append("}");
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
	}

	private void testScriptSizeViaLinearIncreasingNoOpStatements(int testCount) {
		Log.d(CN+".testScriptSizeViaLinearIncreasingNoOpStatements", fmtTestHeader("TEST " + testCount + ": testScriptSize"));
		StringBuilder sb = new StringBuilder();
		int maxScriptSize = 1000000;
		for(int i = 0; i < maxScriptSize; i += (maxScriptSize / 10000)) {
			sb.append(";");
			if(i % (maxScriptSize / 10) == 0) { Log.d(CN+".testScriptSizeViaLinearIncreasingNoOpStatements:" + testCount, "pre: i = " + i); }
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
	}

	
	
	public static String fmtTestHeader(String hdrTitle) {
		return "----- " + hdrTitle + " -----";
	}
}
