package edu.northwestern.cbits.purple_robot_manager.models.trees;

import java.util.Map;

public abstract class TreeNode 
{
	public abstract Map<String, Object> fetchPrediction(Map<String, Object> features) throws TreeNodeException;
	
	public static class TreeNodeException extends Exception
	{
		public TreeNodeException(String message) 
		{
			super(message);
		}

		private static final long serialVersionUID = 3671610661977748070L;
	}
	
	public abstract String toString(int indent) throws TreeNodeException;

	public String toString()
	{
		try 
		{
			return this.toString(0);
		}
		catch (TreeNodeException e) 
		{
			throw new RuntimeException(e);
		}
	}
}
