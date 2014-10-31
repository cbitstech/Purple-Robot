package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;
import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.R;

public class JUnitTestCase extends RobotTestCase
{
    public JUnitTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    public void test()
    {
        if (this.isSelected(this._context) == false)
            return;

        Assert.assertTrue(true);
        Assert.assertFalse(false);
        Assert.assertNull(null);
        Assert.assertNotNull("");
    }

    public String name(Context context)
    {
        return this._context.getString(R.string.name_test_test);
    }

}
