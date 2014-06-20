package edu.northwestern.cbits.purple_robot_manager.models.trees;

import java.util.HashMap;
import java.util.Map;

public class LeafNode extends TreeNode 
{
	public static final String PREDICTION = "prediction";
	public static final String ACCURACY = "accuracy";
	
	private HashMap<String, Object> _prediction;
	
	public LeafNode(HashMap<String, Object> prediction) 
	{
		this._prediction = prediction;
	}

	public Map<String, Object> fetchPrediction(Map<String, Object> features) 
	{
		return this._prediction;
	}

	public String toString(int indent)
	{
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < indent; i++)
			sb.append("  ");
		
		sb.append(this._prediction.get(LeafNode.PREDICTION).toString());
		sb.append(" (");
		sb.append(_prediction.get(LeafNode.ACCURACY).toString());
		sb.append(")");
		
		return sb.toString();
	}
}
