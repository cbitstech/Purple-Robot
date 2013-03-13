package edu.northwestern.cbits.purple_robot_manager.config;

import java.util.List;
import java.util.Map;

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
		rest = this.probesList(ProbeManager.probeConfigurations(this._context)); 
		
		Pair root = new Pair(Symbol.BEGIN, rest);

		return root.toString();
	}

	private Pair probesList(List<Map<String, Object>> configs) 
	{
		Pair rest = Pair.EMPTY;
		
		for (Map<String, Object> config : configs)
		{
			rest = new Pair(new Pair(Symbol.intern(".updateProbe"), new Pair(Symbol.intern("PurpleRobot"), this.pairsList(config))), rest);
		}
		
		return new Pair(new Pair(Symbol.BEGIN, rest), Pair.EMPTY);
	}

	private Pair pairsList(Map<String, Object> config) 
	{
		Pair list = Pair.EMPTY;
		
		for (String key : config.keySet())
		{
			list = new Pair(new Pair(key, config.get(key)), list);
		}
		
		return new Pair(new Pair(Symbol.QUOTE, new Pair(list, Pair.EMPTY)), Pair.EMPTY);
	}
}
