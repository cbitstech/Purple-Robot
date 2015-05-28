package edu.northwestern.cbits.purple_robot_manager.logging;

import android.content.Context;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.triggers.DateTrigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.Trigger;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;

public class ByMinuteTriggerCheck extends SanityCheck
{
    public void runCheck(Context context)
    {
        TriggerManager triggers = TriggerManager.getInstance(context);

        this._errorMessage = null;
        this._errorLevel = SanityCheck.OK;

        for (Trigger trigger : triggers.allTriggers())
        {
            if (trigger instanceof DateTrigger)
            {
                DateTrigger dateTrigger = (DateTrigger) trigger;

                String calString = dateTrigger.getCalendarString();

                if (calString.contains("FREQ=MINUTELY") && calString.contains("BYMINUTE="))
                {
                    this._errorLevel = SanityCheck.WARNING;
                    this._errorMessage = context.getString(R.string.name_sanity_trigger_byminute_warning, dateTrigger.name());

                    return;
                }
            }
        }
    }

    public String name(Context context)
    {
        return context.getString(R.string.name_sanity_trigger_byminute);
    }
}
