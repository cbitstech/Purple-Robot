package edu.northwestern.cbits.purple_robot_manager;

import jscheme.JScheme;
import jsint.DynamicEnvironment;
import jsint.Evaluator;
import jsint.Symbol;

import android.content.Context;

public class SchemeEngine extends BaseScriptEngine
{
	public SchemeEngine(Context context) 
	{
		super(context);
	}
	
	public Object evaluateSource(String source)
	{
		DynamicEnvironment env = new DynamicEnvironment();
		env.setValue(Symbol.intern("PurpleRobot"), this);
		Evaluator eval = new Evaluator(env);
		
		JScheme scheme = new JScheme(eval);
		
		return scheme.eval(source);
	}

	protected String language() 
	{
		return "Scheme";
	}
}
