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
		
		testScriptSize(++testCount);
		
		testScriptSizeWithNonTrivialGeneratedContent(++testCount);

		Log.d(CN+".test", fmtTestHeader("END TESTS!"));
	}

	private void testScriptSizeWithNonTrivialGeneratedContent(int testCount) {
		
	}

	private void testScriptSize(int testCount) {
		Log.d(CN+".test", fmtTestHeader("TEST " + testCount + ": script size"));
		StringBuilder sb = new StringBuilder();
		int maxScriptSize = 1000000;
		for(int i = 0; i < maxScriptSize; i += (maxScriptSize / 1000)) {
			sb.append(";");
			if(i % (maxScriptSize / 10) == 0) { Log.d(CN+".test:01", "pre: i = " + i); }
			BaseScriptEngine.runScript(this._context, sb.toString());
		}
	}

	
	
	public String fmtTestHeader(String hdrTitle) {
		return "----- " + hdrTitle + " -----";
	}
}
