package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;
import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.scripting.BaseScriptEngine;
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
		
//		testScriptSizeViaLinearIncreasingNoOpStatements(++testCount);
		
		testScriptSizeViaLargeNDateInstantiation(++testCount);
		
		testScriptSizeViaLinearSizeIncreasingObjInstantiation(++testCount);

		Log.d(CN+".test", fmtTestHeader("END TESTS!"));
	}

	private void testScriptSizeViaLinearSizeIncreasingObjInstantiation(int testCount) {
		Log.d(CN+".test", fmtTestHeader("TEST " + testCount + ": testScriptSizeWithNonTrivialGeneratedContent"));
		StringBuilder sb = new StringBuilder();

		int numStatements = 10000;
		// generate some JS
		sb.append("var myObj = { a: { b: { }, c: [0] };");
		sb.append("var myObjArr = [];");
		for (int i = 0; i < numStatements; i++) {
			sb.append("for (var i = 0; i < 5; i++) {");
			sb.append("  myObjArr.push(Object.create(myObj));");
			sb.append("}");
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
		
		Log.d(CN+".test", "script = " + (sb.toString()).substring(0, 1000));
		BaseScriptEngine.runScript(this._context, sb.toString());
	}
	
	private void testScriptSizeViaLargeNDateInstantiation(int testCount) {
		Log.d(CN+".test", fmtTestHeader("TEST " + testCount + ": testScriptSizeWithNonTrivialGeneratedContent"));
		StringBuilder sb = new StringBuilder();

		int numStatements = 10000;
		// generate some JS
		for (int i = 0; i < numStatements; i++) {
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
		Log.d(CN+".test", fmtTestHeader("TEST " + testCount + ": testScriptSize"));
		StringBuilder sb = new StringBuilder();
		int maxScriptSize = 1000000;
		for(int i = 0; i < maxScriptSize; i += (maxScriptSize / 1000)) {
			sb.append(";");
			if(i % (maxScriptSize / 10) == 0) { Log.d(CN+".test:" + testCount, "pre: i = " + i); }
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
	}

	
	
	public String fmtTestHeader(String hdrTitle) {
		return "----- " + hdrTitle + " -----";
	}
}
