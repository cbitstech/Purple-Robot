package edu.northwestern.cbits.purple_robot_manager.triggers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;

public class TriggerManager 
{
	private static final TriggerManager _instance = new TriggerManager();
    
	private List<Trigger> _triggers = new ArrayList<Trigger>();
	
    private TriggerManager() 
    {
        if (TriggerManager._instance != null)
            throw new IllegalStateException("Already instantiated");
    }

    public static TriggerManager getInstance() 
    {
        return TriggerManager._instance;
    }

	public void nudgeTriggers(Context context)
	{
		Date now = new Date();
		
		synchronized(this._triggers)
		{
			for (Trigger trigger : this._triggers)
			{
				boolean execute = false;
	
				if (trigger instanceof DateTrigger)
				{
					if (trigger.matches(context, now))
						execute = true;
				}
				
				if (execute)
					trigger.execute(context);
			}
		}
	}

	public void updateTriggers(List<Trigger> triggerList)
	{
		ArrayList<Trigger> toAdd = new ArrayList<Trigger>();
		
		synchronized(this._triggers)
		{
			for (Trigger newTrigger : triggerList)
			{
				boolean found = false;
				
				for (Trigger trigger : this._triggers)
				{
					if (trigger.equals(newTrigger))
					{
						trigger.merge(newTrigger);
						
						found = true;
					}
				}
				
				if (!found)
					toAdd.add(newTrigger);
			}
		
			this._triggers.addAll(toAdd);
		}
	}

	public List<Trigger> triggersForId(String triggerId) 
	{
		ArrayList<Trigger> matches = new ArrayList<Trigger>();
		
		synchronized(this._triggers)
		{
			for (Trigger trigger : this._triggers)
			{
				if (trigger.identifier().equals(triggerId))
					matches.add(trigger);
			}
		}		
		return matches;
	}

	public List<Trigger> allTriggers() 
	{
		return this._triggers;
	}

	public void addTrigger(Trigger t) 
	{
		ArrayList<Trigger> ts = new ArrayList<Trigger>();
		
		ts.add(t);
		
		this.updateTriggers(ts);
	}

	public void removeAllTriggers() 
	{
		synchronized(this._triggers)
		{
			this._triggers.clear();
		}
	}
}
