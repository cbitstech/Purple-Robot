package edu.northwestern.cbits.purple_robot_manager.models;

import android.content.Context;

public class Milieu 
{
	private static Milieu _instance = null;
	
	private Context _context = null;
    
    private Milieu(Context context) 
    {
    	this._context = context.getApplicationContext();
    }

    public static Milieu getInstance(Context context) 
    {
        if (Milieu._instance == null)
        	Milieu._instance = new Milieu(context);

        return Milieu._instance;
    }

}
