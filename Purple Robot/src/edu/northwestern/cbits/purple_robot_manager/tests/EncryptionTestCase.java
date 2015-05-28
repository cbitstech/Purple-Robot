package edu.northwestern.cbits.purple_robot_manager.tests;

import junit.framework.Assert;
import android.content.Context;
import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;

public class EncryptionTestCase extends RobotTestCase
{
    public EncryptionTestCase(Context context, int priority)
    {
        super(context, priority);
    }

    @Override
    public void test()
    {
        if (!this.isSelected(this._context))
            return;

        Assert.assertEquals("55502f40dc8b7c769880b10874abc9d0",
                EncryptionManager.getInstance().createHash(this._context, "test@example.com"));

        Assert.assertEquals("973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b", EncryptionManager
                .getInstance().createHash(this._context, "test@example.com", "SHA-256"));
    }

    @Override
    public String name(Context context)
    {
        return context.getString(R.string.name_encryption_test);
    }
}
