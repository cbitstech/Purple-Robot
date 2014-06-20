package edu.northwestern.cbits.purple_robot_manager.models.trees.parsers;

import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode;
import edu.northwestern.cbits.purple_robot_manager.models.trees.TreeNode.TreeNodeException;

public abstract class TreeNodeParser 
{
	public static class ParserNotFound extends Exception
	{
		public ParserNotFound(String message) 
		{
			super(message);
		}

		private static final long serialVersionUID = 3671610661977748070L;
	}

	public static TreeNode parseString(String content) throws ParserNotFound, TreeNodeException
	{
		TreeNodeParser parser = null;
		
		if (content.startsWith("digraph J48Tree"))
			parser = new WekaJ48TreeParser();
		
		if (parser == null)
			throw new TreeNodeParser.ParserNotFound("Unable to find parser for content.");
		
		return parser.parse(content);
	}
	
	public abstract TreeNode parse(String content) throws TreeNodeException;
}
