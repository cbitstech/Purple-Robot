package edu.northwestern.cbits.purple_robot_manager.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import libsvm.svm;
import libsvm.svm_model;

import android.content.Context;
import android.net.Uri;

public class SvmModel extends TrainedModel 
{
	public static final String TYPE = "svm";
	
	private svm_model _svm = null;
	
	public SvmModel(Context context, Uri uri) 
	{
		super(context, uri);
	}

	protected void generateModel(Context context, String modelString) 
	{
		try 
		{
			this._svm = svm.svm_load_model(new BufferedReader(new StringReader(modelString)));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	protected Object evaluateModel(Context context, HashMap<String, Object> snapshot) 
	{
		if (this._svm != null)
		{
			
		}
		
		return null;
	}

	public String modelType() 
	{
		return SvmModel.TYPE;
	}
}
