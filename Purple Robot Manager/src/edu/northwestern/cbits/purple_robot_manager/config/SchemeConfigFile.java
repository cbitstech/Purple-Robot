package edu.northwestern.cbits.purple_robot_manager.config;

import edu.northwestern.cbits.purple_robot_manager.probes.ProbeManager;
import edu.northwestern.cbits.purple_robot_manager.triggers.TriggerManager;
import jsint.Pair;
import jsint.Symbol;
import android.content.Context;

public class SchemeConfigFile 
{
	private Context _context;
	
	public SchemeConfigFile(Context context)
	{
		this._context = context;
	}
	
	public String toString()
	{
		Pair rest = TriggerManager.getInstance().schemePairs(this._context);
		rest = new Pair(ProbeManager.schemePairs(this._context), rest);
		
		Pair root = new Pair(Symbol.BEGIN, rest);

		return root.toString();
	}
}
