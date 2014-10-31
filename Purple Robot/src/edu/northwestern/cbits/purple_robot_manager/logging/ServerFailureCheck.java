package edu.northwestern.cbits.purple_robot_manager.logging;

import edu.northwestern.cbits.purple_robot_manager.R;
import android.content.Context;

public class ServerFailureCheck extends SanityCheck
{
    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_server_failure);
    }

    public void runCheck(Context context)
    {

    }
}
